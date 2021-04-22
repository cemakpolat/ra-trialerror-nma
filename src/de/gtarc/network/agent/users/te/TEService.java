package de.gtarc.network.agent.users.te;


import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.ServiceQoSType;
import com.gtarc.network.utils.Util;

import de.gtarc.network.agent.no.NetworkManager;
import de.gtarc.network.service.common.Service;
import de.gtarc.network.service.common.ServiceNetworkSliceManager;
import de.gtarc.network.service.common.UserGame;
import de.gtarc.network.service.common.VNFUser;
import de.gtarc.network.service.common.VNFUsers;
import de.gtarc.network.vnf.VNF;
import de.gtarc.network.vnf.VNFPath;
/**
 * 
 * @author cemakpolat, doruksahinel
 *
 */
public class TEService implements Runnable, Service
{
	final Logger logger = LoggerFactory.getLogger(TEService.class);
	
	private static ServiceNetworkSliceManager nsliceManager = null; 	/** The network slice of the smart city service*/
	
	// control loops
	static boolean  serviceIsRunning = true;
	
	private String serviceIdentifier ="te-service"; 					/** The name of the smart city service*/
	public ServiceQoSType serviceQoSType; 								/** QoS type of the smart city service, e.g, xMBB, mMTC, uMTC*/  
	
	static ArrayList<TEUser> users = new ArrayList<TEUser>(); 			/** The list of users connected to the smart city service*/

	// VNF|User assignments
	List<VNFUser> vnfUserList = new ArrayList<VNFUser>(); 				/** List of users connected to the service provider*/
	
	// intern game iteration number
	static int gameIterationNumber = 0;
	
	//one-to-one user games
	public static List<UserGame> userGames = new ArrayList<UserGame> (); /** Internal replicator games that the users play among each other*/
		
	ArrayList<Double> vnfBandwidthAmounts = new ArrayList<Double>();
	static ArrayList<Double> welfare = new ArrayList<Double>();
	ArrayList<PayoffMatrix> payOffMatrixes = new ArrayList<PayoffMatrix>();
	
	private Util util = new Util();
	
	// file operations
	private static FileIO fio = new FileIO(); 	
	public static String folderPath = NetworkManager.folderPath+"/trialanderror";
	public static String currentUserDistribution = "cu_userdistribution";
	String WELFARE_RESULTS="welfare_results";
	
	public int totalUserNumber = 30; 	// default
	public int totalVNFNumber = 2; 		// default
	public int iterationLimitForUsers = 100; 	// default
	public double userDistrubitionRate = 0.75;  

	public int vnfBandwidthDecision = 0; 
	public double vnfDefaultPrice = 1;
	public double vnfDefaultDelay = 1;
	public double vnfReputation = 0;
	
	private static TEService service = null;
	
	public TEService() {}
	  
	public static TEService getInstance() {
	     if(service == null) {
	    	 service = new TEService();
	     }
	     return service;
	}
	
	/**
	 * This function maps the service to a network slice and 
	 * starts the evolutionary game between users.
	 */
	public void initialSetup(){
		fio.createFolder(folderPath+"/users");
		
		nsliceManager = ServiceNetworkSliceManager.getInstance();
		nsliceManager.setServiceRequirements(this.serviceIdentifier, serviceQoSType);
		
		if(nsliceManager.createNetworkSlice()) {
			this.addUsers(totalUserNumber);
			this.createMeasurementFiles();
			for(int i = 0; i < users.size(); i ++){
				this.vnfUserList.add(new VNFUser(users.get(i).getUserIdentity(), users.get(i).assignedVNFServerId));
			}	
			nsliceManager.updateVNFUserTable(calculateVNFUserNumbers());

		}else {
			Console.output("Network Slice cannot be created!");
		}
	}
	
