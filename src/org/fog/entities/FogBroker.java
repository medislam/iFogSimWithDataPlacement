package org.fog.entities;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.fog.Parallel.CloudSimParallel;

public class FogBroker extends PowerDatacenterBroker{

	public FogBroker(String name) throws Exception {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public FogBroker(String name, CloudSimParallel cloudSimParallel) throws Exception {
		super(name, cloudSimParallel);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startEntity() {
		// TODO Auto-generated method stub
		System.out.println(getName()+" is starting...");
	}
	
	
	@Override
	public void startEntity(CloudSimParallel cloudSimParallel) {
		// TODO Auto-generated method stub
		System.out.println(getName()+" is starting...");
	}

	@Override
	public void processEvent(SimEvent ev) {
		// TODO Auto-generated method stub
		System.out.println("ForBroker' processEvent!");
		
	}

	@Override
	public void shutdownEntity() {
		// TODO Auto-generated method stub
		
	}
	
	

}
