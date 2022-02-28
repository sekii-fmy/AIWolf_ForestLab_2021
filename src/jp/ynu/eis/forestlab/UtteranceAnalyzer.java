package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

import net.moraleboost.mecab.Node;

public class UtteranceAnalyzer{
	
	Mecab mecab = new Mecab();
	
	String reg_firstPerson = "わたし|ワタシ|ぼく|ボク|おれ|オレ|あたし|アタシ|わたくし|ワタクシ|わし|ワシ|あたい|アタイ";
	String reg_helper = "も|が|こそ|は";
	String reg_roletext = "村人|占い師|狂人|人狼|占い|狂|狼|村|人間|黒|白";
	String reg_species = "人狼|人間|狼|黒|白";
	String reg_deny = "ない|無い|ありません";
	String reg_CO = "カミングアウト|かみんぐあうと|CO";
	String reg_divined = "占う";
	String reg_think = "思う";
	String reg_trust = "信じる|信用";
	String reg_result = "結果|占い結果";
	String reg_past = "特殊・タ,";
	String reg_vote = "投票|入れる|票|吊る";
	String reg_Agent = "Agent\\[\\d*\\]";
	
	String reg_QuestionMark = "？|\\?";
	String reg_Anker = ">>";
	String reg_How = "どう|どの|どんな";
	String reg_Who = "誰|だれ|ダレ|どいつ|ドイツ";
	String reg_What = "何|なん";
	
	boolean boo_first = false;
	boolean boo_helper = false;
	boolean boo_roletext = false;
	boolean boo_species = false;
	boolean boo_deny = false;
	boolean boo_CO = false;
	boolean boo_divined = false;
	boolean boo_think = false;
	boolean boo_trust = false;
	boolean boo_result = false;
	boolean boo_past = false;
	boolean boo_vote = false;
	boolean boo_Agent = false;
	
	boolean boo_QuestionMark = false;
	boolean boo_Anker = false;
	boolean boo_How = false;
	boolean boo_Who = false;
	boolean boo_What = false;
	
	public UtteranceAnalyzer(){
	}
	
	public Utterance returnUtterance(Talk talk, int myAgentIdx) {
		//String talkText = talk.getText();
		Utterance utterance = new UtteranceConverter().convert(talk);
		String[] splitedArray = utterance.talkText.split("。|．");
		List<UtteranceParameter> uparaList = new ArrayList<UtteranceParameter>();
		
		for(int i = 0; i < splitedArray.length; i++) {
			if(isQuestion(splitedArray[i],myAgentIdx)) {
				uparaList.add(matchQuestionType(splitedArray[i]));
			}else {
				uparaList.add(matchProtocol(splitedArray[i], utterance.agentNum));
			}
		}
		
		utterance.utterparameterList.addAll(uparaList);
		
		return utterance;
	}
	
	public boolean isQuestion(String splited, int myAgentIdx) {
		booleanFalse();
		
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		
		
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_Anker = boo_Anker|Pattern.compile(reg_Anker).matcher(surface).find();
            boo_QuestionMark = boo_QuestionMark|Pattern.compile(reg_QuestionMark).matcher(feature).find();
            
            node = node.next();
        }
		
		if(boo_Anker) {
			if(iscallme(splited, myAgentIdx) && boo_QuestionMark) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public boolean iscallme(String splited, int myAgentIdx) {
		
		String num = splited.replaceAll("[^\\d]", "");
		int targetAgent = -1;
		if(num.length() >= 2) {
			targetAgent = Integer.valueOf(num.substring(0,2));
		}
		
		if(myAgentIdx == targetAgent) {
			return true;
		}else {
			return false;
		}
	}
	
	public UtteranceParameter matchQuestionType(String splited) {
		UtteranceParameter parameter = new UtteranceParameter();
		
		boolean isAskOpinion = matchAskOpinion(splited);
		boolean isAskVoteTarget = matchAskVoteTarget(splited);
		boolean isAskMyRole = matchAskMyRole(splited);
		boolean isAskWhoSEER = matchAskWhoSEER(splited);
		boolean isAskWhoWolf = matchAskWhoWolf(splited);
		
		//System.out.println(isAskOpinion + "\n" + isAskVoteTarget + "\n" + isAskMyRole + "\n" + isAskWhoSEER + "\n" + isAskWhoWolf);
		
		parameter.proto = ProtocolType.Question;
		
		if(isAskOpinion) {
			parameter.ques = QuestionType.AskOpinion;
			
			String num = splited.replaceAll("[^\\d]", "");
			parameter.targetAgent = Integer.valueOf(num.substring(num.length()-2,num.length()));
			//誰のことを
		}else if(isAskWhoSEER) {
			parameter.ques = QuestionType.AskWhoSEER;
		}else if(isAskWhoWolf) {
			parameter.ques = QuestionType.AskWhoWolf;
		}else if(isAskVoteTarget) {
			parameter.ques = QuestionType.AskVoteTarget;
		}else if(isAskMyRole) {
			parameter.ques = QuestionType.AskMyRole;
		}else {
			parameter.ques = QuestionType.Other;
		}
		
		return parameter;
	}
	
	public boolean matchAskOpinion(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_think = boo_think|Pattern.compile(reg_think).matcher(feature).find();
            boo_How = boo_How|Pattern.compile(reg_How).matcher(feature).find();
            
            node = node.next();
        }
		
		if(boo_think && boo_How) {
			/* (どう）(思う)  */
			return true;
		}else {
			return false;
		}
	}
	
