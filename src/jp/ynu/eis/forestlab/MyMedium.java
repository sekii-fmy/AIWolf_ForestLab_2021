package jp.ynu.eis.forestlab;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 霊媒師役エージェントクラス
 */
public class MyMedium extends MyVillager {
	int comingoutDay;
	boolean isCameout;
	Deque<Judge> identQueue = new LinkedList<>();
	Map<Agent, Species> myIdentMap = new HashMap<>();
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int)(Math.random() * 3 + 1);
		isCameout = false;
		identQueue.clear();
		myIdentMap.clear();
	}
	
	public void dayStart() {
		super.dayStart();
		// 霊媒結果を待ち行列に入れる
		Judge ident = currentGameInfo.getMediumResult();
		if(ident != null) {
			identQueue.offer(ident);
			myIdentMap.put(ident.getTarget(), ident.getResult());
		}
	}
	
	protected void chooseVoteCandidate() {
		werewolves.clear();
		// 霊媒師をカミングアウトしている他のエージェントは人狼候補
		for(Agent agent : aliveOthers) {
			if(comingoutMap.get(agent) == Role.MEDIUM) {
				werewolves.add(agent);
			}
		}
		// 自分や殺されたエージェントを人狼と判定、あるいは自分と異なる判定の占い師は人狼候補
		for(Judge j : divinationList) {
			Agent agent = j.getAgent();
			Agent target = j.getTarget();
			if(j.getResult() == Species.WEREWOLF && (target == me || isKilled(target) || myIdentMap.containsKey(target) 
					&& j.getResult() != myIdentMap.get(target))) {
				if(isAlive(agent) && !werewolves.contains(agent)) {
					werewolves.add(agent);
				}
			}
		}
		//候補がいない場合ランダム
		if(werewolves.isEmpty()) {
			if(!aliveOthers.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
			}
		} else {
			if(!werewolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(werewolves);
				// 以前の投票先から変わる場合、新たに推測発言と占い要請をする
				if(canTalk) {
					talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
					talkQueue.offer(new Content(new RequestContentBuilder(null, 
							new Content(new DivinationContentBuilder(voteCandidate)))));
				}
			}
		}
	}
	
	public String talk() {
		//カミングアウトする日になったら、あるいは霊媒結果が人狼だったら
		//あるいはカミングアウトが出たらカミングアウト
		if(!isCameout && (day >= comingoutDay || (!identQueue.isEmpty() && identQueue.peekLast().getResult() 
				== Species.WEREWOLF) || isCO(Role.MEDIUM))) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.MEDIUM)));
			isCameout = true;
		}
		//カミングアウトしたらこれまでの霊媒結果をすべて公開
		if(isCameout) {
			while(!identQueue.isEmpty()) {
				Judge ident = identQueue.poll();
				talkQueue.offer(new Content(new IdentContentBuilder(ident.getTarget(), ident.getResult())));
			}
		}
		return super.talk();
	}

}

