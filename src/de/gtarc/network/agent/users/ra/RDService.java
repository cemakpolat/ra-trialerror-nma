package de.gtarc.network.agent.users.ra;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtarc.network.config.ServiceTestConfig;
import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.ServiceQoSType;
import com.gtarc.network.utils.Util;

import de.gtarc.network.agent.no.NetworkManager;
import de.gtarc.network.agent.no.NetworkSlice;
import de.gtarc.network.service.common.Service;
import de.gtarc.network.service.common.ServiceNetworkSliceManager;
import de.gtarc.network.service.common.UserGame;
import de.gtarc.network.service.common.VNFUser;
import de.gtarc.network.service.common.VNFUsers;
import de.gtarc.network.agent.users.ra.RDUser;
import de.gtarc.network.vnf.VNF;
import de.gtarc.network.vnf.VNFPath;
/**
 * 
 * @author cemakpolat, doruksahinel
 *
 */
public class RDService implements Runnable, Service
{
	final Logger logger = LoggerFactory.getLogger(RDService.class);
	
	private static ServiceNetworkSliceManager nsliceManager = null; /** The network slice of the smart city service*/
	// control loops
	private static boolean serviceIsRunning = true;
	
	private String serviceIdentifier = "rd-service"; /** The name of the smart city service*/
	public ServiceQoSType serviceQoSType; /** QoS type of the smart city service, e.g, xMBB, mMTC, uMTC*/  
	
	static ArrayList<RDUser> users = new ArrayList<RDUser>(); /** The list of users connected to the smart city service*/
	private static double averageUserUtility = 0; /** Average utility of all users connected to the service*/

	// VNF|User assignments
	List<VNFUser> vnfUserList = new ArrayList<VNFUser>(); /** List of users connected to the service provider*/
	
	// intern game iteration number
	static int globalIterationNumber = 0;
	
	//one-to-one user games
	public static List<UserGame> userGames = new ArrayList<UserGame> (); /** Internal replicator games that the users play among each other*/
		
	public static Map<String, ArrayList<Double>> auxiliaryDelayPriceList = new HashMap<String, ArrayList<Double>>();
    ArrayList<Double> vnfBandwidthAmounts = new ArrayList<Double>();
    private double auxiliaryDelayPrice = 0;
		

	Util util = new Util();
	// file operations
	FileIO fio = new FileIO();// file operation
	public static final String resultsPath = "service";
	public static final String averageUserUtilityFile = "averageUserUtility";
	public static String folderPath = NetworkManager.folderPath + "/replicator_dynamics";
	public static String currentUserDistribution = "cu_userdistribution";
		
	public int totalUserNumber = 30; // default
	public int totalVNFNumber = 2; // default
	public int iterationLimitForUsers = 10; // default
	public double userDistrubitionRate = 0.8; // 
	public double vnfDefaultPrice = 1;
	public double vnfDefaultDelay = 1;
	public double vnfReputation = 0;
	
	private static RDService service = null;
	
	public RDService() {}
	public static RDService getInstance() {
	     if(service == null) {
	    	 service = new RDService();
	     }
	     return service;
	}

	/**
	 * This function maps the service to a network slice and 
	 * starts the evolutionary game between users.
	 */
	public void initialSetup(){
		fio.createFolder(folderPath);
		fio.createFolder(folderPath+"/users");
		
		nsliceManager = ServiceNetworkSliceManager.getInstance();
		nsliceManager.setServiceRequirements(this.serviceIdentifier, this.serviceQoSType);
		
		if(nsliceManager.createNetworkSlice()) {
			this.addUsers(totalUserNumber);
			this.createMeasurementFiles();// once vnfs are created
			for(int i = 0; i < users.size(); i ++){
				this.vnfUserList.add(new VNFUser(users.get(i).getUserIdentity(), users.get(i).assignedVNFServerId));
			}	
			nsliceManager.updateVNFUserTable(calculateVNFUserNumbers());
		}else {
			Console.output("Network Slice cannot be created!");
		}
		Console.output("initial setup is completed...");
	}
	
