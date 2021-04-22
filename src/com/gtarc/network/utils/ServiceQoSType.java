package com.gtarc.network.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cemakpolat, doruksahinel
 */

public class ServiceQoSType {
	// default classes
	public static final String xMBB = "xMBB";// xMBB (ExtremeMobileBroadBand)
	public static final String mMTC = "mMTC";//Massive Machine-Type Communication (mMTC)
	public static final String uMtC = "uMtC";// Ultra-reliable Machine/Type Communication (uMtC)

	public String serviceTypeName;
	public double alpha1 = 0; // bandwidth weight
	public double alpha2 = 0; // price weight
	public double alpha3 = 0; // delay weight
	public double alpha4 = 0;

	public static List<ServiceQoSType> qostypes = new ArrayList<ServiceQoSType>();
	public ServiceQoSType(String sname, double d, double e, double f, double r) {
		// TODO Auto-generated constructor stub
		this.serviceTypeName = sname;
		this.alpha1 = d;
		this.alpha2 = e;
		this.alpha3 = f;
		this.alpha4 = r;
	}
	public void addNewQoSType(String qosmodel) {
		// TODO: add the interface to the users.
	}
	public static boolean isQoSTypesAdded = false;
	public static void addDefaultQoSTypes() {
		if(!isQoSTypesAdded) {
			ServiceQoSType xmBB = new ServiceQoSType("xMBB",0.001, 0.015, 0.015, 0.01);
			qostypes.add(xmBB);
			ServiceQoSType mMTC = new ServiceQoSType("mMTC",0, 0, 0, 0);
			qostypes.add(mMTC);
			ServiceQoSType uMtC = new ServiceQoSType("uMtC",0, 0, 0, 0);
			qostypes.add(uMtC);
			isQoSTypesAdded = true;
		}
		
	}
	
	public static ServiceQoSType getServiceQoSType(String type) {
		for(ServiceQoSType stype : qostypes) {
			if(stype.serviceTypeName.equalsIgnoreCase(type)) {
				return stype;
			}
		}
		return null;
	}
}