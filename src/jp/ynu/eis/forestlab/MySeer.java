package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class MySeer extends MyVillager {
	
	int comingoutDay;
	boolean isCameout;
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	
	
	
	//占い結果が狼のプレイヤの信頼度を下げる
	void divinationReliability(Judge judge) {
		
		if(judge != null) {
			if(judge.getAgent() == me) {
				if(judge.getResult() == Species.WEREWOLF) {
					//占い結果が人狼のとき
					mentalAgentList.get(me.getAgentIdx() - 1).reliability[judge.getTarget().getAgentIdx() - 1] -= 1.0;
				}
			}
		}
	}
	
	//信頼度の更新
	void updateReliability() {
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
					case CO:
						//自分と同じ役職をCOしたプレイヤ（村人以外）の信頼度を下げる
						if(parameter.coRole == Role.SEER && parameter.actorAgent != me.getAgentIdx()) {
							mentalAgentList.get(me.getAgentIdx() - 1).reliability[parameter.actorAgent - 1] -= 1.0;
						}
						break;
					case Divined:
						//自分のことを違う役職で占った占い師の信頼度を下げる
						if((parameter.isWolf == Species.WEREWOLF) &&
								parameter.targetAgent == me.getAgentIdx()) {
							mentalAgentList.get(me.getAgentIdx() - 1).reliability[parameter.actorAgent - 1] -= 1.0;
						}
						break;
					case Estimate:
						//自分のことを黒視するプレイヤの信頼度をすこし下げる
						if((parameter.estimateRole == Role.WEREWOLF) &&
								parameter.targetAgent == me.getAgentIdx()) {
							mentalAgentList.get(me.getAgentIdx() - 1).reliability[parameter.actorAgent - 1] -= 0.3;
						}
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
				if(judge != null) {
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
	}
	
	//対応表の更新
	void updateOppositeTable() {
		super.updateOppositeTable();
	}
	
	//占い結果の対応表への適応
	void divinationInference(Judge judge) {
		
		//System.out.println("divinationInference");
		
		if(judge != null) {
			if(judge.getAgent() == me) {
				if(judge.getResult() == Species.HUMAN) {
					//占い結果が人間のとき
					mentalAgentList.get(me.getAgentIdx() - 1).table.table[judge.getTarget().getAgentIdx() - 1][3] = 0;
				}else if(judge.getResult() == Species.WEREWOLF) {
					//占い結果が人狼のとき
					mentalAgentList.get(me.getAgentIdx() - 1).table.table[judge.getTarget().getAgentIdx() - 1][3] = 1;
					mentalAgentList.get(me.getAgentIdx() - 1).table.table[judge.getTarget().getAgentIdx() - 1][0] = 0;
					mentalAgentList.get(me.getAgentIdx() - 1).table.table[judge.getTarget().getAgentIdx() - 1][1] = 0;
					mentalAgentList.get(me.getAgentIdx() - 1).table.table[judge.getTarget().getAgentIdx() - 1][2] = 0;
				}
			}
		}
	}
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = 1;
		isCameout = false;
		divinationQueue.clear();
		myDivinationMap.clear();
		whiteList.clear();
		blackList.clear();
		grayList = new ArrayList<>();
		semiWolves.clear();
		possessedList.clear();
	}
	
	public void dayStart() {
		super.dayStart();
		// 占い結果を待ち行列に入れる
		Judge divination = currentGameInfo.getDivineResult();
		if(divination != null) {
			divinationReliability(divination);
			divinationInference(divination);
			if (divination != null) {
				divinationQueue.offer(divination);
				grayList.remove(divination.getTarget());
				if (divination.getResult() == Species.HUMAN) {
					whiteList.add(divination.getTarget());
				}else {
					blackList.add(divination.getTarget());
				}
				myDivinationMap.put(divination.getTarget(), divination.getResult());
			}
		}
		
	}
	
	protected void chooseVoteCandidate() {
		// 生存人狼がいれば当然投票
		List<Agent> aliveWolves = new ArrayList<>();
		for(Agent a : blackList) {
			if (isAlive(a)) {
				aliveWolves.add(a);
			}
		}
		// 既定の投票先が生存人狼でない場合投票先を変える
		if (!aliveWolves.isEmpty()) {
			if (!aliveWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveWolves);
				if (canTalk) {
					talkQueue.offer(new Content(new RequestContentBuilder(null, new Content(new VoteContentBuilder(voteCandidate)))));
				}
			}
			return;
		}
		// 確定人狼がいない場合は推測する
		werewolves.clear();
		// 偽占い師
		for (Agent a : aliveOthers) {
			if (comingoutMap.get(a) == Role.SEER) {
				werewolves.add(a);
			}
		}
		// 偽霊媒師
		for (Judge j : identList) {
			Agent agent = j.getAgent();
			if((myDivinationMap.containsKey(j.getTarget()) && j.getResult() != myDivinationMap.get(j.getTarget()))) {
				if (isAlive(agent) && !werewolves.contains(agent)) {
					werewolves.add(agent);
				}
			}
		}
		possessedList.clear();
		semiWolves.clear();
		for (Agent a : werewolves) {
			// 人狼候補なのに人間⇒裏切り者
			if (whiteList.contains(a)) {
				if (!possessedList.contains(a)) {
					talkQueue.offer(new Content(new EstimateContentBuilder(a, Role.POSSESSED)));
					possessedList.add(a);
				}
			} else {
				semiWolves.add(a);
			}
		}
		if (!semiWolves.isEmpty()) {
			if (!semiWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(semiWolves);
				// 以前の投票先から変わる場合、新たに推測発言をする
				if (canTalk) {
					talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
				}
			}
		}
		// 人狼候補がいない場合はグレイからランダム
		else {
			if (!grayList.isEmpty()) {
				if (!grayList.contains(voteCandidate)) {
					voteCandidate = randomSelect(grayList);
				}
			}
			// グレイもいない場合ランダム
			else {
				if (!aliveOthers.contains(voteCandidate)) {
					voteCandidate = randomSelect(aliveOthers);
				}
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
			}else if(!isDivination) {
				isDivination = true;
				parameter = makeUtteranceDivination();
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
						text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] COならさきほどしたかもしれないが。私は占い師だよ。";
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
		parameter.coRole = Role.SEER;
		parameter.actorAgent = me.getAgentIdx();
		
		return parameter;
	}	
	
	// 占い結果発言の作成
	public UtteranceParameter makeUtteranceDivination() {
		UtteranceParameter parameter = new UtteranceParameter();
		Judge judge = currentGameInfo.getDivineResult();
		if(judge != null) {
			if(judge.getAgent() != me)return parameter;
			
			parameter.proto = ProtocolType.Divined;
			parameter.actorAgent = judge.getAgent().getAgentIdx();
			parameter.targetAgent = judge.getTarget().getAgentIdx();
			parameter.isWolf = judge.getResult();
		}
		
		return parameter;
	}
	
	//占い先の決定
	public Agent divine() {
		double max = 0;
		List<Agent> candidate = new ArrayList<>();
		for(int i = 1; i < num_villager + 1; i++) {
			if(!myDivinationMap.containsKey(Agent.getAgent(i)) && 
					(max <= mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3]) && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				
				//占っていない自分以外で一番人狼値が高い生存プレイヤを占う
				max = mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3];
			}
		}
		for(int i = 1; i < num_villager + 1; i++) {
			if(!myDivinationMap.containsKey(Agent.getAgent(i)) && 
					(max == mentalAgentList.get(me.getAgentIdx() - 1).table.table[i - 1][3]) && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me)
			candidate.add(Agent.getAgent(i));
		}
		
		//いなければ自分以外からランダムに占う
		if(candidate.isEmpty()) {
			for(int i = 1; i < num_villager + 1; i++) {
				if(!myDivinationMap.containsKey(Agent.getAgent(i)) &&
						isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
					candidate.add(Agent.getAgent(i));
				}
			}
		}
		
		return randomSelect(candidate);
	}
	
	public void checkMyRoleInTable() {
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][0] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][1] = 1.0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][2] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][3] = 0;
	}
	
	public void finish() {
		//super.finish();
		System.out.println("SEER Agent");
	}
}

