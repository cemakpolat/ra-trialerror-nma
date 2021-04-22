package de.gtarc.network.agent.no;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtarc.network.config.NMTestConfig;
import com.gtarc.network.config.ServiceTestConfig;
import com.gtarc.network.config.VNFTestConfig;
import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.FileIO;
import com.gtarc.network.utils.ServiceQoSType;
import com.gtarc.network.utils.Util;

import de.gtarc.network.agent.users.ra.RDService;
import de.gtarc.network.agent.users.te.TEService;
import de.gtarc.network.service.common.Service;
import de.gtarc.network.service.common.UserGame;
import de.gtarc.network.service.common.VNFUsers;
import de.gtarc.network.vnf.Path;
import de.gtarc.network.vnf.RequestResult;
import de.gtarc.network.vnf.VNF;
import de.gtarc.network.vnf.VNFGame;
import de.gtarc.network.vnf.VNFPath;


/**
 * Network manager class
 * @author cemakpolat, doruksahinel
 *Graphs to be extracted from the simulation tests
 *Convergence of the algorithms among users to the equilibrium -> utility of all users are identical: show initial user distribution and final user distribution in
 *Revenue, Cost and Utility of Fog Servers 
 *Utility Increase for Users (Welfare)
 */

public class NetworkManager {
	
	static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
	
	private Util util = new Util();
	
	// service as a source, bandwidth, path and VNF number holds the created network slice informations
	static ArrayList<NetworkSlice> networkSlices = new ArrayList<NetworkSlice> (); 	/** List of network slices given to smart city services */
	static ArrayList<VNF> vnfServerList = new ArrayList<VNF>();						/** List of vnf servers and their prices per user*/
	static ArrayList<Service> services = new ArrayList<Service>(); 					/** List of services connected to network manager*/ 
    public static  Map<String, ArrayList<Double>> auxiliaryDelayPriceList = new HashMap<String, ArrayList<Double>>();
    
    ServiceQoSType assignedQoSType = null;
    public double availableBandwidth = 0;
    private double auxiliaryDelayPrice = 0; // auxiliary delay and price, these are the parameters that have impact on the bandwidth
   
    private static NetworkManager nmanager = null; // network manager 
    
    public static List<VNFGame> vnfGames = new ArrayList<VNFGame>(); // VNF Games VNF-ID | isGamePlayed
    
	private static boolean isVNFPathTableUpdated = false; 	/** checks if the users have given their vnf decisions*/
	static boolean isServiceUpdatedVNFUserTable = false; 	/** checks whether service sent the updated vnf table to network operator */
	private static int gameIterationNumber = 0; 			/** iterates when VNF servers have sent their bandwidth requests*/
	// VNFTable: VNF|User
	static public List<VNFUsers> vnfUserTable = new ArrayList<VNFUsers>(); /** sends the list of users connected to VNF servers */
	
	public static double totalBandwidth = 1;
	public static double totalBandwidth_VNF1 = 1;
	public static double totalBandwidth_VNF2 = 1;

	//file operation
	FileIO fio = new FileIO();
	public static String folderPath = "results";
	public static final String resultsPath = "networkOperator.txt";
		
	public static boolean nmOperatorIsRunning = true;
	public static int maximumIterationNumber = 1;
	
	public NetworkManager() {
		ServiceQoSType.addDefaultQoSTypes();
		fio.createFolder(folderPath);
	}

	public static NetworkManager getInstance() {
		if (nmanager == null) {
			nmanager = new NetworkManager();
		}
		return nmanager;
	}

	/**
     * This function starts the game between VNF servers and the users
     * by giving initial setup parameter values to actors with config files.
    */
	
