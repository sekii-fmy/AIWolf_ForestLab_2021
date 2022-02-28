package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 
 * 人狼役エージェントクラス
 *
 */
public class MyWerewolf extends MyBasePlayer {
	/** 既定人数 */
	int numWolves;
	/** 騙る役職 */
	Role fakeRole;
	
	
	/** カミングアウトする日 */
	int comingoutDay;
	/** カミングアウトするターン */
	int comingoutTurn;
	/** カミングアウト済みか */
	boolean isCameout;
	
	
	List<Judge> fakeDivinationList = new ArrayList<>();
	Deque<Judge> fakeDivinationQueue = new LinkedList<>();
	List<Agent> divinedAgents = new ArrayList<>();
	
	
	/** 偽判定マップ */
	Map<Agent, Species> fakeJudgeMap = new HashMap<>();
	/** 未公表偽判定の待ち行列 */
	Deque<Judge> fakeJudgeQueue = new LinkedList<>();
	/** 裏切り者リスト */
	List<Agent> possessedList = new ArrayList<>();	
	/** 人狼リスト */
	List<Agent> werewolves;
	/** 人間リスト */
	List<Agent> humans;
	/** 村人リスト */
	List<Agent> villagers = new ArrayList<>();
	/** talk()のターン */
	int talkTurn;
	
	
	
