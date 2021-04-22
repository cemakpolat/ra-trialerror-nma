package de.gtarc.network.service.common;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtarc.network.utils.Console;
import com.gtarc.network.utils.ServiceQoSType;

import de.gtarc.network.agent.no.NetworkManager;
import de.gtarc.network.agent.no.NetworkSlice;
import de.gtarc.network.vnf.VNFPath;

public class ServiceNetworkSliceManager {
	final Logger logger = LoggerFactory.getLogger(ServiceNetworkSliceManager.class);
	private static NetworkSlice networkSlice = null; /** The network slice of the smart city service*/
	private static ServiceNetworkSliceManager nsliceManager = null;
	private String serviceId = "";
	private ServiceQoSType qosType = null;
	
	public static ServiceNetworkSliceManager getInstance() {
		if (nsliceManager == null) {
			nsliceManager = new ServiceNetworkSliceManager();
		}
		return nsliceManager;
	}
	
	public void setServiceRequirements(String serviceIdentifier, ServiceQoSType serviceQoSType) {
		this.serviceId = serviceIdentifier;
		this.qosType = serviceQoSType;
	}
	
	public boolean createNetworkSlice(){
		List<NetworkSlice> list = this.requestNetworkSlice(serviceId,qosType);
		if(list != null){
			this.selectSuitableNetworkSlice(list);
			this.showCurrentNetworkSlice();
			return true;
		}else {
			logger.debug("The network slice is not created");
		}
		return false;
	}
	
	/**
	 * This function requests a network slice from the network manager.
	 */
	public List<NetworkSlice> requestNetworkSlice(String serviceIdentifier,ServiceQoSType serviceQoSType){
		List<NetworkSlice> nsList = null; 
		nsList = NetworkManager.getInstance().requestNewSlices(serviceIdentifier,serviceQoSType);		
		return nsList;
	}

	/**
	 * This function selects the network slice from 
	 * the list of alternative network slices suggested 
	 * by the network manager. 
	 */
	public void selectSuitableNetworkSlice (List<NetworkSlice>  nsliceList){
		networkSlice = nsliceList.get(0); // TODO: Selection can be improved!
	}
	
	/**
	 * This function updates the network slice. 
	 */
	public void updateNetworkSlice() {
		NetworkSlice ns = NetworkManager.getInstance().getNetworkSlice(networkSlice.getSliceId()); 
		if(ns != null){
			networkSlice = ns ;
			showCurrentNetworkSlice();
		}
	}
	public void showCurrentNetworkSlice() {
		Map<String, VNFPath> paths = networkSlice.getVNFPaths();
		for (Map.Entry<String, VNFPath> entry : paths.entrySet()) {
		    String key = entry.getKey();
		    VNFPath value = entry.getValue();
		    //Console.output(key+" bandwidth "+value.path.getBandwidth()+"");
		}
	}
	public NetworkSlice getNetworkSlice(){
		if(networkSlice == null){
			Console.output("Network Slice is null");
		}
		return networkSlice;
	}

	public void updateVNFPathTableStatus() {
		NetworkManager.getInstance().setVNFPathTableStatus(false);
	}

	public boolean isVNFPathTableUpdated() {
		return NetworkManager.getInstance().isVNFPathTableUpdated();
	}

	public void updateVNFUserTable(List<VNFUsers> vnfUserTable) {
		NetworkManager.getInstance().updateVNFUserTable(vnfUserTable);		
	}
	
	
	
}
