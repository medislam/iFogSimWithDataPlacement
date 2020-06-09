package org.fog.examples;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.StorageMode.ClosestNodeStorage;
import org.StorageMode.CloudStorage;
import org.StorageMode.FogStorage;
import org.StorageMode.GraphPartitionStorage;
import org.StorageMode.ZoningStorage;
import org.StorageMode.ZoningStorageParallel;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.Parallel.CloudSimParallel;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.DeterministicDistribution;

public class DataPlacement {

	/* Cloudlet length in million of instructions */
	public static final int SNR_TUPLE_CPU_SIZE = 100;
	public static final int HGW_TUPLE_CPU_SIZE = 200;
	public static final int LPOP_TUPLE_CPU_SIZE = 300;
	public static final int RPOP_TUPLE_CPU_SIZE = 300;
	public static final int DC_TUPLE_CPU_SIZE = 500;
	public static final int ACT_TUPLE_CPU_SIZE = 100;

	/* Cloudlet file size in KB */
	public static final int SNR_TUPLE_FILE_SIZE = 96;

	public static final int HGW_TUPLE_FILE_SIZE = 96 * 10;
	public static final int LPOP_TUPLE_FILE_SIZE = 96 * 10;
	public static final int RPOP_TUPLE_FILE_SIZE = 96 * 10;
	private static final int DC_TUPLE_FILE_SIZE = 96 * 10;

	public static final int ACT_TUPLE_FILE_SIZE = 96;

	/* Basis service latencies */
	public static final float leftLatencyDC = 100;
	public static final float rightLatencyDC = 100;
	public static final float leftLatencyRPOP = 5;
	public static final float rightLatencyRPOP = 5;

	public static final float LatencyDCToRPOP = 100;
	public static final float LatencyRPOPToLPOP = 5;
	public static final float LatencyLPOPToHGW = 50;
	public static final float LatencyHGWToSNR = 10;
	public static final float LatencyHGWToACT = 10;

	/* Basis exchange unit on KB */
	public static final int Basis_Exchange_Unit = 65550;

	/* CPU requirement for modules on Fog devices in mips */
	private static final int SERVICE_DC_CPU = 1000; // CPU dans les VMs
	private static final int SERVICE_RPOP_CPU = 1000;
	private static final int SERVICE_LPOP_CPU = 1000;
	private static final int SERVICE_HGW_CPU = 1000;

	/* RAM requirement for modules on Fog devices in Ko */
	public static final int SERVICE_DC_RAM = 100; // RAM dans les VMs
	public static final int SERVICE_RPOP_RAM = 100;
	public static final int SERVICE_LPOP_RAM = 100;
	public static final int SERVICE_HGW_RAM = 100;

	/* Fog devices storage capacity on MB */
	public static final long DC_Storage = 1000000000; // 1PB
	public static final long RPOP_Storage = 1000000000; // 1 TB
	public static final long LPOP_Storage = 1000000000; // 100 GB
	public static final long HGW_Storage = 1000000000; // 1 GB

	/* infrastructure */
//	public static int nb_HGW=500; //6 HGW per LPOP
//	public static final int nb_LPOP = 50; //4 LPOP per RPOP
//	public static final int nb_RPOP = 10; //2 RPOP per DC
//	public static final int nb_DC = 5; //

	public static int nb_HGW=8; //3 HGW per LPOP
	public static final int nb_LPOP = 4; //2 LPOP per RPOP
	public static final int nb_RPOP = 2; //2 RPOP per DC
	public static final int nb_DC = 1; //
	
	public static final int nb_SnrPerHGW = 1;
	public static final int nb_ActPerHGW = 1;

	/* SNR periodic samples in ms*/
	public static int SNR_TRANSMISSION_TIME = 1000;

	/* nb services on each level */
	public static int nb_Service_HGW=nb_HGW;
	public static final int nb_Service_LPOP = nb_LPOP;
	public static final int nb_Service_RPOP = nb_RPOP;
	public static final int nb_Service_DC = nb_DC;

	/* Services config */
	public static final long SERVICE_DC_BW = 1000;
	public static final int SERVICE_DC_MIPS = 1000;

	public static final long SERVICE_RPOP_BW = 1000;
	public static final int SERVICE_RPOP_MIPS = 1000;

