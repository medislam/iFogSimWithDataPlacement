package org.fog.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.GCMParameterSpec;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.ConsProdMatrix;
import org.fog.gui.lpFileConstuction.DataSizeVector;
import org.fog.gui.lpFileConstuction.FreeCapacityVector;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.gui.lpFileConstuction.MakeLPFile;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;


public class GenInfrastructure {
	
	/* Cloudlet length in millio of instrutions*/
	private static final int SNR_TUPLE_CPU_SIZE = 100;
	private static final int HGW_TUPLE_CPU_SIZE = 200;
	private static final int LPOP_TUPLE_CPU_SIZE = 300;
	private static final int RPOP_TUPLE_CPU_SIZE = 300;
	private static final int DC_TUPLE_CPU_SIZE = 500;
	private static final int ACT_TUPLE_CPU_SIZE = 100;
	
	/* Cloudlet file size in KB*/
	private static final int SNR_TUPLE_FILE_SIZE = 96;
	private static final int HGW_TUPLE_FILE_SIZE = 96*10;
	private static final int LPOP_TUPLE_FILE_SIZE = 96*100;
	private static final int RPOP_TUPLE_FILE_SIZE = 96*1000;
	private static final int DC_TUPLE_FILE_SIZE = 96*10000;
	private static final int ACT_TUPLE_FILE_SIZE = 96;
	
	/* Basis service latencies */
	private static final float leftLatencyDC = 100;
	private static final float rightLatencyDC = 100;
	private static final float leftLatencyRPOP = 5;
	private static final float rightLatencyRPOP = 5;

	private static final float LatencyDCToRPOP = 100;
	private static final float LatencyRPOPToLPOP = 5;
	private static final float LatencyLPOPToHGW = 50;
	private static final float LatencyHGWToSNR = 10;
	private static final float LatencyHGWToACT = 10;
	
	
	/* Basis exchange unit on KB */
	public static final int Basis_Exchange_Unit = 65550;
	
	/* CPU requirement for modules on Fog devices in mips */
	private static final int SERVICE_DC_CPU = 1000;  //CPU dans les VMs
	private static final int SERVICE_RPOP_CPU = 1000;
	private static final int SERVICE_LPOP_CPU = 1000;
	private static final int SERVICE_HGW_CPU = 1000;
	
	/* RAM requirement for modules on Fog devices in Ko*/
	private static final int SERVICE_DC_RAM = 100;  //RAM dans les VMs
	private static final int SERVICE_RPOP_RAM = 100;
	private static final int SERVICE_LPOP_RAM = 100;
	private static final int SERVICE_HGW_RAM = 100;
	
	/* Fog devices storage capacity on MB*/
	private static final long DC_Storage = 1000000000;  // 1PB
	private static final long RPOP_Storage = 1000000000; // 1 TB
	private static final long LPOP_Storage = 1000000000; //100 GB
	private static final long HGW_Storage = 100000000; //1 GB
	
	
	/* infrastructure */
	//public static final int nb_HGW = 500; //6 HGW per LPOP
	public static final int nb_LPOP = 50; //4 LPOP per RPOP
	public static final int nb_RPOP = 10; //2 RPOP per DC
	public static final int nb_DC = 5; //
	
	private static final int nb_SnrPerHGW = 100;
	private static final int nb_ActPerHGW = 50;
	
	/* SNR periodic samples */
	static double SNR_TRANSMISSION_TIME = 150;
	
	/* nb services on each level */
	//public static final int nb_Service_HGW = nb_HGW ;
	public static final int nb_Service_LPOP = nb_LPOP * 100/100;
	public static final int nb_Service_RPOP = nb_RPOP * 100/100;
	public static final int nb_Service_DC = nb_DC * 100/100;
	
	
	/* Services config */
	private static final long SERVICE_DC_BW = 1000;
	private static final int SERVICE_DC_MIPS = 1000;
	
	private static final long SERVICE_RPOP_BW = 1000;
	private static final int SERVICE_RPOP_MIPS = 1000;
	
	private static final long SERVICE_LPOP_BW = 1000;
	private static final int SERVICE_LPOP_MIPS = 1000;
	
