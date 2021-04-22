package de.gtarc.network.agent.users.te;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.Util;
import de.gtarc.network.agent.users.te.TEService;
/**
 * 
 * Service user class
 * @author cemakpolat, doruksahinel
 *
 */
public class TEUser implements Runnable{
	
	// network service
	public TEService nservice = null;
	// utils
	private Util util = new Util();
	private static FileIO fio = new FileIO(); 
	
	// QoS param
	private double bandwidth; /**  The bandwidth assigned to user */
    private double vnfPrice; /** The price assigned by the VNF server*/
    private double delay; /** Path delay experienced by the user */
    
    // User related parameters
    private String userId = ""; 
    
    public double userUtility = 0; 			/** User's utility at current iteration*/
    public double userUtility2 = 0; 			/** User's utility at current iteration*/
    public String assignedVNFServerId = "";
    
	private double experimentProbability = 0; 
	private double beta = 4;
	double phi_1= 0.001, phi_2 = 0.1; // response function variables
	
	double acceptanceProbability = 0;
	double v1 = 0.1; // v1 > 0 
	double v2 = 0.3; // such that 0 < g(u-uk)< 1/2 
	
	
	private Mood currentMood;
    private Mood previousMood;
    
	private ArrayList<ActionResult> actionResultHistory = new ArrayList<ActionResult>();
	private String benchmarkAction = "";
	public boolean isPlayed = false;
	private double benchmarkUtility = 0;
	
	//// Utility Parameters from Manzoor Paper
	
	// Bandwidth utility parameters
	double min_bandwidth = 1; // min_bandwidth_requirement
	double max_bandwidth = 580; // max_bandwidth_requirement
	int MOS = 20; // mean opinion score
	double bandwidthUtility = 0; 
	
	//Delay utility parameters
	double min_delay = 10; 		// min_bandwidth_requirement
	double delayUtility = 0;
	
	//Price utility parameters
	double val = 1;				 //private_valuation of the service by the user
	double max_satisfaction = 1; // maximum satisfaction of user based on price
	double priceUtility = 0;
	
	double vnfReputationForUser = 0;
	
	public  String USER_MOODS_FILENAME = "userMoods"; // userMoods.txt
	private static String userFolderPath = "";
	
	public TEUser(TEService ns, String id, String vnfId){
		
		this.nservice = ns;
		this.userId = id; 
		this.assignedVNFServerId = vnfId;
		this.currentMood = Mood.CONTENT;
		this.benchmarkAction = this.assignedVNFServerId;
		this.experimentProbability = Math.exp(-beta);
		USER_MOODS_FILENAME = USER_MOODS_FILENAME+"_"+this.getUserIdentity();
		userFolderPath = TEService.folderPath+"/users/";
		fio.createNewFile(userFolderPath, USER_MOODS_FILENAME);
		//Console.output(this.userId + " vnfId ->"+this.assignedVNFServerId);
	}	
	
	public void setParameters(double expProbability) {
//		if(expProbability != -1) {
//			this.beta = expProbability;
//		}
	}
	/**
     * This function starts after creating the user. At each new iteration, 
     * it first calculates the user utility and then calls the learning algorithm
     * to decide for the VNF server to be used in the next iteration. 
	 */
	private boolean initialCondition = true;
	@Override
	public void run() { 
		int previousGameNumber = 0;
		int currentGameNumber = 0;
		//Console.output("Actual Parameters -> beta:"+this.beta+" v1:"+this.v1+" v2:"+this.v2+" phi_1:"+this.phi_1+" phi_2:"+this.phi_2);
		while(nservice.isGameContinue()){
			currentGameNumber = nservice.getCurrentGameIterationNumber();
			if(currentGameNumber - previousGameNumber > 0){
				// obtain new user utility
				calculateUserUtility(nservice.getUserBandwidth(this.assignedVNFServerId),
						nservice.getPathDelay(this.assignedVNFServerId),
						nservice.getVNFPrice(assignedVNFServerId),
						nservice.getVNFReputation(assignedVNFServerId),
						nservice.serviceQoSType.alpha1,nservice.serviceQoSType.alpha2, nservice.serviceQoSType.alpha3, nservice.serviceQoSType.alpha4);
				trialAndError();
				nservice.updateGameTable(this, this.assignedVNFServerId);// at that point, I say my decision and now I need a second calculation for utility in order to understand
				previousGameNumber = currentGameNumber;
				//iteration, user, mood, action, userutility, benchmarkaction, benchmarkutitility, 
				saveInFile(currentGameNumber, this.getUserIdentity(), this.currentMood, this.assignedVNFServerId,this.userUtility, this.benchmarkAction, this.benchmarkUtility);
				initialCondition = false;
			}
			util.sleep(50);
		}
		Console.output("User thread left the game!");
	}
	public void createStability() {
		calculateUserUtility(nservice.getUserBandwidth(this.assignedVNFServerId),
				nservice.getPathDelay(this.assignedVNFServerId),
				nservice.getVNFPrice(assignedVNFServerId), 
				nservice.getVNFReputation(assignedVNFServerId),
				nservice.serviceQoSType.alpha1,nservice.serviceQoSType.alpha2, nservice.serviceQoSType.alpha3, nservice.serviceQoSType.alpha4);
		nservice.updateGameTable(this, this.assignedVNFServerId);// at that point, I say my decision and now I need a second calculation for utility in order to understand
	}

