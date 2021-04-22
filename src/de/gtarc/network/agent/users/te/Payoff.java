package de.gtarc.network.agent.users.te;


/**
 * This class is used to track the expiring time 
 * of a best benchmark, aka sliding window.
 * @author cemalkilic
 *
 */
public class Payoff {
	
	public static final int COUNT_DOWN = 5;
	
	private double userPayoff;
	private int count;
	
	public Payoff(){
		this(0, COUNT_DOWN);
	}
	
	public Payoff(double userPayoff){
		this(userPayoff, COUNT_DOWN);
	}
	
	public Payoff(double userPayoff, int count) {
		this.userPayoff = userPayoff;
		this.count = count;
	}
	
	public void decrementCount(){
		this.count--;
	}
	
	public void resetCount(){
		this.count = COUNT_DOWN;
	}
	
	public double getUserPayoff() {
		return userPayoff;
	}
	
	public void setUserPayoff(double userPayoff) {
		this.userPayoff = userPayoff;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "[userPayoff=" + userPayoff + ", count=" + count + "]";
	}
}
