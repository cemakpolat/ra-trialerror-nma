package de.gtarc.network.vnf;
/**
 * 
 * This class creates the game between VNF servers
 * @author cemakpolat, doruksahinel
 *
 */
public class VNFGame {
	public String vnfId;
	public boolean isGamePlayed = false;
	
	public VNFGame(String id, boolean gameStatus){
		this.vnfId = id;
		this.isGamePlayed = gameStatus;
	}
	
	public String getVNFId(){
		return this.vnfId;
	}
}
