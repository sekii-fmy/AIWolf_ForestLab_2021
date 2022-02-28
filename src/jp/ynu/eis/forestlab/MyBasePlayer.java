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
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class MyBasePlayer implements Player {
	
	int num_villager = 5;
	List<MentalAgent> mentalAgentList = new ArrayList<MentalAgent>();
	UtteranceAnalyzer analyzer = new UtteranceAnalyzer();
	
	// 発話情報リスト
	List<Utterance> utteranceList = new ArrayList<>();
	
	//南海しゃべったか
	int turn; 
	
	//質問を投げかけられたか
	boolean catchQuestion;
	
	HashMap<Utterance, Boolean> questionMap = new HashMap<>();
	
	/** このエージェント */
	Agent me;
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
	
	/** メンタルスペース */
	MentalSpace mentalSpace;
	
	boolean isGreeting = false;
	boolean isCO = false;
	boolean isDivination = false;
	boolean isIdent = false;
	boolean isEstimate = false;
	boolean isDesire = false;
	boolean isSkip = false;
	
	
	String logtext = "";
	
	
	public String getName() {
		return "ForestLab";
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
		
		isGreeting = false;
		isCO = false;
		isDivination = false;
		isIdent = false;
		isEstimate = false;
		isDesire = false;
	}
	
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// １日最初の呼び出しはdayStart()の前なので何もしない
		if(currentGameInfo.getDay() == day + 1) {
			day = currentGameInfo.getDay();
			
			//メンタルエージェントの作成
			for(Agent agent : currentGameInfo.getAgentList()) {
				mentalAgentList.add(new MentalAgent(agent));
			}
			
			return;
		}
		//２回目の呼び出し以降
		//(夜限定)
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		//GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
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
				if(parameter.proto != null) {
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
			
		}
		talkListHead = currentGameInfo.getTalkList().size();
		
		inference();
		
	}
	
	
	// ゲームデータを基に推論を行う
	public void inference() {
		//信頼度の更新
		updateReliability();
		
		//対応表の更新
		updateOppositeTable();
		
		//質問の確認
		updateQuestion();
		
		//対応表の数値の確認
		checkTable();
		
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
											mentalAgent.table.table[parameter.actorAgent][1] = 0;
											mentalAgent.table.table[parameter.actorAgent][2] = 0;
											mentalAgent.table.table[parameter.actorAgent][3] = 0;
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
											mentalAgent.table.table[parameter.actorAgent][0] = 0;
											mentalAgent.table.table[parameter.actorAgent][2] = 0;
											mentalAgent.table.table[parameter.actorAgent][3] = 0;
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
											mentalAgent.table.table[parameter.actorAgent][0] = 0;
											mentalAgent.table.table[parameter.actorAgent][1] = 0;
											mentalAgent.table.table[parameter.actorAgent][3] = 0;
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
											mentalAgent.table.table[parameter.actorAgent][0] = 0;
											mentalAgent.table.table[parameter.actorAgent][1] = 0;
											mentalAgent.table.table[parameter.actorAgent][2] = 0;
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
										mentalAgent.table.table[parameter.targetAgent][0] = 0;
										mentalAgent.table.table[parameter.targetAgent][1] = 0;
										mentalAgent.table.table[parameter.targetAgent][2] = 0;
										
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
	}
	
	public void updateQuestion() {
		for(Utterance utterance : utteranceList) {
			for(UtteranceParameter parameter : utterance.utterparameterList) {
				if(parameter.proto == ProtocolType.Question && (utterance.day == day)) {
					catchQuestion = true;
					questionMap.put(utterance, false);
				}
			}
		}
	}
	
	
	public void dayStart() {
		canTalk = true;
		canWhisper = false;
		turn = 0;
		
		isDivination = false;
		isIdent = false;
		isEstimate = false;
		isDesire = false;
		isSkip = false;
		
		catchQuestion = false;

		
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
	
	private void generateMentalSpace(Agent me, List<Agent> agentList) {
		mentalSpace = new MentalSpace(me, agentList);
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
		UtteranceParameter parameter = new UtteranceParameter();
		parameter.actorAgent = me.getAgentIdx();
		if(day != 0) {
			if(!isGreeting) {
				isGreeting = true;
				parameter.proto = ProtocolType.Greeting;
			}else if(!isCO) {
				isCO = true;
				parameter.proto = ProtocolType.CO;
				parameter.coRole = Role.VILLAGER;
			}else if(!isEstimate) {
				isEstimate = true;
				parameter.proto = ProtocolType.Estimate;
				parameter.estimateRole = Role.WEREWOLF;
				parameter.targetAgent = randomSelect(aliveOthers).getAgentIdx();
			}else if(!isDesire){
				isDesire = true;
				parameter.proto = ProtocolType.Chatting;
			}else{
				isSkip = true;
				parameter.proto = ProtocolType.SKIP;
			}
		}else if(!isGreeting) {
			isGreeting = true;
			parameter.proto = ProtocolType.Greeting;
		}
		
		
		utterance.utterparameterList.add(parameter);
		utterance.utteranceText = new TalkTextGenerator().GenerateText(utterance);
		
		System.out.println("MyBasePlayer");
		outputTalkLog(utterance.utteranceText);
		
		//return utterance.utteranceText;
		
		return "なのさ。";
	}
	
	/**今日来た質問で返答を返していないものがある？*/
	public boolean unAnswerQuestionExist() {
		for(Utterance utterance : utteranceList) {
			if(questionMap.containsKey(utterance)) {
				if(!questionMap.get(utterance)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**今日来た質問で返答を返していないものがある？
	 * あったらその発言を、無かったらnullをreturn  */
	public Utterance unAnswerQuestion() {
		for(Utterance utterance : utteranceList) {
			if(questionMap.containsKey(utterance)) {
				if(!questionMap.get(utterance)) {
					return utterance;
				}
			}
		}
		
		return null;
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
						text = ">>Agent["+ String.format("%02d",utterance.agentNum) +"] COならさきほどしたかもしれないが。私は村人だよ。";
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
	
	public Agent maxRoleAgent(Role role) {
		int roleNum = 3;
		switch(role) {
			case VILLAGER:
				roleNum = 0;
				break;
			case SEER:
				roleNum = 1;
				break;
			case POSSESSED:
				roleNum = 2;
				break;
			case WEREWOLF:
				roleNum = 3;
				break;
			default:
				break;
		}
		
		double max = 0;
		List<Agent> candidate = new ArrayList<>();
		for(int i = 0; i < num_villager; i++) {
			if(max <= mentalAgentList.get(me.getAgentIdx()).table.table[i][roleNum] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				
				max = mentalAgentList.get(me.getAgentIdx()).table.table[i][roleNum];
			}
		}
		for(int i = 0; i < num_villager; i++) {
			if(max == mentalAgentList.get(me.getAgentIdx()).table.table[i][roleNum] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me)
			candidate.add(Agent.getAgent(i));
		}
		
		return randomSelect(candidate);
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
	
	public Agent vote() {
		canTalk = false;
		return selectVoteAgent();
	}
	
	public Agent attack() {
		canWhisper = false;
		return selectAttackAgent();
	}
	
	public Agent divine() {
		return null;
	}
	
	public Agent guard() {
		return null;
	}
	
	public void finish() {
		//System.out.println("Base Agent");
		
		/*
		LocalDateTime localDateTime = LocalDateTime.now();
		File file = new File(localDateTime + ".txt");
		try {
			FileWriter filewriter = new FileWriter(file, true);
			filewriter.write(logtext);
			
			filewriter.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		*/
	}
	
	
	
	public Agent selectVoteAgent() {
		double max = 0;
		List<Agent> candidate = new ArrayList<>();
		for(int i = 0; i < num_villager; i++) {
			if(max <= mentalAgentList.get(me.getAgentIdx()).table.table[i][3] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me) {
				
				//自分以外で一番人狼値が高いプレイヤに投票
				max = mentalAgentList.get(me.getAgentIdx()).table.table[i][3];
			}
		}
		for(int i = 0; i < num_villager; i++) {
			if(max == mentalAgentList.get(me.getAgentIdx()).table.table[i][3] && 
					isAlive(Agent.getAgent(i)) && Agent.getAgent(i) != me)
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
						&& isAlive(Agent.getAgent(j)) && Agent.getAgent(j) != me) {
					
					//いなかったら自分以外で一番村視しているプレイヤに襲撃
					max = mentalAgentList.get(me.getAgentIdx()).table.table[j][0] 
							+ mentalAgentList.get(me.getAgentIdx()).table.table[j][1];
				}
			}
			for(int j = 0; j < num_villager; j++) {
				if(max == mentalAgentList.get(me.getAgentIdx()).table.table[j][0] 
							+ mentalAgentList.get(me.getAgentIdx()).table.table[j][1] &&
							isAlive(Agent.getAgent(j)) && Agent.getAgent(j) != me)
				candidate.add(Agent.getAgent(j));
			}
		}
		
		return randomSelect(candidate);
	}
	
	// 対応表の矛盾調査
	public void checkTable() {
		
		for(int i = 0; i < num_villager; i++) {
			// 値の範囲の調整
			mentalAgentList.get(i).table.chackRangeOfTable();
			// 自分の役職は迷わない
			checkMyRoleInTable();
		}
	}
	
	public void checkMyRoleInTable() {
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][0] = 1.0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][1] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][2] = 0;
		mentalAgentList.get(me.getAgentIdx() - 1).table.table[me.getAgentIdx() - 1][3] = 0;
	}
	
	
	// Talk履歴の出力
	public void outputTalkLog(String text) {
		logtext += text + "\n";
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