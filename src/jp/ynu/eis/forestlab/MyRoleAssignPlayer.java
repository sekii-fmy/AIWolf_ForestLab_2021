package jp.ynu.eis.forestlab;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class MyRoleAssignPlayer extends AbstractRoleAssignPlayer {
	
	public MyRoleAssignPlayer() {
		setVillagerPlayer(new MyVillager());
		setBodyguardPlayer(new MyBodyguard());
		setMediumPlayer(new MyMedium());
		setSeerPlayer(new MySeer());
		setPossessedPlayer(new MyPossessed());
		setWerewolfPlayer(new MyWerewolf());
	}
	
	public String getName() {
		return "ForestLab";
	}

}
