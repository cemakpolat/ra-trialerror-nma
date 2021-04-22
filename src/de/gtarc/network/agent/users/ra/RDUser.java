package de.gtarc.network.agent.users.ra;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.Util;

import de.gtarc.network.vnf.VNF;

/**
 * 
 * Service user class
 * 
 * @author cemakpolat, doruksahinel
 *
 */
public class RDUser implements Runnable {
	public static RDService service = null;
	
	// The bandwidth assigned to user 
	private double bandwidth;
	// The price assigned by the VNF server 
	private double price;
	// Average utility of all users connected to the smart city service 
	public double averageUserUtility = 0;
	// User's utility at current iteration
	public double userUtility = 0;
	public double userUtility2 = 0;
	// ID of the VNF server that the user is connected to 
	public String assignedVNFServerId = "";
	// ID of the smart city service that the user is connected to 
	public String serviceId = "";
	// Path delay experienced by the user 
	private double delay;
	// The ID of the user
	private String userId = "";
	// public static boolean equilibriumState = false;
	// The ID of the user group that is connected to the same VNF server 
	public String groupId = "";
	// The difference between the user's utility and the average user utility 
	public double rateOfChange = 0;
	// The switch probability of user from a server to another server
	public double switchProbability = 0;
	private Util util = new Util();
	private FileIO fio = new FileIO();
	public static String userFolderPath = "";
	public String userfile ="";
	
	/**
	 * Utility Parameters from Manzoor Paper	
	 * This function calculates the user utility value.
	 */
	// Bandwidth utility parameters
	double min_bandwidth = 1; // min_bandwidth_requirement
	double max_bandwidth = 580; // max_bandwidth_requirement
	int MOS = 20; // mean opinion score
	double bandwidthUtility = 0; 
	
	//Delay utility parameters
	double min_delay = 10; // min_bandwidth_requirement
	double delayUtility = 0;
	
	//Price utility parameters
	double val = 1; //private_valuation of the service by the user
	double max_satisfaction = 1; // maximum satisfaction of user based on price
	double priceUtility = 0;
	double vnfReputationForUser = 0;
		
	double stabilityConstant = 1; // stability constant
	
	public RDUser() {}
	public RDUser(RDService s, String userId ) {
		service = s;
		this.userId = userId;
	}

	/**
	 * This function creates a user with a user ID and a VNF server group.
	 */
	public RDUser(RDService rdService, String userId, String vnfId) {
		service = rdService;
		this.userId = userId;
		this.assignedVNFServerId = vnfId;
		userFolderPath = RDService.folderPath+"/users";
		createMeasurementFiles();
	}
	
	public void createMeasurementFiles() {
		userfile = this.getUserIdentity()+".txt";
		fio.createNewFile(userFolderPath, userfile);
		
	}
	public void saveUserParams(int iteration, double utility, double bw, String selectedvnfid) {
		//Console.output(""+iteration +","+utility+","+bw+","+selectedvnfid);
		fio.appendToFile(userFolderPath+"/"+userfile, iteration +","+utility+","+bw+","+selectedvnfid );
	}

	/**
	 * This function starts after creating the user. At each new iteration, it first
	 * calculates the user utility and then calls the learning algorithm to decide
	 * for the VNF server to be used in the next iteration.
	 */
	
