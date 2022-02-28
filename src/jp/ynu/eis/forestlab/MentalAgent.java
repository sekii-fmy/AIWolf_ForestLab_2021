package jp.ynu.eis.forestlab;

import org.aiwolf.common.data.Agent;

public class MentalAgent {
	
	private final int num_villager = 5;
	private final int num_role = 4;
	
	Agent agent;
	OppositeTable table;
	double[] reliability;
	
	public MentalAgent(Agent agent) {
		this.agent = agent;
		reliability = new double[num_villager];
		for(int i = 0; i < num_villager; i++) {
			reliability[i] = 0.5;
		}
		table = new OppositeTable();
	}
	
	// 信頼度の範囲は[0-1]
	public void checkRangeReliability() {
		for(int i = 0; i < num_villager; i++) {
			if(reliability[i] < 0) reliability[i] = 0;
			if(reliability[i] > 1) reliability[i] = 1;
		}
	}
	
	public int getAgentIdx() {
		return agent.getAgentIdx();
	}

}
