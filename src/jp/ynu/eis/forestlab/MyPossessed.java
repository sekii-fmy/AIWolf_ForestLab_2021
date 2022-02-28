package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 
 * 裏切者エージェントクラス
 *
 */
public class MyPossessed extends MyVillager {
	
	// 占い師を騙る（一日目から）
	Role fakeRole = Role.SEER;
	
	
	int numWolves;
	boolean isCameout;
	List<Judge> fakeDivinationList = new ArrayList<>();
	Deque<Judge> fakeDivinationQueue = new LinkedList<>();
	List<Agent> divinedAgents = new ArrayList<>();
	
	//信頼度の更新
	void updateReliability() {
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
					case CO:
						//自分と同じ役職をCOしたプレイヤ（村人以外）の信頼度を下げる
						break;
					case Divined:
						//自分のことを違う役職で占った占い師の信頼度を下げる
						if((parameter.isWolf == Species.WEREWOLF) &&
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
	void updateOppositeTable() {
		super.updateOppositeTable();
		
	
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
				System.out.println("actor : "+parameter.actorAgent);
				System.out.println("target : "+parameter.targetAgent);
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
		}else if(!isGreeting) {
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
		
		// 一番人狼値が高いプレイヤを人狼だと言う
		/*for(int i = 1; i < num_villager + 1; i++) {
			if(maxWolf < mentalAgentList.get(me.getAgentIdx() - 1).table.table[i + 1][3] && 
				isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				maxWolf = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i + 1][3];
				maxWolfIndex = i;
			}
		}*/
		
		//ランダムで人狼だと言う
		maxWolfIndex = randomSelect(aliveOthers).getAgentIdx();
		
		parameter.estimateRole = Role.WEREWOLF;
		parameter.targetAgent = maxWolfIndex;
		
		if(parameter.targetAgent == 0) {
			parameter.targetAgent = randomSelect(aliveOthers).getAgentIdx();
		}
		
		return parameter;
	}
	
	//投票希望発言の作成
	public UtteranceParameter makeUtteranceDesireVote() {
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.proto = ProtocolType.DesireVote;
		parameter.actorAgent = me.getAgentIdx();
		
		double minWolf = 1;
		int minWolfIndex = 0;
		
		// 一番人狼値が低いプレイヤへの投票をお願いする
		for(int i = 1; i < num_villager + 1; i++) {
			if(minWolf > mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				minWolf = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3];
				minWolfIndex = i;
			}
		}
		
		parameter.targetAgent = minWolfIndex;
		
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
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		isCameout = false;
		fakeDivinationList.clear();
		fakeDivinationQueue.clear();
		divinedAgents.clear();
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
		
		
		// 人狼よりなら人間判定
		// 人間よりなら人狼判定
		Species result = Species.HUMAN;
		int nFakeWolves = 0;
		for (Judge j : fakeDivinationList) {
			if (j.getResult() == Species.WEREWOLF) {
				nFakeWolves++;
			}
		}
		if (nFakeWolves < numWolves && 
				mentalAgentList.get(me.getAgentIdx() - 1).table.table[target.getAgentIdx() - 1][3] < 0.3) {
			result = Species.WEREWOLF;
		}
		return new Judge(day, me, target, result);
	}
	
	public void dayStart() {
		super.dayStart();
		// 偽の判定
		if(day > 0) {
			Judge judge = getFakeDivination();
			if (judge != null) {
				fakeDivinationList.add(judge);
				fakeDivinationQueue.offer(judge);
				divinedAgents.add(judge.getTarget());
			}
		}
	}
	
	protected void chooseVoteCandidate() {
		werewolves.clear();
		List<Agent> candidates = new ArrayList<>();
		// 自分や殺されたエージェントを人狼と判定している占い師は人狼候補
		for (Judge j : divinationList) {
			if (j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))) {
				if (!werewolves.contains(j.getAgent())) {
					werewolves.add(j.getAgent());
				}
			}
		}
		// 対抗カミングアウトのエージェントは投票先候補
		for (Agent a : aliveOthers) {
			if (!werewolves.contains(a) && comingoutMap.get(a) == Role.SEER) {
				candidates.add(a);
			}
		}
		// 人狼と判定したエージェントは投票先候補
		List<Agent> fakeHumans = new ArrayList<>();
		for (Judge j : fakeDivinationList) {
			if (j.getResult() == Species.HUMAN) {
				if (!fakeHumans.contains(j.getTarget())) {
					fakeHumans.add(j.getTarget());
				}
			} else {
				if (!candidates.contains(j.getTarget())) {
					candidates.add(j.getTarget());
				}
			}
		}
		// 候補がいなければ人間と判定していない村人陣営から
		if (candidates.isEmpty()) {
			for (Agent a : aliveOthers) {
				if (!werewolves.contains(a) && !fakeHumans.contains(a)) {
					candidates.add(a);
				}
			}
		}
		// それでも候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			for (Agent a : aliveOthers) {
				if (!werewolves.contains(a)) {
					candidates.add(a);
				}
			}
		}
		if (!candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			// 以前の投票先から変わる場合、新たに推測発言と占い要請をする
			if (canTalk){
				talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
				talkQueue.offer(new Content(new RequestContentBuilder(null, new Content(new DivinationContentBuilder(voteCandidate)))));
			}
		}
	}
	
	public void checkMyRoleInTable() {
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][0] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][1] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][2] = 1.0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][3] = 0;
	}
	
	public void finish() {
		//super.finish();
		System.out.println("POSSESSED Agent");
	}

}
