package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;


public class MentalSpace {
	
	int num_villager = 5;
	List<MentalAgent> mentalAgentList = new ArrayList<MentalAgent>();
	Agent me;
	UtteranceAnalyzer analyzer = new UtteranceAnalyzer();
	
	// 発話情報リスト
	List<Utterance> utteranceList = new ArrayList<>();
	
	/** 日付 */
	int day;
	/** talk()できる時間帯か */
	boolean canTalk;
	/** whiper()できる時間帯か */
	boolean canWhisper;
	/** 最新のゲーム情報 */
	GameInfo currentGameInfo;
	/** 自分以外の生存エージェント */
	List<Agent> aliveOthers;
	/** 追放されたエージェント */
	List<Agent> executedAgents = new ArrayList<>();
	/** 殺されたエージェント */
	List<Agent> killedAgents = new ArrayList<>();
	/** 発言された占い結果報告のリスト */
	List<Judge> divinationList = new ArrayList<>();
	/** 発言された霊媒結果報告のリスト */
	List<Judge> identList = new ArrayList<>();
	/** 発現用待ち行列 */
	Deque<Content> talkQueue = new LinkedList<>();
	/** ささやき用待ち行列 */
	Deque<Content> whisperQueue = new LinkedList<>();
	
	/** 投票先候補 */
	Agent voteCandidate;
	/** 宣言済み投票先候補 */
	Agent declaredVoteCandidate;
	/** 襲撃投票先候補 */
	Agent attackVoteCandidate;
	/** 宣言済み襲撃投票先候補 */
	Agent declaredAttackVoteCandidate;
	/** カミングアウト状況 */
	Map<Agent, Role> comingoutMap = new HashMap<>();
	/** GameInfo.talkList読み込みのヘッド */
	int talkListHead;
	/** 人間リスト */
	List<Agent> humans = new ArrayList<>();
	/** 人狼リスト */
	List<Agent> werewolves = new ArrayList<>();
	
	
	boolean isCO = false;
	boolean isDivination = false;
	boolean isIdent = false;
	boolean isEstimate = false;
	boolean isDesire = false;
	
	
	
	public MentalSpace(Agent me, List<Agent> agents) {
		this.me = me;
		generateMentalAgent(agents);
	}
	
