package de.gtarc.network.vnf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtarc.network.config.ServiceTestConfig;
import com.gtarc.network.config.VNFTestConfig;
import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.Util;

import de.gtarc.network.agent.no.NetworkManager;

/**
 * VNF server class
 * 
 * @author cemakpolat, doruksahinel
 *
 */
public class VNF implements Runnable {
	private String VNFTYPE = "Firewall";
	/** The type of the network function */
	private String nodeName;
	/** The name of the VNF server */
	private double cpu;
	/** The cpu of the VNF server */
	private double currentPricePerUser;
	/** VNF server price per user */
	String vnfId = "";
	/** The unique ID of the VNF server */

	private double assignedBandwidth = 0;
	/** The bandwidth assigned to VNF server by the Network Operator */
	private double bandwidthCost = 0;
	/** Total bandwidth cost of the VNF server */
	private double bandwidthUnitPrice = 0;
	/** Unit cost of bandwidth decided by the Network Operator */
	private double delay = 0;
	/** The delay value of the link assigned to the VNF server */
	private double reputation = 0;
	/** The delay value of the link assigned to the VNF server */

	private double beta1 = 0;
	/** Weight of revenue in VNF utility calculation */
	private double beta2 = 0;
	/** Weight of cost in VNF utility calculation */
	VNFTestConfig config = null;
	/** Config file used to read initial VNF values */

	private double revenue = 0;
	/** Revenue of VNF server at the current iteration of the game */
	// private ArrayList<User> userList = new ArrayList<User> (); /** The list of
	// users attached to the VNF server*/
	private int userSize = 0;
	/** Total number of users attached to the VNF server */
	private int totalUserNumber = ServiceTestConfig.totalUserNumber;
	/** Total number of service users at initial configuration */

	private double vnfUtilityValue;
	/** VNF server utility at the current iteration of the game */
	private double priceIncrease = 0;
	/**
	 * In price test, this parameter increases the price per user at each iteration
	 */
	private boolean isPriceIncreasing = false;
	/** This parameter is true if there is a price test */
	// operation cost
	double operatingCost = 0;
	/** Operating and maintenance costs of the VNF server */
	// ArrayList<Service> servicesBeingServed = new ArrayList<Service> (); /** The
	// list of smart city services using the VNF server*/
	private String serviceId = "";
	/** The unique ID of the smart city service connected to the VNF server */

	final Logger logger = LoggerFactory.getLogger(VNF.class);

	private int gameIterationNumber = 0;
	/** Current iteration of the game */

	private Util util = new Util();

	FileIO fio = new FileIO(); // file operation
	public static String folderPath = "results";
	/** Parameters used in reinforcement learning algorithm */
	private double score_i = 0;
	private double stepSize = 0;
	private double requestBandwidth = 0;

	public VNF() {}

	/**
	 * This function creates a new VNF server
	 */
	public VNF(VNFTestConfig config, String vnfId) {
		this.vnfId = vnfId;
		this.config = config;
		this.beta1 = config.getBeta1();
		this.beta2 = config.getBeta2();
		this.bandwidthUnitPrice = config.getBandwidthUnitPrice();
		this.assignedBandwidth = config.getAssignedBandwidth();
		this.userSize = config.getTotalUserNumber();
		this.priceIncrease = config.getPriceIncrease();
		this.isPriceIncreasing = config.isPriceIncreasing();
		this.currentPricePerUser = config.getInitialUserPrice();
		this.delay = config.getDelay();
		this.reputation = config.getReputation();
		this.createVNFMeasurementFiles();
	}

	/**
	 * This function starts after creating the VNF server. At each new iteration, it
	 * first calls the learning algorithm and then updates VNF server parameters
	 * used by the network manager.
	 */
	boolean loop = true;

	@Override
	public void run() {
		saveUserDistribution();
		while (loop && NetworkManager.getInstance().isRunning()) {
			if (gameIterationNumber < NetworkManager.getInstance().getGameIterationNumber()) {
				Console.output(this.vnfId+" is running for the "+gameIterationNumber+". game");
				gameIterationNumber = NetworkManager.getInstance().getGameIterationNumber();
				this.reinforcementLearning();
				NetworkManager.getInstance().updateGameTable(this);
			}
			util.sleep(10);
		}
		loop = false;
	}

	/**
	 * Simple getter to receive the number of users connected to the VNF server.
	 * 
	 * @return userSize
	 */
	public int getTotatAssignedUserNumber() {
		this.userSize = NetworkManager.getInstance().getTotalAssignedUserNumber(this.vnfId);
		return this.userSize;
	}

	/**
	 * This function sends a bandwidth request to the network operator.
	 */
	public void requestBandwidth(double requestedBandwidth) {
		RequestResult result = NetworkManager.getInstance().requestBandwidth(this.serviceId, this.vnfId,
				requestedBandwidth);
		if (result.accepted) {
			Console.output(this.vnfId+" "+this.requestBandwidth);
			this.assignedBandwidth = result.requestedBandwidth;
			this.bandwidthUnitPrice = result.bandwidthUnitPrice;
			this.bandwidthCost = result.bandwidthUnitPrice * result.requestedBandwidth;

		} else {
			System.out.println("Bandwidth request is rejected!!!");
		}
	}

