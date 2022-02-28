package jp.ynu.eis.forestlab;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;


public class MyVillager extends MyBasePlayer {
	
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
		if(day != 0 ) {
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
			}else if(turn < 3){
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
		return utterance.utteranceText;
		
		//return "腹が減ったなのさ";
	}
	
	//COの生成
	public UtteranceParameter makeUtteranceCO() {
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.proto = ProtocolType.CO;
		parameter.coRole = Role.VILLAGER;
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
	
	public void checkMyRoleInTable() {
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][0] = 1.0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][1] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][2] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][3] = 0;
	}
	
	public String whisper() {
		throw new UnsupportedOperationException();
	}
	
	public Agent attack() {
		throw new UnsupportedOperationException();
	}
	
	public Agent divine() {
		throw new UnsupportedOperationException();
	}
	
	public Agent guard() {
		throw new UnsupportedOperationException();
	}

	public void finish() {
		//super.finish();
		System.out.println("VILLAGER Agent");
	}
}