	public void addUsers(int totalUserNumber){
		int vnf1userNumber = (int)(totalUserNumber*userDistrubitionRate);
		int vnf1usercounter = 0;
		List<String> vnfList = this.getListOfVNFs();
		for (int i = 0 ; i < totalUserNumber ; i++){
			TEUser user = null;
			if(vnf1usercounter < vnf1userNumber) {
				user = new TEUser(this, "user_"+i,vnfList.get(0));
				vnf1usercounter ++;
			}else {
				user = new TEUser(this, "user_"+i,vnfList.get(1));
			}
			
			Thread userThread = new Thread(user);
			userThread.start();
			users.add(user); 	
		}
		//Console.output("vnf1 assigned user number:"+ vnf1usercounter);
		
	}
 	
	/**
	 * This function is used to print the decisions 
	 * of users after the game. 
	 */
	
	@Override
	public void run() {
		Console.output("service is started!");
		initialSetup();
		while(NetworkManager.getInstance().isRunning()){
			if(nsliceManager.isVNFPathTableUpdated()){
				nsliceManager.updateNetworkSlice();
				nsliceManager.updateVNFPathTableStatus();
				runUsersGame();		
				nsliceManager.updateVNFUserTable(calculateVNFUserNumbers());
			}
			util.sleep(100);
		}
		serviceIsRunning = false;
		Console.output("service is stopped!");
	}
	
	public void runUsersGame() {
		int iteration = 0;
		while(iteration < iterationLimitForUsers) {
			userGames = getUserGameList();
			gameIterationNumber++;
			Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
			while(!checkAllUsersArePlayed()){
				this.util.sleep(100);
			}
			this.util.sleep(100);
			this.saveCurrentVNFUserDistribution();
			this.saveWelfare( calculateSumOfCurrentUtility(), calculateSumOfBenchmarkUtility());
			Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
			iteration++;
		
		}
	}
	
	private Double calculateSumOfCurrentUtility() {
		 double sum = 0;
		 for(int i = 0 ; i < userGames.size() ; i++){
			 	sum = sum + userGames.get(i).playerUtility;
			}
			return sum;
	}
	
	private Double calculateSumOfBenchmarkUtility() {
		 double sum = 0;
		 for(int i = 0 ; i < userGames.size() ; i++){
			 	sum = sum + userGames.get(i).benchmarkUtility;
			}
			return sum;
	}
	
	public synchronized void updateGameTable(TEUser user, String decision){
		boolean userFound = false;
		for(int i = 0 ; i < userGames.size() ; i++){
			if(userGames.get(i).player.getUserIdentity().equalsIgnoreCase(user.getUserIdentity())){
				userGames.get(i).isGamePlayed = true;
				userGames.get(i).decision = decision;
				userGames.get(i).benchmarkUtility = user.getBenchmarkUtility();
				userGames.get(i).playerUtility = user.getUserUtility();
				userFound = true;
			}
		}
		if(!userFound) {
			Console.output("user cannot be found in the list:"+user.getUserIdentity());
		}
	}
	private void saveWelfare(double sumOfUserUtilility, Double sumOfBenchmarkUtility) {
		welfare.add(sumOfBenchmarkUtility);
		StringBuilder sb = new StringBuilder();
			sb.append(gameIterationNumber+",")
			.append(sumOfUserUtilility)
			.append(",")
			.append(sumOfBenchmarkUtility);
		fio.appendToFile(folderPath+"/"+WELFARE_RESULTS, sb.toString());	
	}

	private List<UserGame> getUserGameList() {
		List<UserGame> list = new ArrayList<UserGame>();
		for(TEUser user:users) {
			list.add(new UserGame(user));
		}
		return list;
	}