	public void calculateUserUtility(double userBandwidth, double delay, double vnfPrice, double vnfReputation, double alpha1, double alpha2, double alpha3, double alpha4){
		if(userBandwidth <= min_bandwidth){
			bandwidthUtility = 0;
		}
		    else if (userBandwidth > min_bandwidth && userBandwidth < max_bandwidth){
		    bandwidthUtility = MOS * ((1-Math.exp(-alpha1*(userBandwidth - min_bandwidth))) / (1-Math.exp(-alpha1*(max_bandwidth - userBandwidth))));        
		}   
		    else {
		    bandwidthUtility = 2.55*MOS;     
		}
		if (delay <= min_delay){
			delayUtility = 1;
		}
		else{ 
			delayUtility = Math.exp(-delay*alpha3);    
		}
		priceUtility = max_satisfaction - ((max_satisfaction / 1-Math.exp(val)) * Math.exp(- vnfPrice*alpha2));
		this.vnfReputationForUser = vnfReputation * alpha4;
		this.userUtility = bandwidthUtility * delayUtility * priceUtility + vnfReputationForUser;
//		this.userUtility = Math.log(alpha1 * userBandwidth)  - (vnfPrice * alpha2) - (delay * alpha3) + vnfReputation;		
    }
	
//public void calculateUserUtility(double userBandwidth, double delay, double vnfPrice, double vnfReputation, double alpha1, double alpha2, double alpha3, double alpha4){
//		this.bandwidthUtility = MOS * ((1-Math.exp(-alpha1*(userBandwidth - min_bandwidth))));
//		if (delay <= min_delay){
//			this.delayUtility = 1;
//		}
//		else{ 
//			this.delayUtility = Math.exp(-delay*alpha3);    
//		}
//		this.priceUtility = Math.exp(-vnfPrice*alpha2);
//		
//		this.vnfReputationForUser = vnfReputation * alpha4;
//		this.userUtility = this.bandwidthUtility * this.delayUtility * this.priceUtility + this.vnfReputationForUser;
////		Console.output(" this.bandwidthUtility "+ this.bandwidthUtility +"delayUtility:"+delayUtility +" priceUtility:"+this.priceUtility +" vnfReputationForUser:"+vnfReputationForUser);
////		Console.output(this.userId+" "+this.assignedVNFServerId+" user bandwidth:"+userBandwidth +" utility:"+this.userUtility);
//		//this.userUtility2 = Math.log(alpha1 * userBandwidth)  - (vnfPrice * alpha2) - (delay * alpha3) + vnfReputation;
//		//Console.output("orig userUtility:"+userUtility +"new utility:"+this.userUtility2);
//    }

	public Mood getUserMood() {
		return this.currentMood;
	}
	
