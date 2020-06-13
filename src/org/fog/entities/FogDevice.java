package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.Parallel.CloudSimParallel;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.cplex.DataAllocation;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

public class FogDevice extends PowerDatacenter {
	protected Queue<Tuple> northTupleQueue;
	protected Queue<Pair<Tuple, Integer>> southTupleQueue;
	
	protected List<String> activeApplications;
	
	protected Map<String, Application> applicationMap;
	protected Map<String, List<String>> appToModulesMap;
	protected Map<Integer, Float> childToLatencyMap;
 
	protected Map<Integer, Integer> cloudTrafficMap;
	
	protected double lockTime;
	
	private int level;
	/**	
	 * ID of the parent Fog Device
	 */
	protected int parentId;
	
	/**
	 * IDs of left right
	 */
	protected int leftId;
	protected int rightId;
	
	/**
	 * IDs of the children Fog devices
	 */
	protected List<Integer> childrenIds;
	
	
	/**
	 * Latencies of left and right FogDivces
	 */
	protected float leftLatency;
	protected float rightLatency;
	protected float uplinkLatency;
	
	
	protected float uplinkBandwidth;
	protected float downlinkBandwidth;
	protected float leftlinkBandwidth;
	protected float rightlinkBandwidth;
	/**
	 * ID of the Controller
	 */
	protected int controllerId;
	

	protected Map<Integer, List<String>> childToOperatorsMap;
	
	/**
	 * Flag denoting whether the link southwards from this FogDevice is busy
	 */
	protected boolean isSouthLinkBusy;
	
	/**
	 * Flag denoting whether the link northwards from this FogDevice is busy
	 */
	protected boolean isNorthLinkBusy;
	
	
	/**
	 * Flag denoting whether the link leftwards from this FogDevice is busy
	 */
	protected boolean isLeftLinkBusy;
	
	/**
	 * Flag denoting whether the link rightwards from this FogDevice is busy
	 */
	protected boolean isRightLinkBusy;
	
	
	protected List<Pair<Integer, Double>> associatedActuatorIds;
	
	protected double energyConsumption;
	protected double lastUtilizationUpdateTime;
	protected double lastUtilization;
	
	
	protected double ratePerMips;
	
	protected double totalCost;
	
	protected Map<String, Map<String, Integer>> moduleInstanceCount;
	
	public FogDevice(
			String name, 
			FogDeviceCharacteristics characteristics, 
			VmAllocationPolicy vmAllocationPolicy, 
			List<Storage> storageList,  
			int rightId, 
			int leftId, 
			float rightLatency, 
			float leftLatency, 
			double schedulingInterval,
            float uplinkBandwidth, 
            float downlinkBandwidth, 
            float uplinkLatency, 
            double ratePerMips
            ) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setLastProcessTime(0.0);
		setStorageList(storageList);
		setVmList(new ArrayList<Vm>());
		setSchedulingInterval(schedulingInterval);
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setRatePerMips(ratePerMips);
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		
		setLeftId(leftId);
		setRightId(rightId);
		setRightLatency(rightLatency);
		setLeftLatency(leftLatency);
		
