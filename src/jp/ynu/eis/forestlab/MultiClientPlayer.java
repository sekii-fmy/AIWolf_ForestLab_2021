package jp.ynu.eis.forestlab;

import java.io.IOException;

public class MultiClientPlayer {

	public static void main(String arg[]) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		ServerPlayer svp = new ServerPlayer();
		svp.start(5);
	}
}