	/**
	 * This function checks whether all users have 
	 * already played in the given round. 
	 */
	public boolean checkAllUsersArePlayed(){
		if(userGames.size() > 0) {
			for(int i = 0 ; i < userGames.size() ; i++){
				if(!userGames.get(i).isGamePlayed){
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * This function calculates VNF users' numbers for the network manager. 
	 */
	public List<VNFUsers> calculateVNFUserNumbers(){
		List<VNF> vnflist = this.getVNFList();
		List<VNFUsers> vnfUserList= new ArrayList<VNFUsers>();
		for(int k = 0;k < vnflist.size(); k++ ){
			vnfUserList.add(new VNFUsers(vnflist.get(k).getVNFId()));
		}
		
		for(int i = 0;i<users.size(); i++ ){
			for(int j = 0;j < vnfUserList.size(); j++ ){
				if(users.get(i).assignedVNFServerId.equalsIgnoreCase(vnfUserList.get(j).getVNFId())){
					vnfUserList.get(j).addUserUtility(users.get(i).getUserUtility());
				}
			}
		}
		return vnfUserList;
	}

	
	/**
	 * Simple getter.
	 * @return internGameIterationNumber
	 */
	public int getCurrentGameIterationNumber(){
		return gameIterationNumber;
	}

	synchronized public double getPathDelay(String vnfId) {
		VNFPath vp = null;
		if(vnfId != null ){
			vp = nsliceManager.getNetworkSlice().vnfWithPaths.get(vnfId);
			if(vp != null ){
				return vp.path.getDelay();
			}else {
				Console.output("vnf path is null!");
			}
		}
		return vnfDefaultDelay;
	}
	
	synchronized public double getVNFPrice (String vnfId){
		VNFPath vp = null;
		if(vnfId != null ){
			vp =  nsliceManager.getNetworkSlice().vnfWithPaths.get(vnfId);
			if(vp != null ){
				return vp.vnf.getCurrentPricePerUser();
			}else {
				Console.output("vnf path is null!");
			}
		}
		return vnfDefaultPrice;
	}
	
	synchronized public double getVNFReputation (String vnfId){
		VNFPath vp = null;
		if(vnfId != null ){
			vp =  nsliceManager.getNetworkSlice().vnfWithPaths.get(vnfId);
			if(vp != null ){
				return vp.vnf.getActualReputation();
			}else {
				Console.output("vnf path is null!");
			}
		}
		return vnfReputation;
	}
	
	public int getAssignedUserNumberToVNF(String vnfId) {
		int number = 0 ;
		for(int i = 0; i < users.size(); i ++) {
			if(users.get(i).assignedVNFServerId.equalsIgnoreCase(vnfId)) {
				number ++;
			}
		}
		return number;
	}
	
	synchronized public double getUserBandwidth(String vnfId) {
		if(vnfId != null){
			if(nsliceManager.getNetworkSlice().vnfWithPaths != null){
				VNFPath vp =  nsliceManager.getNetworkSlice().vnfWithPaths.get(vnfId);
				if(vp != null){
					//Console.output(vnfId+ " total bandwidth "+vp.path.getBandwidth() +" total user number 1 "+getAssignedUserNumberToVNF(vp.vnf.getVNFId()));
					//Console.output(vnfId+ " bandwidth per user "+vp.path.getBandwidth() / getAssignedUserNumberToVNF(vp.vnf.getVNFId()));
					if(gameIterationNumber == 0) { //used to read the initial parameters
						return vp.path.getBandwidth() / getAssignedUserNumberToVNF(vp.vnf.getVNFId());
					}
					return vp.path.getBandwidth() / getAssignedUserNumberToVNF(vp.vnf.getVNFId());
				}
			}else {
				logger.debug("The network slice is not created");
			}
		}

		return 0;
	}
		
	// vnf1 - vnf1 | vnf1 - vnf2 | vnf2- vnf1 | vnf2- vnf2 
	// (u1, u2) | (u1, u2) | 
	//<currentstate, nextState, benchmarkutility_nextstate-benchmarutility_currentState>	
	// TODO: This function is currently not used!
	public void calculateStability() {
		for(int j = 0 ; j < users.size() ; j ++) {
			TEUser user = users.get(j);
			Console.output("user"+ j +"-----");
			//for (Entry<String, VNF> vnfbw : vnfs.entrySet()){
			for (Entry<String, VNFPath> vnfpath :  nsliceManager.getNetworkSlice().vnfWithPaths.entrySet()){
				user.assignedVNFServerId = vnfpath.getValue().vnf.getVNFId();
				String previousAction = user.assignedVNFServerId;
				for (Entry<String, VNFPath> vnfpath2 :  nsliceManager.getNetworkSlice().vnfWithPaths.entrySet()){
					previousAction = user.assignedVNFServerId;
					user.assignedVNFServerId = vnfpath2.getValue().vnf.getVNFId();
					user.createStability();
					payOffMatrixes.add(new PayoffMatrix(previousAction, user.assignedVNFServerId, user.getUserUtility()));
					Console.output(previousAction+" -> "+user.assignedVNFServerId +" -> "+ user.getUserUtility());
				}
			}
		}
	}
	
	public ArrayList<String> getListOfVNFs(){
		ArrayList<String> vnfList = new ArrayList<String>();
		List<VNF> currentVNFList = this.getVNFList();
		for(VNF vnf: currentVNFList) {
			vnfList.add(vnf.getVNFId());
		}
		return vnfList;
	}
	
	public  List<VNF> getVNFList(){
		List<VNF> vnfList = new ArrayList<VNF>();
		for (VNFPath vp : nsliceManager.getNetworkSlice().vnfWithPaths.values()) {
			vnfList.add(vp.vnf);
		}
		return vnfList;
	}	

	public void createMeasurementFiles() {
		List<VNF> vlist = this.getVNFList();
		for (int i = 0 ; i < vlist.size() ; i ++) {
			//fio.createNewFile(folderPath, "te_userdistribution_"+vlist.get(i).getVNFId()+".txt"); // This is not needed in the trial and error algorithm
			fio.createNewFile(folderPath, "cu_userdistribution_"+vlist.get(i).getVNFId()+".txt");
		}
		fio.createNewFile(folderPath, WELFARE_RESULTS);
	}
	
	
	public void saveCurrentVNFUserDistribution() {
		Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
		int vnf1usernumber= getAssignedUserNumberToVNF("VNFID1");
		int vn2usernumber = getAssignedUserNumberToVNF("VNFID2");
//		List<VNF> list = this.getVNFList();
//		for( int j = 0 ; j < list.size() ; j ++) {
//			Console.output("saved");
			fio.appendToFile(folderPath+"/"+currentUserDistribution+"_VNFID1.txt", gameIterationNumber +","+vnf1usernumber );
			fio.appendToFile(folderPath+"/"+currentUserDistribution+"_VNFID2.txt", gameIterationNumber +","+vn2usernumber);
//		}
		
	}

	/**
	 * Save the actual VNF User Distribution
	 * @param uList
	 */
	public void saveUserDistribution(ArrayList<String> uList) {
		String vnfId = "";
		String userDistribution = "";
		
		for(int i = 0 ;i < uList.size(); i++) {
			String [] list = uList.get(i).split(",");
			if(list.length == 2) {
				vnfId = list[0];
				userDistribution = list[1];
				fio.appendToFile(folderPath+"/"+"te_userdistribution_"+vnfId+".txt", gameIterationNumber +","+userDistribution);
			}
			fio.appendToFile(folderPath+"/"+"te_userdistribution.txt", gameIterationNumber +","+uList.get(i).toString());
		}
	}
	
	public void setTEServiceId(String id) {
		this.serviceIdentifier = id;
	}
	
	
	public void setQoSType(ServiceQoSType qosType) {
		this.serviceQoSType = qosType;
	}
	
	public ServiceQoSType getServiceType() {
		return serviceQoSType;
	}
	
	public String getServiceID() {
		return this.serviceIdentifier;
	}
	public boolean isGameContinue() {
		return serviceIsRunning;
	}

}