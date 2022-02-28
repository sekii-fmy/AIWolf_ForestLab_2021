package jp.ynu.eis.forestlab;

import java.util.ArrayList;
import java.util.List;


public class Utterance {
	String utteranceText;
	int day;
	int serialID;
	int turn;
	int agentNum;
	String talkText;
	boolean isSkip;
	boolean isOver;
	List<UtteranceParameter> utterparameterList = new ArrayList<UtteranceParameter>();
	
	public Utterance() {
		this.utteranceText = "";
		this.day = 0;
		this.serialID = 0;
		this.turn = 0;
		this.agentNum = 1;
		this.talkText = null;
		this.isSkip = false;
		this.isOver = false;
	}
	
	public Utterance(String utteranceText) {
		this.utteranceText = utteranceText;
		this.day = 0;
		this.serialID = 0;
		this.turn = 0;
		this.agentNum = 1;
		this.talkText = utteranceText;
		this.isSkip = false;
		this.isOver = false;
	}
	
	public Utterance(String utteranceText, int day, int serialID, int turn, int agentNum, String talkText) {
		this.utteranceText = utteranceText;
		this.day = day;
		this.serialID = serialID;
		this.turn = turn;
		this.agentNum = agentNum;
		this.talkText = talkText;
		this.isSkip = false;
		this.isOver = false;
	}
	
	public void printUtterance() {
		String printStr = "day : " + day + "\n"
						 + "ID : " + serialID + "\n"
						 + "turn : " + turn + "\n"
						 + "Agent : " + agentNum + "\n"
						 + talkText;
		
		System.out.println(printStr);
	}
}

