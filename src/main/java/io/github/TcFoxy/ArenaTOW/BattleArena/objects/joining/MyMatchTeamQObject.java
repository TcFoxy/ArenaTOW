package io.github.TcFoxy.ArenaTOW.BattleArena.objects.joining;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.Matchup;
import mc.alk.arena.objects.teams.ArenaTeam;

import java.util.List;

public class MyMatchTeamQObject extends MyQueueObject{
	final Matchup matchup;

	public MyMatchTeamQObject(Matchup matchup){
		super(matchup.getJoinOptions());
//		matchParams = matchup.getMatchParams();
		this.matchup = matchup;
		this.priority = matchup.getPriority();
		for (ArenaTeam t: matchup.getTeams()){
			numPlayers += t.size();
		}
        this.listeners = matchup.getArenaListeners();
    }

	@Override
	public Integer getPriority() {
		return priority;
	}

	@Override
	public boolean hasMember(ArenaPlayer p) {
		return matchup.hasMember(p);
	}

	@Override
	public ArenaTeam getTeam(ArenaPlayer p) {
		return matchup.getTeam(p);
	}

	@Override
	public int size() {
		return matchup.size();
	}

	@Override
	public String toString(){
		return priority+" " + matchup.toString();
	}

	@Override
	public List<ArenaTeam> getTeams() {
		return matchup.getTeams();
	}

	public Matchup getMatchup() {
		return matchup;
	}

	@Override
	public boolean hasTeam(ArenaTeam team) {
		List<ArenaTeam> teams = matchup.getTeams();
		return teams != null && teams.contains(team);
	}

}