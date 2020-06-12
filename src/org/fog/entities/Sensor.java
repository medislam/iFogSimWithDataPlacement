package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.Parallel.CloudSimParallel;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoLocation;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.Distribution;

public class Sensor extends SimEntity{
	
	private int gatewayDeviceId;
	private GeoLocation geoLocation;
	private long outputSize;
	private String appId;
	private int userId;
	private String tupleType;
	private String sensorName;
	private String destModuleName;
	private Distribution transmitDistribution;
	private int controllerId;
	private Application app;
	private float latency;
	
	public Sensor(String name, int userId, String appId, int gatewayDeviceId, float latency, GeoLocation geoLocation, 
			Distribution transmitDistribution, int cpuLength, int nwLength, String tupleType, String destModuleName) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		this.geoLocation = geoLocation;
		this.outputSize = 3;
		this.setTransmitDistribution(transmitDistribution);
		setUserId(userId);
		setDestModuleName(destModuleName);
		setTupleType(tupleType);
		setSensorName(sensorName);
		setLatency(latency);
	}
	
	public Sensor(String name, int userId, String appId, int gatewayDeviceId, float latency, GeoLocation geoLocation, 
			Distribution transmitDistribution, String tupleType) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		this.geoLocation = geoLocation;
		this.outputSize = 3;
		this.setTransmitDistribution(transmitDistribution);
		setUserId(userId);
		setTupleType(tupleType);
		setSensorName(sensorName);
		setLatency(latency);
	}
	
	/**
	 * This constructor is called from the code that generates PhysicalTopology from JSON
	 * @param name
	 * @param tupleType
	 * @param string 
	 * @param userId
	 * @param appId
	 * @param transmitDistribution
	 */
	public Sensor(String name, String tupleType, int userId, String appId, Distribution transmitDistribution) {
		super(name);
		this.setAppId(appId);
		this.setTransmitDistribution(transmitDistribution);
		setTupleType(tupleType);
		setSensorName(tupleType);
		setUserId(userId);
	}
	
	public Sensor(String name, String tupleType, int userId, String appId, Distribution transmitDistribution, CloudSimParallel cloudSimParallel) {
		super(name, cloudSimParallel);
		this.setAppId(appId);
		this.setTransmitDistribution(transmitDistribution);
		setTupleType(tupleType);
		setSensorName(tupleType);
		setUserId(userId);
	}
	
	public void transmit(){
		AppEdge _edge = null;
		////System.out.println("getTupleType():"+getTupleType());
		
		_edge = getApp().getEdgeMap().get(getTupleType());
		
		if(_edge == null){
			System.out.println("Error!!!");
			System.out.println("Application n:"+getAppId()+" hasn't tuple type:"+getTupleType()+" for Sensor:"+getSensorName());
			System.exit(0);
		}
		
//		for(AppEdge edge : getApp().getEdges()){
//			if(edge.getTupleType().equals(getTupleType())){
//				// //System.out.println("Tuple are finded!");
//				_edge = edge;
//				break;
//			}
//		}
		
		////System.out.println(_edge.toString());
		/* construct the new tuple */
		long cpuLength = (long) _edge.getTupleCpuLength();
		long nwLength = (long) _edge.getTupleNwLength();
		
		Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength, outputSize, 
				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
		tuple.setUserId(getUserId());
		tuple.setTupleType(getTupleType());
		
		tuple.setDestModuleName(_edge.getDestination());
		tuple.setSrcModuleName(getSensorName());
		Logger.debug(getName(), "Sending tuple with tupleId = "+tuple.getCloudletId());
//		Logger.debug(getName(), "Sending tuple "+tuple.getCloudletId()+"to "+tuple.getDestModuleName()+" with delay="+delay);

		int actualTupleId = updateTimings(getSensorName(), tuple.getDestModuleName().get(0));
		tuple.setActualTupleId(actualTupleId);
		
		int ex = DataPlacement.Basis_Exchange_Unit;
		long tupleDataSize = tuple.getCloudletFileSize();
		int nb_Unit = (int) (tupleDataSize / ex);
		if(tupleDataSize % ex != 0) nb_Unit++;
		float delay = getLatency();
	
//		LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+delay*nb_Unit);
//		LatencyStats.add_Overall_write_Letency(LatencyStats.getOverall_write_Latency()+delay*nb_Unit);
		
		//System.out.println("Node name:"+getName());
		//System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
		//System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
		//System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());

		send(gatewayDeviceId, delay*nb_Unit, FogEvents.TUPLE_PROCESS,tuple);
	}
	public void transmit(CloudSimParallel cloudSimParallel){
		AppEdge _edge = null;
		////System.out.println("getTupleType():"+getTupleType());
		
		_edge = getApp().getEdgeMap().get(getTupleType());
		
		if(_edge == null){
			System.out.println("Error!!!");
			System.out.println("Application n:"+getAppId()+" hasn't tuple type:"+getTupleType()+" for Sensor:"+getSensorName());
			System.exit(0);
		}
		
//		for(AppEdge edge : getApp().getEdges()){
//			if(edge.getTupleType().equals(getTupleType())){
//				// //System.out.println("Tuple are finded!");
//				_edge = edge;
//				break;
//			}
//		}
		
		////System.out.println(_edge.toString());
		/* construct the new tuple */
		long cpuLength = (long) _edge.getTupleCpuLength();
		long nwLength = (long) _edge.getTupleNwLength();
		
		Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength, outputSize, 
				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
		tuple.setUserId(getUserId());
		tuple.setTupleType(getTupleType());
		
		tuple.setDestModuleName(_edge.getDestination());
		tuple.setSrcModuleName(getSensorName());
		Logger.debug(getName(), "Sending tuple with tupleId = "+tuple.getCloudletId());
//		Logger.debug(getName(), "Sending tuple "+tuple.getCloudletId()+"to "+tuple.getDestModuleName()+" with delay="+delay);

		int actualTupleId = updateTimings(getSensorName(), tuple.getDestModuleName().get(0));
		tuple.setActualTupleId(actualTupleId);
		
		int ex = DataPlacement.Basis_Exchange_Unit;
		long tupleDataSize = tuple.getCloudletFileSize();
		int nb_Unit = (int) (tupleDataSize / ex);
		if(tupleDataSize % ex != 0) nb_Unit++;
		float delay = getLatency();
	
//		LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+delay*nb_Unit);
//		LatencyStats.add_Overall_write_Letency(LatencyStats.getOverall_write_Latency()+delay*nb_Unit);
		
		//System.out.println("Node name:"+getName());
		//System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
		//System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
		//System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());

		send(gatewayDeviceId, delay*nb_Unit, FogEvents.TUPLE_PROCESS,tuple, cloudSimParallel);
	}
	
	private int updateTimings(String src, String dest){
		Application application = getApp();
		for(AppLoop loop : application.getLoops()){
			if(loop.hasEdge(src, dest)){
				
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				return tupleId;
			}
		}
		return -1;
	}
	
	@Override
	public void startEntity() {
		send(gatewayDeviceId, CloudSim.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, geoLocation);
		//sendNow(gatewayDeviceId, FogEvents.SENSOR_JOINED, geoLocation);
		send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
	}
	
	@Override
	public void startEntity(CloudSimParallel cloudSimParallel) {
		System.out.println(this.getName()+": Send snecor joined to:"+gatewayDeviceId);
		send(gatewayDeviceId, cloudSimParallel.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, geoLocation, cloudSimParallel);
		//sendNow(gatewayDeviceId, FogEvents.SENSOR_JOINED, geoLocation);
		System.out.println(this.getName()+": Send snecor emit tuple to"+ getId());
		send2(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE, cloudSimParallel);
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ACK:
			//nothing
			break;
		case FogEvents.SENSOR_JOINED:
			//nothing
			break;
		case FogEvents.EMIT_TUPLE:
			transmit();
			System.out.println("Send ev for the next period:getNextValue() seq ="+getTransmitDistribution().getNextValue());
			send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
			break;
		}
			
	}
	
	@Override
	public void processEvent(SimEvent ev, CloudSimParallel cloudSimParallel) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ACK:
			//nothing
			break;
		case FogEvents.SENSOR_JOINED:
			//nothing
			break;
		case FogEvents.EMIT_TUPLE:
			transmit(cloudSimParallel);
			System.out.println("Send ev for the next period:getNextValue() par="+getTransmitDistribution().getNextValue());
			send2(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE, cloudSimParallel);
			break;
		}
			
	}

	@Override
	public void shutdownEntity() {
		
	}

	public int getGatewayDeviceId() {
		return gatewayDeviceId;
	}

	public void setGatewayDeviceId(int gatewayDeviceId) {
		this.gatewayDeviceId = gatewayDeviceId;
	}

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getTupleType() {
		return tupleType;
	}

	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getDestModuleName() {
		return destModuleName;
	}

	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}

	public Distribution getTransmitDistribution() {
		return transmitDistribution;
	}

	public void setTransmitDistribution(Distribution transmitDistribution) {
		this.transmitDistribution = transmitDistribution;
	}

	public int getControllerId() {
		return controllerId;
	}

	public void setControllerId(int controllerId) {
		this.controllerId = controllerId;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	public float getLatency() {
		return latency;
	}

	public void setLatency(float latency) {
		this.latency = latency;
	}

	@Override
	public void shutdownEntity(CloudSimParallel cloudSimParallel) {
		// TODO Auto-generated method stub
		
	}

}