	@Override
	public void run() {
		int previousGameNumber = 0;
		int currentGameNumber = 0;
		//String selectedAlgorithm = "replicatorCalculation";
		String selectedAlgorithm = "averagePayoffComparison";

		while (service.isGameContinue()) {
			currentGameNumber = service.getCurrentGameIterationNumber();
			if (currentGameNumber - previousGameNumber > 0 ) {
				calculateUserUtility(service.getUserBandwidth(this.assignedVNFServerId),
						service.getPathDelay(this.assignedVNFServerId), service.getVNFPrice(assignedVNFServerId),
						service.getVNFReputation(assignedVNFServerId),
						service.serviceQoSType.alpha1, service.serviceQoSType.alpha2, service.serviceQoSType.alpha3, service.serviceQoSType.alpha4);
				executeSelectedAlgorithm(selectedAlgorithm);
				userDecision(selectedAlgorithm, this.rateOfChange, service.getVNFList(),
						service.getUserBandwidth(this.assignedVNFServerId),
						service.getPathDelay(this.assignedVNFServerId), service.serviceQoSType.alpha1,
						service.serviceQoSType.alpha2, service.serviceQoSType.alpha3);
				
				service.updateGameTable(this, this.assignedVNFServerId);
				previousGameNumber = currentGameNumber;
				//Console.output(this.getUserIdentity()+"->previousGameNumber:" +previousGameNumber);
				this.saveUserParams(currentGameNumber,this.userUtility, this.bandwidth, this.assignedVNFServerId);
			}			
			util.sleep(20);
		}
		System.out.println("User thread left the game!");
	}

	/**
	 * This function is used to decide for the user's VNF selection algorithm .
	 */
	private void executeSelectedAlgorithm(String alg) {
		switch (alg) {
		case "replicatorCalculation":
			replicatorCalculation();
			break;
		case "averagePayoffComparison":// request max user utility
			averagePayoffComparison();
			break;
		case "randomVNFSelection":
			randomVNFSelection();
			break;
		default:
			replicatorCalculation();
			break;
		}
	}

	private void randomVNFSelection() {
		if (service.getAverageUserUtility() != 0) {
			rateOfChange = this.userUtility - service.getAverageUserUtility();
		} else
			throw new IllegalArgumentException("Argument 'divisor' is 0");
	}

	/**
	 * This function compares user utility to average user utility to decide for the
	 * VNF server to be used in the next round.
	 */
	// algorithm 2
	public void averagePayoffComparison() {
		if (service.getAverageUserUtility() != 0) {
			rateOfChange = this.userUtility - service.getAverageUserUtility();

		} //else
		//	throw new IllegalArgumentException("Argument 'divisor' is 0");
	}
	

	public void calculateUserUtility(double userBandwidth, double delay, double vnfPrice, double vnfReputation, double alpha1, double alpha2, double alpha3, double alpha4){
		
		this.bandwidthUtility = MOS * ((1-Math.exp(-alpha1*(userBandwidth - min_bandwidth))));
		if (delay <= min_delay){
			this.delayUtility = 1;
		}
		else{ 
			this.delayUtility = Math.exp(-delay*alpha3);    
		}
		
		this.priceUtility = Math.exp(-vnfPrice*alpha2);
		
		this.vnfReputationForUser = vnfReputation * alpha4;
		this.userUtility = this.bandwidthUtility * this.delayUtility * this.priceUtility + this.vnfReputationForUser;
		Console.output(" this.bandwidthUtility "+ this.bandwidthUtility +"delayUtility:"+delayUtility +" priceUtility:"+this.priceUtility +" vnfReputationForUser:"+vnfReputationForUser);
		//Console.output("user bandwidth:"+userBandwidth +" utility:"+this.userUtility);
		//this.userUtility2 = Math.log(alpha1 * userBandwidth)  - (vnfPrice * alpha2) - (delay * alpha3) + vnfReputation;
		Console.output("orig userUtility:"+userUtility +"new utility:"+this.userUtility2);
    }
	/**
	 * This function compares user utility to a random user's utility to decide for
	 * the VNF server to be used in the next round.
	 */
	public void replicatorCalculation() {
		RDUser user = service.getAssignedUser(this);
		if (user != null) {
			rateOfChange = this.userUtility - user.getUserUtility();
			//Console.output("user1 util:"+this.userUtility + " user2 util:"+ user.getUserUtility());
			if (rateOfChange < 0) {
				this.assignedVNFServerId = user.assignedVNFServerId;
			}
		}
	}

