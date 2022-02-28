package jp.ynu.eis.forestlab;

import java.util.Random;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

public class TalkTextGenerator {
	Utterance utterance;

	public TalkTextGenerator() {
	}
	
	public String GenerateText(Utterance utterance) {
		String utteranceText = "";
		for(UtteranceParameter parameter : utterance.utterparameterList) {
			
			System.out.println("proto : "+parameter.proto);
			System.out.println("actor : "+parameter.actorAgent);
			System.out.println("target : "+parameter.targetAgent);
			
			switch(parameter.proto) {
			case Greeting:
				utteranceText += GenerateGreetingText(parameter);
				break;
			case CO:
				utteranceText += GenerateCOText(parameter);
				break;
			case Estimate:
				utteranceText += GenerateEstimateText(parameter);
				break;
			case Divined:
				utteranceText += GenerateDivinedText(parameter);
				break;
			case DeclareDivine:
				utteranceText += GenerateDeclareDivineText(parameter);
				break;
			case DesireDivine:
				utteranceText += GenerateDesireDivineText(parameter);
				break;
			case DeclareVote:
				utteranceText += GenerateDeclareVoteText(parameter);
				break;
			case DesireVote:
				utteranceText += GenerateDesireVoteText(parameter);
				break;
			case Chatting:
				utteranceText += GenerateChattingText(utterance);
				break;
			case SKIP:
				utteranceText += Talk.SKIP;
				break;
			case OVER:
				utteranceText = Talk.OVER;
				break;
			default:
				break;
			}
		}
		
		return utteranceText;
	}
	
	String GenerateGreetingText(UtteranceParameter parameter) {
		String text;
		String[] chatTexts = new String[] {
				"こんにちは、私はAgent[" + String.format("%02d",parameter.actorAgent) + "]って言うんだ。よろしくね。",
				"Agent["  + String.format("%02d",parameter.actorAgent) + "とは私のこと。よしなに。"
		};
		
		text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		
		return text;
	}
	
	String GenerateCOText(UtteranceParameter parameter) {
		String text;
		if(!parameter.deny) {
			String[] chatTexts = new String[] {
				"カミングアウトしておこうかな。私の役職は"+ ChangeRoletoJapanese(parameter.coRole) + "なんだ。",
				"私は"	+ ChangeRoletoJapanese(parameter.coRole) + "だよ。驚いた？",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}else {
			String[] chatTexts = new String[] {
				"残念なことに私は" + ChangeRoletoJapanese(parameter.coRole) + "ではないんだ。",
				"ちなみに私の役職は" + ChangeRoletoJapanese(parameter.coRole) + "ではありませんので、あしからず。",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}
		return text;
	}
	
	String GenerateEstimateText(UtteranceParameter parameter) {
		String text;
		if(!parameter.deny) {
			String[] chatTexts = new String[] {
				"Agent[" + String.format("%02d",parameter.targetAgent) + "]は" + ChangeRoletoJapanese(parameter.estimateRole) + "なんじゃないかなあ。",
				"Agent[" + String.format("%02d",parameter.targetAgent) + "]のことは" + ChangeRoletoJapanese(parameter.estimateRole) + "だと思っているよ。",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}else {
			String[] chatTexts = new String[] {
				"Agent[" + String.format("%02d",parameter.targetAgent) + "]は" + ChangeRoletoJapanese(parameter.estimateRole) + "とは違う気がするなあ。",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}
		
		return text;
	}
	
	String GenerateDivinedText(UtteranceParameter parameter) {
		String text;
		if(parameter.isWolf == Species.WEREWOLF) {
			String[] chatTexts = new String[] {
				"今日はAgent[" + String.format("%02d",parameter.targetAgent) + "]はを占ったんだが、なんと結果は人狼だったよ。",
				"占いの結果が出たよ。なんとAgent[" + String.format("%02d",parameter.targetAgent) + "]は人狼だ。",
				"残念な結果だ。Agent[" + String.format("%02d",parameter.targetAgent) + "]は狼であるという占いがされてしまった。",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}else {
			String[] chatTexts = new String[] {
				"今日はAgent[" + String.format("%02d",parameter.targetAgent) + "]はを占ったんだが、結果は人間だったよ。",
				"占いの結果が出たよ。Agent[" + String.format("%02d",parameter.targetAgent) + "]は人狼ではなかったようだね。",
				"喜ばしい知らせだ。Agent[" + String.format("%02d",parameter.targetAgent) + "]は狼ではないという占い結果が出たよ。",
			};
			text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		}
		return text;
	}
	
	String GenerateDeclareDivineText(UtteranceParameter parameter) {
		return "";
	}
	
	String GenerateDesireDivineText(UtteranceParameter parameter) {
		String text;
		text = "Agent[" + String.format("%02d",parameter.targetAgent) + "]の役職がちょっと気になるなあ。占い師さん、明日の占いにどうかな。";
		
		return text;
	}
	
	String GenerateDeclareVoteText(UtteranceParameter parameter) {
		String text;
		String[] chatTexts = new String[] {
			"今日はAgent[" + String.format("%02d",parameter.targetAgent) + "]に投票することにしたよ。",
			"どうも私はAgent[" + String.format("%02d",parameter.targetAgent) + "]が怪しくから、投票させてもらうよ。",
			"私はAgent[" + String.format("%02d",parameter.targetAgent) + "]に投票しようと思うんだ。",
		};
		text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		
		return text;
	}
	
	String GenerateDesireVoteText(UtteranceParameter parameter) {
		String text;
		String[] chatTexts = new String[] {
			"Agent["+ String.format("%02d",parameter.targetAgent) +"]に投票しないか？きっといい結果になる。",
			"Agent["+ String.format("%02d",parameter.targetAgent) +"]はきっと狼だ。みんなで票を集めないか？",
			"みんなにAgent[" + String.format("%02d",parameter.targetAgent) + "]に投票してほしいんだ。",
		};
		text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		
		return text;
	}
	
	String GenerateChattingText(Utterance utterance) {
		String text;
		
		String[] chatTexts = new String[] {
				"さあ、どうしたものかな。",
				"そろそろ投票先でも決めようか。",
				"なんだか雲行きが怪しくなってきたね。",
				"議論が長引きそうだ。ちょっと退屈だよ。",
				"ねむくなってきたなぁ。",
		};
		
		text = chatTexts[new Random().nextInt(chatTexts.length - 1)];
		
		return text;
	}
	
	//Role型の列挙子を日本語テキストにする
	public String ChangeRoletoJapanese(Role role) {
		switch(role) {
			case VILLAGER:
				return "村人";
			case SEER:
				return "占い師";
			case MEDIUM:
				return "霊能者";
			case BODYGUARD:
				return "狩人";
			case POSSESSED:
				return "狂人";
			case WEREWOLF:
				return "人狼";
			default:
				return "";
		}
	}
	
	public void printUtteranceParameter(UtteranceParameter parameter) {
		String printStr = 
				"actorAgent : " + parameter.actorAgent + "\n" +
				"tergetAgent : " + parameter.targetAgent + "\n" +
				"isWolf : " + parameter.isWolf + "\n" +
				"estimateRole : " + parameter.estimateRole + "\n" +
				"coRole : " + parameter.coRole + "\n" +
				"deny : " + parameter.deny + "\n" +
				"Protocol : " + parameter.proto;
		
		System.out.println(printStr);
	}

}
