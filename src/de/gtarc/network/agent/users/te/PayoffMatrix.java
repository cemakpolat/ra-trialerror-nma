package de.gtarc.network.agent.users.te;

class PayoffMatrix{
	public String currentState = "";
	public String nextState = "";
	public double bmUtilityDifference = 0;
	public PayoffMatrix(String c, String n, double utility) {
		this.currentState = c;
		this.nextState = n;
		this.bmUtilityDifference  = utility ;
		
	}
}