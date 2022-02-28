package jp.ynu.eis.forestlab;

import org.aiwolf.common.data.Talk;


public class UtteranceConverter {
	String utteranceText;
	String talkText;
	int day;
	int serialID;
	int turn;
	int AgentNum;
	
	public UtteranceConverter(){
		
	}
	
	public Utterance convert(Talk talk) {
		//Mecab mecab = new Mecab();
		
		Utterance utterance = new Utterance(talk.getText());
		
		utterance.day = talk.getDay();
		utterance.serialID = talk.getIdx();
		utterance.turn = talk.getTurn();
		utterance.agentNum = talk.getAgent().getAgentIdx();
		if(utterance.agentNum == 0) utterance.agentNum++;
		utterance.talkText = talk.getText();
		
        return utterance;
	}

}