	public void addUsers(int totalUserNumber){
		int vnf1userNumber = (int)(totalUserNumber*userDistrubitionRate);
		int vnf1usercounter = 0;
		List<String> vnfList = this.getListOfVNFs();
		for (int i = 0 ; i < totalUserNumber ; i++){
			RDUser user = null;
			if(vnf1usercounter < vnf1userNumber) {
				user = new RDUser(this, "user_"+i,vnfList.get(0));
				vnf1usercounter ++;
			}else {
				user = new RDUser(this, "user_"+i,vnfList.get(1));
			}
			
			Thread userThread = new Thread(user);
			userThread.start();
			users.add(user); 	
		}
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
				Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
				runUsersGame();
			     Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
				Console.output("service users are all played!");
				nsliceManager.updateVNFUserTable(calculateVNFUserNumbers());
			}
			util.sleep(200);
		}
		Console.output("service is stopped!");
		serviceIsRunning = false;
	}
		
	public void runUsersGame() {
		int iteration = 0;
		while(iteration < iterationLimitForUsers) {
			userGames = new ArrayList<UserGame>();
			//Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
			this.mapUsers();
			globalIterationNumber++;
			while(!checkAllUsersArePlayed()){
				util.sleep(10);
			}
			util.sleep(100);
			//Console.output("vnf1 | vnf2 user numbers:" + getAssignedUserNumberToVNF("VNFID1")+"|"+ getAssignedUserNumberToVNF("VNFID2"));
			// game is over, make calculations...
			this.calculateAverageUserUtility(users); 	// average user utility for all users
			this.calculateAverageUtilityForVNFs(users); // average user utility for vnfs
			this.calculateTheoriticalUserDistribution();// theoretical user distribution
			this.saveCurrentVNFUserDistribution();
			this.saveCurrentIterationStatus();
			iteration++;
		}
	}
	
	public void calculateAverageUtilityForVNFs(List<RDUser> uList) {
		List<VNF> list = this.getVNFList();
		for( int j = 0 ; j < list.size() ; j ++) {
			double totalUserUtility = 0;
			int totalUser = 0;
			for(int i = 0;i<uList.size(); i++ ){
				if(list.get(j).getVNFId().equalsIgnoreCase(uList.get(i).assignedVNFServerId)) {
					totalUserUtility +=uList.get(i).getUserUtility();
					totalUser++;
				}
			}
			
			if(totalUser == 0 ) {
				list.get(j).setCurrentAverageUserUtility(0);	
			}else {
				list.get(j).setCurrentAverageUserUtility(totalUserUtility/totalUser);	
			}
			
		}
		saveAverageUserUtilitesPerVNF(list);
	}

	public void calculateTheoriticalUserDistribution(){
		auxiliaryDelayPriceList = new HashMap<String, ArrayList<Double>>();
		List<VNF> vlist = this.getVNFList();
		   for(int i = 0 ; i < vlist.size(); i ++){
			   VNF vnf = vlist.get(i);
			   double vnfPriceService1 = this.getVNFPrice(vnf.getVNFId());
			   double delay1 = this.getPathDelay(vnf.getVNFId());

			   ArrayList<Double> priceList = new ArrayList<Double>();
			   for(int j = 0 ; j < vlist.size(); j ++){
				   VNF vnf2 = vlist.get(j);
				   double vnfPriceService2 =  this.getVNFPrice(vnf2.getVNFId());
				   double delay2 = this.getPathDelay(vnf2.getVNFId());
				   auxiliaryDelayPrice = Math.exp(serviceQoSType.alpha2 * (vnfPriceService1-vnfPriceService2) + serviceQoSType.alpha3 * (delay1-delay2));
				   priceList.add(auxiliaryDelayPrice);
			   }
			   auxiliaryDelayPriceList.put(vnf.getVNFId(), priceList);
		   }
		   
		   double userDistribution = 0;
		   ArrayList<String> userDistributionList = new ArrayList<String>();
		   for(int i = 0 ; i < vlist.size(); i ++){
			   VNF vnf = vlist.get(i);
			   VNF otherVnf = vlist.get((i+1)%2);		   
			   double tbandwidth = vnf.getCurrentBandwidth() + auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get((i + 1) % 2) * otherVnf.getCurrentBandwidth();
			   //Console.output(vnf.getVNFId()+ " aux bw:"+tbandwidth);
			   userDistribution = totalUserNumber * vnf.getCurrentBandwidth() / tbandwidth;
			   
			   if (Double.isNaN(userDistribution)) {
				    userDistribution = 0;
				}
			   userDistributionList.add(vlist.get(i).getVNFId()+"," + Math.round(userDistribution));
		   }
		   this.saveUserDistribution(userDistributionList);
	}
	
	/**
	 * This function is used to map users randomly to each other 
	 * so that they can compare their current utility values.
	 */
	public void mapUsers() {
		List<RDUser> allusers = new ArrayList<>(users);
		 while(allusers.size() > 0){
			 RDUser user1 = allusers.get(0);
			allusers.remove(user1);
			RDUser user2 = getRandomUser(allusers);
			userGames.add(new UserGame(user1,user2));
			userGames.add(new UserGame(user2,user1));
			allusers.remove(user2);	 
		 }
	}

	/**
	 * This function updates the game table with user decisions.
	 */
	synchronized void updateGameTable(RDUser user, String result){
		for(int i = 0 ; i < userGames.size() ; i++){
			if(userGames.get(i).player1.getUserIdentity().equalsIgnoreCase(user.getUserIdentity())){
				userGames.get(i).isGamePlayed = true;
				userGames.get(i).decision = result;
				userGames.get(i).player1Utility = user.getUserUtility();
			}
		}
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
	 * This function returns the mapped users. 
	 */
	public RDUser getAssignedUser(RDUser user) {
		for(int i = 0 ; i < userGames.size() ; i++){
			if(userGames.get(i).player1.getUserIdentity().equalsIgnoreCase(user.getUserIdentity())){
				return userGames.get(i).player2;
			}
		}
		return null;
	}

	public double getPathDelay(String vnfId) {
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
	
	public double getVNFPrice (String vnfId){
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
	
	public double getVNFReputation(String vnfId) {
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
	/**
	 * Simple getter.
	 * @return vp.path.bandwidth
	 */	
	public double getUserBandwidth(String vnfId) {
		if(vnfId != null){
			if(  nsliceManager.getNetworkSlice().vnfWithPaths != null){
				VNFPath vp =  nsliceManager.getNetworkSlice().vnfWithPaths.get(vnfId);
				if(vp != null){
					if(globalIterationNumber == 0) { //used to read the initial parameters
						return vp.path.getBandwidth() / getAssignedUserNumberToVNF(vp.vnf.getVNFId());
					}
					return vp.path.getBandwidth() / vp.vnf.getTotatAssignedUserNumber();
				}
			}else {
				logger.debug("The network slice is not created");
			}
		}

		return 0;
	}
		
	public double getAverageUserUtility(){
		return averageUserUtility;
	}
	
	/**
	 * This function calculates average user utility.
	 */
	public void calculateAverageUserUtility(List<RDUser> uList){
		double totalUserUtility = 0;
		
		for(int i = 0;i < uList.size(); i++ ){
			totalUserUtility += uList.get(i).getUserUtility();
		}
		
		if( uList.size() == 0) {
			averageUserUtility = 0;
		} else {
			averageUserUtility = totalUserUtility / uList.size();
			
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
	
	public int getCurrentGameIterationNumber(){
		return globalIterationNumber;
	}

	public void setServiceId(String id) {
		this.serviceIdentifier = id;
	}
	public String getServiceId() {
		return this.serviceIdentifier;
	}
	
	public void setQoSType(ServiceQoSType qosType) {
		serviceQoSType = qosType;
	}
	public ServiceQoSType getServiceType() {
		return serviceQoSType;
	}
	
	/**
	 * This function returns a random user to be mapped with another user 
	 * for comparison.
	 */	
	public RDUser getRandomUser(	List<RDUser>  users){
		if(users.size() == 1){
			return users.get(0);
		}
		int min = 0;
		int max = users.size();
		// random number is the position in the user list, it starts from 0 and ends at (user list size - 1)
		int randomNum = ThreadLocalRandom.current().nextInt(min, max - 1);
		
		return users.get(randomNum);
	}

	/**
	 * This function creates a text file to be used in Matlab to plot graphs.
	 */	
	public void createMeasurementFiles() {
		fio.createNewFile(folderPath, resultsPath + ".txt");
		fio.createNewFile(folderPath, averageUserUtilityFile + ".txt");
		fio.createNewFile(folderPath, averageUserUtilityFile + "_total.txt");
		List<VNF> vlist = this.getVNFList();
		for (int i = 0 ; i < vlist.size() ; i ++) {
			fio.createNewFile(folderPath, averageUserUtilityFile+"_"+vlist.get(i).getVNFId()+".txt");
			fio.createNewFile(folderPath, averageUserUtilityFile+"_total_"+vlist.get(i).getVNFId()+".txt");
		}
		for (int i = 0 ; i < vlist.size() ; i ++) {
			fio.createNewFile(folderPath, "te_userdistribution_"+vlist.get(i).getVNFId()+".txt");
			fio.createNewFile(folderPath, "cu_userdistribution_"+vlist.get(i).getVNFId()+".txt");
		}
	}
	    
	public void saveCurrentVNFUserDistribution() {
		List<VNF> list = this.getVNFList();
		for( int j = 0 ; j < list.size() ; j ++) {
			fio.appendToFile(folderPath+"/"+currentUserDistribution+"_"+list.get(j).getVNFId()+".txt", globalIterationNumber +","+list.get(j).getAssignedUserNumber()+"" );
		}
		
	}
	
	private void saveAverageUserUtilitesPerVNF(List<VNF> list) {
		for( int j = 0 ; j < list.size() ; j ++) {
			fio.appendToFile(folderPath+"/"+averageUserUtilityFile+"_"+list.get(j).getVNFId()+".txt", globalIterationNumber +","+list.get(j).getCurrentAverageUserUtility()+"" );
			fio.appendToFile(folderPath+"/"+averageUserUtilityFile+"_total_"+list.get(j).getVNFId()+".txt", globalIterationNumber +","+list.get(j).getCurrentAverageUserUtility()*this.totalUserNumber+"" );
		}
	}
	
	/**
	 * This function saves the current game status and writes it to a text file.
	 */	
	public void saveCurrentIterationStatus() {
		if(globalIterationNumber != 1) { 
			String outputString = "";
			for(RDUser user: users) {
				outputString = globalIterationNumber +","+user.getUserIdentity()+","+user.assignedVNFServerId+","+Util.round(user.getUserUtility(),3);
				fio.appendToFile(folderPath+"/"+resultsPath + ".txt", outputString);
			}
			
			outputString = globalIterationNumber +","+ averageUserUtility;
			fio.appendToFile(folderPath+"/"+averageUserUtilityFile + ".txt", outputString);
			fio.appendToFile(folderPath+"/"+averageUserUtilityFile + "_total.txt", globalIterationNumber +","+ averageUserUtility*100);
		}
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
				fio.appendToFile(folderPath+"/"+"te_userdistribution_"+vnfId+".txt", globalIterationNumber +","+userDistribution);
			}
			fio.appendToFile(folderPath+"/"+"te_userdistribution.txt", globalIterationNumber +","+uList.get(i).toString());
		}
	}
	
	/**
	 * This function calculates average user utility. - Alternative version
	 */
	public void calculateAverageUserUtility2(List<RDUser> uList){
		double totalUserUtility = 0;
		// find the number of vnf users
		
		List<VNF> vnflist = this.getVNFList();
		List<VNFUsers> vnfUserList= new ArrayList<VNFUsers>();
		for(int k = 0;k < vnflist.size(); k++ ){
			vnfUserList.add(new VNFUsers(vnflist.get(k).getVNFId()));
		}
		
		for(int i = 0;i<uList.size(); i++ ){
			for(int j = 0;j < vnfUserList.size(); j++ ){
				if(uList.get(i).assignedVNFServerId.equalsIgnoreCase(vnfUserList.get(j).getVNFId())){
					vnfUserList.get(j).addUserUtility(uList.get(i).getUserUtility());
				}
			}
		}
		
		for(int i = 0;i < vnfUserList.size(); i++ ){
			for(int k = 0;k<vnfUserList.get(i).userUtil.size(); k++ ){
				totalUserUtility += vnfUserList.get(i).userUtil.size()/uList.size() * vnfUserList.get(i).userUtil.get(k).doubleValue();	
			}
		}
		
		for(int i = 0;i<uList.size(); i++ ){
			totalUserUtility +=uList.get(i).getUserUtility(); 
		}
		
		this.averageUserUtility = totalUserUtility;
	}
	public boolean isGameContinue() {
		return serviceIsRunning;
	}
	
	private void calculateAvailableResourcesOfVNFs() {
		Map<String,Boolean> vnfResourceState = new HashMap<String, Boolean>();
		List<VNF> vnfList = this.getVNFList();
		for(int i = 0 ; i < vnfList.size(); i ++ ) {
			if(vnfList.get(i).getAssignedUserNumber() == 0) {
				vnfResourceState.put(vnfList.get(i).getVNFId(), false);
			}
			vnfResourceState.put(vnfList.get(i).getVNFId(), true);
		}
	}
}