	public static final long SERVICE_LPOP_BW = 1000;
	public static final int SERVICE_LPOP_MIPS = 1000;

	public static final long SERVICE_HGW_BW = 1000;
	public static final int SERVICE_HGW_MIPS = 1000;

	public static final String CloudStorage = "CloudStorage";
	public static final String ClosestNode = "ClosestNode";
	public static final String FogStorage = "FogStorage";
	public static final String ZoningStorage = "ZoningStorage";
	public static final String ZoningStorageParallel = "ZoningStorageParallel";
	public static final String GraphPartitionStorage = "GraphPartitionStorage";

	//public static final List<String> storageModes = Arrays.asList(CloudStorage,ClosestNode,FogStorage,ZoningStorage,GraphPartitionStorage);
	public static final List<String> storageModes = Arrays.asList(CloudStorage,ZoningStorage,ZoningStorageParallel);
	//public static final List<String> storageModes = Arrays.asList(CloudStorage, ClosestNode);

	public static final List<Integer> nb_zones_list = Arrays.asList(2);
	public static final List<Integer> nb_partitions_list = Arrays.asList(2,5);

	
	public static int nb_zone;
	
	public static int nb_partitions;

	public static String storageMode;

	public static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	public static List<Sensor> sensors = new ArrayList<Sensor>();
	public static List<Actuator> actuators = new ArrayList<Actuator>();

	public static int nb_DataCons_By_DataProd ;

	public static boolean parallel = true;
	
	public static final String zoning = "zoning";
	public static final String mixed = "mixed";
	public static final String distributed = "distributed";
	
	//private static final List<String> dataflows = Arrays.asList(zoning,mixed,distributed);
	public static final List<String> dataflows = Arrays.asList(distributed);
	
	public static final String FloydPath = "/home/t430/Bureau/";
	
	public static String dataflow_used;
	
	
	public static int nb_externCons=0;
	
	public static boolean trace_flag = true; // mean trace events
	public static Calendar calendar;
	public static int num_user = 1; // number of cloud users
	
