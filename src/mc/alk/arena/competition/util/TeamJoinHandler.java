package mc.alk.arena.competition.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mc.alk.arena.competition.Competition;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.queues.TeamQObject;
import mc.alk.arena.objects.teams.CompositeTeam;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamHandler;

public abstract class TeamJoinHandler implements TeamHandler {

	public static final TeamJoinResult CANTFIT = new TeamJoinResult(TeamJoinStatus.CANT_FIT,-1,null);
	public static final TeamJoinResult NOTOPEN = new TeamJoinResult(TeamJoinStatus.NOT_OPEN,-1,null);

	public static enum TeamJoinStatus{
		ADDED, CANT_FIT, ADDED_TO_EXISTING, WAITING_FOR_PLAYERS, NOT_OPEN
	}

	public static class TeamJoinResult{
		final public TeamJoinStatus status;
		final public int remaining;
		final public ArenaTeam team;

		public TeamJoinResult(TeamJoinStatus status, int remaining, ArenaTeam team){
			this.status = status; this.remaining = remaining; this.team = team;}
		public TeamJoinStatus getEventType(){ return status;}
		public int getRemaining(){return remaining;}
	}

	List<ArenaTeam> teams = new ArrayList<ArenaTeam>();

	ArrayList<ArenaTeam> pickupTeams = new ArrayList<ArenaTeam>();
	Competition competition;
	int minTeamSize,maxTeamSize;
	int minTeams,maxTeams;
	Class<? extends ArenaTeam> clazz;

	public TeamJoinHandler(MatchParams params, Competition competition){
		this(params,competition,CompositeTeam.class);
	}
	public TeamJoinHandler(MatchParams params, Competition competition, Class<? extends ArenaTeam> clazz) {
		setParams(params);
		this.clazz = clazz;
		setCompetition(competition);
	}
	public void setCompetition(Competition comp) {
		this.competition = comp;
		if (comp != null)
			this.teams = this.competition.getTeams();
	}
	public void setParams(MatchParams mp){
		this.minTeamSize = mp.getMinTeamSize(); this.maxTeamSize = mp.getMaxTeamSize();
		this.minTeams = mp.getMinTeams(); this.maxTeams = mp.getMaxTeams();
	}

	protected void addToTeam(ArenaTeam team, Set<ArenaPlayer> players) {
		team.addPlayers(players);
		if (competition != null){
			competition.addedToTeam(team,players);
		}
	}

	protected void addTeam(ArenaTeam team) {
		if (competition != null){
			competition.addTeam(team);
		} else {
			teams.add(team);
		}
	}

	public void deconstruct() {
		for (ArenaTeam t: pickupTeams){
			TeamController.removeTeamHandler(t, this);
		}
		pickupTeams.clear();
	}

	public abstract TeamJoinResult joiningTeam(TeamQObject tqo);

	public boolean canLeave(ArenaPlayer p) {
		return true;
	}

	public boolean leave(ArenaPlayer p) {
		for (ArenaTeam t: pickupTeams){
			if (t.hasMember(p)){
				pickupTeams.remove(t);
				return true;
			}
		}
		return false;
	}

	public Set<ArenaPlayer> getExcludedPlayers() {
		Set<ArenaPlayer> tplayers = new HashSet<ArenaPlayer>();
		for (ArenaTeam t: pickupTeams){
			tplayers.addAll(t.getPlayers());}
		return tplayers;
	}


	public List<ArenaTeam> removeImproperTeams(){
		List<ArenaTeam> improper = new ArrayList<ArenaTeam>();
		Iterator<ArenaTeam> iter = teams.iterator();
		while(iter.hasNext()){
			ArenaTeam t = iter.next();
			if (t.size() < minTeamSize || t.size() > maxTeamSize){
				iter.remove();
				TeamController.removeTeamHandler(t, this);
				improper.add(t);
			}
		}
		return improper;
	}

	public boolean hasEnough(boolean allowDifferentTeamSizes){
		if (teams ==null)
			return false;
		final int teamssize = teams.size();
		if (teamssize < minTeams)
			return false;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int valid = 0;
		for (ArenaTeam t: teams){
			final int tsize = t.size();
			if (tsize < minTeamSize || tsize > maxTeamSize)
				continue;
			if (!allowDifferentTeamSizes){
				min = Math.min(min, tsize);
				max = Math.max(max, tsize);
				if (min != tsize || max != tsize)
					continue;
			}
			valid++;
		}
		return valid >= minTeams && valid <= maxTeams;
	}

	public boolean isFull() {
		/// Check to see if we have filled up our number of teams
		if ( maxTeams > teams.size()){
			return false;}
		/// Check to see if there is any space left on the team
		for (ArenaTeam t: teams){
			if (t.size() < maxTeamSize){
				return false;}
		}
		/// we can't add a team.. and all teams are full
		return true;
	}
	public List<ArenaTeam> getTeams() {
		return teams;
	}

	protected ArenaTeam addToPreviouslyLeftTeam(ArenaPlayer player) {
		for (ArenaTeam t: teams){
			if (t.hasLeft(player)){
				t.addPlayer(player);
				return t;
			}
		}
		return null;
	}

}
