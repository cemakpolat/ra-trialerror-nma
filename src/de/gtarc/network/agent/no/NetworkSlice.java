package de.gtarc.network.agent.no;

import java.util.Map;

import de.gtarc.network.vnf.VNFPath;

/**
 * 
 * This class is used to create network slices for smart city services inside the given network topology.
 * @author cemakpolat, doruksahinel
 *
 */

public class NetworkSlice {
	private String sliceId;
	public Map<String,VNFPath> vnfWithPaths = null;
	public NetworkSlice(){}
	public NetworkSlice(String id, Map<String,VNFPath> vnfWithPaths){
		this.sliceId = id;
		this.vnfWithPaths = vnfWithPaths;
		
	}
	public  Map<String,VNFPath> getVNFPaths() {
		return vnfWithPaths;
	}
	public String getSliceId() {
		return this.sliceId;
	}
}