	public static boolean generate_log_file = true;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Starting the simulation");
		Log.writeInLogFile("DataPlacement", "Starting the simulation");
		try {
			
			
			Log.disable();
			calendar = Calendar.getInstance();
			//Log.initializeLogFile();
			System.out.println();	
			
			/* this specifies the number of data consumers that share the same data */
			for(int i = 1; i < 2; i++){
			nb_DataCons_By_DataProd=i;
			System.out.println("nb_DataCons_By_DataProd="+ nb_DataCons_By_DataProd);
			Log.writeInLogFile("DataPlacement", "nb_DataCons_By_DataProd="+ nb_DataCons_By_DataProd);
			
			for(String df: dataflows){
				dataflow_used = df;
			
			long b_sim, e_sim,  b_simZone, e_simZone, t_simZone;
				
			b_sim = Calendar.getInstance().getTimeInMillis();
			
			//for(int simul=0;simul<5;simul++){

			for(String storMode : storageModes){
				storageMode = storMode;

			if (storageMode.equals(CloudStorage)) {
				CloudStorage cloud = new CloudStorage();
				cloud.sim();
			

			} else if (storageMode.equals(ClosestNode)) {
				ClosestNodeStorage closestnode = new ClosestNodeStorage();
				closestnode.sim();
				
				
			} else if (storageMode.equals(FogStorage)) {
				FogStorage fog = new FogStorage();
				fog.sim();
				
				
			} else if (storageMode.equals(ZoningStorage)) {
				ZoningStorage zoning = new ZoningStorage();
				
				b_simZone = Calendar.getInstance().getTimeInMillis();
				zoning.sim();
				e_simZone = Calendar.getInstance().getTimeInMillis();
				t_simZone = e_simZone - b_simZone;
				System.out.println("Zoning Storage Simualtion Time ="+t_simZone);
				org.fog.examples.Log.writeSimulationTimeParallel(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + " Zoning Sim time:"+String.valueOf(t_simZone));


			}else if (storageMode.equals(ZoningStorageParallel)) {
				ZoningStorageParallel zoningParallel = new ZoningStorageParallel();
				
				b_simZone = Calendar.getInstance().getTimeInMillis();
				zoningParallel.sim();
				e_simZone = Calendar.getInstance().getTimeInMillis();
				t_simZone = e_simZone - b_simZone;
				System.out.println("Zoning Storage Parallel Simualtion Time ="+t_simZone);
				org.fog.examples.Log.writeSimulationTimeParallel(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + " Zoning Parallel Sim time:"+String.valueOf(t_simZone));


			} 
			
			else if (storageMode.equals(GraphPartitionStorage)) {
				GraphPartitionStorage graphpartition = new org.StorageMode.GraphPartitionStorage();
				graphpartition.sim();
				
			} 
		}
			e_sim = Calendar.getInstance().getTimeInMillis();
			//org.fog.examples.Log.writeSimulationTime(nb_HGW, String.valueOf((e_sim - b_sim)/60000));
		}
			
	}	

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Unwanted errors happen");
		}
	}

	

	/**
	 * Create Fog Devices
	 * 
	 * @param userId
	 * @param appId
	 */

	public static void createFogDevices(int userId, String appId) {
		/* create Datacenters */
		for (int i = 0; i < nb_DC; i++) {
			// FogDevice DC = createFogDevice(fogId, nodeName, mips, ram, upBw,
			// downBw, level, ratePerMips, busyPower, idlePower);
			FogDevice DC = createFogDevice("DC" + i, 44800, 40000, 10000,
					10000, 4, 0.01, 16 * 103, 16 * 83.25);
			DC.setParentId((int) -1);
			fogDevices.add(DC);
		}

		/* create RPOP */
		for (int i = 0; i < nb_RPOP; i++) {
			FogDevice RPOP = createFogDevice("RPOP" + i, 2800, 4000, 10000,
					10000, 3, 0.0, 107.339, 83.4333);

			RPOP.setParentId((i / (nb_RPOP / nb_DC)) + 3);
			RPOP.setUplinkLatency(LatencyDCToRPOP);
			fogDevices.add(RPOP);
		}

		/* create LPOP */
		for (int i = 0; i < nb_LPOP; i++) {
			FogDevice LPOP = createFogDevice("LPOP" + i, 2800, 4000, 10000,
					10000, 2, 0.0, 107.339, 83.4333);

			LPOP.setParentId((i / (nb_LPOP / nb_RPOP)) + nb_DC + 3);
			LPOP.setUplinkLatency(LatencyRPOPToLPOP);
			fogDevices.add(LPOP);
		}

		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = createFogDevice("HGW" + i, 2800, 4000, 10000,
					10000, 1, 0.0, 107.339, 83.4333);

			HGW.setParentId((i / (nb_HGW / nb_LPOP)) + nb_DC + nb_RPOP + 3);
			HGW.setUplinkLatency(LatencyLPOPToHGW);
			fogDevices.add(HGW);
		}
	}
	
	
	
	
	public static void createFogDevices(int userId, String appId, CloudSimParallel cluodSimParallel) {
		/* create Datacenters */
		for (int i = 0; i < nb_DC; i++) {
			// FogDevice DC = createFogDevice(fogId, nodeName, mips, ram, upBw,
			// downBw, level, ratePerMips, busyPower, idlePower);
			FogDevice DC = createFogDevice("DC" + i, 44800, 40000, 10000,
					10000, 4, 0.01, 16 * 103, 16 * 83.25, cluodSimParallel);
			DC.setParentId((int) -1);
			fogDevices.add(DC);
		}

		/* create RPOP */
		for (int i = 0; i < nb_RPOP; i++) {
			FogDevice RPOP = createFogDevice("RPOP" + i, 2800, 4000, 10000,
					10000, 3, 0.0, 107.339, 83.4333, cluodSimParallel);

			RPOP.setParentId((i / (nb_RPOP / nb_DC)) + 3);
			RPOP.setUplinkLatency(LatencyDCToRPOP);
			fogDevices.add(RPOP);
		}

		/* create LPOP */
		for (int i = 0; i < nb_LPOP; i++) {
			FogDevice LPOP = createFogDevice("LPOP" + i, 2800, 4000, 10000,
					10000, 2, 0.0, 107.339, 83.4333, cluodSimParallel);

			LPOP.setParentId((i / (nb_LPOP / nb_RPOP)) + nb_DC + 3);
			LPOP.setUplinkLatency(LatencyRPOPToLPOP);
			fogDevices.add(LPOP);
		}

		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = createFogDevice("HGW" + i, 2800, 4000, 10000,
					10000, 1, 0.0, 107.339, 83.4333, cluodSimParallel);

			HGW.setParentId((i / (nb_HGW / nb_LPOP)) + nb_DC + nb_RPOP + 3);
			HGW.setUplinkLatency(LatencyLPOPToHGW);
			fogDevices.add(HGW);
		}
	}

	
	/**
	 * Create Sensors and actuators
	 * 
	 * @param userId
	 * @param appId
	 */
	

	public static void createSensorsAndActuators(int userId, String appId) {
		/* create HGW */
		int id_snr = 0;
		int id_act = 0;
		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = fogDevices.get(i + nb_DC + nb_RPOP + nb_LPOP);

			/* create sensors */
			for (int j = 0; j < nb_SnrPerHGW; j++, id_snr++) {
				Sensor snr = new Sensor("s-" + id_snr, "TempSNR"+ (int) (id_snr), userId, appId,new DeterministicDistribution(SNR_TRANSMISSION_TIME)); 
				sensors.add(snr);
				snr.setGatewayDeviceId(HGW.getId());
				snr.setLatency(LatencyHGWToSNR); 
			}

			/* create actuators */
			for (int k = 0; k < nb_ActPerHGW; k++, id_act++) {
				Actuator act = new Actuator("a-" + id_act, userId, appId,"DISPLAY" + (int) (id_act));
				actuators.add(act);
				act.setGatewayDeviceId(HGW.getId());
				act.setLatency(LatencyHGWToACT); 
			}

		}

	}
	
	
	public static void createSensorsAndActuators(int userId, String appId, CloudSimParallel cloudSimParallel) {
		/* create HGW */
		int id_snr = 0;
		int id_act = 0;
		for (int i = 0; i < nb_HGW; i++) {
			FogDevice HGW = fogDevices.get(i + nb_DC + nb_RPOP + nb_LPOP);

			/* create sensors */
			for (int j = 0; j < nb_SnrPerHGW; j++, id_snr++) {
				Sensor snr = new Sensor("s-" + id_snr, "TempSNR"+ (int) (id_snr), userId, appId,new DeterministicDistribution(SNR_TRANSMISSION_TIME), cloudSimParallel); 
				sensors.add(snr);
				snr.setGatewayDeviceId(HGW.getId());
				snr.setLatency(LatencyHGWToSNR); 
			}

			/* create actuators */
			for (int k = 0; k < nb_ActPerHGW; k++, id_act++) {
				Actuator act = new Actuator("a-" + id_act, userId, appId,"DISPLAY" + (int) (id_act), cloudSimParallel);
				actuators.add(act);
				act.setGatewayDeviceId(HGW.getId());
				act.setLatency(LatencyHGWToACT); 
			}

		}

	}

	
	public static long storageAllocation(String name) {
		if (name.startsWith("DC"))
			return DC_Storage;
		else if (name.startsWith("RPOP"))
			return RPOP_Storage;
		else if (name.startsWith("LPOP"))
			return LPOP_Storage;
		else if (name.startsWith("HGW"))
			return HGW_Storage;
		else
			return -1;
	}
	
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips,
			double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); 

		int hostId = FogUtils.generateEntityId();

		long storage = storageAllocation(nodeName); // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw), storage, peList,
				new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); 

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem,costPerStorage, costPerBw);

		int right = getRight(nodeName);
		int left = getleft(nodeName);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics,new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right),getLeftLatency(nodeName, left), 10, upBw, downBw, 0,ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips,
			double busyPower, double idlePower, CloudSimParallel cloudSimParallel) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); 

		int hostId = FogUtils.generateEntityId();

		long storage = storageAllocation(nodeName); // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw), storage, peList,
				new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); 

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem,costPerStorage, costPerBw);

		int right = getRight(nodeName);
		int left = getleft(nodeName);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics,new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right),getLeftLatency(nodeName, left), 10, upBw, downBw, 0,ratePerMips, cloudSimParallel);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}
	

	private static float getRightLatency(String nodeName, int right) {
		if ((nodeName.startsWith("DC")) && (right != -1))
			return rightLatencyDC;
		else if ((nodeName.startsWith("RPOP")) && (right != -1))
			return rightLatencyRPOP;
		return -1;
	}
	

	private static float getLeftLatency(String nodeName, int left) {
		if ((nodeName.startsWith("DC")) && (left != -1))
			return leftLatencyDC;
		else if ((nodeName.startsWith("RPOP")) && (left != -1))
			return leftLatencyRPOP;
		return -1;
	}
	

	private static int getleft(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if (fogId > 0) {
				return fogId - 1 + 3;
			} else {
				return -1;
			}
		} else if ((nodeName.startsWith("RPOP"))) {
			fogId = Integer.valueOf(nodeName.substring(4)) + nb_DC;
			if (fogId > (nb_DC)) {
				return fogId - 1 + 3;
			} else {
				return -1;
			}
		} else
			return -1;
	}
	

	private static int getRight(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if ((nb_DC > 1) && (fogId < (nb_DC - 1))) {
				return fogId + 1 + 3;
			} else {
				return -1;
			}
		} else if ((nodeName.startsWith("RPOP"))) {

			fogId = Integer.valueOf(nodeName.substring(4)) + nb_DC;
			if ((nb_RPOP > 1) && (fogId < (nb_DC + nb_RPOP) - 1)) {
				return fogId + 1 + 3;
			} else {
				return -1;
			}
		} else
			return -1;
	}


	/**
	 * Create Application Add Modules Add AppEdges "Data flow" Add Tuples
	 * Mapping "Tuples Frequencies" Add AppLoop "Control"
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	
	
	private static List<String> getArrayListOfServices() {
		List<String> modulesList = new ArrayList<String>();
		try {
			if (DataPlacement.nb_DC > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_DC; i++) {
					modulesList.add("ServiceDC" + i);
				}
			}
			if (DataPlacement.nb_RPOP > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_RPOP; i++) {
					modulesList.add("ServiceRPOP" + i);
				}
			}
			if (DataPlacement.nb_LPOP > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_LPOP; i++) {
					modulesList.add("ServiceLPOP" + i);
				}
			}
			if (DataPlacement.nb_HGW > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_HGW; i++) {
					modulesList.add("ServiceHGW" + i);
					for (int j = 0; j < DataPlacement.nb_SnrPerHGW; j++) {
						modulesList.add("s-" + (int) (j + i * DataPlacement.nb_SnrPerHGW));
					}
					for (int k = 0; k < DataPlacement.nb_ActPerHGW; k++) {
						modulesList.add("DISPLAY"+ (int) (k + i * DataPlacement.nb_ActPerHGW));
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Error in getArrayListOfServices()!");
		}

		return modulesList;
	}

    public static  Application createApplication(String appId, int userId){
		Application application = new Application(appId, userId);
		application.addServicesToApplication();

		/*
		 * Defining application loops to monitor the latency of. Here, we add
		 * only one loop for monitoring : EEG(sensor) -> Client -> Concentration
		 * Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop = new AppLoop(getArrayListOfServices());

		List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop);}};
		application.setLoops(loops);
		return application;
	}

	/**
	 * Print devices "FogDevices, Actuators, Sensors"
	 */

	private static void printDevices() {
		// System.out.println("\nFog devices : ");
		for (FogDevice fogdev : fogDevices) {
			 System.out.println(fogdev.getName()+"  idEntity = "+fogdev.getId()+" up= "+fogdev.getParentId()+" left ="+fogdev.getLeftId()+" leftLatency = "+fogdev.getLeftLatency()+" right ="+fogdev.getRightId()+" rightLatency="+fogdev.getRightLatency()+" children = "+fogdev.getChildrenIds()+" childrenLatencies ="+fogdev.getChildToLatencyMap()+" Storage = "+fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage()+" |	");
		}

		// System.out.println("\nSensors : ");
		for (Sensor snr : sensors) {
			 System.out.println(snr.getName()+"  HGW_ID = "+snr.getGatewayDeviceId()+" TupleType = "+snr.getTupleType()+" Latency = "+snr.getLatency()+" |	");
		}
		// System.out.println("\nActuators : ");
		for (Actuator act : actuators) {
			 System.out.println(act.getName()+" GW_ID = "+act.getGatewayDeviceId()+" Act_Type= "+act.getActuatorType()+" Latency = "+act.getLatency()+" |	");
		}
		 System.out.println("\n");

	}

}