		for (Host host : getCharacteristics().getHostList()) {
			host.setDatacenter(this);
		}
		setActiveApplications(new ArrayList<String>());
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		// stores id of this class
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, Application>();
		appToModulesMap = new HashMap<String, List<String>>();
		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setChildToLatencyMap(new HashMap<Integer, Float>());
	}
	
	
	public FogDevice(
			String name, 
			FogDeviceCharacteristics characteristics, 
			VmAllocationPolicy vmAllocationPolicy, 
			List<Storage> storageList,  
			int rightId, 
			int leftId, 
			float rightLatency, 
			float leftLatency, 
			double schedulingInterval,
            float uplinkBandwidth, 
            float downlinkBandwidth, 
            float uplinkLatency, 
            double ratePerMips, 
            CloudSimParallel cloudSimParallel
            ) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, cloudSimParallel);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setLastProcessTime(0.0);
		setStorageList(storageList);
		setVmList(new ArrayList<Vm>());
		setSchedulingInterval(schedulingInterval);
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setRatePerMips(ratePerMips);
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		
		setLeftId(leftId);
		setRightId(rightId);
		setRightLatency(rightLatency);
		setLeftLatency(leftLatency);
		
		for (Host host : getCharacteristics().getHostList()) {
			host.setDatacenter(this);
		}
		setActiveApplications(new ArrayList<String>());
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		// stores id of this class
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, Application>();
		appToModulesMap = new HashMap<String, List<String>>();
		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setChildToLatencyMap(new HashMap<Integer, Float>());
	}
	
	/**
	 * Overrides this method when making a new and different type of resource. <br>
	 * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void registerOtherEntity() {
		
	}
	
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev);
			break;
		case FogEvents.RELEASE_OPERATOR:
			processOperatorRelease(ev);
			break;
		case FogEvents.SENSOR_JOINED:
			processSensorJoining(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			updateNorthTupleQueue();
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			updateSouthTupleQueue();
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			updateActiveApplications(ev);
			break;
		case FogEvents.ACTUATOR_JOINED:
			processActuatorJoined(ev);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			updateModuleInstanceCount(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev);
			break;
		case FogEvents.TUPLE_STORAGE:
			processTupleStorage(ev);
			break;
		case FogEvents.TUPLE_PROCESS:
			processTupleProcess(ev);
			break;
		case CloudSimTags.VM_DATACENTER_EVENT:
			checkCloudletCompletion((Tuple) ev.getData());
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void processOtherEvent(SimEvent ev, CloudSimParallel cloudSimParallel) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev, cloudSimParallel);
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev, cloudSimParallel);
			break;
		case FogEvents.RELEASE_OPERATOR:
			processOperatorRelease(ev, cloudSimParallel);
			break;
		case FogEvents.SENSOR_JOINED:
			processSensorJoining(ev, cloudSimParallel);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev, cloudSimParallel);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev, cloudSimParallel);
			break;
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			updateNorthTupleQueue(cloudSimParallel);
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			updateSouthTupleQueue(cloudSimParallel);
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			updateActiveApplications(ev, cloudSimParallel);
			break;
		case FogEvents.ACTUATOR_JOINED:
			processActuatorJoined(ev, cloudSimParallel);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			updateModuleInstanceCount(ev, cloudSimParallel);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev, cloudSimParallel);
			break;
		case FogEvents.TUPLE_STORAGE:
			processTupleStorage(ev, cloudSimParallel);
			break;
		case FogEvents.TUPLE_PROCESS:
			processTupleProcess(ev, cloudSimParallel);
			break;
		case CloudSimTags.VM_DATACENTER_EVENT:
			checkCloudletCompletion((Tuple) ev.getData(), cloudSimParallel);
			break;
		default:
			break;
		}
	}
	
	private void processTupleStorage(SimEvent ev) {
		// TODO Auto-generated method stub
		////*System.out.println("Tuple Storage:"+ev.toString());
		Log.writeInLogFile(this.getName(), "Tuple Storage:"+ev.toString());
		Tuple tuple = (Tuple) ev.getData();
		
		////*System.out.println("tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "tuple:"+tuple.toString());
		
		for(String destModuleName : tuple.getDestModuleName()){
			List<String> dest = new ArrayList<String>();
			dest.add(destModuleName);

			String deviceName = ModuleMapping.getDeviceHostModule(destModuleName);
					
			Application application = applicationMap.get(tuple.getAppId());

		
			int destDevId = application.getFogDeviceByName(deviceName).getId();
			float latency = BasisDelayMatrix.getFatestLink(getId(), destDevId);
			
			int ex = DataPlacement.Basis_Exchange_Unit;
			long tupleDataSize = tuple.getCloudletFileSize();
			int nb_Unit = (int) (tupleDataSize / ex);
			if(tupleDataSize % ex != 0) nb_Unit++;
			
			//nb_Unit = 1;
			LatencyStats.add_Overall_read_Letency(LatencyStats.getOverall_read_Latency()+latency*nb_Unit);
			LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+latency*nb_Unit);
			
			////*System.out.println("Node name:"+getName());
			////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
			////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
			////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
			
			
			Tuple tupleSend =  (Tuple) tuple.clone();

			tupleSend.setDestModuleName(dest);
			
			////*System.out.println("tupleSend:"+tupleSend.toString());
			Log.writeInLogFile(this.getName(), "tupleSend:"+tupleSend.toString());
	
			send(destDevId, latency*nb_Unit, FogEvents.TUPLE_PROCESS, tupleSend);
		}
		
		
		
	}
	
	
	private void processTupleStorage(SimEvent ev, CloudSimParallel cloudSimParallel) {
		// TODO Auto-generated method stub
		////*System.out.println("Tuple Storage:"+ev.toString());
		Log.writeInLogFile(this.getName(), "Tuple Storage:"+ev.toString());
		Tuple tuple = (Tuple) ev.getData();
		
		////*System.out.println("tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "tuple:"+tuple.toString());
		
		for(String destModuleName : tuple.getDestModuleName()){
			List<String> dest = new ArrayList<String>();
			dest.add(destModuleName);

			String deviceName = ModuleMapping.getDeviceHostModule(destModuleName);
					
			Application application = applicationMap.get(tuple.getAppId());

		
			int destDevId = application.getFogDeviceByName(deviceName).getId();
			float latency = BasisDelayMatrix.getFatestLink(getId(), destDevId);
			
			int ex = DataPlacement.Basis_Exchange_Unit;
			long tupleDataSize = tuple.getCloudletFileSize();
			int nb_Unit = (int) (tupleDataSize / ex);
			if(tupleDataSize % ex != 0) nb_Unit++;
			
			//nb_Unit = 1;
			LatencyStats.add_Overall_read_Letency(LatencyStats.getOverall_read_Latency()+latency*nb_Unit);
			LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+latency*nb_Unit);
			
			////*System.out.println("Node name:"+getName());
			////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
			////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
			////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
			
			
			Tuple tupleSend =  (Tuple) tuple.clone();

			tupleSend.setDestModuleName(dest);
			
			////*System.out.println("tupleSend:"+tupleSend.toString());
			Log.writeInLogFile(this.getName(), "tupleSend:"+tupleSend.toString());
	
			send(destDevId, latency*nb_Unit, FogEvents.TUPLE_PROCESS, tupleSend, cloudSimParallel);
		}
		
		
		
	}

	/**
	 * Perform miscellaneous resource management tasks
	 * @param ev
	 */
	private void manageResources(SimEvent ev) {
		updateEnergyConsumption();
		send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}
	
	private void manageResources(SimEvent ev, CloudSimParallel cloudSimParallel) {
		updateEnergyConsumption(cloudSimParallel);
		send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT,cloudSimParallel);
	}

	/**
	 * Updating the number of modules of an application module on this device
	 * @param ev instance of SimEvent containing the module and no of instances 
	 */
	private void updateModuleInstanceCount(SimEvent ev) {
		ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		String appId = config.getModule().getAppId();
		
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		////*System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
	}
	
	private void updateModuleInstanceCount(SimEvent ev, CloudSimParallel cloudSimParallel) {
		ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		String appId = config.getModule().getAppId();
		
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		////*System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
	}
	
	public void updateModuleInstanceCount(ModuleLaunchConfig config, CloudSimParallel cloudSimParallel) {
		
		String appId = config.getModule().getAppId();
		
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		////*System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
	}

	/**
	 * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
	 * @param ev SimEvent instance containing the edge to send tuple on
	 */
	private void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModule = edge.getSource();
		AppModule module = null;
		for(Vm vm : getHost().getVmList()){
			if(((AppModule)vm).getName().equals(srcModule)){
				module=(AppModule)vm;
				break;
			}
		}
		if(module == null)
			return;
		
		int instanceCount = getModuleInstanceCount().get(module.getAppId()).get(srcModule);
		
		/*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
		 */
		for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
			Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
			////*System.out.println("Sending tuple :"+tuple.toString());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);			
		}
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}
	
	private void sendPeriodicTuple(SimEvent ev, CloudSimParallel cloudSimParallel) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModule = edge.getSource();
		AppModule module = null;
		for(Vm vm : getHost().getVmList()){
			if(((AppModule)vm).getName().equals(srcModule)){
				module=(AppModule)vm;
				break;
			}
		}
		if(module == null)
			return;
		
		int instanceCount = getModuleInstanceCount().get(module.getAppId()).get(srcModule);
		
		/*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
		 */
		for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
			Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
			////*System.out.println("Sending tuple :"+tuple.toString());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);			
		}
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge, cloudSimParallel);
	}

	protected void processActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