	boolean experimentDone = false;
	int changeTrigger = 0;
	private void trialAndError() {
		
		double change = 0;
	
		Random rand = new Random(); // random number generator
		//Console.output("START: "+this.userId+", user utility:"+this.userUtility  +" benchmarkUtility:"+benchmarkUtility);
		benchmarkUtility = getLatestBestBenchmark(this.assignedVNFServerId);
		// calculate the difference between currentBenchmark and bestBenchmark
		change = Double.compare(this.userUtility, benchmarkUtility); // compare it with the best available one
		//Console.output("START: "+this.userId+" current action:"+assignedVNFServerId+" mood:"+this.currentMood+", user utility:"+this.userUtility  +" benchmarkUtility:"+benchmarkUtility+" change: "+change );
		switch(currentMood) {
		case CONTENT:
			this.previousMood = this.currentMood;
			if(benchmarkAction.equalsIgnoreCase(this.assignedVNFServerId)) {
				if (change < 0) {
					this.currentMood = Mood.WATCHFUL;
					//Console.output(this.userId + " WATCHFUL in CONTENT");
				} else if (change == 0) {
					this.currentMood = Mood.CONTENT;
				} else {
					this.currentMood = Mood.HOPEFUL;
				}
				if(initialCondition) {
					this.benchmarkUtility = this.userUtility;
					this.benchmarkAction = this.assignedVNFServerId;	
					this.updateBestBenchmarkForAction(benchmarkAction);
				}
				if (currentMood == Mood.CONTENT) {
					// update here the experiment probability
					double randomValue = rand.nextDouble();
					if( this.experimentProbability > randomValue  ){
						Console.output("EXPERIMENT:"+ this.userId +" is taking new experiment in CONTENT Mood, rv:" + randomValue);
						this.assignedVNFServerId = experimentAnAction(); // select an action from the action list
					}	
				}
			}else { // new action is taken  -> experimentDone
				//Console.output( " change: " + change);
				if(change <= 0  ) {
					this.assignedVNFServerId = benchmarkAction;
					//Console.output(this.userId + " CONTENT in CONTENT");
				} else if (change > 0 ) {
					acceptanceProbability =  Math.pow(experimentProbability, -v1*(this.userUtility-this.benchmarkUtility)+v2);
					double random = rand.nextDouble() ;
					Console.output("acceptanceProbability:"+acceptanceProbability +" random:"+random);
					if (acceptanceProbability > random ){
						//Console.output(this.userId +" CONTENT in CONTENT");
						this.benchmarkUtility = this.userUtility;
						this.benchmarkAction = this.assignedVNFServerId; // new action is assigned to the benchmark action
						this.updateBestBenchmarkForAction(benchmarkAction);
					}else {
						this.assignedVNFServerId = benchmarkAction;	
					}
				}
				this.currentMood = Mood.CONTENT;
			}
			break;
		case WATCHFUL:
			// user calls the same benchmarkAction
			this.previousMood = this.currentMood;
			if(change < 0) {
				//if(changeTrigger == 3) {
					this.currentMood = Mood.DISCONTENT;
				//	changeTrigger = 0;
				//}else {
				//	changeTrigger++;
				//}
			} else if (change == 0) {
				this.currentMood = Mood.CONTENT;
			} else {
				this.currentMood = Mood.HOPEFUL;
			}
			break;
		case HOPEFUL:
			// user calls the same benchmarkAction
			this.previousMood = this.currentMood;
			if(change < 0){
				this.currentMood = Mood.WATCHFUL;
			} else if(change == 0) {
				this.currentMood = Mood.CONTENT;
			} else {
				this.currentMood = Mood.CONTENT;
				this.benchmarkUtility = this.userUtility;
				this.updateBestBenchmarkForAction(benchmarkAction);
				//Console.output("CONTENT in HOPEFUL");
			}
			break;
		case DISCONTENT:
			this.previousMood = this.currentMood;
			boolean result = false;
			if (experimentDone) {
				result = this.responseFunction(this.userUtility);
				experimentDone = false;
			}
			if(!result) {
				this.currentMood = Mood.DISCONTENT;
				experimentDone = false;
			} else {
				this.currentMood = Mood.CONTENT;
				this.benchmarkUtility = this.userUtility;
				this.benchmarkAction = this.assignedVNFServerId;
				this.deleteBestBenchmarkForAction();
				this.updateBestBenchmarkForAction(this.benchmarkAction);
				experimentDone = false;
			}
			if(this.currentMood == Mood.DISCONTENT) {
				//Console.output(this.getUserIdentity()+"NEW EXPERIMENT IN DISCONTENT");
				this.assignedVNFServerId = experimentAnAction();
				this.experimentDone  = true;
			}
			
			break;
			
		default:
			// do nothing
			System.out.println("switch case default in trialAndError");
		}
		//Console.output("END: "+this.userId+ " action:"+this.assignedVNFServerId+" mood:"+this.currentMood+", user utility:"+this.userUtility  +" benchmarkUtility:"+benchmarkUtility);
	}

	// another approach can be taken here for the selection of new experiment!
	private String experimentAnAction() {
		// select here just one of new experiment to be taken.
		String currentAction = "";
		ArrayList<String> actionList = nservice.getListOfVNFs();
		Random rand = new Random();
		if(actionList.size() > 0) {
			String action = "";
			//Console.output("old action:" +assignedVNFServerId);
			boolean isActionDifferent = false;
			while(!isActionDifferent) {
				action = actionList.get(rand.nextInt(actionList.size()));
				if(!action.equalsIgnoreCase(this.assignedVNFServerId)) {
					isActionDifferent = true;
					currentAction = action;
				}
			}
		}		
		return currentAction;
	}
	