	public void initialSetup() {
		Console.output("GAME is starting...");

		// network operator config		
		NMTestConfig nmConfig = new NMTestConfig();
		nmConfig.readConfigFile();

		//this.setAvailableBandwidth(NMTestConfig.getTotalBandwidth());
		
		totalBandwidth_VNF1 = NMTestConfig.getVnf_path_bandwidth_1();
		totalBandwidth_VNF2 = NMTestConfig.getVnf_path_bandwidth_2();
		
		this.assignServiceQoSType(ServiceQoSType.xMBB);
		
		// add vnf servers
		VNFTestConfig config1 = new VNFTestConfig();
		config1.readConfigFile(VNFTestConfig.configFileName1);
		
		VNFTestConfig config2 = new VNFTestConfig();
		config2.readConfigFile(VNFTestConfig.configFileName2);
		
		config1.setTotalUserNumber(NMTestConfig.getTotalUserNumber()*3/4)
		.setBandwidthUnitPrice(NMTestConfig.getBandwidthUnitPrice())
		.setDelay(NMTestConfig.getVnf_path_delay_1()).setAssignedBandwidth(NMTestConfig.getVnf_path_bandwidth_1());
		
		config2.setTotalUserNumber(NMTestConfig.getTotalUserNumber()*1/4)
		.setBandwidthUnitPrice(NMTestConfig.getBandwidthUnitPrice())
		.setDelay(NMTestConfig.getVnf_path_delay_2())
		.setAssignedBandwidth(NMTestConfig.getVnf_path_bandwidth_2());

		// register and start vnf services 
		
		VNF vnf1 = new VNF(config1, NMTestConfig.getVnf_Id_1());
		VNF vnf2 = new VNF(config2, NMTestConfig.getVnf_Id_2());

		this.registerVNFServer(vnf1);
		this.registerVNFServer(vnf2);
		
		// start services
		//RDService service = new RDService(); 	// replicator dynamics
		TEService service = new TEService(); 	// trial and error
		service.setQoSType(this.assignedQoSType);
		
		this.registerNewService(service);
		Thread tservice = new Thread(service);
		tservice.start();
		
		this.createMeasurementFiles();
	}
	

	public double getAvailableBandwidth() {
		return availableBandwidth;
	}

	public void setAvailableBandwidth(double availableBandwidth) {
		this.availableBandwidth = availableBandwidth;
	}
	
	/**
     * This function starts the Network Manager.
     */

	public void run() {

		int i = 0;
		nmOperatorIsRunning = true;
		initialSetup();
		while (nmOperatorIsRunning && i < maximumIterationNumber) {
			if (isServiceVNFUserTableUpdated()) {
				Console.output("==== Game Iteration "+ gameIterationNumber);
				this.setServiceVNFUserTableStatus(false);
				this.calculateAuxiliaryVariable(vnfServerList, "serviceId");
				this.calculateTotalBandwidth();
				gameIterationNumber++;
				createVNFsGame();
				while(checkAllVNFServersArePlayed()){
					util.sleep(50);
				}
				setVNFPathTableStatus(true);
				this.saveCurrentIterationStatus();
				i++;
			}
			util.sleep(200);
		}
		Console.output("Game is over!");
		nmOperatorIsRunning = false;
	}
	private void clearPlayedGames() {
		vnfGames.clear();
	}
	private void createVNFsGame() {
		clearPlayedGames();
		for(int i = 0 ; i < vnfServerList.size() ; i++) {
			vnfGames.add(new VNFGame(vnfServerList.get(i).getVNFId(), false));
		}
	}
	/**
     * This function updates the game table.
     */
	public synchronized void updateGameTable(VNF vnf) {
		for (int i = 0; i < vnfGames.size(); i++) {
			if (vnfGames.get(i).getVNFId().equalsIgnoreCase(vnf.getVNFId())) {
				vnfGames.get(i).isGamePlayed = true;
			}
		}
	}
	
	/**
     * This function check if VNF servers completed playing
     * the game at the current iteration.
     */
	public boolean checkAllVNFServersArePlayed() {
		for (int i = 0; i < vnfGames.size(); i++) {
			if (!vnfGames.get(i).isGamePlayed) {
				return false;
			}
		}
		return true;
	}
	