	/**
	 * With this function, the user decides for the VNF server to be used in the
	 * next iteration.
	 */
	public String userDecision(String algorithm, double rateOfChange, List<VNF> allVNFServerList, double userBandwidth, double delay, double alpha1, double alpha2, double alpha3) {
		
		List<Double> userUtilityList = new ArrayList<Double>();
		
		if (rateOfChange < 0) { // user utility is less than the average utility
			if (algorithm.equalsIgnoreCase("averagePayoffComparison")) {
				double random = new Random().nextDouble(); //Util.getRandomNumber(0,0.3) 
				if (this.userUtility > 0) {
					//switchProbability = (service.getAverageUserUtility() - this.userUtility) / service.getAverageUserUtility();
					switchProbability = (stabilityConstant)*(service.getAverageUserUtility() - this.userUtility) / service.getAverageUserUtility();
					//System.out.println("Switch Probability is: " + switchProbability + " random "+random);
					if (random <= switchProbability) {
						String oldVNFID = this.assignedVNFServerId;	
						this.assignedVNFServerId = this.getMaximumUserUtility(RDService.users);
						//System.out.println("1 oldVNFID: " + oldVNFID+ " newVNFID: " + this.assignedVNFServerId);
					}
				} else if (this.userUtility < 0 && service.getAverageUserUtility()  < 0) {
					switchProbability =(stabilityConstant)* Math.abs((service.getAverageUserUtility() - this.userUtility) / this.userUtility);
					if (random<= switchProbability) {
						this.assignedVNFServerId = this.getMaximumUserUtility(RDService.users);
						//System.out.println("2 oldVNFID: " + oldVNFID+ " newVNFID: " + this.assignedVNFServerId);
					}
				}else if (this.userUtility < 0 && service.getAverageUserUtility()  > 0) {
					switchProbability = (stabilityConstant)*(service.getAverageUserUtility() - Math.abs(this.userUtility) / service.getAverageUserUtility()); // 0.36
					if (random<= switchProbability) {
						this.assignedVNFServerId = this.getMaximumUserUtility(RDService.users);
						//System.out.println("2 oldVNFID: " + oldVNFID+ " newVNFID: " + this.assignedVNFServerId);
					}
				}
			}
			else if (algorithm.equalsIgnoreCase("randomVNFSelection")) {
				if (this.userUtility > 0) {
					//switchProbability = (service.getAverageUserUtility() - this.userUtility) / service.getAverageUserUtility();
					switchProbability = (stabilityConstant)*(service.getAverageUserUtility() - this.userUtility) / service.getAverageUserUtility();
					if (new Random().nextDouble() <= switchProbability) {	
						this.assignedVNFServerId = this.getRandomVNFSelection(userUtilityList);
					}
				} else if (this.userUtility < 0) {
					//switchProbability = Math.abs((service.getAverageUserUtility() - this.userUtility) / this.userUtility);
					switchProbability =(stabilityConstant)* Math.abs((service.getAverageUserUtility() - this.userUtility) / this.userUtility);
					//System.out.println("Switch Probability is: " + switchProbability);
					if (new Random().nextDouble() <= switchProbability) {
						this.assignedVNFServerId = this.getRandomVNFSelection(userUtilityList);
					}
				}
			}
			return this.assignedVNFServerId;

		}
		return this.assignedVNFServerId;
	}

	private String getRandomVNFSelection(List<Double> userUtilityList) {
		int vnfSize = service.getListOfVNFs().size();
		int selectedVNFID = getRandomNumberInRange(0, vnfSize);
		return service.getListOfVNFs().get(selectedVNFID);
	}
	
	private static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

	/**
	 * Get the maximum utility value that a user can get and select the VNF server
	 * that provides this utility value.
	 */
	private String getMaximumUserUtility(ArrayList<RDUser> users) {
		double userUtility = 0;
		String vnfIndex = "";
		for (int i = 0; i < users.size(); i++) {
			if (userUtility < users.get(i).getUserUtility()) {
				userUtility = users.get(i).getUserUtility();
				vnfIndex = users.get(i).assignedVNFServerId;
			}
		}
		return vnfIndex;
	}

	public double getUserUtility() {
		return this.userUtility;
	}

	public void setUserIdentity(String userId) {
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

	public void setBandwidth(double bw) {
		this.bandwidth = bw;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setUserUtility(double userUtility) {
		this.userUtility = userUtility;
	}
}
