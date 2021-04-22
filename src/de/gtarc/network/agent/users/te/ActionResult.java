package de.gtarc.network.agent.users.te;

public class ActionResult {
	public Mood previousMood;
	public Mood currentMood;
	public String action;
	public Payoff benchmarkUtility;
	public ActionResult(Mood pre, Mood current, String action, Payoff utility) {
		this.previousMood = pre;
		this.currentMood = current;
		this.action = action;
		this.benchmarkUtility = utility;
	}
	public ActionResult(Mood current, String action, Payoff utility) {
		this.currentMood = current;
		this.action = action;
		this.benchmarkUtility = utility;
	}
}
