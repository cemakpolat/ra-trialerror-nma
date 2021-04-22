package de.gtarc.network.service.common;

import de.gtarc.network.agent.users.ra.RDUser;
import de.gtarc.network.agent.users.te.TEUser;

/**
 * This class creates the game between smart city service users.
 * @author cemakpolat
 *
 */
// GameTable
// User1 | User2 | isGamePlayed (true, false)| Decision (VNF-Id) | UserUtility
// User2 | User1 | isGamePlayed (true, false)| Decision (VNF-Id) | UserUtility

public class UserGame {
	// common parameters
	public boolean isGamePlayed = false;
	public String decision = "";
	
	// replicator dynamic params
	public RDUser player1;
	public RDUser player2;
	public double player1Utility = 0;
	public UserGame(RDUser p1, RDUser p2){
		this.player1 = p1;
		this.player2 = p2;
		this.isGamePlayed = false;
	}

	// for trial and error, there is no player to be imitating 
	public TEUser player;
	public double playerUtility = 0;
	public double benchmarkUtility = 0;
	public UserGame(TEUser name){
		this.player = name;
		this.isGamePlayed = false;
	}
}