	// 信頼度の更新
	void updateReliability() {
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
					case CO:
						//自分と同じ役職をCOしたプレイヤ（村人以外）の信頼度を下げる
						break;
					case Divined:
						//自分のことを人間判定した自称占い師は狂人
						if((parameter.isWolf == Species.HUMAN) &&
								parameter.targetAgent == me.getAgentIdx()) {
							mentalAgentList.get(me.getAgentIdx() - 1).reliability[parameter.actorAgent - 1] -= 1.0;
						}
						break;
					case Estimate:
						break;
					default:
						break;
				}
			}
		}
		
		/*for(Agent executed : executedAgents) {
			
		}*/
		
		for(Agent killed : killedAgents) {
			//襲撃されたエージェントに黒出しした占い師の信頼度を下げる
			for(Judge judge : divinationList) {
				if((judge.getTarget() == killed) && (judge.getResult() == Species.WEREWOLF)) {
					for(MentalAgent mentalAgent : mentalAgentList) {
						if(mentalAgent.getAgentIdx() != judge.getAgent().getAgentIdx()) {
							mentalAgent.reliability[judge.getAgent().getAgentIdx() - 1] -= 1.0;
						}
					}
				}
			}
		}
	}
	
	
	//対応表の更新
	//人狼は人間/人狼判定ができるためCO・占い結果・考察発言を聞き入れない
	void updateOppositeTable() {
		super.updateOppositeTable();
		
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
					case CO:
						switch(parameter.coRole) {
							case SEER:
								//占い師COしたエージェントは占い師かもしれない
								mentalAgentList.get(me.getAgentIdx() - 1).table.table[parameter.actorAgent][1] += 0.5;
								break;
							default:
								break;
						}
						break;
					case Divined:
						//自分のことを人間判定した自称占い師は狂人
						if((parameter.isWolf == Species.HUMAN) &&
								parameter.targetAgent == me.getAgentIdx()) {
							mentalAgentList.get(me.getAgentIdx() - 1).table.table[parameter.actorAgent - 1][2] += 1.0;
							mentalAgentList.get(me.getAgentIdx() - 1).table.table[parameter.actorAgent - 1][0] = 0;
							mentalAgentList.get(me.getAgentIdx() - 1).table.table[parameter.actorAgent - 1][1] = 0;
							mentalAgentList.get(me.getAgentIdx() - 1).table.table[parameter.actorAgent - 1][3] = 0;
						}
						break;
					case Estimate:
						break;
					default:
						break;
				}
			}
		}
		
		for(Agent executed : executedAgents) {
			for(MentalAgent mentalAgent : mentalAgentList) {
				mentalAgent.table.table[executed.getAgentIdx() - 1][3] = 0;
				//処刑されたエージェントは非人狼(1人狼ルールのとき)
			}
		}
		
		for(Agent killed : killedAgents) {
			for(MentalAgent mentalAgent : mentalAgentList) {
				mentalAgent.table.table[killed.getAgentIdx() - 1][3] = 0;
				//襲撃されたエージェントは非人狼
			}
		}
		
		for(MentalAgent mentalAgent : mentalAgentList) {
			if(!werewolves.contains(mentalAgent.agent)) {
				mentalAgentList.get(me.getAgentIdx() - 1).table.table[mentalAgent.getAgentIdx() - 1][3] = 0;
				//人狼仲間でないエージェントの人狼値を0
			}
		}
	}
	
	
	//発言を生成する
	// COしていなかったらCO
	//（占い結果・霊能結果の公表）
	// 考察発言をする
	// 投票してほしい相手への投票希望
	// (質問に答える)
	// 雑談
	public String talk() {
		Utterance utterance = new Utterance();
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.actorAgent = me.getAgentIdx();
		if(day != 0) {
			if(!isGreeting) {
				isGreeting = true;
				parameter.proto = ProtocolType.Greeting;
			}else if(!isCO) {
				isCO = true;
				parameter = makeUtteranceCO();
			}else if(!isEstimate) {
				isEstimate = true;
				parameter = makeUtteranceEstimate();
			}else if(!isDesire){
				isDesire = true;
				parameter = makeUtteranceDesireVote();
			}else if(unAnswerQuestionExist()){
				turn++;
				Utterance unAnswer = unAnswerQuestion();
				if(unAnswer != null) {
					utterance.utteranceText = generateAnswer(unAnswer);
					questionMap.put(unAnswer, true);
				}else {
					parameter.proto = ProtocolType.Chatting;
				}
			}else  if(turn < 3){
				turn++;
				parameter.proto = ProtocolType.Chatting;
			}else {
				utterance.isSkip = true;
				utterance.utteranceText = Talk.SKIP;
				parameter.proto = ProtocolType.SKIP;
			}
		}if(!isGreeting) {
			isGreeting = true;
			parameter.proto = ProtocolType.Greeting;
		}
		
		
		utterance.utterparameterList.add(parameter);
		if(parameter.proto != null) {
			utterance.utteranceText = new TalkTextGenerator().GenerateText(utterance);
		}
		
		outputTalkLog(utterance.utteranceText);
		//System.out.println(utterance.utteranceText);
		return utterance.utteranceText;
		
		//return "腹が減ったなのさ";
	}
	
	
	/** 質問に対する返答を作成 */
	public String generateAnswer(Utterance utterance) {
		String text = null;
		
		for(UtteranceParameter parameter : utterance.utterparameterList) {
			if(parameter.proto == ProtocolType.Question) {
				switch(parameter.ques) {
					case AskOpinion:
						double max = 0;
						List<Agent> candidate = new ArrayList<>();
						for(int i = 0; i < 4; i++) {
							if(max <= mentalAgentList.get(me.getAgentIdx()).table.table[parameter.targetAgent][i]) {
								
								max = i;
							}
						}
						if(max == 0) {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] Agent["+ String.format("%02d",parameter.targetAgent)+ "]は村人だと思うよ。";
						}else if(max == 1) {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] Agent["+ String.format("%02d",parameter.targetAgent)+ "]は占い師だと思うよ。";
						}else if(max == 2) {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] Agent["+ String.format("%02d",parameter.targetAgent)+ "]は狂人だと思うよ。";
						}else if(max == 3) {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] Agent["+ String.format("%02d",parameter.targetAgent)+ "]は人狼だと思うよ。";
						}else {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] Agent["+ String.format("%02d",parameter.targetAgent)+ "]かあ、ちょっとわからないなあ。";
						}
						break;
					case AskVoteTarget:
						text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] 今日はAgent["+ String.format("%02d",maxRoleAgent(Role.WEREWOLF).getAgentIdx())+ "]に投票しようかな。";
						break;
					case AskMyRole:
						if(fakeRole == Role.SEER) {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] COならさきほどしたかもしれないが。私は占い師だよ。";
						}else {
							text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] COならさきほどしたかもしれないが。私は村人だよ。";
						}
						break;
					case AskWhoSEER:
						text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] 占い師はAgent["+ String.format("%02d",maxRoleAgent(Role.SEER).getAgentIdx())+ "]が本物だと思っているよ。";
						break;
					case AskWhoWolf:
						text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] 難しいところだけど、Agent["+ String.format("%02d",maxRoleAgent(Role.WEREWOLF).getAgentIdx())+ "]が人狼な気がするなあ。";
						break;
					case Other:
						text = "それはちょっと教えられないなあ。";
						break;
					default:
						break;
				}
			}
		}
		
		return text;
	}
	
	//COの生成
	public UtteranceParameter makeUtteranceCO() {
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.proto = ProtocolType.CO;
		
		// 一定確率で占い師CO
		if( Math.random() < 0.3 ) {
			fakeRole = Role.SEER;
		}else {
			fakeRole = Role.VILLAGER;
		}
		
		parameter.coRole = fakeRole;
		parameter.actorAgent = me.getAgentIdx();
		
		return parameter;
	}
	
	//考察発言の作成
	public UtteranceParameter makeUtteranceEstimate() {
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.proto = ProtocolType.Estimate;
		parameter.actorAgent = me.getAgentIdx();
		
		double maxWolf = 0;
		int maxWolfIndex = 0;
		double maxSeer = 0;
		int maxSeerIndex = 0;
		
		if(fakeRole == Role.SEER){
			//一番人狼値が高いプレイヤを人狼だと言う
			for(int i = 1; i < num_villager + 1; i++) {
				if(maxWolf < mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
					maxWolf = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3];
					maxWolfIndex = i;
				}
			}
			parameter.estimateRole = Role.WEREWOLF;
			parameter.targetAgent = maxWolfIndex;
			
		}else {
			// 一番人狼値が高いプレイヤを人狼だと言う
			// or 一番占い師値が高いプレイヤを占い師だと言う
			for(int i = 1; i < num_villager + 1; i++) {
				if(maxWolf < mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3] && 
						isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
					maxWolf = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3];
					maxWolfIndex = i;
				}
				
				if(maxSeer < mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][1] && 
						isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
					maxSeer = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][1];
					maxSeerIndex = i;
				}
			}
			
			if(maxWolf < maxSeer) {
				parameter.estimateRole = Role.SEER;
				parameter.targetAgent = maxSeerIndex;
			}else {
				parameter.estimateRole = Role.WEREWOLF;
				parameter.targetAgent = maxWolfIndex;
			}
		}
		
		if(parameter.targetAgent == 0) {
			parameter.targetAgent = randomSelect(aliveOthers).getAgentIdx();
		}
		
		 /*
		//ランダムで人狼だと言う
		maxWolfIndex = randomSelect(aliveOthers).getAgentIdx();
		*/
		
		return parameter;
	}
	
	//投票希望発言の作成
	public UtteranceParameter makeUtteranceDesireVote() {
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.proto = ProtocolType.DesireVote;
		parameter.actorAgent = me.getAgentIdx();
		
		double maxWolf = 0;
		int maxWolfIndex = 0;
		
		// 一番人狼値が高いプレイヤへの投票をお願いする
		for(int i = 1; i < num_villager + 1; i++) {
			if(maxWolf < mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				maxWolf = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3];
				maxWolfIndex = i;
			}
		}
		
		parameter.targetAgent = maxWolfIndex;
		
		if(parameter.targetAgent == 0) {
			parameter.targetAgent = randomSelect(aliveOthers).getAgentIdx();
		}
		
		return parameter;
	}
	
	// 占い結果発言の作成
	public UtteranceParameter makeUtteranceDivination() {
		UtteranceParameter parameter = new UtteranceParameter();
		Judge judge = fakeDivinationList.get(fakeDivinationList.size() - 1);
		if(judge.getAgent() != me)return null;
		
		parameter.proto = ProtocolType.Divined;
		parameter.actorAgent = judge.getAgent().getAgentIdx();
		parameter.targetAgent = judge.getTarget().getAgentIdx();
		parameter.isWolf = judge.getResult();
		
		return parameter;
	}
	
	
	
	
	private Judge getFakeDivination() {
		Agent target = null;
		List<Agent> candidates = new ArrayList<>();
		
		// 自分以外のまだ占っていないエージェントを占う（いなければランダム）
		for (Agent a : aliveOthers) {
			if (!divinedAgents.contains(a) && comingoutMap.get(a) != Role.SEER) {
				candidates.add(a);
			}
		}
		if (!candidates.isEmpty()) {
			target = randomSelect(candidates);
		} else {
			target = randomSelect(aliveOthers);
		}
		
		// 一定確率で人狼判定
		Species result = Species.HUMAN;
		int nFakeWolves = 0;
		for (Judge j : fakeDivinationList) {
			if (j.getResult() == Species.WEREWOLF) {
				nFakeWolves++;
			}
		}
		if (nFakeWolves < numWolves && 
				Math.random() <  0.3) {
			result = Species.WEREWOLF;
		}
		return new Judge(day, me, target, result);
	}
	
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		werewolves = new ArrayList<>(gameInfo.getRoleMap().keySet());
		humans = new ArrayList<>();
		for (Agent a :aliveOthers) {
			if (!werewolves.contains(a)) {
				humans.add(a);
			}
		}
		isCameout = false;
		fakeJudgeMap.clear();
		fakeJudgeQueue.clear();
		possessedList.clear();
	}
	
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);
		// 占い/霊媒結果が嘘の場合、裏切り者候補
		for (Judge j : divinationList) {
			Agent agent = j.getAgent();
			if(!werewolves.contains(agent) && ((humans.contains(j.getTarget()) && j.getResult() == Species.WEREWOLF) 
					|| (werewolves.contains(j.getTarget()) && j.getResult() == Species.HUMAN))) {
				if(!possessedList.contains(agent)) {
					possessedList.add(agent);
					whisperQueue.offer(new Content(new EstimateContentBuilder(agent, Role.POSSESSED)));
					
				}
			}
		}
		villagers.clear();
		for (Agent agent : aliveOthers) {
			if (!werewolves.contains(agent) && !possessedList.contains(agent)) {
				villagers.add(agent);
			}
		}
	}
	
	public void dayStart() {
		super.dayStart();
		talkTurn = -1;
		if (day == 0) {
			//whisperQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
		}
		// 偽の判定
		else {
			Judge judge = getFakeDivination();
			if(judge != null) {
				fakeDivinationList.add(judge);
				fakeDivinationQueue.offer(judge);
				divinedAgents.add(judge.getTarget());
				//fakeJudgeMap.put(judge.getTarget(), judge.getResult());
			}
		}
	}
	
	
	//襲撃先を選択
	public Agent selectAttackAgent() {
		canWhisper = false;
		
		List<Agent> candidate = new ArrayList<>();
		
		for(int i = 0; i < num_villager; i++) {
			if(comingoutMap.containsKey(Agent.getAgent(i)) &&
					comingoutMap.get(Agent.getAgent(i)) == Role.SEER && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				
				//自分以外の、占い師にCOしている生存者がいたら襲撃
				candidate.add(Agent.getAgent(i));
			}
		}
		if(candidate.isEmpty()) {
			double min = 1;
			for(int j = 0; j < num_villager; j++) {
				if(min >= (mentalAgentList.get(me.getAgentIdx() - 1).table.table[j][2])
						&& isAlive(Agent.getAgent(j)) && Agent.getAgent(j) != me) {
					
					//いなかったら一番狂人ではなさそうなプレイヤに襲撃
					min = mentalAgentList.get(me.getAgentIdx() - 1).table.table[j][2];
				}
			}
			for(int j = 0; j < num_villager; j++) {
				if(min == mentalAgentList.get(me.getAgentIdx() - 1).table.table[j][0] && 
							isAlive(Agent.getAgent(j)) && Agent.getAgent(j) != me)
				candidate.add(Agent.getAgent(j));
			}
		}
		
		return randomSelect(candidate);
	}
	
	public void checkMyRoleInTable() {
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][0] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][1] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][2] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][3] = 1.0;
	}

	public void finish() {
		//super.finish();
		System.out.println("WOLF Agent");
	}
}