	/**
     * This function  shows whether VNF|User table update
     * at the service side is received by the Network operator.
     */
	public boolean isServiceVNFUserTableUpdated() {
		return isServiceUpdatedVNFUserTable;
	}

	/**
     * This function checks whether user path selections are 
     * updated by the smart city service.
     */
	public void setServiceVNFUserTableStatus(boolean status) {
		isServiceUpdatedVNFUserTable = status;
	}
	
	/**
     * Service calls this method to inform Network Operator about the modified
	 * vnf|user table
     */
	public void updateVNFUserTable(List<VNFUsers> table) {
		vnfUserTable = table;
		this.setServiceVNFUserTableStatus(true);
	}

	/**
     * Service call this method to understand whether 
     * there is a change in VNF server prices.
     */
	public boolean isVNFPathTableUpdated() {
		return isVNFPathTableUpdated;
	}

	/**
     * Service can mark after processing the iteration
	 * Network operator can trigger the service via this method in order to
	 * start the game on the service side.
	 */
	public void setVNFPathTableStatus(boolean status) {
		isVNFPathTableUpdated = status;
	}
	 
	/**
     * VNF server calls this method to play the game
     */
	public int getGameIterationNumber() {
		return gameIterationNumber;
	}
	 
    /**
	 * VNF Server gets the assigned user number
	 */
	 public int getTotalAssignedUserNumber(String vnfId) {
		// extract the user number from the updated vnf table
		for (int i = 0; i < vnfUserTable.size(); i++) {
			if (vnfUserTable.get(i).getVNFId().equalsIgnoreCase(vnfId)) {
				//Console.output(vnfId+" "+vnfUserTable.get(i).getUserNumber());
				return vnfUserTable.get(i).getUserNumber();
			}
		}
		return 0;
	}