	public boolean matchAskVoteTarget(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_vote = boo_vote|Pattern.compile(reg_vote).matcher(feature).find();
            boo_Who = boo_Who|Pattern.compile(reg_Who).matcher(feature).find();
            
            node = node.next();
        }
		
		if(boo_vote && boo_Who) {
			/* （だれ）（投票） */
			return true;
		}else {
			return false;
		}
	}
	
	public boolean matchAskMyRole(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_vote = boo_vote|Pattern.compile(reg_vote).matcher(feature).find();
            boo_think = boo_think|Pattern.compile(reg_think).matcher(feature).find();
            boo_What = boo_What|Pattern.compile(reg_What).matcher(feature).find();
            boo_divined = boo_divined|Pattern.compile(reg_divined).matcher(feature).find();
            boo_result = boo_result|Pattern.compile(reg_result).matcher(feature).find();
            
            node = node.next();
        }
		
		if(!boo_vote && !boo_think && !boo_divined && !boo_result && boo_What) {
			/* (!投票) (!思う) (!占う) (!結果) (＊何＊) */
			return true;
		}else {
			return false;
		}
	}
	
	public boolean matchAskWhoSEER(String splited) {
		booleanFalse();
		
		boolean boo_seer = false;
		
		boo_How = boo_How|Pattern.compile(reg_How).matcher(splited).find();
        boo_Who = boo_Who|Pattern.compile(reg_Who).matcher(splited).find();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_seer = boo_seer|Pattern.compile("占い師|真占い").matcher(feature).find();
            boo_How = boo_How|Pattern.compile(reg_How).matcher(feature).find();
            boo_Who = boo_Who|Pattern.compile(reg_Who).matcher(feature).find();
            
            node = node.next();
        }
		
		if(boo_seer && (boo_How | boo_Who)) {
			/* （どのorだれ）（占い師） */
			return true;
		}else {
			return false;
		}
	}
	
	public boolean matchAskWhoWolf(String splited) {
		booleanFalse();
		
		boolean boo_wolf = false;
		
		boo_How = boo_How|Pattern.compile(reg_How).matcher(splited).find();
        boo_Who = boo_Who|Pattern.compile(reg_Who).matcher(splited).find();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_wolf = boo_wolf|Pattern.compile("狼|黒|あやしい|アヤシイ|あやしむ|アヤシム").matcher(feature).find();
            boo_How = boo_How|Pattern.compile(reg_How).matcher(feature).find();
            boo_Who = boo_Who|Pattern.compile(reg_Who).matcher(feature).find();
            
            node = node.next();
        }
		
		if(boo_wolf && (boo_How | boo_Who)) {
			/* （どのorだれ）（人狼） */
			return true;
		}else {
			return false;
		}
	}
	
	public UtteranceParameter matchProtocol(String splited, int talkerIdx) {
		boolean isCO = matchCO(splited);
		boolean isDivined = matchDivined(splited);
		boolean isEstimate = matchEstimate(splited);
		
		if(isCO) {
			return extractProtocol(splited, talkerIdx, ProtocolType.CO);
		}
		
		if(isEstimate) {
			return extractProtocol(splited, talkerIdx, ProtocolType.Estimate);
		}
		
		if(isDivined) {
			return extractProtocol(splited, talkerIdx, ProtocolType.Divined);
		}
		
		return new UtteranceParameter();
	}
	
	public boolean matchCO(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		
		boo_CO = boo_CO|Pattern.compile(reg_CO).matcher(splited).find();
        
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_first = boo_first|Pattern.compile(reg_firstPerson).matcher(feature).find();
            boo_helper = boo_helper|Pattern.compile(reg_helper).matcher(feature).find();
            boo_roletext = boo_roletext|Pattern.compile(reg_roletext).matcher(feature).find();
            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(feature).find();
            //boo_CO = boo_CO|Pattern.compile(reg_CO).matcher(feature).find();
            boo_think = boo_think|Pattern.compile(reg_think).matcher(feature).find();
            
            node = node.next();
        }
		
        if(boo_first && boo_helper && boo_roletext && !boo_think) {
        	return true;
        	// (私) (は) (*役職*) (!思う)
        }else if(boo_roletext && boo_CO && !boo_think) {
        	return true;
        	// (*役職*) (CO) (!思う)
        }
		return false;
	}
	
	public boolean matchDivined(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		
		boo_Agent = boo_Agent|Pattern.compile(reg_Agent).matcher(splited).find();
        
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            boo_species = boo_species|Pattern.compile(reg_species).matcher(feature).find();
            boo_divined = boo_divined|Pattern.compile(reg_divined).matcher(feature).find();
            boo_result = boo_result|Pattern.compile(reg_result).matcher(feature).find();
            //boo_Agent = boo_Agent|Pattern.compile(reg_Agent).matcher(surface).find();
            boo_past = boo_past|Pattern.compile(reg_past).matcher(feature).find();
            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(feature).find();
            
            node = node.next();
        }
		
        if(boo_Agent && boo_divined && boo_species) {
        	return true;
        	// (Agent) (占う) (人間/人狼)
        }else if(boo_result && boo_Agent && boo_species) {
        	return true;
        	// (結果) (Agent) (人間/人狼)
        }else if(boo_Agent && boo_species && boo_past) {
        	// (Agent) (人間/人狼) (だった)
        	return true;
        }
		
		return false;
	}
	
	public boolean matchEstimate(String splited) {
		booleanFalse();
		
		Node node = mecab.mecabParse(splited);
		node = node.next();
		
		boo_Agent = boo_Agent|Pattern.compile(reg_Agent).matcher(splited).find();
        
		while (node != null) {
            String surface = node.surface();
            String feature = node.feature();
            
            //boo_Agent = boo_Agent|Pattern.compile(reg_Agent).matcher(surface + feature).find();
            boo_helper = boo_helper|Pattern.compile(reg_helper).matcher(surface + feature).find();
            boo_roletext = boo_roletext|Pattern.compile(reg_roletext).matcher(surface + feature).find();
            boo_think = boo_think|Pattern.compile(reg_think).matcher(surface + feature).find();
            boo_trust = boo_trust|Pattern.compile(reg_trust).matcher(surface + feature).find();
            boo_vote = boo_vote|Pattern.compile(reg_vote).matcher(surface + feature).find();
            boo_divined = boo_divined|Pattern.compile(reg_divined).matcher(surface + feature).find();
            boo_past = boo_past|Pattern.compile(reg_past).matcher(surface + feature).find();
            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(surface + feature).find();
            
            node = node.next();
        }
		
        if(boo_Agent && boo_helper && boo_roletext && boo_think) {
        	return true;
        	// (Agent) (は) (*役職*) (思う)
        }else if(boo_Agent && boo_helper && boo_roletext && !boo_divined && !boo_past) {
        	return true;
        	// (Agent) (は) (*役職*) (!占う) (!だった)
        }else if(boo_Agent && boo_vote && !boo_deny) {
        	return true;
        	// (Agent) (投票) (!ない) (!だった)
        }else if(boo_Agent && boo_trust && !boo_past) {
        	return true;
        	// (Agent) (は) (信じる) (!だった)
        }
		
		return false;
	}
	
	public UtteranceParameter extractProtocol(String splited, int talkerIdx, ProtocolType proto) {
		UtteranceParameter utterparameter = new UtteranceParameter();
		utterparameter.proto = proto;
		
		if(proto == ProtocolType.CO) {
			//誰が
			utterparameter.actorAgent = talkerIdx;
			
			//どの役職に
			if(splited.contains("村人")||splited.contains("人間")||splited.contains("白")) {
				utterparameter.coRole = Role.VILLAGER;
			}else if(splited.contains("占い")) {
				utterparameter.coRole = Role.SEER;
			}else if(splited.contains("狂人")) {
				utterparameter.coRole = Role.POSSESSED;
			}else if(splited.contains("人狼") || splited.contains("狼")||splited.contains("黒")) {
				utterparameter.coRole = Role.WEREWOLF;
			}
			
			//COした/非COした
			boo_deny = false;
			Node node = mecab.mecabParse(splited);
			node = node.next();
			while (node != null) {
				String surface = node.surface();
	            String feature = node.feature();
	            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(surface + feature).find();
	            node = node.next();
	        }
			utterparameter.deny = boo_deny;
		}
		
		if(proto == ProtocolType.Divined) {
			//誰が
			utterparameter.actorAgent = talkerIdx;
			
			//誰を占って
			String num = splited.replaceAll("[^\\d]", "");
			utterparameter.targetAgent = Integer.valueOf(num.substring(num.length()-2,num.length()));
			
			//肯定/否定
			boo_deny = false;
			Node node = mecab.mecabParse(splited);
			node = node.next();
			while (node != null) {
				String surface = node.surface();
	            String feature = node.feature();
	            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(surface + feature).find();
	            node = node.next();
	        }
			utterparameter.deny = boo_deny;
			
			//結果はどうだった
			if((splited.contains("人間") && !boo_deny) || splited.contains("人狼") && boo_deny) {
				utterparameter.isWolf = Species.HUMAN;
			}else if(splited.contains("人狼") && !boo_deny || (splited.contains("人間") && boo_deny)) {
				utterparameter.isWolf = Species.WEREWOLF;
			}
		}
		
		if(proto == ProtocolType.Estimate) {
			//誰が
			utterparameter.actorAgent = talkerIdx;
			
			//誰のことを
			String num = splited.replaceAll("[^\\d]", "");
			if(num.length() >= 2) {
				utterparameter.targetAgent = Integer.valueOf(num.substring(num.length()-2,num.length()));
			}else {
				utterparameter.targetAgent = 1;
			}
			
			//どの役職だと
			if(splited.contains("村人") || splited.contains("人間")) {
				utterparameter.estimateRole = Role.VILLAGER;
			}else if(splited.contains("占い師")) {
				utterparameter.estimateRole = Role.SEER;
			}else if(splited.contains("狂人")) {
				utterparameter.estimateRole = Role.POSSESSED;				
			}else if(splited.contains("人狼")) {
				utterparameter.estimateRole = Role.WEREWOLF;
			}else if(splited.contains("票")||splited.contains("入れ")||splited.contains("吊")){
				utterparameter.estimateRole = Role.WEREWOLF;
			}
			
			//思っている/いない
			boo_deny = false;
			boo_trust = false;
			Node node = mecab.mecabParse(splited);
			node = node.next();
			while (node != null) {
				String surface = node.surface();
	            String feature = node.feature();
	            boo_deny = boo_deny|Pattern.compile(reg_deny).matcher(surface + feature).find();
	            boo_trust = boo_trust|Pattern.compile(reg_trust).matcher(surface + feature).find();
	            node = node.next();
	        }
			utterparameter.deny = boo_deny;
			
			if(boo_trust && !boo_deny) {
				utterparameter.estimateRole = Role.VILLAGER;
			}else if(boo_trust && boo_deny){
				utterparameter.estimateRole = Role.WEREWOLF;
			}
		}
		
		return utterparameter;
	}
	
	public void booleanFalse() {
		boo_first = false;
		boo_helper = false;
		boo_roletext = false;
		boo_species = false;
		boo_deny = false;
		boo_think = false;
		boo_trust = false;
		boo_CO = false;
		boo_divined = false;
		boo_result = false;
		boo_past = false;
		boo_vote = false;
		boo_Agent = false;
	}
	
	public void printboolean() {
		if(boo_first)System.out.println("boo_first");
		if(boo_helper)System.out.println("boo_helper");
		if(boo_roletext)System.out.println("boo_roletext");
		if(boo_species)System.out.println("boo_species");
		if(boo_deny)System.out.println("boo_deny");
		if(boo_think)System.out.println("boo_think");
		if(boo_trust)System.out.println("boo_trust");
		if(boo_CO)System.out.println("boo_CO");
		if(boo_divined)System.out.println("boo_divined");
		if(boo_result)System.out.println("boo_result");
		if(boo_past)System.out.println("boo_past");
		if(boo_vote)System.out.println("boo_vote");
		if(boo_Agent)System.out.println("boo_Agent");
		System.out.println(":");
		
	}
	
	public void printAnalyzeResult(Utterance utterance, List<UtteranceParameter> uparaList) {
		
		for(UtteranceParameter parameter : uparaList) {
			if(parameter.proto != null) {
				switch(parameter.proto) {
				case CO:
					System.out.println("CO");
					System.out.println("\tActor : Agent[" + String.format("%02d", parameter.actorAgent) + "]");
					System.out.println("\tCORole : " + parameter.coRole);
					if(parameter.deny)System.out.println("\tDeny : " + parameter.deny);
					break;
				case Divined:
					System.out.println("占い結果");
					System.out.println("\tActor : Agent[" + String.format("%02d", parameter.actorAgent) + "]");
					System.out.println("\tTarget : Agent[" + String.format("%02d", parameter.targetAgent) + "]");
					System.out.println("\tResult : " + (parameter.isWolf));
					break;
				case Estimate:
					System.out.println("役職推理");
					System.out.println("\tActor : Agent[" + String.format("%02d", parameter.actorAgent) + "]");
					System.out.println("\tTarget : Agent[" + String.format("%02d", parameter.targetAgent) + "]");
					System.out.println("\tRole : " + parameter.estimateRole);
					if(parameter.deny)System.out.println("\tDeny : " + parameter.deny);
					break;
				default:
					break;
				}
			}else {
				//System.out.println("Can't find paramter");
			}
		}
	}

}
