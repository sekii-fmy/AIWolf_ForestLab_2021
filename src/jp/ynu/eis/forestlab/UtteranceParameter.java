package jp.ynu.eis.forestlab;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

public class UtteranceParameter {
	int actorAgent;
	int targetAgent;
	Species isWolf;
	Role estimateRole;
	Role coRole;
	boolean deny;
	ProtocolType proto;
	Species result;
	
	QuestionType ques;
	
	public UtteranceParameter() {
		actorAgent = 0;
		targetAgent = 0;
		isWolf = Species.ANY;
		estimateRole = null;
		coRole = null;
		deny = false;
		proto = null;
		
		ques = QuestionType.None;
	}

}