	void generateMentalAgent(List<Agent> agents) {
		for(Agent agent : agents) {
			mentalAgentList.add(new MentalAgent(agent));
		}
	}
	
	
	
	
	// ゲームデータを受け取り、内部状態の更新
	public void receiveGameInfo(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// １日最初の呼び出しはdayStart()の前なので何もしない
		if(currentGameInfo.getDay() == day + 1) {
			day = currentGameInfo.getDay();			
			return;
		}
		//２回目の呼び出し以降
		//(夜限定)
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		
		for(int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			
			Utterance utterance = analyzer.returnUtterance(talk, me.getAgentIdx());
			
			//サンプルエージェントの部分
			
			Agent talker = talk.getAgent();
			if(talker == me) {
				continue;
			}
			//GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
				case CO:
					comingoutMap.put(talker, parameter.coRole);
					break;
				case Divined:
					divinationList.add(new Judge(day,talker, Agent.getAgent(parameter.targetAgent), parameter.isWolf));
					break;
				case Identified:
					identList.add(new Judge(day, talker, Agent.getAgent(parameter.targetAgent), parameter.isWolf));
					break;
				default:
					break;
				}
			}
			
		}
		talkListHead = currentGameInfo.getTalkList().size();
	}
	
	// ゲームデータを基に推論を行う
	public void inference() {
		//信頼度の更新
		updateReliability();
		
		//対応表の更新
		updateOppositeTable();
		
	}
	
	
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
							mentalAgent.reliability[judge.getAgent().getAgentIdx()] -= 1.0;
						}
					}
				}
			}
		}
		
		
	}
	
	
	//対応表の更新
	void updateOppositeTable() {
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				switch(parameter.proto) {
					case CO:
						
						for(MentalAgent mentalAgent : mentalAgentList) {
							switch(parameter.coRole) {
								case VILLAGER:
									if(parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][0] = 1.0;
											//COした人の対応表で村人値を１
										}else {
											mentalAgent.table.table[parameter.actorAgent][0] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で村人値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][0] = 0.0;
											//COした人の対応表で村人値を0
										}else {
											mentalAgent.table.table[parameter.actorAgent][0] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で村人値を下げる
										}
									}
									break;
								case SEER:
									if(parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][1] = 1.0;
											//COした人の対応表で占い師値を１
										}else {
											mentalAgent.table.table[parameter.actorAgent][1] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で占い師値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][1] = 0.0;
											//COした人の対応表で占い師値を0
										}else {
											mentalAgent.table.table[parameter.actorAgent][1] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で占い師値を下げる
										}
									}
									break;
								case POSSESSED:
									if(parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][2] = 1.0;
											//COした人の対応表で狂人値を１
										}else {
											mentalAgent.table.table[parameter.actorAgent][2] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で狂人値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][2] = 0.0;
											//COした人の対応表で狂人値を0
										}else {
											mentalAgent.table.table[parameter.actorAgent][2] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で狂人値を下げる
										}
									}
									break;
								case WEREWOLF:
									if(parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][3] = 1.0;
											//COした人の対応表で人狼値を１
										}else {
											mentalAgent.table.table[parameter.actorAgent][3] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で人狼値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.actorAgent][3] = 0.0;
											//COした人の対応表で人狼値を0
										}else {
											mentalAgent.table.table[parameter.actorAgent][3] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//COした人以外の対応表で人狼値を下げる
										}
									}
									break;
								default:
									break;
							}
						}
						
						break;
					case Divined:
						
						for(MentalAgent mentalAgent : mentalAgentList) {
							switch(parameter.isWolf) {
								case WEREWOLF:
									if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
										mentalAgent.table.table[parameter.targetAgent][3] = 1.0;
										//占い師の対応表で占い対象の人狼値を１
									}else {
										mentalAgent.table.table[parameter.targetAgent][3] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
										//占い対象の人狼値を上げる
									}
									break;
								case HUMAN:
									if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
										mentalAgent.table.table[parameter.targetAgent][03] = 0;
										//占い師の対応表で占い対象の人狼値を0
									}else {
										mentalAgent.table.table[parameter.targetAgent][03] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
										//占い対象の人狼値を下げる
									}
									break;
								default:
									break;
							}
						}
						
						break;
					case Estimate:
						
						for(MentalAgent mentalAgent : mentalAgentList) {
							switch(parameter.estimateRole) {
								case VILLAGER:
									if(!parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][0] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の村人値を上げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][0] += 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の村人値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][0] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の村人値を下げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][0] -= 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の村人値を下げる
										}
									}
									break;
								case SEER:
									if(!parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][1] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の占い師値を上げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][1] += 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の占い師値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][1] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の占い師値を下げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][1] -= 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の占い師値を下げる
										}
									}
									break;
								case POSSESSED:
									if(!parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][2] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の狂人値を上げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][2] += 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の狂人値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][2] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の狂人値を下げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][2] -= 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の狂人値を下げる
										}
									}
									break;
								case WEREWOLF:
									if(!parameter.deny) {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][3] += 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の人狼値を上げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][3] += 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の人狼値を上げる
										}
									}else {
										if(parameter.actorAgent == mentalAgent.getAgentIdx()) {
											mentalAgent.table.table[parameter.targetAgent][3] -= 0.5 * mentalAgent.reliability[parameter.actorAgent];
											//発言者の対応表で対象の人狼値を下げる
										}else {
											mentalAgent.table.table[parameter.targetAgent][3] -= 0.3 * mentalAgent.reliability[parameter.actorAgent];
											//対象の人狼値を下げる
										}
									}
									break;
								default:
									break;
							}
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
			for(MentalAgent mentalAgent : mentalAgentList) {
				mentalAgent.table.table[killed.getAgentIdx()][3] = 0;
				//襲撃されたエージェントは非人狼
			}
		}
	}
	
	
	/** 投票先を選びvoteCandidateにセットする */
	protected void chooseVoteCandidate() {
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
		
		if(!isCO) {
			isCO = true;
		}else if(!isEstimate) {
			isEstimate = true;
		}else if(!isDesire){
			isDesire = true;
		}else {
			utterance.utteranceText = Talk.SKIP;
		}
		
		return utterance.utteranceText;
	}
	
	/** 襲撃先候補を選びattackVoteCandidateにセットする */
	protected void chooseAttackVoteCandidate() {
	}
	
	public String whisper() {
		chooseAttackVoteCandidate();
		if(attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}
	
	public Agent selectVoteAgent() {
		canTalk = false;
		double max = 0;
		List<Agent> candidate = new ArrayList<>();
		for(int i = 0; i < num_villager; i++) {
			if(max <= mentalAgentList.get(me.getAgentIdx()).table.table[i][3] && 
					Agent.getAgent(i) != me) {
				
				//自分以外で一番人狼値が高いプレイヤに投票
				max = mentalAgentList.get(me.getAgentIdx()).table.table[i][3];
			}
		}
		for(int i = 0; i < num_villager; i++) {
			if(max == mentalAgentList.get(me.getAgentIdx()).table.table[i][3] && 
					Agent.getAgent(i) != me)
			candidate.add(Agent.getAgent(i));
		}
		
		return randomSelect(candidate);
	}
	
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
			double max = 0;
			for(int j = 0; j < num_villager; j++) {
				if(max <= (mentalAgentList.get(me.getAgentIdx()).table.table[j][0] 
							+ mentalAgentList.get(me.getAgentIdx()).table.table[j][1])
						&& Agent.getAgent(j) != me) {
					
					//いなかったら自分以外で一番村視しているプレイヤに襲撃
					max = mentalAgentList.get(me.getAgentIdx()).table.table[j][0] 
							+ mentalAgentList.get(me.getAgentIdx()).table.table[j][1];
				}
			}
			for(int j = 0; j < num_villager; j++) {
				if(max == mentalAgentList.get(me.getAgentIdx()).table.table[j][0] 
							+ mentalAgentList.get(me.getAgentIdx()).table.table[j][1] &&
						Agent.getAgent(j) != me)
				candidate.add(Agent.getAgent(j));
			}
		}
		
		return randomSelect(candidate);
	}
	
	public Agent divine() {
		return null;
	}
	
	public Agent guard() {
		return null;
	}
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		day = -1;
		me = gameInfo.getAgent();
		aliveOthers = new ArrayList<>(gameInfo.getAliveAgentList());
		aliveOthers.remove(me);
		executedAgents.clear();
		killedAgents.clear();
		divinationList.clear();
		comingoutMap.clear();
		werewolves.clear();
		
		isCO = false;
		isDivination = false;
		isIdent = false;
		isEstimate = false;
		isDesire = false;
	}
	
	
	public void dayStart() {
		canTalk = true;
		canWhisper = false;
		
		isDivination = false;
		isIdent = false;
		isEstimate = false;
		isDesire = false;
		
		if(currentGameInfo.getRole() == Role.WEREWOLF) {
			canWhisper = true;
		}
		talkQueue.clear();
		whisperQueue.clear();
		declaredAttackVoteCandidate = null;
		attackVoteCandidate = null;
		talkListHead = 0;
		//前日に追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getExecutedAgent());
		//昨夜死亡した（襲撃された）エージェントを登録
		if(!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
		}
	}
	
	private void addExecutedAgent(Agent executedAgent) {
		if(executedAgent != null) {
			aliveOthers.remove(executedAgent);
			if(!executedAgents.contains(executedAgent)) {
				executedAgents.add(executedAgent);
			}
		}
	}
	
	private void addKilledAgent(Agent killedAgent) {
		if(killedAgent != null) {
			aliveOthers.remove(killedAgent);
			if(!killedAgents.contains(killedAgent)) {
				killedAgents.add(killedAgent);
			}
		}
	}
	
	
	/** エージェントが生きているかどうかを返す */
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}
	
	/** エージェントが殺されたかどうかを返す */
	protected boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}
	
	/** エージェントがカミングアウトしたかどうかを返す */
	protected boolean isCO(Agent agent) {
		return comingoutMap.containsKey(agent);
	}
	
	/** 役職がカミングアウトされたかどうかを返す */
	protected boolean isCO(Role role) {
		return comingoutMap.containsKey(role);
	}
	
	/** エージェントが人間かどうかを返す */
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}
	
	/** エージェントが人狼かどうかを返す */
	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}
	
	/** リストからランダムに選んで返す */
	protected <T> T randomSelect(List<T> list) {
		if(list.isEmpty()) {
			return null;
		} else {
			return list.get((int)(Math.random() * list.size()));
		}
	}

}