	private synchronized void deleteBestBenchmarkForAction() {
		actionResultHistory.clear();
	}
	
//	private synchronized void deleteBestBenchmarkForAction(String action) {	
//		for (Iterator<ActionResult> it = actionResultHistory.iterator(); it.hasNext(); ) {
//			ActionResult ar = it.next();
//			if(it.hasNext()) {
//				it.remove();
//			}
//			//if(ar.action.equalsIgnoreCase(action)) {
//		    //    it.remove();
//		    //}
//		}
//		Console.output("Benchmark is deleted, action history" + this.actionResultHistory.size());
//	}
	
	private void updateBestBenchmarkForAction(String action) {
		boolean notAvailable = true;
		for(ActionResult ar: this.actionResultHistory) {
			if(ar.action.equalsIgnoreCase(action)) {
				ar.benchmarkUtility = new Payoff(this.benchmarkUtility);
				notAvailable = false;
			} 
		}		
		if(notAvailable) {
			this.actionResultHistory.add(new ActionResult(this.previousMood,this.currentMood,this.benchmarkAction, new Payoff(this.benchmarkUtility)));
		}
		//Console.output("end: Benchmark is updated, action history" + this.actionResultHistory.size() );
	}
	
	private double getLatestBestBenchmark(String action) {
		for(ActionResult ar: this.actionResultHistory) {
			if(ar.action.equalsIgnoreCase(action)) {
				return ar.benchmarkUtility.getUserPayoff();
			} 
		}	
		if(initialCondition) {
			//Console.output("initial condition");
			//return this.userUtility;
			return 0.6;
		}
		return this.benchmarkUtility;
	}
	
	//private void updateBestBenchmarkForAction(String previousMood, String currentMood, String action, String utility) {}
	public boolean responseFunction(double userUtility) {
		Random rand = new Random(); // random number generator
		double acceptSearchValueForUser = -phi_1*userUtility+phi_2; 
		if ( rand.nextDouble() <= Math.pow(experimentProbability, acceptSearchValueForUser)) {
			return true;
		}
		return false;
	}

	public void writeParameters() {
		StringBuilder sb = new StringBuilder();
		sb.append("----PARAMETERS----")
			.append("\nalpha1: ")
			.append(nservice.serviceQoSType.alpha1+"")
			.append("\nalpha2: ")
			.append(nservice.serviceQoSType.alpha2)
			.append("\nalpha3: ")
			.append(nservice.serviceQoSType.alpha3)
			.append("\n---- USER INFO ----\n")
			.append("iteration | uid | current-vnf | mood | currentBenchmark | otherBenchmark: \n");

		fio.appendToFile(userFolderPath+"/"+USER_MOODS_FILENAME, sb.toString());	
	}
	
	private void saveInFile(int currentGameNumber, String userIdentity, Mood currentMood, String currentAction, double userutility,
			String benchmarkAction, double benchmarkUtility) {
    	StringBuilder sb = new StringBuilder();
		sb.append(currentGameNumber+",")
			.append(userIdentity+",")
			.append(currentMood+",")
			.append(currentAction+",")
			.append(userutility+",")
			//.append(this.userUtility2+",")
			.append(benchmarkAction+",")
			.append(benchmarkUtility);
		fio.appendToFile(userFolderPath+"/"+USER_MOODS_FILENAME, sb.toString());	
	}
	
	private void writeLogs(){
		StringBuilder userLog = new StringBuilder();
		userLog.append(nservice.getCurrentGameIterationNumber())
			.append(",")
			.append(this.userId)
			.append(",")
			.append(this.userUtility)
			.append(",")
			.append(this.assignedVNFServerId)
			.append(",")
			.append(this.currentMood)
			.append(",");
		fio.appendToFile(userFolderPath+"/"+USER_MOODS_FILENAME, userLog.toString());
		
	}

	public double getUserUtility() {
		return this.userUtility;
	}
	
	public void setUserIdentity(String userId){
		this.userId = userId;
	}
	
	public String getUserIdentity() {
		// TODO Auto-generated method stub
		return this.userId;
	}
	
	public double getDelay() {
		return delay;
	}
	
	public void setDelay(double delay) {
		this.delay = delay;
	}
	
	public double getBw() {
		return bandwidth;
	}
	
	public void setBw(double bw) {
		this.bandwidth = bw;
	}
	
	public double getPrice() {
		return vnfPrice;
	}

	public void setPrice(float price) {
		vnfPrice = price;
	}

	public void setUserUtility(float userUtility) {
		this.userUtility = userUtility;
	}

	public double getBenchmarkUtility() {
		return this.benchmarkUtility;
	}
	

	
}
