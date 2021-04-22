package de.gtarc.network.vnf;

/**
 * This class creates the data path between service users and VNF servers.
 * @author cemakpolat, doruksahinel
 *
 */
public class VNFPath {
	public VNF vnf;
	public Path path;
	public VNFPath(VNF v, Path p){
		this.vnf = v;
		this.path = p;
	}
}