	///########## AuxiliaryVariables 
   /**
    * This function calculates the auxiliary variables used in
    * reinforcement learining algorithm inside the VNF server.
    */
   public void calculateAuxiliaryVariable(ArrayList<VNF> vlist, String serviceId){
	   ServiceQoSType stype = this.assignedQoSType;
	   auxiliaryDelayPriceList = new HashMap<String, ArrayList<Double>>();
	   for(int i = 0 ; i < vlist.size(); i ++){
		   VNF vnf = vlist.get(i);
		   double vnfPriceService1 = this.getVNFPrice(vnf.getVNFId(),"serviceId"); // network operator has already table  bandwidth|delay|vnfprice| source|target
		   double delay1 = this.getDelayOfVNF(vnf.getVNFId(),"serviceId");
		   // network status
		   ArrayList<Double> priceList = new ArrayList<Double>();
		   for(int j = 0 ; j < vlist.size(); j ++){
			   VNF vnf2 = vlist.get(j);
			   if(!vnf2.getVNFId().equalsIgnoreCase(vnf.getVNFId())) {
				   double vnfPriceService2 =  this.getVNFPrice(vnf2.getVNFId(),"serviceId");
				   double delay2 = this.getDelayOfVNF(vnf2.getVNFId(),"serviceId");
				   //auxiliaryDelayPrice = Math.exp(stype.alpha2 * (vnfPriceService1-vnfPriceService2) + stype.alpha3 * (delay1-delay2));
				   auxiliaryDelayPrice = (stype.alpha2 * (vnfPriceService1-vnfPriceService2) + stype.alpha3 * (delay1-delay2));
				   Console.output("calculateAuxiliaryVariable :"+auxiliaryDelayPrice);
				   //Console.output("calculateAuxiliaryVariable :"+auxiliaryDelayPrice);
				   priceList.add(auxiliaryDelayPrice);				   
			   }
		   }
		   auxiliaryDelayPriceList.put(vnf.getVNFId(), priceList);
		  //Console.output("Inner loop Aux List Size: "+vnf.getVNFId()+priceList.size());
	   }
	   
	   // The following code measures the user distribution, which is already performed by the service providers, so being commented!
//	   double userDistribution = 0;
//	   ArrayList<String> userDistributionList = new ArrayList<String>();
//	   for(int i = 0 ; i < vlist.size(); i ++){
//		   //Console.output("Array List:"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get(i));
//		   //Console.output("Array List:"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get((i+1)%2));
//		   // Console.output("Whole array list :"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()));
//		   VNF vnf = vlist.get(i);
//		   VNF otherVnf = vlist.get((i+1)%2);
//		   // Console.output("VNF ID :"+vnf.getVNFId());
//		   Console.output("VNF Bandwidth :"+vnf.getCurrentBandwidth());
//		   userDistribution = totalUserNumber * (vnf.getCurrentBandwidth() / (vnf.getCurrentBandwidth() + auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get((i+1)%2) * otherVnf.getCurrentBandwidth()));
//		   //intUserDistribution = (int) Math.round(userDistribution);
//		   // Console.output("userDistribution:"+ userDistribution);
//		   userDistributionList.add(vlist.get(i).getVNFId()+"|" + Math.round(userDistribution));
//		   Console.output("totalUserNumber: " + totalUserNumber);
//		   Console.output("userDistribution:"+ userDistribution);
//		   Console.output("userDistributionList:"+userDistributionList);
//	   }
//	   saveUserDistribution(userDistributionList);
	   
	             double userDistribution = 0;
	             ArrayList<String> userDistributionList = new ArrayList<String>();
	             for(int i = 0 ; i < vlist.size(); i ++){
	                     //Console.output("Array List:"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get(i));
	                     //Console.output("Array List:"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get((i+1)%2));
	                     // Console.output("Whole array list :"+auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()));
	                     VNF vnf = vlist.get(i);
	                     VNF otherVnf = vlist.get((i+1)%2);
	                      Console.output("VNF ID :"+vnf.getVNFId() +" vnf.getCurrentBandwidth() "+vnf.getCurrentBandwidth());
	                     Console.output("VNF Bandwidth :"+vnf.getCurrentBandwidth());
	                     userDistribution = NMTestConfig.getTotalUserNumber()* (vnf.getCurrentBandwidth() / (vnf.getCurrentBandwidth() + auxiliaryDelayPriceList.get(vlist.get(i).getVNFId()).get(0)
	    * otherVnf.getCurrentBandwidth()));
	                     //intUserDistribution = (int) Math.round(userDistribution);
	                     // Console.output("userDistribution:"+ userDistribution);
	                     userDistributionList.add(vlist.get(i).getVNFId()+"|" + Math.round(userDistribution));
	                     Console.output("totalUserNumber: " + NMTestConfig.getTotalUserNumber());
	                     Console.output("userDistribution:"+ userDistribution);
	                     Console.output("userDistributionList:"+userDistributionList);
	             }
	             saveUserDistribution(userDistributionList);
   } 
    
  public double getAuxiliaryDelayPrice (){
	   	//Console.output("This auxiliary delay price value is: " + this.auxiliaryDelayPrice);
		return this.auxiliaryDelayPrice;
   }
 
   
   public Map<String, ArrayList<Double>>  getAuxiliaryDelayPriceList (){	
	   return auxiliaryDelayPriceList;
   }
   
   /**
  	 * Simple getter.
  	 * @return list of VNF servers 
  	 */
   public ArrayList<VNF>  getVNFServerList (String VnfType){
	   ArrayList<VNF> list = new ArrayList<VNF> ();
	   for(int i = 0 ; i < NetworkManager.vnfServerList.size() ; i++){
		   if(NetworkManager.vnfServerList.get(i).getVNFType().equalsIgnoreCase(VnfType)){
			   list.add(NetworkManager.vnfServerList.get(i));
		   }
	   }
	   return list;
   }   
      
