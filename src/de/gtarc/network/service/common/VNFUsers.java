package de.gtarc.network.service.common;

import java.util.ArrayList;
import java.util.List;
/**
 * 
 * This class creates the list of VNF servers and users connected to them.
 * @author cemakpolat, doruksahinel
 *
 */
public class VNFUsers {
	public String vnfId = "";
	public List<Double> userUtil = new ArrayList<Double>(); // number of users // user utility
	public int totalUsers = 0;
	
	public VNFUsers(String vnfId, int usernumber){
		this.vnfId = vnfId;
		this.totalUsers = usernumber;
	}
	
	public VNFUsers(String vnfId){
		this.vnfId = vnfId;
	}
	public void addUserUtility(Double util){
		userUtil.add(util);
	}
	public String getVNFId(){
		return this.vnfId;
	}
	public int getUserNumber(){
		return this.userUtil.size();
	}
	public void setUserNumber(String un) {
		this.totalUsers = Integer.parseInt(un);
	}
}
