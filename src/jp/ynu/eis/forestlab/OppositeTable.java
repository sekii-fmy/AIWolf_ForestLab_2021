package jp.ynu.eis.forestlab;

public class OppositeTable {
	
	final int num_villager = 5;
	final int num_role = 4;
	public double[][] table;
	
	public OppositeTable() {
		table = new double[num_villager][num_role];
		initializeTable();
	}
	
	// 対応表を初期化する
	// 初期値は各役職の全体に対する人数比
	void initializeTable() {
		for(int i = 0; i < num_villager; i++) {
			table[i][0] = 0.400;
			table[i][1] = 0.200;
			table[i][2] = 0.200;
			table[i][3] = 0.200;
		}
	}
	
	// 対応表の値の範囲は[0-1]
	// マイナスの時は0にし、すべての役職の値の総和が1になるように修正
	void chackRangeOfTable() {
		for(int i = 0; i < num_villager; i++) {
			
			double sumAll = 0;
			for(int j = 0; j < num_role; j++) {
				if(table[i][j] < 0) {
					table[i][j] = 0;
				}
				sumAll += table[i][j];
			}
			
			for(int k = 0; k < num_role; k++) {
				table[i][k] = Math.round(table[i][k] / sumAll);
			}
		}
	}

}