   /**
  	 * Register VNF server to network manager.
  	 */
   public void registerVNFServer(VNF vnf){
		vnfServerList.add(vnf);
		new Thread(vnf).start();
	}

   /**
 	 * Register smart city service to network manager.
 	 */
   public void registerNewService(Service service){
	   services.add(service);
   }

   /**
 	 * With this function, network manager assigns service
 	 * QoS types to smart city services
 	 */
   public void assignServiceQoSType(String stype){
	   this.assignedQoSType = ServiceQoSType.getServiceQoSType(stype);
   }
   
   /**
  	 * Simple getter.
  	 * @return currentPricePerUser of VNF servers
  	 */
   public double getVNFPrice(String vnfId, String serviceId){
	   for(int i = 0; i < vnfServerList.size() ; i ++){
		   if(vnfServerList.get(i).getVNFId().equalsIgnoreCase(vnfId)){
			   return vnfServerList.get(i).getCurrentPricePerUser();
		   }
	   }
	   return 0;
   }
   
   
   /**
 	 * Simple getter.
 	 * @return delay value of the path to VNF server
 	 */
   // TODO: Delay must be retrieved from the network manager itself, not from VNF Server!
   public double getDelayOfVNF(String vnfId, String serviceId){
	   for(int i = 0; i < vnfServerList.size() ; i ++){
		   if(vnfServerList.get(i).getVNFId().equalsIgnoreCase(vnfId)){
			   return vnfServerList.get(i).getDelay(); 
		   }
	   }
	   return 0;
   }   

   
     /**
	 * Simple getter.
	 * @return maximum available bandwidth value of the path to VNF server
	 */	
   public double getMaximumAvailableBandwidthFor(String vnfId) {
//	   if(vnfId.equalsIgnoreCase("VNFID1")) {
//			return (double)this.totalBandwidth_VNF1;
//			
//		} else if(vnfId.equalsIgnoreCase("VNFID2")) {
//			return (double)this.totalBandwidth_VNF2;
//		}
//	   return 0;
		this.availableBandwidth = NMTestConfig.getTotalBandwidth();
		return this.availableBandwidth; 
		
		// The following code block will be later reopened
		/*
		networkSlices = new ArrayList<NetworkSlice> ();
		 for(NetworkSlice slice: networkSlices){
			 Map<String,VNFPath> vpath = slice.getVNFPaths();
			 VNFPath vp = vpath.get(vnfId);
			 if(vp!=null){
				 return vp.path.getBandwidth(); // return total bandwidth on the link
			 }
			 
		 }
		return 10;
		*/
	}

      
   /**
    * This function calculates the total bandwidth available 
    * for VNF servers in their respective links. This result is used in
    * VNF reinforcement learning calculation. 
    */
    
//	public void calculateTotalBandwidth() {
//	    ArrayList<VNF> vnfList = getVNFServerList("Firewall");// VNF Type  
//	    double totalVNFBandwidth = 0;
//	    
//	    for (int i =  0; i< vnfServerList.size(); i++) {
//    		for (int j =  0; j < vnfServerList.size(); j++) {
//    			totalVNFBandwidth += vnfList.get(i).getCurrentBandwidth()* auxiliaryDelayPriceList.get(vnfList.get(i).getVNFId()).get(j).doubleValue();
//    		}
//	    }
//	    totalBandwidth = totalVNFBandwidth;
//	}

