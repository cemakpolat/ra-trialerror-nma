package com.gtarc.network.utils;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Util {

	
	public void sleep(long sleepDuration){
		try {
			Thread.sleep(sleepDuration);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	// generate universally unique identifiers.

	  public static UUID generateRandomUUID() {
		  return UUID.randomUUID();
	  }
	  public static int getRandomNumber(int low, int high) {
		  Random r = new Random();
		  return r.nextInt(high-low) + low;
	  }
	  public static double getRandomNumber(double low, double high) {
		  
		  return ThreadLocalRandom.current().nextDouble(low, high);

	  }
	  
}