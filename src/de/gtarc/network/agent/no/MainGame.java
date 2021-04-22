package de.gtarc.network.agent.no;

/**
 * Use this class to start all resource allocation games and the actors.
 * @author cemakpolat, doruksahinel
 */
public class MainGame {
	
	public static void main(String[] args){
		// start network operator
	
		NetworkManager nm = new NetworkManager();
		nm.run();
	}
	
}
	