//		double delay = (double)ev.getData();
//		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
		
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, (double) 10));
	}
	
	protected void processActuatorJoined(SimEvent ev, CloudSimParallel cloudSimParallel) {
		int actuatorId = ev.getSource();
//		double delay = (double)ev.getData();
//		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
		
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, (double) 10));
	}

	
	protected void updateActiveApplications(SimEvent ev) {
		Application app = (Application)ev.getData();
		getActiveApplications().add(app.getAppId());
	}
	
	protected void updateActiveApplications(SimEvent ev, CloudSimParallel cloudSimParallel) {
		Application app = (Application)ev.getData();
		getActiveApplications().add(app.getAppId());
	}

	
	public String getOperatorName(int vmId){
		for(Vm vm : this.getHost().getVmList()){
			if(vm.getId() == vmId)
				return ((AppModule)vm).getName();
		}
		return null;
	}
	
	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		////*System.out.println("updateCloudetProcessingWithoutSchedulingFutureEventsForce -> fogDevice.java");
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			if (time < minTime) {
				minTime = time;
			}

			Log.formatLine("%.2f: [Host #%d] utilization is %.2f%%", currentTime, host.getId(), host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine("\nEnergy consumption for the last time frame from %.2f to %.2f:", getLastProcessTime(), currentTime);

			for (PowerHost host : this.<PowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(previousUtilizationOfCpu, utilizationOfCpu, timeDiff);
				
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%", currentTime, host.getId(), getLastProcessTime(), previousUtilizationOfCpu * 100, utilizationOfCpu * 100);
				Log.formatLine("%.2f: [Host #%d] energy is %.2f W*sec", currentTime, host.getId(), timeFrameHostEnergy);
			}

			Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec\n", currentTime, timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		checkCloudletCompletion();

		/** Remove completed VMs **/
		/**
		 * Change made by HARSHIT GUPTA
		 */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/
		
		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}
	
	protected void checkCloudletCompletion(Tuple tuple){
		////*System.out.println("checkCloudletCompletion -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "Tuple is:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "checkCloudletCompletion -> FogDevice.java");
				
		Vm vm = getVmAllocationPolicy().getHostList().get(0).getVmList().get(0);
		
	
		Application application = getApplicationMap().get(tuple.getAppId());
		
		List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName().get(0), tuple, getId());
		////*System.out.println("resultantTuples="+resultantTuples.size());
		Log.writeInLogFile(this.getName(), "resultantTuples="+resultantTuples.size());
		for(Tuple resTuple : resultantTuples){
			////*System.out.println("Tuple:"+resTuple.toString());
			Log.writeInLogFile(this.getName(), "Tuple:"+resTuple.toString());
			resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
			resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
			updateTimingsOnSending(resTuple);
			sendToSelf(resTuple);
		}
	}
	
	protected void checkCloudletCompletion(Tuple tuple, CloudSimParallel cloudSimParallel){
		////*System.out.println("checkCloudletCompletion -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "Tuple is:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "checkCloudletCompletion -> FogDevice.java");
				
		Vm vm = getVmAllocationPolicy().getHostList().get(0).getVmList().get(0);
		
	
		Application application = getApplicationMap().get(tuple.getAppId());
		
		List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName().get(0), tuple, getId());
		////*System.out.println("resultantTuples="+resultantTuples.size());
		Log.writeInLogFile(this.getName(), "resultantTuples="+resultantTuples.size());
		for(Tuple resTuple : resultantTuples){
			////*System.out.println("Tuple:"+resTuple.toString());
			Log.writeInLogFile(this.getName(), "Tuple:"+resTuple.toString());
			resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
			resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
			updateTimingsOnSending(resTuple, cloudSimParallel);
			sendToSelf(resTuple, cloudSimParallel);
		}
	}
	
	protected void updateTimingsOnSending(Tuple resTuple) {
		// TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE. 
		// WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
		String srcModule = resTuple.getSrcModuleName();
		String destModule = resTuple.getDestModuleName().get(0);
		////*System.out.println("udpateTimingsOnSending Tuple");
		
		for(AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()){
			if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				resTuple.setActualTupleId(tupleId);
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				
				//Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);
				
			}
		}
	}
	
	protected void updateTimingsOnSending(Tuple resTuple, CloudSimParallel cloudSimParallel) {
		// TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE. 
		// WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
		String srcModule = resTuple.getSrcModuleName();
		String destModule = resTuple.getDestModuleName().get(0);
		////*System.out.println("udpateTimingsOnSending Tuple");
		
		for(AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()){
			if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				resTuple.setActualTupleId(tupleId);
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, cloudSimParallel.clock());
				
				//Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);
				
			}
		}
	}

	protected int getChildIdWithRouteTo(int targetDeviceId){
		for(Integer childId : getChildrenIds()){
			if(targetDeviceId == childId)
				return childId;
			if(((FogDevice)CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
				return childId;
		}
		return -1;
	}
	
	protected int getChildIdWithRouteTo(int targetDeviceId,  CloudSimParallel cloudSimParallel){
		for(Integer childId : getChildrenIds()){
			if(targetDeviceId == childId)
				return childId;
			if(((FogDevice)cloudSimParallel.getEntityById(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
				return childId;
		}
		return -1;
	}
	
	protected int getChildIdForTuple(Tuple tuple){
		if(tuple.getDirection() == Tuple.ACTUATOR){
			int gatewayId = ((Actuator)CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
			return getChildIdWithRouteTo(gatewayId);
		}
		return -1;
	}
	
	protected int getChildIdForTuple(Tuple tuple, CloudSimParallel cloudSimParallel){
		if(tuple.getDirection() == Tuple.ACTUATOR){
			int gatewayId = ((Actuator)cloudSimParallel.getEntityById(tuple.getActuatorId())).getGatewayDeviceId();
			return getChildIdWithRouteTo(gatewayId, cloudSimParallel);
		}
		return -1;
	}
	
	protected void updateAllocatedMips(String incomingOperator){
		getHost().getVmScheduler().deallocatePesForAllVms();
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
			}
		}
		
		updateEnergyConsumption();
		
	}
	
	protected void updateAllocatedMips(String incomingOperator, CloudSimParallel cloudSimParallel){
		getHost().getVmScheduler().deallocatePesForAllVms();
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
			}
		}
		
		updateEnergyConsumption(cloudSimParallel);
		
	}
	
	private void updateEnergyConsumption() {
		////*System.out.println("update energy consumption and cost on device :"+getName());
		double totalMipsAllocated = 0;
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler().getAllocatedMipsForVm(operator));
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
		}
		
		double timeNow = CloudSim.clock();
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption + (timeNow-lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization);
		setEnergyConsumption(newEnergyConsumption);
	
		/*if(getName().equals("d-0")){
			////*System.out.println("------------------------");
			////*System.out.println("Utilization = "+lastUtilization);
			////*System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			////*System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/
		
		double currentCost = getTotalCost();
		double newcost = currentCost + (timeNow-lastUtilizationUpdateTime)*getRatePerMips()*lastUtilization*getHost().getTotalMips();
		setTotalCost(newcost);
		
		lastUtilization = Math.min(1, totalMipsAllocated/getHost().getTotalMips());
		lastUtilizationUpdateTime = timeNow;
	}
	
	private void updateEnergyConsumption(CloudSimParallel cloudSimParallel) {
		////*System.out.println("update energy consumption and cost on device :"+getName());
		double totalMipsAllocated = 0;
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(cloudSimParallel.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler().getAllocatedMipsForVm(operator));
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
		}
		
		double timeNow = cloudSimParallel.clock();
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption + (timeNow-lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization);
		setEnergyConsumption(newEnergyConsumption);
	
		/*if(getName().equals("d-0")){
			////*System.out.println("------------------------");
			////*System.out.println("Utilization = "+lastUtilization);
			////*System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			////*System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/
		
		double currentCost = getTotalCost();
		double newcost = currentCost + (timeNow-lastUtilizationUpdateTime)*getRatePerMips()*lastUtilization*getHost().getTotalMips();
		setTotalCost(newcost);
		
		lastUtilization = Math.min(1, totalMipsAllocated/getHost().getTotalMips());
		lastUtilizationUpdateTime = timeNow;
	}

	protected void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		applicationMap.put(app.getAppId(), app);
	}
	
	public void addApptoApplicationMap(Application app) {
		applicationMap.put(app.getAppId(), app);
	}
	
	protected void processAppSubmit(SimEvent ev, CloudSimParallel cloudSimParallel) {
		Application app = (Application)ev.getData();
		applicationMap.put(app.getAppId(), app);
	}

	protected void addChild(int childId){
		if(CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
			return;
		if(!getChildrenIds().contains(childId) && childId != getId())
			getChildrenIds().add(childId);
		if(!getChildToOperatorsMap().containsKey(childId))
			getChildToOperatorsMap().put(childId, new ArrayList<String>());
	}
	
	protected void updateCloudTraffic(){
		int time = (int)CloudSim.clock()/1000;
		if(!cloudTrafficMap.containsKey(time))
			cloudTrafficMap.put(time, 0);
		cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
	}
	
	protected void updateCloudTraffic(CloudSimParallel cloudSimParallel){
		int time = (int)cloudSimParallel.clock()/1000;
		if(!cloudTrafficMap.containsKey(time))
			cloudTrafficMap.put(time, 0);
		cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
	}
	
	protected void sendTupleToActuator(Tuple tuple){
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
		
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
			if(tuple.getDestModuleName().get(0).equals(actuatorType)){
				int ex = DataPlacement.Basis_Exchange_Unit;
				long tupleDataSize = tuple.getCloudletFileSize();
				int nb_Unit = (int) (tupleDataSize / ex);
				if(tupleDataSize % ex != 0) nb_Unit++;
			
//				LatencyStats.add_Overall_read_Letency(LatencyStats.getOverall_read_Latency()+delay*nb_Unit);
//				LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+delay*nb_Unit);
				
				////*System.out.println("Node name:"+getName());
				////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
				////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
				////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
				
				send(actuatorId, delay * nb_Unit, FogEvents.TUPLE_PROCESS, tuple);
				return;
			}
		}
//		for(int childId : getChildrenIds()){
//			sendDown(tuple, childId);
//		}
	}
	
	protected void sendTupleToActuator(Tuple tuple, CloudSimParallel cloudSimParallel){
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
		
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)cloudSimParallel.getEntityById(actuatorId)).getActuatorType();
			if(tuple.getDestModuleName().get(0).equals(actuatorType)){
				int ex = DataPlacement.Basis_Exchange_Unit;
				long tupleDataSize = tuple.getCloudletFileSize();
				int nb_Unit = (int) (tupleDataSize / ex);
				if(tupleDataSize % ex != 0) nb_Unit++;
			
//				LatencyStats.add_Overall_read_Letency(LatencyStats.getOverall_read_Latency()+delay*nb_Unit);
//				LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+delay*nb_Unit);
				
				////*System.out.println("Node name:"+getName());
				////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
				////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
				////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
				
				send(actuatorId, delay * nb_Unit, FogEvents.TUPLE_PROCESS, tuple, cloudSimParallel);
				return;
			}
		}
//		for(int childId : getChildrenIds()){
//			sendDown(tuple, childId);
//		}
	}
	
	int numClients=0;
	
	protected void processTupleProcess(SimEvent ev){

		////*System.out.println("processTupleProcess: process tuple and send vm-data-center to self");
		Log.writeInLogFile(this.getName(), "processTupleProcess: process tuple and send vm-data-center to self");
		Tuple tuple = (Tuple)ev.getData();
		////*System.out.println("Tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Tuple:"+tuple.toString());
		if(ev.getDestination()!=getId()){
			////*System.out.println("Error!!! ev.Destination Id:"+ev.getDestination()+" is different to the Entity Id"+getId());
			Log.writeInLogFile(this.getName(), "Error!!! ev.Destination Id:"+ev.getDestination()+" is different to the Entity Id"+getId());
			System.exit(0);
		}


		if(ev.getSource()!=getId()){
			/* Send tuple ack */
			////*System.out.println("Sending Tuple Ack to evt source!");
			//Log.writeInLogFile(this.getName(), "Sending Tuple Ack to evt source!");
			//send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		}
		
		
		if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName().get(0))){
			/* Search the destination module (vm) in this host */	
			int vmId = -1;
			for(Vm vm : getHost().getVmList()){
				if(((AppModule)vm).getName().equals(tuple.getDestModuleName().get(0))){
					vmId = vm.getId();
					break;
				}
			}
			
			
			
			Application application = applicationMap.get(tuple.getAppId());
			String deviceName = ModuleMapping.getDeviceHostModule(tuple.getDestModuleName().get(0));
			//////*System.out.println(deviceName);
			
			int destDevId = application.getFogDeviceByName(deviceName).getId();
			
			if(destDevId != getId()){
				//*System.out.println("Error! Tuple destination module is not deployed in this entity! ->"+destDevId+"   !=   "+getId());
				System.exit(0);
			}
			
			
			if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName().get(0)) &&  tuple.getModuleCopyMap().get(tuple.getDestModuleName().get(0))!=vmId )){
				//*System.out.println("Error! vm Id < 0 or ...");
				Log.writeInLogFile(this.getName(), "Error! vm Id < 0 or ...");
				System.exit(0);
			}
			
			

			tuple.setVmId(vmId);
			//Logger.error(getName(), "Executing tuple for operator " + moduleName);
			
			//updateTimingsOnReceipt(tuple);
			
			executeTuple(ev, tuple.getDestModuleName().get(0));
			
		}
		
	}
	
	protected void processTupleProcess(SimEvent ev, CloudSimParallel cloudSimParallel){

//		System.out.println("Thread N:"+cloudSimParallel.getNumThread()+"  processTupleProcess: process tuple and send vm-data-center to self");
		Log.writeInLogFile(this.getName(), "processTupleProcess: process tuple and send vm-data-center to self");
		Tuple tuple = (Tuple)ev.getData();
//		System.out.println("Thread N:"+cloudSimParallel.getNumThread()+"  Tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Tuple:"+tuple.toString());
		if(ev.getDestination()!=getId()){
			System.out.println("Error!!! ev.Destination Id:"+ev.getDestination()+" is different to the Entity Id"+getId());
			Log.writeInLogFile(this.getName(), "Error!!! ev.Destination Id:"+ev.getDestination()+" is different to the Entity Id"+getId());
			System.exit(0);
		}


		if(ev.getSource()!=getId()){
			/* Send tuple ack */
			////*System.out.println("Sending Tuple Ack to evt source!");
			//Log.writeInLogFile(this.getName(), "Sending Tuple Ack to evt source!");
			//send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		}
		
		
		
		
		List<String> lisapp = appToModulesMap.get(tuple.getAppId());
		
//		System.out.println("lisapp"+lisapp);
//		System.out.println("appToModulesMap print");
//		for(String app : appToModulesMap.keySet()) {
//			System.out.println("list of module:"+appToModulesMap.get(app));
//		}
		
		if(lisapp.contains(tuple.getDestModuleName().get(0))){
			/* Search the destination module (vm) in this host */	
			int vmId = -1;
			for(Vm vm : getHost().getVmList()){
				if(((AppModule)vm).getName().equals(tuple.getDestModuleName().get(0))){
					vmId = vm.getId();
					break;
				}
			}
			
			
			
			Application application = applicationMap.get(tuple.getAppId());
			String deviceName = ModuleMapping.getDeviceHostModule(tuple.getDestModuleName().get(0));
			//////*System.out.println(deviceName);
			
			int destDevId = application.getFogDeviceByName(deviceName).getId();
			
			if(destDevId != getId()){
				//*System.out.println("Error! Tuple destination module is not deployed in this entity! ->"+destDevId+"   !=   "+getId());
				System.exit(0);
			}
			
			
			if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName().get(0)) &&  tuple.getModuleCopyMap().get(tuple.getDestModuleName().get(0))!=vmId )){
				//*System.out.println("Error! vm Id < 0 or ...");
				Log.writeInLogFile(this.getName(), "Error! vm Id < 0 or ...");
				System.exit(0);
			}
			

			tuple.setVmId(vmId);
			//Logger.error(getName(), "Executing tuple for operator " + moduleName);
			
			//updateTimingsOnReceipt(tuple);
			
			executeTuple(ev, tuple.getDestModuleName().get(0), cloudSimParallel);
			
		}
		
	}

	protected void processTupleArrival(SimEvent ev){
		////*System.out.println("processTupleArrival: for send tuple to other entites...");
		Log.writeInLogFile(this.getName(), "processTupleArrival: for send tuple to other entites...");
		Tuple tuple = (Tuple)ev.getData();
		//*System.out.println("Tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Tuple:"+tuple.toString());
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
	
		
		if(tuple.getDirection() == Tuple.ACTUATOR){
			sendTupleToActuator(tuple);
			return;
		}
		
		if(tuple.getDestModuleName()!=null){
			if(tuple.getDirection() == Tuple.UP)
				sendUp(tuple);
			}else{
					////*System.out.println(getName()+"\tError! There is no destination module! for tupel:"+ev.toString());
					Log.writeInLogFile(this.getName(), "Error! There is no destination module! for tupel:"+ev.toString());
					System.exit(0);
			}
	}
	
	protected void processTupleArrival(SimEvent ev, CloudSimParallel cloudSimParallel){
		////*System.out.println("processTupleArrival: for send tuple to other entites...");
		Log.writeInLogFile(this.getName(), "processTupleArrival: for send tuple to other entites...");
		Tuple tuple = (Tuple)ev.getData();
		//*System.out.println("Tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Tuple:"+tuple.toString());
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+cloudSimParallel.getEntityName(ev.getSource())+"|Dest : "+cloudSimParallel.getEntityName(ev.getDestination()));
	
		
		if(tuple.getDirection() == Tuple.ACTUATOR){
			sendTupleToActuator(tuple, cloudSimParallel);
			return;
		}
		
		if(tuple.getDestModuleName()!=null){
			if(tuple.getDirection() == Tuple.UP)
				sendUp(tuple, cloudSimParallel);
			}else{
					////*System.out.println(getName()+"\tError! There is no destination module! for tupel:"+ev.toString());
					Log.writeInLogFile(this.getName(), "Error! There is no destination module! for tupel:"+ev.toString());
					System.exit(0);
			}
	}

	protected void updateTimingsOnReceipt(Tuple tuple) {
		////*System.out.println("updateTimingsOnReceipt -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "updateTimingsOnReceipt -> FogDevice.java");
		Application app = getApplicationMap().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName().get(0);
		List<AppLoop> loops = app.getLoops();
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				if(startTime==null)
					break;
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}

	}
	
	protected void updateTimingsOnReceipt(Tuple tuple, CloudSimParallel cloudSimParallel) {
		////*System.out.println("updateTimingsOnReceipt -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "updateTimingsOnReceipt -> FogDevice.java");
		Application app = getApplicationMap().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName().get(0);
		List<AppLoop> loops = app.getLoops();
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				if(startTime==null)
					break;
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = cloudSimParallel.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}

	}

	protected void processSensorJoining(SimEvent ev){
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED);
	}
	
	protected void processSensorJoining(SimEvent ev, CloudSimParallel cloudSimParallel){
		send(ev.getSource(), cloudSimParallel.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, cloudSimParallel);
	}
	
	protected void executeTuple(SimEvent ev, String operatorId){
		////*System.out.println("executeTuple -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "executeTuple -> FogDevice.java");

		//TODO Power funda
		Logger.debug(getName(), "Executing tuple on module "+operatorId);
		////*System.out.println("Execute tuple on module "+operatorId);
		Tuple tuple = (Tuple)ev.getData();
		////*System.out.println("Tupel:"+tuple.toString());
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		//updateAllocatedMips(operatorId);
		processCloudletSubmit(ev, false);
		//updateAllocatedMips(operatorId);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/

	}
	
	protected void executeTuple(SimEvent ev, String operatorId, CloudSimParallel cloudSimParallel){
		////*System.out.println("executeTuple -> FogDevice.java");
		Log.writeInLogFile(this.getName(), "executeTuple -> FogDevice.java");

		//TODO Power funda
		Logger.debug(getName(), "Executing tuple on module "+operatorId);
		////*System.out.println("Execute tuple on module "+operatorId);
		Tuple tuple = (Tuple)ev.getData();
		////*System.out.println("Tupel:"+tuple.toString());
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		//updateAllocatedMips(operatorId);
		processCloudletSubmit(ev, false, cloudSimParallel);
		//updateAllocatedMips(operatorId);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/

	}
	
	protected void processModuleArrival(SimEvent ev){
		AppModule module = (AppModule)ev.getData();
		String appId = module.getAppId();
		
		//////*System.out.println("Creating module "+module.getName()+" on device "+getName());
		if(!appToModulesMap.containsKey(appId)){
			appToModulesMap.put(appId, new ArrayList<String>());
		}
		
		appToModulesMap.get(appId).add(module.getName());
		//processVmCreate(ev, false);
		if (module.isBeingInstantiated()) {
			module.setBeingInstantiated(false);
		}
		
		
		initializePeriodicTuples(module);
		
		module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module));
	}
	
	
	protected void processModuleArrival(SimEvent ev, CloudSimParallel cloudSimParallel){
		AppModule module = (AppModule)ev.getData();
		String appId = module.getAppId();
		
		//System.out.println("Creating module "+module.getName()+" on device "+getName()+"+\tappId="+appId);
		if(!appToModulesMap.containsKey(appId)){
			appToModulesMap.put(appId, new ArrayList<String>());
		}
		
		//System.out.println("appToModulesMap.get("+appId+").add("+module.getName()+")");
		appToModulesMap.get(appId).add(module.getName());
		//processVmCreate(ev, false);
		if (module.isBeingInstantiated()) {
			module.setBeingInstantiated(false);
		}
		
		
		initializePeriodicTuples(module, cloudSimParallel);
		
		module.updateVmProcessing(cloudSimParallel.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module));
	}
	
	public void processModuleArrival(AppModule module, CloudSimParallel cloudSimParallel){
		String appId = module.getAppId();
		
		//System.out.println("Creating module "+module.getName()+" on device "+getName()+"+\tappId="+appId);
		if(!appToModulesMap.containsKey(appId)){
			appToModulesMap.put(appId, new ArrayList<String>());
		}
		
		//System.out.println("appToModulesMap.get("+appId+").add("+module.getName()+")");
		appToModulesMap.get(appId).add(module.getName());
		//processVmCreate(ev, false);
		if (module.isBeingInstantiated()) {
			module.setBeingInstantiated(false);
		}
		
		
		initializePeriodicTuples(module, cloudSimParallel);
		
		module.updateVmProcessing(cloudSimParallel.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module));
	}
	
	private void initializePeriodicTuples(AppModule module) {
		////*System.out.println("Sending of perioding tuples from "+getName()+"?");
		Log.writeInLogFile(this.getName(), "Sending of perioding tuples from "+getName()+"?");
		String appId = module.getAppId();
		Application app = getApplicationMap().get(appId);
		
		//if there are a list of periodic tuples
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		for(AppEdge edge : periodicEdges){
			////*System.out.println("Sending of perdiong tuple :"+edge.toString());
			Log.writeInLogFile(this.getName(), "Sending of perdiong tuple :"+edge.toString());
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
		}
	}
	
	private void initializePeriodicTuples(AppModule module, CloudSimParallel cloudSimParallel) {
		//System.out.println("Sending of perioding tuples from "+getName()+"?");
		Log.writeInLogFile(this.getName(), "Sending of perioding tuples from "+getName()+"?");
		String appId = module.getAppId();
		Application app = getApplicationMap().get(appId);
		
		//if there are a list of periodic tuples
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		for(AppEdge edge : periodicEdges){
			//System.out.println("Sending of perdiong tuple :"+edge.toString());
			Log.writeInLogFile(this.getName(), "Sending of perdiong tuple :"+edge.toString());
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge, cloudSimParallel);
		}
	}

	protected void processOperatorRelease(SimEvent ev){
		this.processVmMigrate(ev, false);
	}
	
	protected void processOperatorRelease(SimEvent ev, CloudSimParallel cloudSimParallel){
		this.processVmMigrate(ev, false);
	}
	
	protected void updateNorthTupleQueue(){
//		if(!getNorthTupleQueue().isEmpty()){
//			Tuple tuple = getNorthTupleQueue().poll();
//			sendUpFreeLink(tuple);
//		}else{
//			setNorthLinkBusy(false);
//		}
	}
	
	protected void updateNorthTupleQueue(CloudSimParallel cloudSimParallel){
//		if(!getNorthTupleQueue().isEmpty()){
//			Tuple tuple = getNorthTupleQueue().poll();
//			sendUpFreeLink(tuple);
//		}else{
//			setNorthLinkBusy(false);
//		}
	}
		
	protected void sendUpFreeLink(Tuple tuple){
		/* With data placement */
		////*System.out.println("tuple:"+tuple.toString());
		int storageNodeId = getStorageNodeId(tuple.getTupleType());
		////*System.out.println("Send tuple to the storage node:"+storageNodeId);
		Log.writeInLogFile(this.getName(), "Send tuple to the storage node:"+storageNodeId);
		
		float latency = BasisDelayMatrix.getFatestLink(getId(), storageNodeId);

		int ex = DataPlacement.Basis_Exchange_Unit;
		long tupleDataSize = tuple.getCloudletFileSize();
		int nb_Unit = (int) (tupleDataSize / ex);
		if(tupleDataSize % ex != 0) nb_Unit++;
		
		LatencyStats.add_Overall_write_Letency(LatencyStats.getOverall_write_Latency()+latency*nb_Unit);
		LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+latency*nb_Unit);
		
		////*System.out.println("source node name:"+getName());
		////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
		////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
		////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
		
		send(storageNodeId, latency*nb_Unit , FogEvents.TUPLE_STORAGE, tuple);
		
		//send(parentId, networkDelay+getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		//NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());

	}
	
	protected void sendUpFreeLink(Tuple tuple, CloudSimParallel cloudSimParallel){
		/* With data placement */
		////*System.out.println("tuple:"+tuple.toString());
		int storageNodeId = getStorageNodeId(tuple.getTupleType());
		////*System.out.println("Send tuple to the storage node:"+storageNodeId);
		Log.writeInLogFile(this.getName(), "Send tuple to the storage node:"+storageNodeId);
		
		float latency = BasisDelayMatrix.getFatestLink(getId(), storageNodeId);

		int ex = DataPlacement.Basis_Exchange_Unit;
		long tupleDataSize = tuple.getCloudletFileSize();
		int nb_Unit = (int) (tupleDataSize / ex);
		if(tupleDataSize % ex != 0) nb_Unit++;
		
		LatencyStats.add_Overall_write_Letency(LatencyStats.getOverall_write_Latency()+latency*nb_Unit);
		LatencyStats.add_Overall_Letency(LatencyStats.getOverall_Latency()+latency*nb_Unit);
		
		////*System.out.println("source node name:"+getName());
		////*System.out.println("Overal read latency:"+LatencyStats.getOverall_read_Latency());
		////*System.out.println("Overal write latency:"+LatencyStats.getOverall_write_Latency());
		////*System.out.println("Overal latency:"+LatencyStats.getOverall_Latency());
		
		send(storageNodeId, latency*nb_Unit , FogEvents.TUPLE_STORAGE, tuple, cloudSimParallel);
		
		//send(parentId, networkDelay+getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		//NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());

	}
	
	
	private int getStorageNodeId(String tupleType) {
		// TODO Auto-generated method stub
		if(DataPlacement.storageMode.equals(DataPlacement.CloudStorage) ){
			////*System.out.println("CloudStorage -> a DataCenter will be choosed!");
			Log.writeInLogFile(this.getName(), "CloudStorage -> a DataCenter will be choosed!");
			return 3+(int) (Math.random()*DataPlacement.nb_DC);
			
		}else if(DataPlacement.storageMode.equals(DataPlacement.ClosestNode)){
			////*System.out.println("Closed node Storage -> the closet Fog node will be choosed!");
			Log.writeInLogFile(this.getName(), "Closed node Storage -> the closet Fog node will be choosed!");
			return this.getId();
			
		}else if(DataPlacement.storageMode.equals(DataPlacement.FogStorage)){
			////*System.out.println("Fog node Storage -> ideal Fog node will be choosed!");
			Log.writeInLogFile(this.getName(), "Fog node Storage -> ideal Fog node will be choosed!");
			return DataAllocation.getEmplacementNodeId(tupleType);
			
		}else if(DataPlacement.storageMode.equals(DataPlacement.ZoningStorage)){
			////*System.out.println("Zonnig Storage -> zonnig Fog node will be choosed!");
			Log.writeInLogFile(this.getName(), "Zonnig Storage -> zonnig Fog node will be choosed!");
			return DataAllocation.getEmplacementNodeId(tupleType);
			
		}else if(DataPlacement.storageMode.equals(DataPlacement.ZoningStorageParallel)){
			////*System.out.println("Zonnig Storage Parallel -> zonnig Fog node will be choosed!");
			Log.writeInLogFile(this.getName(), "Zonnig Storage Parallel -> zonnig Fog node will be choosed!");
			return DataAllocation.getEmplacementNodeId(tupleType);
			
		}else if(DataPlacement.storageMode.equals(DataPlacement.GraphPartitionStorage)){
			////*System.out.println("Graph Partition Storage -> a Fog node will be choosed!");
			Log.writeInLogFile(this.getName(), "Graph Partition Storage -> a Fog node will be choosed!");
			////*System.out.println("tupleType:"+tupleType);
			Log.writeInLogFile(this.getName(), "tupleType:"+tupleType);
			return DataAllocation.getEmplacementNodeId(tupleType);
			
		}else{
			////*System.out.println("Error on Storage mode, please choose 1..4");
			Log.writeInLogFile(this.getName(), "Error on Storage mode, please choose 1..4");
			System.exit(0);
		}
		return -1;
	}

	protected void sendUp(Tuple tuple){
		sendUpFreeLink(tuple);

	}
	
	protected void sendUp(Tuple tuple, CloudSimParallel cloudSimParallel){
		sendUpFreeLink(tuple, cloudSimParallel);

	}
	
	protected void updateSouthTupleQueue(){
		if(!getSouthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getSouthTupleQueue().poll(); 
			sendDownFreeLink(pair.getFirst(), pair.getSecond());
		}else{
			setSouthLinkBusy(false);
		}
	}
	
	protected void updateSouthTupleQueue(CloudSimParallel cloudSimParallel){
		if(!getSouthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getSouthTupleQueue().poll(); 
			sendDownFreeLink(pair.getFirst(), pair.getSecond(), cloudSimParallel);
		}else{
			setSouthLinkBusy(false);
		}
	}
	
	protected void sendDownFreeLink(Tuple tuple, int childId){
		double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
		//Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
		setSouthLinkBusy(true);
		float latency = getChildToLatencyMap().get(childId);
		send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
		send(childId, networkDelay+latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendDownFreeLink(Tuple tuple, int childId, CloudSimParallel cloudSimParallel){
		double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
		//Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
		setSouthLinkBusy(true);
		float latency = getChildToLatencyMap().get(childId);
		send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE, cloudSimParallel);
		send(childId, networkDelay+latency, FogEvents.TUPLE_ARRIVAL, tuple, cloudSimParallel);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	
	protected void sendDown(Tuple tuple, int childId){
		if(getChildrenIds().contains(childId)){
			if(!isSouthLinkBusy()){
				sendDownFreeLink(tuple, childId);
			}else{
				southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
			}
		}
	}
	
	protected void sendDown(Tuple tuple, int childId, CloudSimParallel cloudSimParallel){
		if(getChildrenIds().contains(childId)){
			if(!isSouthLinkBusy()){
				sendDownFreeLink(tuple, childId, cloudSimParallel);
			}else{
				southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
			}
		}
	}
	
	protected void sendToSelf(Tuple tuple){
		////*System.out.println("Sending the tuple to self for processing tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Sending the tuple to self for processing tuple:"+tuple.toString());
		//send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
		sendNow(getId(),  FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	protected void sendToSelf(Tuple tuple, CloudSimParallel cloudSimParallel){
		////*System.out.println("Sending the tuple to self for processing tuple:"+tuple.toString());
		Log.writeInLogFile(this.getName(), "Sending the tuple to self for processing tuple:"+tuple.toString());
		//send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
		sendNow(getId(),  FogEvents.TUPLE_ARRIVAL, tuple, cloudSimParallel);
	}
	
	public PowerHost getHost(){
		return (PowerHost) getHostList().get(0);
	}
	
	public int getParentId() {
		return parentId;
	}
	
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	
	public List<Integer> getChildrenIds() {
		return childrenIds;
	}
	
	public void setChildrenIds(List<Integer> childrenIds) {
		this.childrenIds = childrenIds;
	}
	
	public float getUplinkBandwidth() {
		return uplinkBandwidth;
	}
	
	public void setUplinkBandwidth(float uplinkBandwidth) {
		this.uplinkBandwidth = uplinkBandwidth;
	}
	
	public float getUplinkLatency() {
		return uplinkLatency;
	}
	
	public void setUplinkLatency(float uplinkLatency) {
		this.uplinkLatency = uplinkLatency;
	}
	
	public boolean isSouthLinkBusy() {
		return isSouthLinkBusy;
	}
	
	public boolean isNorthLinkBusy() {
		return isNorthLinkBusy;
	}
	
	public void setSouthLinkBusy(boolean isSouthLinkBusy) {
		this.isSouthLinkBusy = isSouthLinkBusy;
	}
	
	public void setNorthLinkBusy(boolean isNorthLinkBusy) {
		this.isNorthLinkBusy = isNorthLinkBusy;
	}
	
	public int getControllerId() {
		return controllerId;
	}
	
	public void setControllerId(int controllerId) {
		this.controllerId = controllerId;
	}
	
	public List<String> getActiveApplications() {
		return activeApplications;
	}
	
	public void setActiveApplications(List<String> activeApplications) {
		this.activeApplications = activeApplications;
	}
	
	public Map<Integer, List<String>> getChildToOperatorsMap() {
		return childToOperatorsMap;
	}
	
	public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
		this.childToOperatorsMap = childToOperatorsMap;
	}

	public Map<String, Application> getApplicationMap() {
		return applicationMap;
	}

	public void setApplicationMap(Map<String, Application> applicationMap) {
		this.applicationMap = applicationMap;
	}

	public Queue<Tuple> getNorthTupleQueue() {
		return northTupleQueue;
	}

	public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
		this.northTupleQueue = northTupleQueue;
	}

	public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
		return southTupleQueue;
	}

	public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
		this.southTupleQueue = southTupleQueue;
	}

	public double getDownlinkBandwidth() {
		return downlinkBandwidth;
	}

	public void setDownlinkBandwidth(float downlinkBandwidth) {
		this.downlinkBandwidth = downlinkBandwidth;
	}

	public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
		return associatedActuatorIds;
	}

	public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
		this.associatedActuatorIds = associatedActuatorIds;
	}
	
	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
	
	public Map<Integer, Float> getChildToLatencyMap() {
		return childToLatencyMap;
	}

	public void setChildToLatencyMap(Map<Integer, Float> childToLatencyMap) {
		this.childToLatencyMap = childToLatencyMap;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getRatePerMips() {
		return ratePerMips;
	}

	public void setRatePerMips(double ratePerMips) {
		this.ratePerMips = ratePerMips;
	}
	
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public Map<String, Map<String, Integer>> getModuleInstanceCount() {
		return moduleInstanceCount;
	}

	public void setModuleInstanceCount(
			Map<String, Map<String, Integer>> moduleInstanceCount) {
		this.moduleInstanceCount = moduleInstanceCount;
	}

	public int getLeftId(){
		return this.leftId;
	}
	
	public void setLeftId(int leftId){
		this.leftId=leftId;
	}
	
	public int getRightId(){
		return this.rightId;
	}
	
	public void setRightId(int rightId){
		this.rightId=rightId;
	}
	
	public float getRightLatency(){
		return this.rightLatency;
	}
	
	public void setRightLatency(float rightLatency){
		this.rightLatency=rightLatency;
	}
	
	public float getLeftLatency(){
		return this.leftLatency;
	}
	
	public void setLeftLatency(float leftLatency){
		this.leftLatency=leftLatency;
	}
}