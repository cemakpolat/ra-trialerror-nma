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
public class NMTestConfig {

	private static String vnf_Id_1 = "VNFID1";
	private static double vnf_path_delay_1 = 0;
	private static double vnf_path_bandwidth_1 = 0;

	private static String vnf_Id_2 = "VNFID2";
	private static double vnf_path_delay_2 = 0;
	private static double vnf_path_bandwidth_2 = 0;
	
	private static int totalUserNumber = 0;
	private static double totalBandwidth = 0;
	private static double bandwidthUnitPrice = 0;
	public static int totalUserGroup = 0;

	public NMTestConfig() {

	}

	// user number
	
	public NMTestConfig setVNF(String vnfId, double vnf_delay, double vnf_path_bw) {
		if(vnfId.equalsIgnoreCase(vnf_Id_1)) {
			NMTestConfig.vnf_path_delay_1 = vnf_delay;
			NMTestConfig.vnf_path_bandwidth_1 = vnf_path_bw;
		} else if(vnfId.equalsIgnoreCase(vnf_Id_2)) {
			NMTestConfig.vnf_path_delay_2 = vnf_delay;
			NMTestConfig.vnf_path_bandwidth_2 = vnf_path_bw;
		}
		return this;
	}
	public static String getVnf_Id_1() {
		return vnf_Id_1;
	}

	public static void setVnf_Id_1(String vnf_Id_1) {
		NMTestConfig.vnf_Id_1 = vnf_Id_1;
	}

	public static double getVnf_path_delay_1() {
		return vnf_path_delay_1;
	}

	public static void setVnf_path_delay_1(double vnf_path_delay_1) {
		NMTestConfig.vnf_path_delay_1 = vnf_path_delay_1;
	}

	public static double getVnf_path_bandwidth_1() {
		return vnf_path_bandwidth_1;
	}

	public static void setVnf_path_bandwidth_1(double vnf_path_bandwidth_1) {
		NMTestConfig.vnf_path_bandwidth_1 = vnf_path_bandwidth_1;
	}

	public static String getVnf_Id_2() {
		return vnf_Id_2;
	}

	public static void setVnf_Id_2(String vnf_Id_2) {
		NMTestConfig.vnf_Id_2 = vnf_Id_2;
	}

	public static double getVnf_path_delay_2() {
		return vnf_path_delay_2;
	}

	public static void setVnf_path_delay_2(double vnf_path_delay_2) {
		NMTestConfig.vnf_path_delay_2 = vnf_path_delay_2;
	}

	public static double getVnf_path_bandwidth_2() {
		return vnf_path_bandwidth_2;
	}

	public static void setVnf_path_bandwidth_2(double vnf_path_bandwidth_2) {
		NMTestConfig.vnf_path_bandwidth_2 = vnf_path_bandwidth_2;
	}

	public static int getTotalUserGroup() {
		return totalUserGroup;
	}
	
	public static void setTotalUserGroup(int value) {
		totalUserGroup = value;
	}
	
	public static int getTotalUserNumber() {
		return totalUserNumber;
	}

	public static double getTotalBandwidth() {
		return totalBandwidth;
	}

	public static double getBandwidthUnitPrice() {
		return bandwidthUnitPrice;
	}

	public NMTestConfig setUserGroupNumber(int value) {
		this.totalUserGroup = value;
		return this;
	}

	public NMTestConfig setTotalUserNumber(int value) {
		this.totalUserNumber = value;
		return this;
	}

	public NMTestConfig setBandwidthUnitPrice(double d) {
		this.bandwidthUnitPrice = d;
		return this;
	}
	
	public NMTestConfig setTotalBandwidth(double value) {
		this.totalBandwidth = value;
		return this;
	}

	public static int getUserNumber(String vnf_Id) {
		if(vnf_Id.equalsIgnoreCase(vnf_Id_1)) {
			return totalUserNumber/2;
		} else if(vnf_Id.equalsIgnoreCase(vnf_Id_2)) {
			return totalUserNumber/2;
		}
		return 0;
	}
	// move this parameters later to the service config 
	public double alpha1 = 0;
	public double alpha2 = 0;
	public double alpha3 = 0;
	
	public void setAlphas(double alpha1,double alpha2,double alpha3) {
		this.alpha1 = alpha1;
		this.alpha2 = alpha2;
		this.alpha3 = alpha3;
	}
	public double getAlpha(String alphaName) {
		switch(alphaName) {
			case "alpha1":
				return this.alpha1;
			case "alpha2":
				return this.alpha2;
			case "alpha3":
				return this.alpha3;
			default:
				Console.output("Unknown alpha parameter");
		}
		return 0;
	}
	public String configFolder = "configs";
	public String configFile ="NMConfig.txt";
	public void readConfigFile() {
		String fileNameFullPath = configFolder+"/"+configFile;	
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
                //System.out.println(count+": "+line);
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
		//System.out.println(value);
		String[] param = value.split("=");
		switch(param[0].replaceAll("\\s","")) {
			case "totalUserNumber":
				this.setTotalUserNumber(Integer.parseInt(param[1].replaceAll("\\s","")));
				break;
			case "totalUserGroup":
				this.setUserGroupNumber(Integer.parseInt(param[1].replaceAll("\\s","")));
				break;
			case "banwidthUnitPrice":
				this.setBandwidthUnitPrice(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "totalBandwidth":
				this.setTotalBandwidth(Double.parseDouble(param[1].replaceAll("\\s","")));
				break;
			case "vnf":
				String[] subparams = param[1].split(",");
				this.setVNF(subparams[0].replaceAll("\\s",""), Double.parseDouble(subparams[1].replaceAll("\\s","")),Double.parseDouble(subparams[2].replaceAll("\\s","")));
				// get vnfId, delay, bandwidth
			case "alphas":
				String[] alphas = param[1].split(",");
				double alpha1 = 0 ;
				double alpha2 = 0 ;
				double alpha3 = 0 ;
				for(int i = 0 ;i < alphas.length ; i ++) {
					String[] aparams = alphas[i].split(":");
					if(aparams[0].replaceAll("\\s","").equalsIgnoreCase("alpha1")) {
						alpha1 = Double.parseDouble(aparams[1].replaceAll("\\s",""));  
					} else if(aparams[0].replaceAll("\\s","").equalsIgnoreCase("alpha2")) {
						alpha2 = Double.parseDouble(aparams[1].replaceAll("\\s",""));  
					} else if(aparams[0].replaceAll("\\s","").equalsIgnoreCase("alpha3")) {
						alpha3 = Double.parseDouble(aparams[1].replaceAll("\\s",""));  
					}
				}
				this.setAlphas(alpha1, alpha2, alpha3);
				break;
			default:
				Console.output("Unknown variable!");
		}
	}
	public static void main(String[] args){
//		nmConfig.setUserGroupNumber(2).setTotalUserNumber(20).setTotalBandwidth(10000)
//		.setBandwidthUnitPrice(1).setVNF("VNFID1", 5, 500).setVNF("VNFID2",40.0, 500);
		
		NMTestConfig config = new NMTestConfig();
		config.readConfigFile();
		Console.output(config.getAlpha("alpha1")+"");
		//Console.output(config.getVnf_path_bandwidth_2()+"");
	}

}