	public void calculateTotalBandwidth() {
      ArrayList<VNF> vnfList = getVNFServerList("Firewall");// VNF Type
       //double totalVNFBandwidth = 0;
       for (int j =  0; j < vnfServerList.size(); j++) {

           if(vnfServerList.get(j).getVNFId().toString().equalsIgnoreCase("VNFID1")) {
                   double vnfId1_bw = vnfServerList.get(j).getCurrentBandwidth();
                   for (int i =  0; i < vnfServerList.size(); i++) {
                           if(vnfServerList.get(i).getVNFId().toString().equalsIgnoreCase("VNFID2")) {
                                   double vnfId2_bw =  vnfServerList.get(i).getCurrentBandwidth();
                                   double aux_value = auxiliaryDelayPriceList.get(vnfServerList.get(j).getVNFId()).get(0).doubleValue();
                                   Console.output("vnfId1_bw "+vnfId1_bw + " vnfId2_bw "+vnfId2_bw+" aux_value:"+aux_value);
                                   totalBandwidth_VNF1 = vnfId1_bw + vnfId2_bw * aux_value;
                                   //Console.output("totalBandwidth_VNF1 ->"+totalBandwidth_VNF1);
                           }
                   }
           }
           else if(vnfServerList.get(j).getVNFId().toString().equalsIgnoreCase("VNFID2")) {
                   double vnfId2_bw = vnfServerList.get(j).getCurrentBandwidth();
                   Console.output("totalBandwidth_VNF2 ->"+vnfServerList.get(j).getVNFId().toString().equalsIgnoreCase("VNFID2"));
                   for (int k =  0; k < vnfServerList.size(); k++) {
                           if(vnfServerList.get(k).getVNFId().toString().equalsIgnoreCase("VNFID1")) {
                                   double vnfId1_bw =  vnfServerList.get(k).getCurrentBandwidth();
                                   double aux_value = auxiliaryDelayPriceList.get(vnfServerList.get(j).getVNFId()).get(0).doubleValue();
                                   Console.output("vnfId2_bw "+vnfId2_bw + " vnfId1_bw "+vnfId1_bw+" aux_value:"+aux_value);
                                   totalBandwidth_VNF2 = vnfId2_bw + vnfId1_bw * aux_value;
                                   //Console.output("totalBandwidth_VNF2 ->"+totalBandwidth_VNF2);
                           }
                   }
           }
        }
	}
	public double calculateTotalVNFUtility() {
		ArrayList<VNF> vnfList = getVNFServerList("Firewall");// VNF Type  
	    double totalUtility = 0;
	    
	    for (int i =  0; i< vnfServerList.size(); i++) {
	    	totalUtility += vnfList.get(i).getCurrentVNFUtility();
	    }
	    return totalUtility;
	}
	/**
	 * Simple getter.
	 * @return totalBandwidth
	 */	
	public double getTotalBandwidth() {
		return totalBandwidth;
	}
	 
	/**
	 * 
	 * @param vnfId
	 * @return
	 */
	public double getTotalBandwidth(String vnfId) {
		//return totalBandwidth;
		if(vnfId.equalsIgnoreCase("VNFID1")) {
			return this.totalBandwidth_VNF1;
		} else if(vnfId.equalsIgnoreCase("VNFID2")) {
			return this.totalBandwidth_VNF2;
		}
		return 0;
	}


	/**
	 * With this function, network operator decides 
	 * whether to accept VNF servers' bandwidth request. 
	 */
	public RequestResult requestBandwidth(String serviceId, String vnfId, double requestBandwidth) {
		//Console.output(vnfId + " requests the bandwidth -> "+ requestBandwidth);
		RequestResult result = new RequestResult();
		if(requestBandwidth > availableBandwidth){
			result.accepted = false;
			Console.output("Requested bandwidth is more than available!");
		} else{
			result.accepted = true;
			result.bandwidthUnitPrice = NMTestConfig.getBandwidthUnitPrice();
			result.requestedBandwidth = requestBandwidth;
			  if(vnfId.equalsIgnoreCase("VNFID1")) {
					totalBandwidth_VNF1 = requestBandwidth;
				} else if(vnfId.equalsIgnoreCase("VNFID2")) {
					totalBandwidth_VNF2 = requestBandwidth;
				}
			this.updateVNFPathTable(vnfId,serviceId, requestBandwidth,this.getDelayOfVNF(vnfId, serviceId),getVNFPrice(vnfId,serviceId));
		}
		return result;
	}
 
