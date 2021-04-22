package de.gtarc.network.vnf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * This class is used to attach price, bandwidth and delay values to a path inside the network topology.
 * @author cemakpolat, doruksahinel
 *
 */
public class Path {
	double bandwidth = 0;
	double delay = 0;
	String sourceNode ="";
	String targetNode ="";
	double price = 0;
	public Path(double b, double delay, String s, String t){
		this.bandwidth = b;
		this.delay = delay;
		this.sourceNode = s;
		this.targetNode = t;
	}
	public double getBandwidth() {
		return this.bandwidth;
	}
	public double getDelay() {
		return this.delay;
	}
	public void setPrice(double price) {
		// TODO Auto-generated method stub
		this.price = price;
		
	}
	public void setBandwidth(double bandwidth2) {
		// TODO Auto-generated method stub
		this.bandwidth = bandwidth2;
	}
	public void setDelay(double delay2) {
		// TODO Auto-generated method stub
		this.delay = delay2;
	}
	
}