	private static final long SERVICE_HGW_BW = 1000;
	private static final int SERVICE_HGW_MIPS = 1000;
	
	public static final String CloudStorage = "CloudStorage";
	public static final String ClosestNode = "ClosestNode";
	public static final String FogStorage = "FogStorage";
	public static final String ZoningStorage = "ZoningStorage";
	
	private static int nb_zone = 2;
	
	public static final String storageMode = FogStorage;
	
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	
	private static final int nb_DataCons_By_DataProd = 1;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Starting the simulation");
		try{
			Log.enable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			long begin_t, end_t, period_t;
			
			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "Data_Placement"; // identifier of the application
			
			
			
			int nb_HGW = 50;
			
			for(int i=1;i<11;i++){
				
				FogBroker broker = new FogBroker("broker");
				System.out.println("Creating of the Fog devices!");
				createFogDevices(broker.getId(), appId,i * nb_HGW);
				
				System.out.println("Creating of Sensors and Actuators!");
				createSensorsAndActuators(broker.getId(), appId, i*nb_HGW);
				
				/* Module deployment */
				System.out.println("Creating and Adding modules to devices");
				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
				addModulesToFogDevices(moduleMapping,i*nb_HGW);
				moduleMapping.setModuleToHostMap();
				
				Application application = createApplication(appId, broker.getId(), i*nb_HGW);
				application.setUserId(broker.getId());
				application.setFogDeviceList(fogDevices);
				
				System.out.println("Controller!");
				Controller controller = new Controller("master-controller", fogDevices, sensors, actuators, moduleMapping);
				controller.submitApplication(application, 0);

				TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
				
				
				/*
				 *  generate basis delay matrix
				 */
				begin_t = System.currentTimeMillis();
				System.out.println("Basis Delay Matrix computing!");
				BasisDelayMatrix delayMatrix = new BasisDelayMatrix(fogDevices,nb_HGW * i, nb_Service_LPOP, nb_Service_RPOP, nb_Service_DC,
						nb_HGW * i, nb_LPOP, nb_RPOP, nb_DC, application);
				
				delayMatrix.getDelayMatrix(fogDevices);
				
				end_t = System.currentTimeMillis();
				period_t = end_t - begin_t;
				
				//org.fog.examples.Log.writeLog("BasisDelayMatrix:"+String.valueOf(period_t));
				

				/* saving the configurations */
				System.out.println("Saving infrastructure ...");
				saveBasisDelayMatrix(delayMatrix,i, i*nb_HGW);
				System.out.println("End of saving");
				application = null;
				delayMatrix =null;
				controller=null;
				moduleMapping=null;
				broker = null;
				fogDevices.clear();
				System.gc();
				
			}
			

			
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unwanted errors happen");
		}
	}

	
	private static void saveBasisDelayMatrix(BasisDelayMatrix delayMatrix, int i, int nb_HGW) throws IOException {
			System.out.println("Generating Basis Latency file ...");
			FileWriter fichier = new FileWriter("BasisLatency"+i+".txt");
			try{
				BufferedWriter fw = new BufferedWriter(fichier);
				int maxRow = nb_HGW+nb_LPOP+nb_RPOP+nb_DC;
				int maxCol = nb_HGW+nb_LPOP+nb_RPOP+nb_DC;
				for(int row=0;row<(maxRow);row++){
					for(int col=0;col<(maxCol);col++){
						fw.write(String.valueOf(delayMatrix.mDelayMatrix[row][col])+"\t");
					}
					fw.write("\n");
				}
				fw.close();			
			}catch (FileNotFoundException e){
				e.printStackTrace();
			}catch (IOException e){
				e.printStackTrace();
			}
	}

	
	private static void createFogDevices(int userId, String appId, int nb_HGWs) {
		/* create Datacenters */
		for(int i=0;i<nb_DC;i++){
			//FogDevice DC = createFogDevice(fogId, nodeName, mips, ram, upBw, downBw, level, ratePerMips, busyPower, idlePower);
			FogDevice DC = createFogDevice("DC"+i, 44800, 40000, 10000, 10000, 4, 0.01, 16*103, 16*83.25);
			DC.setParentId((int) -1);
			fogDevices.add(DC);
		}
		
		/* create RPOP */
		for(int i=0;i<nb_RPOP;i++){
			FogDevice RPOP = createFogDevice("RPOP"+i, 2800, 4000, 10000, 10000, 3, 0.0, 107.339, 83.4333);
			
			RPOP.setParentId((i/(nb_RPOP/nb_DC))+3);
			RPOP.setUplinkLatency(LatencyDCToRPOP);
			fogDevices.add(RPOP);
		}
		
		/* create LPOP */
		for(int i=0;i<nb_LPOP;i++){
			FogDevice LPOP = createFogDevice("LPOP"+i, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
			
			LPOP.setParentId((i/(nb_LPOP/nb_RPOP))+nb_DC+3);
			LPOP.setUplinkLatency(LatencyRPOPToLPOP);
			fogDevices.add(LPOP);
		}
		
		for(int i=0;i<nb_HGWs;i++){
			FogDevice HGW = createFogDevice("HGW"+i, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
			
			HGW.setParentId((i/(nb_HGWs/nb_LPOP))+nb_DC+nb_RPOP+3);
			HGW.setUplinkLatency(LatencyLPOPToHGW);
			fogDevices.add(HGW);
		}
	}
	
	
	/**
	 * Create Sensors and actuators
	 * @param userId
	 * @param appId
	 */
	
	
	private static void createSensorsAndActuators(int userId, String appId, int nb_HGW){
		/* create HGW */
		int id_snr =0;
		int id_act =0;
		for(int i=0;i<nb_HGW;i++){
			FogDevice HGW = fogDevices.get(i+nb_DC+nb_RPOP+nb_LPOP);

			/* create sensors */
			for(int j=0;j<nb_SnrPerHGW;j++,id_snr++){
				Sensor eegSensor = new Sensor("s-"+id_snr,"TempSNR"+(int) (id_snr), userId, appId, new DeterministicDistribution(SNR_TRANSMISSION_TIME)); // inter-transmission time of  sensor follows a deterministic distribution
				sensors.add(eegSensor);
				eegSensor.setGatewayDeviceId(HGW.getId());
				eegSensor.setLatency(LatencyHGWToSNR);  // latency of connection between sensors and the HGW is 10 ms
			}
			
			/* create actuators */
			for(int k=0;k<nb_ActPerHGW;k++,id_act++){
				Actuator display = new Actuator("a-"+id_act,userId, appId, "DISPLAY"+(int) (id_act));
				actuators.add(display);
				display.setGatewayDeviceId(HGW.getId());
				display.setLatency(LatencyHGWToACT);  // latency of connection between Display actuator and the parent HGW is 10 ms
			}
			
		}
		
	}
			
	private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		
		long storage = storageAllocation(nodeName); // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

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
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		
		int right = getRight(nodeName);
		int left = getleft(nodeName);
		

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList, right, left, getRightLatency(nodeName,right), getLeftLatency(nodeName,left),10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	private static float getRightLatency(String nodeName, int right) {
		if((nodeName.startsWith("DC")) && ( right != -1 )) return rightLatencyDC;
		else if((nodeName.startsWith("RPOP")) && ( right != -1 )) return rightLatencyRPOP;
		return -1;
	}

	private static float getLeftLatency(String nodeName, int left) {
		if((nodeName.startsWith("DC")) && ( left != -1 )) return leftLatencyDC;
		else if((nodeName.startsWith("RPOP")) && ( left != -1 )) return leftLatencyRPOP;
		return -1;
	}

	private static int getleft(String nodeName) {
		int fogId;
		if((nodeName.startsWith("DC"))){
			fogId=Integer.valueOf(nodeName.substring(2));
			if ( fogId > 0 ){
				return fogId-1+3;
			}else{
				return -1;
			}
		}
		else if((nodeName.startsWith("RPOP"))){
			fogId=Integer.valueOf(nodeName.substring(4))+nb_DC;
			if(fogId > (nb_DC)){
				return fogId-1+3;
			}else{
				return -1;
			}
		}else
			return -1;
	}

	private static int getRight(String nodeName) {
		int fogId;
		if((nodeName.startsWith("DC"))){
			fogId=Integer.valueOf(nodeName.substring(2));
			if((nb_DC > 1) && ( fogId < (nb_DC-1))){
				return fogId+1+3;
			}else{
				return -1;
			}
		}else if((nodeName.startsWith("RPOP"))){
			
			fogId=Integer.valueOf(nodeName.substring(4))+nb_DC;
			if((nb_RPOP > 1) && ( fogId < (nb_DC+nb_RPOP)-1)){
				return fogId+1+3;	
			}else {
				return -1;
			}
		}else 
			return -1;
	}

	private static long storageAllocation(String name){
		if(name.startsWith("DC")) return DC_Storage;
		else if(name.startsWith("RPOP")) return RPOP_Storage;
		else if(name.startsWith("LPOP")) return LPOP_Storage;
		else if(name.startsWith("HGW")) return HGW_Storage;
		else return -1;
	}
		
	
	/**
	 * Create Application
	 * Add Modules
	 * Add AppEdges "Data flow"
	 * Add Tuples Mapping "Tuples Frequencies"
	 * Add AppLoop "Control"
	 * @param appId
	 * @param userId
	 * @return
	 */
 	
	

	private static Application createApplication(String appId, int userId, int nb_HGW){
		
		//Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		Application application = new Application(appId, userId);
		addServicesToApplication(application, nb_HGW);
		
		/*
		 * Defining application loops to monitor the latency of. 
		 * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
		 */
		final AppLoop loop = new AppLoop(getArrayListOfServices(nb_HGW));
		
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop);}};
		application.setLoops(loops);
		
		return application;
 	}
 	
 	
	/**
 	 * Add Modules and Sensors and Actuators to Loop Control
 	 * @return
 	 */
	
 	
 	private static List<String> getArrayListOfServices(int nb_HGW) {
 		List<String> modulesList = new ArrayList<String>();
 		try{
			if(nb_DC>0){
				for(int i=0;i<nb_Service_DC;i++){
					modulesList.add("ServiceDC"+i);
				}
			}
			if(nb_RPOP>0){
				for(int i=0;i<nb_Service_RPOP;i++){
					modulesList.add("ServiceRPOP"+i);
				}
			}
			if(nb_LPOP>0){
				for(int i=0;i<nb_Service_LPOP;i++){
					modulesList.add("ServiceLPOP"+i);
				}
			}
			if(nb_HGW>0){
				for(int i=0;i<nb_HGW;i++){
					modulesList.add("ServiceHGW"+i);
					for(int j=0;j<nb_SnrPerHGW;j++){
						modulesList.add("s-"+(int) (j+i*nb_SnrPerHGW));
					}
					for(int k=0;k<nb_ActPerHGW;k++){
						modulesList.add("DISPLAY"+(int) (k+i*nb_ActPerHGW));
					}

				}
			}
		}catch (Exception e) {
		e.printStackTrace();
		System.out.println("Error in getArrayListOfServices()!");
	}

		return modulesList;
	}

	
 	/**
 	 * Add tuple Mapping Fraction
 	 * @param application
 	 */
 	
 	private static void addModulesToFogDevices(ModuleMapping moduleMapping, int nb_HGW){
		FogDevice device =null;
		//String chosedDev ;
		//List<String> nonChosedDC = new ArrayList<String>();
		//List<String> nonChosedRPOP = new ArrayList<String>();
		//List<String> nonChosedLPOP = new ArrayList<String>();
		
		/*Initialization */
//		for(FogDevice dev : fogDevices){
//			if(dev.getName().startsWith("DC")){
//				nonChosedDC.add(dev.getName());
//			}else if(dev.getName().startsWith("RPOP")){
//				nonChosedRPOP.add(dev.getName());
//			}else if(dev.getName().startsWith("LPOP")){
//				nonChosedLPOP.add(dev.getName());
//			}
//		}
		
		/* Module deployment at DCs */
		for(int dc=0;dc<nb_Service_DC;dc++){
//			chosedDev = choseDevice(nonChosedDC);
//			nonChosedDC.remove(chosedDev);
//			moduleMapping.addModuleToDevice("serviceDC"+dc, chosedDev, 1);
			moduleMapping.addModuleToDevice("serviceDC"+dc, "DC"+dc, 1);
			//System.out.println("Add "+"serviceDC"+dc+" to  "+device.getName());

		}
		
		/* Module deployment at RPOPs */
		for(int RPOP=0;RPOP<nb_Service_RPOP;RPOP++){
//			chosedDev = choseDevice(nonChosedRPOP);
//			nonChosedRPOP.remove(chosedDev);
//			moduleMapping.addModuleToDevice("serviceRPOP"+(int) (RPOP), chosedDev, 1);
			moduleMapping.addModuleToDevice("serviceRPOP"+RPOP, "RPOP"+RPOP, 1);
			//System.out.println("Add "+"serviceRPOP"+(int) (RPOP)+" to  "+device.getName());
		}
		
		/* Module deployment at LPOPs */
		for(int LPOP=0;LPOP<nb_Service_LPOP;LPOP++){
//			chosedDev = choseDevice(nonChosedLPOP);
//			nonChosedLPOP.remove(chosedDev);
//			moduleMapping.addModuleToDevice("serviceLPOP"+(int) (LPOP), chosedDev, 1);
			moduleMapping.addModuleToDevice("serviceLPOP"+LPOP, "LPOP"+LPOP, 1);
			//System.out.println("Add "+"serviceLPOP"+(int) (LPOP)+" to  "+device.getName());
		}
		
		/* Module deployment at HGWs */
		for(int HGW=0;HGW<nb_HGW;HGW++){
//			device = fogDevices.get(HGW+nb_DC+nb_RPOP+nb_LPOP);
//			moduleMapping.addModuleToDevice("serviceHGW"+(int) (HGW), device.getName(), 1);
			moduleMapping.addModuleToDevice("serviceHGW"+HGW, "HGW"+HGW, 1);
			//System.out.println("Add "+"serviceHGW"+(int) (HGW)+" to  "+device.getName());
		}
		
	}
 	
 	
 	private static void addTupleMappingFraction(Application application, int nb_HGW) {
 		System.out.println("Creating Selectivity Map");
 		try{
 			/* add tuple mapping between TempSNR->ServiceHGW->TempHGW  and TempSNR->ServiceHGW->TempAct */
			for(int i=0;i<nb_HGW;i++){
				/* Tuple from SNR --HGW-- LPOP */
				application.addTupleMapping("serviceHGW"+i, "TempSNR"+(int) i*nb_SnrPerHGW, "TempHGW"+i, new FractionalSelectivity(1));
				/* Tuple from SNR -- HGW-- ACT */
				for(int actIndex=0;actIndex<nb_ActPerHGW;actIndex++)
					application.addTupleMapping("serviceHGW"+i, "TempSNR"+(int) i*nb_SnrPerHGW, "TempAct"+(int) (actIndex+i*nb_ActPerHGW), new FractionalSelectivity(1));
 			}
			
			/* add tuple mapping between ?->ServiceLPOP->TempLPOP  and ?->ServiceRPOP->TempRPOP */
 			for(String inputTuple : application.getEdgeMap().keySet()){
 				List<String> destinationServices = application.getEdgeMap().get(inputTuple).getDestination();
	 	 			for(String destService: destinationServices){	
	 	 				if(destService.startsWith("serviceLPOP") || destService.startsWith("serviceRPOP")){
	 	 					AppModule mod = application.getModuleByName(destService);
	 	 					String outputTuple = "Temp"+destService.substring(7);
	 	 					Map<Pair<String, String>, SelectivityModel> selectivityMap = mod.getSelectivityMap();
	 	 					Pair<String,String> selec = Pair.create(inputTuple,outputTuple);
	 	 					if(!selectivityMap.keySet().contains(selec) && selectivityMap.size() < 1){
	 	 						application.addTupleMapping(mod.getName(), inputTuple, outputTuple, new FractionalSelectivity(1));
	 	 						
	 	 						List<String> input = new ArrayList<String>();
	 							input.add(inputTuple);
	 							if(application.mapSelectivity.containsKey(outputTuple)){
	 								input.addAll(application.mapSelectivity.get(outputTuple));
	 							}
	 							application.mapSelectivity.put(outputTuple, input);
	 	 					}
	 	 				
	 	 				}								
 					}
 			}
 			
// 			for(AppModule mod:application.getModules()){
// 				System.out.println("Selectivity size for:"+mod.getName()+"   = "+mod.getSelectivityMap().size());
// 				if(mod.getSelectivityMap().size()==0){
// 					System.out.println("selectivity is null, adding one");
// 					for(AppEdge edge:application.getEdges()){
// 						if(edge.getDestination().contains(mod.getName())){
// 							System.out.println("ajouté");
//	 	 					application.addTupleMapping(mod.getName(), edge.getTupleType(), "Temp"+mod.getName().substring(7), new FractionalSelectivity(1));
//	 	 					break;
// 						}
// 					}
// 				}
// 				
// 			}
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in addTupleMappingFraction!");
		}
		
	}

	
 	/**
 	 * Add Data Flow in application | the data Flow is AppEdges in application
 	 * @param application
 	 */
 	
 	
 	private static void addAppEdgesToApplication(Application application, int nb_HGW) {
		//application.addAppEdge("Source", "Destination", tupleCpuLength, tupleNwLength, "tupleType", direction, edgeType,tupleDataSize);
 		try{
 			/* From sensors to HGW */
 			for(int snrIndex=0;snrIndex<nb_HGW*nb_SnrPerHGW;snrIndex++){
 				application.addAppEdge("s-"+snrIndex, Arrays.asList("serviceHGW"+(int) snrIndex/nb_SnrPerHGW), SNR_TUPLE_CPU_SIZE, SNR_TUPLE_FILE_SIZE, "TempSNR"+snrIndex, Tuple.UP, AppEdge.SENSOR);
 			}
 			
 			/* From Hgws to Lpop */
 			for(int hgwIndex=0;hgwIndex<nb_HGW;hgwIndex++){
 				application.addAppEdge("serviceHGW"+hgwIndex, getDataConsOfService(getPossibleList("serviceHGW"+hgwIndex,application)), HGW_TUPLE_CPU_SIZE, HGW_TUPLE_FILE_SIZE, "TempHGW"+hgwIndex, Tuple.UP, AppEdge.MODULE);
 			}
 			
 			/* From Hgws to Acts */
 			for(int actIndex=0;actIndex<nb_HGW*nb_ActPerHGW;actIndex++){
 				application.addAppEdge("serviceHGW"+(int) (int) actIndex/nb_ActPerHGW, Arrays.asList("DISPLAY"+actIndex), ACT_TUPLE_CPU_SIZE, ACT_TUPLE_FILE_SIZE, "TempAct"+actIndex, Tuple.DOWN, AppEdge.ACTUATOR); 
 			}
 			
 			/* From Lpops to Rpop */
 			for(int lpopIndex=0;lpopIndex<nb_Service_LPOP;lpopIndex++){
 				application.addAppEdge("serviceLPOP"+lpopIndex, getDataConsOfService(getPossibleList("serviceLPOP"+lpopIndex, application)), LPOP_TUPLE_CPU_SIZE, LPOP_TUPLE_FILE_SIZE, "TempLPOP"+lpopIndex, Tuple.UP, AppEdge.MODULE);
  			}
 			
 			/* From Rpop to Dc*/
 			for(int rpopIndex=0;rpopIndex<nb_Service_RPOP;rpopIndex++){
 				application.addAppEdge("serviceRPOP"+rpopIndex, getDataConsOfService(getPossibleList("serviceRPOP"+rpopIndex, application)), RPOP_TUPLE_CPU_SIZE, RPOP_TUPLE_FILE_SIZE, "TempRPOP"+rpopIndex, Tuple.UP, AppEdge.MODULE);
  			}
 		}catch (Exception e){
 			e.printStackTrace();
			System.out.println("Error in addAppEdgesToApplication!");
 		}
	}
 	
	private static List<String> getDataConsOfService(List<String> possibleServiceList) {
		List<String> chosedList = new ArrayList<String>();		
		for(int i=0; i<nb_DataCons_By_DataProd;i++){
		//for(int i=0; i<(int) (Math.random()* (nb_DataCons_By_DataProd-1) +1);i++){
			if(possibleServiceList.size()>i){
				boolean chose = false;
				while(!chose){
					int rand = (int) (Math.random()*possibleServiceList.size());
					if(!chosedList.contains(possibleServiceList.get(rand))){
						chosedList.add(possibleServiceList.get(rand));
						chose=true;
					}
				}
			}else{
				return chosedList;
			}
		}
		return chosedList;
	}


	private static List<String> getPossibleList(String moduleName, Application application){
		List<String> list = new ArrayList<String>();
		//System.out.println("get possible list for module :"+moduleName);
		FogDevice host = application.getFogDeviceByName(ModuleMapping.getDeviceHostModule(moduleName));
		//System.out.println("Host:"+host.getName());

		if(moduleName.startsWith("serviceHGW") || moduleName.startsWith("serviceLPOP")){
			FogDevice parent = application.getFogDeviceById(host.getParentId());
			FogDevice superparent = application.getFogDeviceById(parent.getParentId());
			
			list.add("service"+superparent.getName());
			List<Integer> childrensIds = superparent.getChildrenIds();	
			for(int childId:childrensIds){
				FogDevice dev = application.getFogDeviceById(childId);
				list.add("service"+dev.getName());
			}
			//System.out.println("possible list for "+moduleName+" is "+list.toString());
			return list;
			
		}
		else if(moduleName.startsWith("serviceRPOP")){
			//List<Integer> childrensIds = parent.getChildrenIds();
			for(int i=0;i<nb_Service_DC;i++){
				list.add("serviceDC"+i);
			}
			//System.out.println("possible list for "+moduleName+" is "+list.toString());
			return list;
			
		}else if(moduleName.startsWith("serviceDC")){
			System.out.println("Services on DataCenters hasn't consumpted services!");
			return null;
		}else{
			return null;
		}
		
	}


	/**
 	 * Add Services To Application | the services are the modules in application
 	 * @param application
 	 */
	
 	
	private static void addServicesToApplication(Application application, int nb_HGW) {
 		//application.addAppModule("ModuleName", ram);
 		try{
				if(nb_DC>0){
					
					for(int i=0;i<nb_Service_DC;i++){
						application.addAppModule("serviceDC"+(int) i, SERVICE_DC_RAM, SERVICE_DC_MIPS, storageAllocation("DC"), SERVICE_DC_BW);
					}
					System.out.println("Nb allocated service on datacenters = "+nb_Service_DC);
				}
				if(nb_RPOP>0){
					
					for(int i=0;i<nb_Service_RPOP;i++){
						application.addAppModule("serviceRPOP"+(int) (i), SERVICE_RPOP_RAM, SERVICE_RPOP_MIPS, storageAllocation("RPOP"), SERVICE_RPOP_BW);
					}
					System.out.println("Nb allocated service on rpops = "+nb_Service_RPOP);
				}
				if(nb_LPOP>0){
					
					for(int i=0;i<nb_Service_LPOP;i++){
						application.addAppModule("serviceLPOP"+(int) (i), SERVICE_LPOP_RAM, SERVICE_LPOP_MIPS, storageAllocation("LPOP"), SERVICE_LPOP_BW);
					}
					System.out.println("Nb allocated service on lpops = "+nb_Service_LPOP);
				}
				if(nb_HGW>0){
					for(int i=0;i<nb_HGW;i++){
						application.addAppModule("serviceHGW"+(int) (i), SERVICE_HGW_RAM, SERVICE_HGW_MIPS, storageAllocation("HGW"), SERVICE_HGW_BW);
					}
					System.out.println("Nb allocated service on HGW = "+nb_HGW);
				}
 		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in addServicesToApplication!");
		}
	}
}