	/**
	 * This function handles network slice requests of smart city service providers. 
	 */
	public List<NetworkSlice> requestNewSlices(String serviceIdentifier, ServiceQoSType serviceQoSType) {
		
		Map<String,VNFPath> vnfWithPaths = new HashMap<String,VNFPath>();
		for(int i = 0; i < vnfServerList.size() ; i++){
			if(vnfServerList.get(i).getVNFId().equalsIgnoreCase(NMTestConfig.getVnf_Id_1())){
				vnfWithPaths.put(vnfServerList.get(i).getVNFId(), new VNFPath(vnfServerList.get(i),new Path(NMTestConfig.getVnf_path_bandwidth_1(),NMTestConfig.getVnf_path_delay_1(), NMTestConfig.getVnf_Id_1(), serviceIdentifier)));	
			}
			if(vnfServerList.get(i).getVNFId().equalsIgnoreCase(NMTestConfig.getVnf_Id_2())){
				vnfWithPaths.put(vnfServerList.get(i).getVNFId(), new VNFPath(vnfServerList.get(i),new Path(NMTestConfig.getVnf_path_bandwidth_1(),NMTestConfig.getVnf_path_delay_2(), NMTestConfig.getVnf_Id_2(), serviceIdentifier)));
			}
		}
		
		NetworkSlice slice = new NetworkSlice(serviceIdentifier,vnfWithPaths);
		networkSlices.add(slice);
		
		return networkSlices;
	}

	/**
	 * This function updates the status of 
	 * paths to VNF servers.
	 */
	// bandwidth|delay|vnfprice|source|target|active user number
	public void updateVNFPathTable(String vnfId, String serviceId, double bandwidth, double delay, double price) {
		 for(NetworkSlice slice: networkSlices){
			 if(slice != null) {
				 Map<String,VNFPath> vpath = slice.getVNFPaths();
				 VNFPath vp = vpath.get(vnfId);
				 if(vp!= null){
					 vp.path.setBandwidth(bandwidth);
					 vp.path.setDelay(delay);
					 vp.path.setPrice(price);
					 showCurrentNetworkSlice(slice);
					 break;
				 }	 
			 }
		 }
		 this.setVNFPathTableStatus(true);
	}

	public void showCurrentNetworkSlice(NetworkSlice slice) {
		Map<String, VNFPath> paths = slice.getVNFPaths();
		for (Map.Entry<String, VNFPath> entry : paths.entrySet()) {
		    String key = entry.getKey();
		    VNFPath value = entry.getValue();
		    //Console.output(key+" bandwidth "+value.path.getBandwidth()+"");
		}
	}
	
	
	/**
	 * Simple getter.
	 * @return network Slice with the given sliceID
	 */	
	public NetworkSlice getNetworkSlice(String sliceId) {
		for(int i = 0 ; i < networkSlices.size() ; i ++) {
			if (networkSlices.get(i).getSliceId().equalsIgnoreCase(sliceId)) {
				return networkSlices.get(i);
			}
		}
		return null;
	}
	
	/**
	 * This function creates a text file 
	 * to be used in Matlab to plot graphs.
	 */	
		
	public void createMeasurementFiles() {
		for (int i = 0 ; i < vnfServerList.size() ; i ++) {
			fio.createNewFile(folderPath, vnfServerList.get(i).getVNFId()+".txt");
		}
		// create user distribution file
		//fio.createNewFile(folderPath, "userdistribution.txt");
	}

	public void saveUserDistribution(ArrayList<String> uList) {
		for(int i = 0 ;i < uList.size(); i++) {
			fio.appendToFile(folderPath + "/" +"userdistribution.txt", uList.get(i).toString());	
		}
	}
	