	/**
	 * This function calculates VNF utility at each iteration of the game.
	 * TODO: Price can change according to the VNF Server strategies, we may think of this strategies...
	 */
	public void calculateVNFUtility() {
		// Calculate bandwidth cost:
		bandwidthCost = assignedBandwidth * bandwidthUnitPrice;
		// Calculate revenue:
		this.revenue = currentPricePerUser * this.getTotatAssignedUserNumber();
		// Calculate utilityValue:// + betha3*operatingCost+...
		vnfUtilityValue = beta1 * revenue - (beta2 * (bandwidthCost + operatingCost));
		// Console.output(this.vnfId+" "+"bandwidthCost:"+bandwidthCost +",revenue "+revenue +", usernumber"+this.userSize);
		Console.output(this.vnfId + " " + "UtilityValue:" + vnfUtilityValue);

	}

	/**
	 * This function calculates the marginal utility used in reinforcement learning
	 * algorithm.
	 */
	public double calculateMarginalUtility() {
		
		double priceTilda = totalUserNumber * (beta1 * currentPricePerUser - beta2 * operatingCost);
		if (priceTilda <= 0) {
			this.loop = false;
			Console.output("Price is " + priceTilda + " game over");
		}
		double PI_i = beta2 * bandwidthUnitPrice;
		double auxiliaryDelayPrice = 1;		
		///double totalVNFBandwidth = NetworkManager.getInstance().getTotalBandwidth();
		double totalVNFBandwidth = NetworkManager.getInstance().getTotalBandwidth(this.vnfId);
		double totalVNFBandwidthExceptOwnVNF = totalVNFBandwidth - this.assignedBandwidth * auxiliaryDelayPrice;// auxiliaryDelayPrice == 1
		// marginal utility
		double marginalUtility = priceTilda * (totalVNFBandwidthExceptOwnVNF / (totalVNFBandwidth * totalVNFBandwidth)) - PI_i;
		 Console.output(this.vnfId+" marginalUtility:"+marginalUtility);
		//double totalVNFUtility = NetworkManager.getInstance().calculateTotalVNFUtility();
		//double totalVNFUtilityExceptOwnVNF = NetworkManager.getInstance().calculateTotalVNFUtility()- this.vnfUtilityValue;
		//double marginalUtility = priceTilda * (totalVNFUtilityExceptOwnVNF / (totalVNFUtility * totalVNFUtility)) - PI_i;
		return marginalUtility;
	}

	/**
	 * This function implements exponential reinforcing learning algorithm.
	 */
	public void reinforcementLearning() {
		stepSize = (double) (1.0 / gameIterationNumber);
		double maximumAvailableBandwidthForVNF = NetworkManager.getInstance().getMaximumAvailableBandwidthFor("VNF-ID");
		double marginalUtility = this.calculateMarginalUtility();
		score_i = score_i + (stepSize * (marginalUtility));
		// link capacity toward VNF link/path B_i
		requestBandwidth = maximumAvailableBandwidthForVNF * (Double) ((Math.exp(score_i) / (1 + Math.exp(score_i))));
		//Console.output("VNF requested bandwidth:"+ maximumAvailableBandwidthForVNF +" "+score_i+" "+(Math.exp(score_i) / (1 + Math.exp(score_i))));
		this.requestBandwidth(requestBandwidth);
		this.saveUserDistribution();
		this.calculateVNFUtility();
		this.setNewPrice();
	}

	/**
	 * Create text file for requested bandwidth
	 */
	public void createVNFMeasurementFiles() {
		fio.createNewFile(folderPath, vnfId + "_bandwidthRequest.txt");
	}

	public void saveUserDistribution() {
		String	bandwidthString = gameIterationNumber + "," + requestBandwidth + "," + vnfUtilityValue + ','+ this.currentPricePerUser + "," + this.score_i;
		fio.appendToFile(folderPath + "/" + vnfId + "_bandwidthRequest.txt", bandwidthString);
	}

	/**
	 * This function is used in a test in which the VNF servers price is constantly
	 * increasing at each iteration.
	 * TO DO - DORUK: Add the new algorithm where p(t+1)is adjusted based on the
	 */
	public double setNewPrice() {
		if (this.isPriceIncreasing) {
			this.currentPricePerUser = this.currentPricePerUser + (this.config.getPriceIncrease());
		}
		return this.currentPricePerUser;
	}

	/**
	 * This function calculates the operational costs of the VNF server at each
	 * iteration.
	 * TODO:add here the operation based on the energy cost
	 * 	use CPU in the calculation of the operation cost...
	 */
	public void VNFOperatingCost() {}

	/**
	 * This function maximizes revenue by adjusting price per user
	 */
	public void revenueMaximization() {
	}

	public double getBandwidthUnitPrice() {
		return this.bandwidthUnitPrice;
	}

	public String getVNFId() {
		return this.vnfId;
	}

	public double getCurrentPricePerUser() {
		return this.currentPricePerUser;
	}

	public double getDelay() {
		return this.delay;
	}

	public String getVNFType() {
		return this.VNFTYPE;
	}

	public double getCurrentBandwidth() {
		return this.assignedBandwidth;
	}

	public double getCurrentRevenue() {
		return this.revenue;
	}

	public double getCurrentVNFUtility() {
		return this.vnfUtilityValue;
	}

	public double currentAverageUserUtility = 0;

	public void setCurrentAverageUserUtility(double currentAverageUserUtility) {
		this.currentAverageUserUtility = currentAverageUserUtility;
	}

	public double getCurrentAverageUserUtility() {
		return currentAverageUserUtility;
	}

	public int getAssignedUserNumber() {
		return this.userSize;
	}

	public double getActualReputation() {
		return this.reputation;
	}

	public void setReputation(double reputation) {
		this.reputation = reputation;
	}
}