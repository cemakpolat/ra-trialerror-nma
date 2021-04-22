package com.gtarc.network.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.gtarc.network.utils.Console;

/**
 * 
 * @author cemakpolat, doruksahinel
 *
 */

public class VNFTestConfig {

	// user number
	private int totalUserNumber = 0;
	private double bandwidthUnitPrice = 0;
	private double delay = 0; // delay

	// VNF Configs
	private double beta1 = 0; // revenue weight
	private double beta2 = 0; // cost weight
	private double revenue = 0;
	private double reputation = 0; // reputation

	// VNF Price Increase
	private double priceIncrease = 0;
	private boolean isPriceIncreasing = false;
	private double initialUserPrice = 0;

	private  double assignedBandwidth = 0;

	
	public VNFTestConfig() {

	}

	public double getAssignedBandwidth() {
		return this.assignedBandwidth;
	}

	public void setAssignedBandwidth(double assignedBandwidth) {
		this.assignedBandwidth = assignedBandwidth;
	}

	public VNFTestConfig setTotalUserNumber(int value) {
		this.totalUserNumber = value;
		return this;
	}

	public VNFTestConfig setBandwidthUnitPrice(double value) {
		this.bandwidthUnitPrice = value;
		return this;
	}

	public VNFTestConfig setDelay(double value) {
		this.delay = value;
		return this;
	}
	
	public VNFTestConfig setReputation(double value) {
		this.reputation = value;
		return this;
	}
	public VNFTestConfig setIsPriceIncreasing(boolean value) {
		this.isPriceIncreasing = value;
		return this;
	}

	public VNFTestConfig setPriceIncrease(double value) {
		this.priceIncrease = value;
		return this;
	}

	public VNFTestConfig setBetaParam(double value1, double value2) {
		this.beta1 = value1;
		this.beta2 = value2;
		return this;
	}

	public VNFTestConfig setRevenue(double value) {
		this.revenue = value;
		return this;
	}

	public VNFTestConfig setInitialPricePerUser(double value) {
		this.initialUserPrice = value;
		return this;
	}

	public double getBeta1() {
		return beta1;
	}

	public void setBeta1(double beta1) {
		this.beta1 = beta1;
	}

	public double getBeta2() {
		return beta2;
	}

	public void setBeta2(double beta2) {
		this.beta2 = beta2;
	}

	public double getBandwidthUnitPrice() {
		return bandwidthUnitPrice;
	}

	public int getTotalUserNumber() {
		return totalUserNumber;
	}

	public double getPriceIncrease() {
		return priceIncrease;
	}

	public boolean isPriceIncreasing() {
		return isPriceIncreasing;
	}

	public double getInitialUserPrice() {
		return initialUserPrice;
	}

	public double getDelay() {
		return delay;
	}
	
	public String configFolder = "configs";
	public static String configFileName1 ="VNFConfig1.txt";
	public static String configFileName2 ="VNFConfig2.txt";
	
	public void readConfigFile(String configFileName) {
		String fileNameFullPath = configFolder+"/"+configFileName;	
		try {

			FileReader input = new FileReader(fileNameFullPath);
			BufferedReader bufRead = new BufferedReader(input);
            String line; 
            line = bufRead.readLine();
            if(line != null && !line.contains("##") && !line.isEmpty()) {
            		assignParameter(line);
            }
			// Read through file one line at time. Print line # and line
            while (line != null){
                //System.out.println(": "+line);
                line = bufRead.readLine();
                if(line != null && !line.contains("##") && !line.isEmpty()) {
            			assignParameter(line);
                }
            }
            bufRead.close();
			
        }catch (ArrayIndexOutOfBoundsException e){
			System.out.println("Usage: java ReadFile filename\n");			
		}catch (IOException e){
            e.printStackTrace();
        }
	}
	
	public void assignParameter(String value) {
		String[] param = value.split("=");
		//Console.output(param[0] +" "+param[1]);
		switch(param[0].replaceAll("\\s","")) {
			case "totalUserNumber":
				this.setTotalUserNumber(Integer.parseInt(param[1].replaceAll("\\s","")));
				break;
			case "banwidthUnitPrice":
				this.setBandwidthUnitPrice(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "delay":
				this.setDelay(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "priceIncrease":
				String[] subparams = param[1].split(",");
				this.setIsPriceIncreasing(Boolean.parseBoolean(subparams[0].replaceAll("\\s","")));
				this.setPriceIncrease(Double.parseDouble(subparams[1].replaceAll("\\s","")));
				break;
			case "betas":
				String[] betas = param[1].split(",");
				double beta1 = 0 ;
				double beta2 = 0 ;
				for(int i = 0 ;i < betas.length ; i ++) {
					String[] bparams = betas[i].split(":");
					if(bparams[0].replaceAll("\\s","").equalsIgnoreCase("beta1")) {
						beta1 = Double.parseDouble(bparams[1].replaceAll("\\s",""));  
					} else if(bparams[0].replaceAll("\\s","").equalsIgnoreCase("beta2")) {
						beta2 = Double.parseDouble(bparams[1].replaceAll("\\s",""));  
					}
				}
				this.setBetaParam(beta1, beta2);
				break;
			case "revenue":
				this.setRevenue(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "initialPricePerUser":
				this.setInitialPricePerUser(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "assignedBandwidth":
				this.setAssignedBandwidth(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "reputation":
				this.setReputation(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			default:
				Console.output("Unknown variable! ->"+value);
		}
	}
	public static void main(String[] args){
		VNFTestConfig config = new VNFTestConfig();
		config.readConfigFile(VNFTestConfig.configFileName2);
		
	}

	public double getReputation() {
		return this.reputation;
	}
	
	
//	  public static void main(String[] args) { VNFTestConfig case1 = new
//	  VNFTestConfig();
//	  case1.setUserGroupNumber(2).setUserNumber(3000).setBandwidthUnitPrice(1).
//	  setDelay(5).setPriceIncrease(1).setIsPriceIncreasing(false).setBetaParam(1,1)
//	  .setRevenue(0);
//	  
//	  VNFTestConfig case2 = new VNFTestConfig();
//	  case1.setUserGroupNumber(2).setUserNumber(3000).setBandwidthUnitPrice(1).
//	  setDelay(40).setPriceIncrease(30).setIsPriceIncreasing(true).setBetaParam(1,1
//	  ).setRevenue(0);
//	  
//	  VNF vnf1 = new VNF(case1, "VNFID1"); VNF vnf2 = new VNF(case2, "VNFID2");
//	  
//	  NetworkManager nm = new NetworkManager();
//	  
//	  nm.addVNFServer(vnf1); nm.addVNFServer(vnf2);
//	  
//	  
//	  }
	 
}