	public void saveCurrentIterationStatus() {
		
		String outputString = "";
		for(NetworkSlice slice: networkSlices){
			for (Map.Entry<String, VNFPath> entry : slice.getVNFPaths().entrySet())
			{
			    VNFPath vnfPath =  entry.getValue();
			    String vnfId =  entry.getKey();  
			    outputString = NetworkManager.gameIterationNumber +":"+ Util.round(vnfPath.vnf.getCurrentBandwidth(),3) +":"+vnfPath.vnf.getVNFId() + ":"+ vnfPath.vnf.getTotatAssignedUserNumber()+":"+Util.round(vnfPath.vnf.getCurrentRevenue(), 3)+":"+Util.round(vnfPath.vnf.getCurrentVNFUtility(),3) +":"+vnfPath.vnf.getVNFType() +":"+Util.round(vnfPath.vnf.getBandwidthUnitPrice(),3) + ":"+Util.round(vnfPath.vnf.getCurrentPricePerUser(),3);
			    fio.appendToFile(folderPath + "/" +vnfId+".txt", outputString);
			   //readFile(vnfId);
			}
		}
	}
	
	public void readFile(String fileName){
	
		String fileNameFullPath = folderPath + "/" +fileName+".txt";	
		try {
				
			/*	Sets up a file reader to read the file passed on the command
				line one character at a time */
			FileReader input = new FileReader(fileNameFullPath);
            
			/* Filter FileReader through a Buffered read to read a line at a
			   time */
			BufferedReader bufRead = new BufferedReader(input);
			
            String line; 	// String that holds current file line
            int count = 0;	// Line number of count 
            
            // Read first line
            line = bufRead.readLine();
            String[] textToJson = line.split(":");
            JSONObject VNFjson  = new JSONObject();
		   	VNFjson.put("bandwidth", textToJson[1]);
			VNFjson.put("utility", textToJson[5]);
			VNFjson.put("userNumber", textToJson[3]);
			VNFjson.put("revenue", textToJson[4]);
			VNFjson.put("name", textToJson[2]);
			VNFjson.put("iteration", textToJson[0]);
			VNFjson.put("vnfType", textToJson[6]);
			VNFjson.put("bandwidthUnitPrice", textToJson[7]);
			VNFjson.put("pricePerUser", textToJson[8]);
			
			JSONArray currentStatus = new JSONArray();
			currentStatus.put(VNFjson);
			// Read through file one line at time. Print line # and line
            while (line != null){
                System.out.println(count+": "+line);
                line = bufRead.readLine();
                if(line != null){
                textToJson = line.split(":");
                VNFjson  = new JSONObject();
    		   	VNFjson.put("bandwidth", textToJson[1]);
    			VNFjson.put("utility", textToJson[5]);
    			VNFjson.put("userNumber", textToJson[3]);
    			VNFjson.put("revenue", textToJson[4]);
    			VNFjson.put("name", textToJson[2]);
    			VNFjson.put("iteration", textToJson[0]);
    			VNFjson.put("vnfType", textToJson[6]);
    			VNFjson.put("bandwidthUnitPrice", textToJson[7]);
    			VNFjson.put("pricePerUser", textToJson[8]);
    			currentStatus.put(VNFjson);
                }   
            }
            
            bufRead.close();
			try (FileWriter file = new FileWriter(folderPath + "/" +fileName+".json")) {
				
	            file.write(currentStatus.toString());
	            file.flush();

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
        }catch (ArrayIndexOutOfBoundsException e){
            /* If no file was passed on the command line, this expception is
			generated. A message indicating how to the class should be
			called is displayed */
			System.out.println("Usage: java ReadFile filename\n");			

		}catch (IOException e){
			// If another exception is generated, print a stack trace
            e.printStackTrace();
        }
	}

	public boolean isRunning() {
		// TODO Auto-generated method stub
		return nmOperatorIsRunning;
	}
}
