package jp.ynu.eis.forestlab;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class RoleAssignPlayer extends AbstractRoleAssignPlayer {

	public RoleAssignPlayer() {
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
