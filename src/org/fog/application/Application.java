package org.fog.application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;
import org.fog.examples.DataPlacement;
import org.fog.placement.ModuleMapping;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoCoverage;

/**
 * Class represents an application in the Distributed Dataflow Model.
 * @author Harshit Gupta
 *
 */
public class Application {
	
	private String appId;
	private int userId;
	private GeoCoverage geoCoverage;

	/**
	 * List of application modules in the application
	 */
	private List<AppModule> modules;
	
	/**
	 * List of application edges in the application
	 */
	private static List<AppEdge> edges;
	
	/**
	 * List of application loops to monitor for delay
	 */
	private List<AppLoop> loops;
	
	private static Map<String, AppEdge> edgeMap;
	
	private List<FogDevice> fogDevices;
	private Map<String, FogDevice> mapFogDevicesByName;
	private Map<Integer, FogDevice> mapFogDevicesById;
	
	public static Map<String, List<String>> mapSelectivity = new HashMap<String,List<String>>();

	/**
	 * Creates a plain vanilla application with no modules and edges.
	 * @param appId
	 * @param userId
	 * @return
	 */
	
	
	
	
	/**
	 * Adds an application module to the application.
	 * @param moduleName
	 * @param ram
	 */
	public void addAppModule(String moduleName, int ram, int mips, long size, long bw){
//		int mips = 1000;
//		long size = 10000;
//		long bw = 1000;
		String vmm = "Xen";
		
		AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, userId, mips, ram, bw, size, vmm, new TupleScheduler(mips, 1), new HashMap<Pair<String, String>, SelectivityModel>());
		
		getModules().add(module);
		
	}
	
	/**
	 * Adds a non-periodic edge to the application model.
	 * @param source
	 * @param destination
	 * @param tupleCpuLength
	 * @param tupleNwLength
	 * @param tupleType
	 * @param direction
	 * @param edgeType
	 */
	public void addAppEdge(String source, List<String> destination, double tupleCpuLength, double tupleNwLength, String tupleType, int direction, int edgeType){
		AppEdge edge = new AppEdge(source, destination, tupleCpuLength, tupleNwLength, tupleType, direction, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	/**
	 * Adds a periodic edge to the application model.
	 * @param source
	 * @param destination
	 * @param tupleCpuLength
	 * @param tupleNwLength
	 * @param tupleType
	 * @param direction
	 * @param edgeType
	 */
	public void addAppEdge(String source, List<String> destination, double periodicity, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int direction, int edgeType){
		AppEdge edge = new AppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength, tupleType, direction, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	/**
	 * Define the input-output relationship of an application module for a given input tuple type.
	 * @param moduleName Name of the module
	 * @param inputTupleType Type of tuples carried by the incoming edge
	 * @param outputTupleType Type of tuples carried by the output edge
	 * @param selectivityModel Selectivity model governing the relation between the incoming and outgoing edge
	 */
	public void addTupleMapping(String moduleName, String inputTupleType, String outputTupleType, SelectivityModel selectivityModel){
		AppModule module = getModuleByName(moduleName);
		module.getSelectivityMap().put(new Pair<String, String>(inputTupleType, outputTupleType), selectivityModel);
	}
	
	/**
	 * Get a list of all periodic edges in the application.
	 * @param srcModule
	 * @return
	 */
	public List<AppEdge> getPeriodicEdges(String srcModule){
		List<AppEdge> result = new ArrayList<AppEdge>();
		for(AppEdge edge : edges){
			if(edge.isPeriodic() && edge.getSource().equals(srcModule))
				result.add(edge);
		}
		return result;
	}
	
	public Application(String appId, int userId) {
		setAppId(appId);
		setUserId(userId);
		setModules(new ArrayList<AppModule>());
		setEdges(new ArrayList<AppEdge>());
		setGeoCoverage(null);
		setLoops(new ArrayList<AppLoop>());
		setEdgeMap(new HashMap<String, AppEdge>());
	}
	
	public Application(String appId, List<AppModule> modules,
			List<AppEdge> edges, List<AppLoop> loops, GeoCoverage geoCoverage) {
		setAppId(appId);
		setModules(modules);
		setEdges(edges);
		setGeoCoverage(geoCoverage);
		setLoops(loops);
		setEdgeMap(new HashMap<String, AppEdge>());
		for(AppEdge edge : edges){
			getEdgeMap().put(edge.getTupleType(), edge);
		}
	}

	/**
	 * Search and return an application module by its module name
	 * @param name the module name to be returned
	 * @return
	 */
	public AppModule getModuleByName(String name){
		for(AppModule module : modules){
			if(module.getName().equals(name))
				return module;
		}
		return null;
	}
	
	public FogDevice getFogDeviceByName(String name){
		if(mapFogDevicesByName.containsKey(name)){
			return mapFogDevicesByName.get(name);
		}
		return null;
	}
	
	public FogDevice getFogDeviceById(Integer id){
		if(mapFogDevicesById.containsKey(id)){
			return mapFogDevicesById.get(id);
		}
		return null;
	}

	
	/**
	 * Get the tuples generated upon execution of incoming tuple <i>inputTuple</i> by module named <i>moduleName</i>
	 * @param moduleName name of the module performing execution of incoming tuple and emitting resultant tuples
	 * @param inputTuple incoming tuple, whose execution creates resultant tuples
	 * @param sourceDeviceId
	 * @return
	 */
	public List<Tuple> getResultantTuples(String moduleName, Tuple inputTuple, int sourceDeviceId){
		List<Tuple> tuples = new ArrayList<Tuple>();
		AppModule module = getModuleByName(moduleName);
		////*System.out.println("Searching for selectivity map on module:"+module.getName()+" for Tuple:"+inputTuple.getTupleType()+"  on device:"+sourceDeviceId);
		//////*System.out.println("Comparing Module Name with edgeSource()");
		for(AppEdge edge : getEdges()){
			//////*System.out.println(edge.getSource()+":    =?=     :"+moduleName);
			if(edge.getSource().equals(moduleName)){
				Pair<String, String> pair = new Pair<String, String>(inputTuple.getTupleType(), edge.getTupleType());
				
				if(module.getSelectivityMap().get(pair)==null){
					////*System.out.println("Selectivity Map on module:"+moduleName+"   "+inputTuple.getTupleType()+"  with   "+edge.getTupleType()+"  is Null");
					continue;
				}
				
				////*System.out.println("There are is a mapping tuple on module: "+moduleName+" with inputTuple: "+pair.getFirst()+" outputTuple: "+pair.getSecond());	
				Log.writeInLogFile("Application", "There are is a mapping tuple on module: "+moduleName+" with inputTuple: "+pair.getFirst()+" outputTuple: "+pair.getSecond());
				SelectivityModel selectivityModel = module.getSelectivityMap().get(pair);
				
				boolean selective = selectivityModel.canSelect();
				//*System.out.println("CanSelect()="+selective);
				Log.writeInLogFile("Application", "CanSelect()="+selective);
				if(selective){
					
					//TODO check if the edge is ACTUATOR, then create multiple tuples
					if(edge.getEdgeType() == AppEdge.ACTUATOR){
						//for(Integer actuatorId : module.getActuatorSubscriptions().get(edge.getTupleType())){
							Log.writeInLogFile("Application", "An actuator edge destination");
							Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),  
									(long) (edge.getTupleCpuLength()),
									inputTuple.getNumberOfPes(),
									(long) (edge.getTupleNwLength()),
									inputTuple.getCloudletOutputSize(),
									inputTuple.getUtilizationModelCpu(),
									inputTuple.getUtilizationModelRam(),
									inputTuple.getUtilizationModelBw()
									);
							tuple.setActualTupleId(inputTuple.getActualTupleId());
							tuple.setUserId(inputTuple.getUserId());
							tuple.setAppId(inputTuple.getAppId());
							tuple.setDestModuleName(edge.getDestination());
							tuple.setSrcModuleName(edge.getSource());
							tuple.setDirection(Tuple.ACTUATOR);
							tuple.setTupleType(edge.getTupleType());
							tuple.setSourceDeviceId(sourceDeviceId);
							//tuple.setActuatorId(actuatorId);
							
							tuples.add(tuple);
						//}
					}else{
						Log.writeInLogFile("Application", "A Fog node edge destination");
						Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),  
								(long) (edge.getTupleCpuLength()),
								inputTuple.getNumberOfPes(),
								(long) (edge.getTupleNwLength()),
								inputTuple.getCloudletOutputSize(),
								inputTuple.getUtilizationModelCpu(),
								inputTuple.getUtilizationModelRam(),
								inputTuple.getUtilizationModelBw()
								);
						tuple.setActualTupleId(inputTuple.getActualTupleId());
						tuple.setUserId(inputTuple.getUserId());
						tuple.setAppId(inputTuple.getAppId());
						tuple.setDestModuleName(edge.getDestination());
						tuple.setSrcModuleName(edge.getSource());
						tuple.setDirection(edge.getDirection());
						tuple.setTupleType(edge.getTupleType());
						tuples.add(tuple);
					}
				}
			}
		}
		
		return tuples;
	}
	
	/**
	 * Create a tuple for a given application edge
	 * @param edge
	 * @param sourceDeviceId
	 * @return
	 */
	public Tuple createTuple(AppEdge edge, int sourceDeviceId){
		AppModule module = getModuleByName(edge.getSource());
		if(edge.getEdgeType() == AppEdge.ACTUATOR){
			for(Integer actuatorId : module.getActuatorSubscriptions().get(edge.getTupleType())){
				Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),  
						(long) (edge.getTupleCpuLength()),
						1,
						(long) (edge.getTupleNwLength()),
						100,
						new UtilizationModelFull(), 
						new UtilizationModelFull(), 
						new UtilizationModelFull()
						);
				tuple.setUserId(getUserId());
				tuple.setAppId(getAppId());
				tuple.setDestModuleName(edge.getDestination());
				tuple.setSrcModuleName(edge.getSource());
				tuple.setDirection(Tuple.ACTUATOR);
				tuple.setTupleType(edge.getTupleType());
				tuple.setSourceDeviceId(sourceDeviceId);
				tuple.setActuatorId(actuatorId);
				
				return tuple;
			}
		}else{
			Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),  
					(long) (edge.getTupleCpuLength()),
					1,
					(long) (edge.getTupleNwLength()),
					100,
					new UtilizationModelFull(), 
					new UtilizationModelFull(), 
					new UtilizationModelFull()
					);
			//tuple.setActualTupleId(inputTuple.getActualTupleId());
			tuple.setUserId(getUserId());
			tuple.setAppId(getAppId());
			tuple.setDestModuleName(edge.getDestination());
			tuple.setSrcModuleName(edge.getSource());
			tuple.setDirection(edge.getDirection());
			tuple.setTupleType(edge.getTupleType());
			return tuple;
		}
		return null;
	}
	
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public List<AppModule> getModules() {
		return modules;
	}
	public void setModules(List<AppModule> modules) {
		this.modules = modules;
	}
	public List<AppEdge> getEdges() {
		return edges;
	}
	public void setEdges(List<AppEdge> edges) {
		this.edges = edges;
	}
	public GeoCoverage getGeoCoverage() {
		return geoCoverage;
	}
	public void setGeoCoverage(GeoCoverage geoCoverage) {
		this.geoCoverage = geoCoverage;
	}
	
	public void printSelectivityMap(){
		for(String key:mapSelectivity.keySet()){
			System.out.println(key+" ->  "+mapSelectivity.get(key));
		}
	}

	public List<AppLoop> getLoops() {
		return loops;
	}

	public void setLoops(List<AppLoop> loops) {
		this.loops = loops;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public static Map<String, AppEdge> getEdgeMap() {
		return edgeMap;
	}

	public void setEdgeMap(Map<String, AppEdge> edgeMap) {
		this.edgeMap = edgeMap;
	}
	
	public void setFogDeviceList(List<FogDevice> fogDevices){
		this.fogDevices = fogDevices;
		createMapFogDevicesByName();
		createMapFogDevicesById();
	}
	
	private void createMapFogDevicesByName() {
		Map<String,FogDevice> map = new HashMap<String,FogDevice>();
		for(FogDevice fogdev:fogDevices){
			map.put(fogdev.getName(), fogdev);
		}
		
		mapFogDevicesByName = map;
		////*System.out.println("MapFogDevicesByName is created!");
	}

    private void createMapFogDevicesById() {
		Map<Integer,FogDevice> map = new HashMap<Integer,FogDevice>();
		for(FogDevice fogdev:fogDevices){
			map.put(fogdev.getId(), fogdev);
		}
		
		mapFogDevicesById = map;
		////*System.out.println("MapFogDevicesById is created!");
	}
	
	public List<FogDevice> getFogDevices(){
		return fogDevices;
	}
	
	public List<String> getDataConsIndexOfDataProd(String tupleType){
		return edgeMap.get(tupleType).getDestination();
	}
	
	
	public void addServicesToApplication() {
		// application.addAppModule("ModuleName", ram);
		try {
			if (DataPlacement.nb_DC > 0) {

				for (int i = 0; i < DataPlacement.nb_Service_DC; i++) {
					addAppModule("serviceDC" + (int) i, DataPlacement.SERVICE_DC_RAM, DataPlacement.SERVICE_DC_MIPS, DataPlacement.storageAllocation("DC"), DataPlacement.SERVICE_DC_BW);
				}
				// System.out.println("Nb allocated service on datacenters = "+nb_Service_DC);
			}
			if (DataPlacement.nb_RPOP > 0) {

				for (int i = 0; i < DataPlacement.nb_Service_RPOP; i++) {
					addAppModule("serviceRPOP" + (int) (i), DataPlacement.SERVICE_RPOP_RAM, DataPlacement.SERVICE_RPOP_MIPS, DataPlacement.storageAllocation("RPOP"), DataPlacement.SERVICE_RPOP_BW);
				}
				// System.out.println("Nb allocated service on rpops = "+nb_Service_RPOP);
			}
			if (DataPlacement.nb_LPOP > 0) {

				for (int i = 0; i < DataPlacement.nb_Service_LPOP; i++) {
					addAppModule("serviceLPOP" + (int) (i), DataPlacement.SERVICE_LPOP_RAM, DataPlacement.SERVICE_LPOP_MIPS, DataPlacement.storageAllocation("LPOP"), DataPlacement.SERVICE_LPOP_BW);
				}
				// System.out.println("Nb allocated service on lpops = "+nb_Service_LPOP);
			}
			if (DataPlacement.nb_HGW > 0) {
				for (int i = 0; i < DataPlacement.nb_Service_HGW; i++) {
					addAppModule("serviceHGW" + (int) (i), DataPlacement.SERVICE_HGW_RAM, DataPlacement.SERVICE_HGW_MIPS, DataPlacement.storageAllocation("HGW"), DataPlacement.SERVICE_HGW_BW);
				}
				// System.out.println("Nb allocated service on HGW = "+nb_Service_HGW);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Error in addServicesToApplication!");
		}
	}
	
	public void loadTupleMappingFraction() throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		// System.out.println("Loading Tuple Mapping Fraction");
		FileReader fichier = new FileReader(DataPlacement.nb_HGW + "selectivityMap_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		BufferedReader in = null;
		try {
			
			
			in = new BufferedReader(fichier);
			String line = null;
			while ((line = in.readLine()) != null) {
				// //System.out.println("line:"+line);
				if (!line.equals("")) {
					String[] splited = line.split(",");
					for (String mapFraction : splited) {
						String[] split = mapFraction.split("\t");
						addTupleMapping(split[0],split[1],split[2],new FractionalSelectivity(Double.valueOf(split[3])));

						List<String> input = new ArrayList<String>();
						input.add(split[1]);
						if (mapSelectivity.containsKey(split[2])) {
							input.addAll(mapSelectivity.get(split[2]));
						}
						// //System.out.println("Add selectivity Map:"+split[2]+"  -> "+input);
						mapSelectivity.put(split[2], input);
					}
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	public void saveTupleMappingFraction() throws IOException {
		// TODO Auto-generated method stub
		FileWriter fichier = new FileWriter(DataPlacement.nb_HGW + "selectivityMap_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try {
			BufferedWriter fw = new BufferedWriter(fichier);
			for (AppModule mod : getModules()) {
				if (!mod.getName().contains("DC"))
					fw.write(mod.getSelectitvityToString() + "\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	public void addTupleMappingFraction() {
		// System.out.println("Creating Selectivity Map");
		try {
			/*
			 * add tuple mapping between TempSNR->ServiceHGW->TempHGW and
			 * TempSNR->ServiceHGW->TempAct
			 */
			for (int i = 0; i < DataPlacement.nb_Service_HGW; i++) {
				/* Tuple from SNR --HGW-- LPOP */
				addTupleMapping("serviceHGW" + i, "TempSNR"+ (int) i * DataPlacement.nb_SnrPerHGW, "TempHGW" + i, new FractionalSelectivity(0.1));
				/* Tuple from SNR -- HGW-- ACT */
				for (int actIndex = 0; actIndex < DataPlacement.nb_ActPerHGW; actIndex++)
					addTupleMapping("serviceHGW" + i, "TempSNR"+ (int) i * DataPlacement.nb_SnrPerHGW, "TempAct"+ (int) (actIndex + i * DataPlacement.nb_ActPerHGW),new FractionalSelectivity(0.3));
			}

			/*
			 * add tuple mapping between ?->ServiceLPOP->TempLPOP and
			 * ?->ServiceRPOP->TempRPOP
			 */
			for (String inputTuple : getEdgeMap().keySet()) {
				List<String> destinationServices = getEdgeMap()
						.get(inputTuple).getDestination();
				for (String destService : destinationServices) {
					if (destService.startsWith("serviceLPOP")
							|| destService.startsWith("serviceRPOP")) {
						AppModule mod = getModuleByName(destService);
						String outputTuple = "Temp" + destService.substring(7);
						Map<Pair<String, String>, SelectivityModel> selectivityMap = mod
								.getSelectivityMap();
						Pair<String, String> selec = Pair.create(inputTuple,
								outputTuple);
						if (!selectivityMap.keySet().contains(selec) && selectivityMap.size() < 1) {
							addTupleMapping(mod.getName(),inputTuple, outputTuple,new FractionalSelectivity(1));

							List<String> input = new ArrayList<String>();
							input.add(inputTuple);
							if (mapSelectivity.containsKey(outputTuple)) {
								input.addAll(mapSelectivity.get(outputTuple));
							}
							mapSelectivity.put(outputTuple, input);
						}

					}
				}
			}

			// for(AppModule mod:application.getModules()){
			// //System.out.println("Selectivity size for:"+mod.getName()+"   = "+mod.getSelectivityMap().size());
			// if(mod.getSelectivityMap().size()==0){
			// //System.out.println("selectivity is null, adding one");
			// for(AppEdge edge:application.getEdges()){
			// if(edge.getDestination().contains(mod.getName())){
			// //System.out.println("ajouté");
			// application.addTupleMapping(mod.getName(), edge.getTupleType(),
			// "Temp"+mod.getName().substring(7), new FractionalSelectivity(1));
			// break;
			// }
			// }
			// }
			//
			// }
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Error in addTupleMappingFraction!");
		}

	}
	
	public  void addAppEdgesToApplication() {
		// application.addAppEdge("Source", "Destination", tupleCpuLength,
		// tupleNwLength, "tupleType", direction, edgeType,tupleDataSize);
		try {
			/* From sensors to HGW */
			for (int snrIndex = 0; snrIndex < DataPlacement.nb_Service_HGW * DataPlacement.nb_SnrPerHGW; snrIndex++) {
				addAppEdge("s-" + snrIndex,Arrays.asList("serviceHGW" + (int) snrIndex/ DataPlacement.nb_SnrPerHGW), DataPlacement.SNR_TUPLE_CPU_SIZE,DataPlacement.SNR_TUPLE_FILE_SIZE, "TempSNR" + snrIndex, Tuple.UP,AppEdge.SENSOR);
			}

			/* From Hgws to Lpop */
			for (int hgwIndex = 0; hgwIndex < DataPlacement.nb_Service_HGW; hgwIndex++) {
				addAppEdge("serviceHGW" + hgwIndex, getDataConsOfService(getPossibleList("serviceHGW"+ hgwIndex)), DataPlacement.HGW_TUPLE_CPU_SIZE, DataPlacement.HGW_TUPLE_FILE_SIZE, "TempHGW" + hgwIndex, Tuple.UP, AppEdge.MODULE);
			}

			/* From Hgws to Acts */
			for (int actIndex = 0; actIndex < DataPlacement.nb_Service_HGW * DataPlacement.nb_ActPerHGW; actIndex++) {
				addAppEdge("serviceHGW" + (int) (int) actIndex/ DataPlacement.nb_ActPerHGW, Arrays.asList("DISPLAY" + actIndex),DataPlacement.ACT_TUPLE_CPU_SIZE, DataPlacement.ACT_TUPLE_FILE_SIZE, "TempAct"+ actIndex, Tuple.DOWN, AppEdge.ACTUATOR);
			}

			/* From Lpops to Rpop */
			for (int lpopIndex = 0; lpopIndex < DataPlacement.nb_Service_LPOP; lpopIndex++) {
				addAppEdge("serviceLPOP" + lpopIndex,getDataConsOfService(getPossibleList("serviceLPOP"+ lpopIndex)),DataPlacement.LPOP_TUPLE_CPU_SIZE, DataPlacement.LPOP_TUPLE_FILE_SIZE, "TempLPOP"+ lpopIndex, Tuple.UP, AppEdge.MODULE);
			}

			/* From Rpop to Dc */
			for (int rpopIndex = 0; rpopIndex < DataPlacement.nb_Service_RPOP; rpopIndex++) {
				addAppEdge("serviceRPOP" + rpopIndex,getDataConsOfService(getPossibleList("serviceRPOP"+ rpopIndex)),DataPlacement.RPOP_TUPLE_CPU_SIZE, DataPlacement.RPOP_TUPLE_FILE_SIZE, "TempRPOP"+ rpopIndex, Tuple.UP, AppEdge.MODULE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Error in addAppEdgesToApplication!");
		}
	}

	private List<String> getDataConsOfService(List<String> possibleServiceList) {
		List<String> chosedList = new ArrayList<String>();
		for (int i = 0; i < DataPlacement.nb_DataCons_By_DataProd; i++) {
			if (possibleServiceList.size() > i) {
				boolean chose = false;
				while (!chose) {
					int rand = (int) (Math.random() * possibleServiceList.size());
					if (!chosedList.contains(possibleServiceList.get(rand))) {
						chosedList.add(possibleServiceList.get(rand));
						chose = true;
					}
				}
			} else {
				return chosedList;
			}
		}
		return chosedList;
	}

	private List<String> getPossibleList(String moduleName) {
		List<String> list = new ArrayList<String>();
		// //System.out.println("get possible list for module :"+moduleName);
		FogDevice host = getFogDeviceByName(ModuleMapping.getDeviceHostModule(moduleName));
		// //System.out.println("Host:"+host.getName());
		int service_index;
		switch (DataPlacement.dataflow_used){
			case DataPlacement.zoning:
				if (moduleName.startsWith("serviceHGW")) {
					//service_index= Integer.valueOf(moduleName.substring(10));
					
					FogDevice parent = getFogDeviceById(host.getParentId());
					FogDevice superparent = getFogDeviceById(parent.getParentId());
		
					list.add("service" + superparent.getName());
		
					for (int childId : superparent.getChildrenIds()) {
						FogDevice dev = getFogDeviceById(childId);
						list.add("service" + dev.getName());
						for(int smallChildId :dev.getChildrenIds()){
							FogDevice smallDev = getFogDeviceById(smallChildId);
							list.add("service" + smallDev.getName());
						}
					}
					
//					System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
		
				}else if (moduleName.startsWith("serviceLPOP")) {
					//service_index= Integer.valueOf(moduleName.substring(11));
					FogDevice parent = getFogDeviceById(host.getParentId());
					FogDevice superparent = getFogDeviceById(parent.getParentId());
					list.add("service" + superparent.getName());
					List<Integer> childrensIds = superparent.getChildrenIds();
					
					for (int childId : childrensIds){
						FogDevice dev = getFogDeviceById(childId);
						list.add("service" + dev.getName());
						for(int smallChildId :dev.getChildrenIds()){
							FogDevice smallDev = getFogDeviceById(smallChildId);
							list.add("service" + smallDev.getName());
						}
					}
//					System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
		
				}else if (moduleName.startsWith("serviceRPOP")) {
					
					for(int i=0; i<DataPlacement.nb_DC;i++){
						list.add("serviceDC"+i);
					}
					
//					FogDevice parent = application.getFogDeviceById(host.getParentId());
//					list.add("service" + parent.getName());
//					List<Integer> childrensIds = parent.getChildrenIds();
//					
//					for (int childId : childrensIds){
//						FogDevice dev = application.getFogDeviceById(childId);
//						list.add("service" + dev.getName());
//					}
					
//					service_index= Integer.valueOf(moduleName.substring(11));
//					FogDevice fognode = application.getFogDeviceByName("RPOP"+service_index);
//					list.add(moduleName);
//					
//					List<Integer> childrensIds = fognode.getChildrenIds();
//					for (int childId : childrensIds) {
//						FogDevice dev = application.getFogDeviceById(childId);
//						list.add("service" + dev.getName());
//						for(int smallChildId :dev.getChildrenIds()){
//							FogDevice smallDev = application.getFogDeviceById(smallChildId);
//							list.add("service" + smallDev.getName());
//						}
//					}
//					System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
				}
				
			case DataPlacement.distributed:
				if(moduleName.startsWith("serviceHGW")){
					for(int i=0; i<DataPlacement.nb_RPOP;i++){
						list.add("serviceRPOP"+i);
					}
					for(int i=0; i<DataPlacement.nb_LPOP;i++){
						list.add("serviceLPOP"+i);
					}
					for(int i=0; i<DataPlacement.nb_HGW;i++){
						list.add("serviceHGW"+i);
					}
				}else if(moduleName.startsWith("serviceLPOP")){
					for(int i=0; i<DataPlacement.nb_DC;i++){
						list.add("serviceDC"+i);
					}
					for(int i=0; i<DataPlacement.nb_RPOP;i++){
						list.add("serviceRPOP"+i);
					}
					for(int i=0; i<DataPlacement.nb_LPOP;i++){
						list.add("serviceLPOP"+i);
					}
				}else if(moduleName.startsWith("serviceRPOP")){
					for(int i=0; i<DataPlacement.nb_DC;i++){
						list.add("serviceDC"+i);
					}
				}
				
				//System.out.println("possible list for "+moduleName+" is "+list.toString());
				return list;
				
			case DataPlacement.mixed:
				if (moduleName.startsWith("serviceHGW")) {
					service_index= Integer.valueOf(moduleName.substring(10));
					if(service_index % (2)==0){
						FogDevice parent = getFogDeviceById(host.getParentId());
						FogDevice superparent = getFogDeviceById(parent.getParentId());
			
						list.add("service" + superparent.getName());
			
						for (int childId : superparent.getChildrenIds()) {
							FogDevice dev = getFogDeviceById(childId);
							list.add("service" + dev.getName());
							for(int smallChildId :dev.getChildrenIds()){
								FogDevice smallDev = getFogDeviceById(smallChildId);
								list.add("service" + smallDev.getName());
							}
						}
	
					}else{
						for(int i=0; i<DataPlacement.nb_RPOP;i++){
							list.add("serviceRPOP"+i);
						}
						for(int i=0; i<DataPlacement.nb_LPOP;i++){
							list.add("serviceLPOP"+i);
						}
						for(int i=0; i<DataPlacement.nb_HGW;i++){
							list.add("serviceHGW"+i);
						}
					}
					//System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
		
				}else if (moduleName.startsWith("serviceLPOP")) {
					service_index= Integer.valueOf(moduleName.substring(11));
					if(service_index % (2)==0){
						FogDevice parent = getFogDeviceById(host.getParentId());
						FogDevice superparent = getFogDeviceById(parent.getParentId());
						list.add("service" + superparent.getName());
						List<Integer> childrensIds = superparent.getChildrenIds();
						
						for (int childId : childrensIds){
							FogDevice dev = getFogDeviceById(childId);
							list.add("service" + dev.getName());
							for(int smallChildId :dev.getChildrenIds()){
								FogDevice smallDev = getFogDeviceById(smallChildId);
								list.add("service" + smallDev.getName());
							}
						}
						
					}else{
							for(int i=0; i<DataPlacement.nb_DC;i++){
								list.add("serviceDC"+i);
							}
							for(int i=0; i<DataPlacement.nb_RPOP;i++){
								list.add("serviceRPOP"+i);
							}
							for(int i=0; i<DataPlacement.nb_LPOP;i++){
								list.add("serviceLPOP"+i);
							}			
					}
					//System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
		
				}else if (moduleName.startsWith("serviceRPOP")) {
					for(int i=0; i<DataPlacement.nb_DC;i++){
						list.add("serviceDC"+i);
					}
//					service_index= Integer.valueOf(moduleName.substring(11));
//					if(service_index % (2)==0){
//						FogDevice parent = application.getFogDeviceById(host.getParentId());
//						list.add("service" + parent.getName());
//						List<Integer> childrensIds = parent.getChildrenIds();
//						
//						for (int childId : childrensIds){
//							FogDevice dev = application.getFogDeviceById(childId);
//							list.add("service" + dev.getName());
//						}
//						
//					}else{
//						for(int i=0; i<nb_DC;i++){
//							list.add("serviceDC"+i);
//						}
//						for(int i=0; i<nb_RPOP;i++){
//							list.add("serviceRPOP"+i);
//						}					
//					}
					//System.out.println("possible list for "+moduleName+" is "+list.toString());
					return list;
				}	
		}
		return null;
	}
	
	public void loadApplicationEdges() throws FileNotFoundException, InterruptedException {
		// System.out.println("Loading application Edges");
		FileReader fichier = new FileReader(DataPlacement.nb_HGW + "edges_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		BufferedReader in = null;
		try {

			in = new BufferedReader(fichier);
			String line = null;

			while ((line = in.readLine()) != null) {
				String source;
				List<String> destination = new ArrayList<String>();
				double tupleCpuLength;
				double tupleNwLength;
				String tupleType;
				int direction;
				int edgeType;
				// double periodicity;
				// boolean isPeriodic;

				String[] splited = line.split("\t");
				source = splited[0];

				String des = splited[1].substring(1, splited[1].length() - 1);

				if (des.contains(",")) {
					String[] sp = des.split(",");
					for (String s : sp) {
						if (s.substring(0, 1).equals(" "))
							s = s.substring(1);
						destination.add(s);
					}
				} else {
					destination.add(des);
				}

				tupleCpuLength = Double.valueOf(splited[2]);
				tupleNwLength = Double.valueOf(splited[3]);
				tupleType = splited[4];
				direction = Integer.valueOf(splited[5]);
				edgeType = Integer.valueOf(splited[6]);
				// periodicity = Double.valueOf(splited[7]);
				// isPeriodic = Boolean.valueOf(splited[8]);
				addAppEdge(source, destination, tupleCpuLength,tupleNwLength, tupleType, direction, edgeType);
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveApplicationEdges()
			throws IOException {
		// TODO Auto-generated method stub
		FileWriter fichier = new FileWriter(DataPlacement.nb_HGW + "edges_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try {
			BufferedWriter fw = new BufferedWriter(fichier);
			for (AppEdge edge : edges) {
				fw.write(edge.toString() + "\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printModules(){
		////*System.out.println("Modules:");
		for(AppModule appmod : this.modules){
			////*System.out.print("appmmod.name = "+appmod.getName());
			////*System.out.print("		appmmod.appId = "+appmod.getAppId());
			////*System.out.println("	  appmmod.getActuatorSubscriptions = "+appmod.getActuatorSubscriptions());
		}	
	}

	public void printEdges(){
		////*System.out.println("Edges:");
		for(AppEdge appEdge:edges){
			////*System.out.println(appEdge);
		}
	}

	public void printLoops(){
		for(AppLoop appLoop : loops){
			List <String> appLoopModules = appLoop.getModules();
			////*System.out.println("\nloops = "+appLoopModules);
		}
	}

	public void printEdgeMap(){
		////*System.out.println("Print edge Map:");
		for(String key : edgeMap.keySet()){
			////*System.out.println(key+"     "+edgeMap.get(key));
			
		}
	}

	public void printModulesSelectivityMap(){
		////*System.out.println("Print Modules Selectitvity Map:");
		for(AppModule mod : modules){
			////*System.out.println(mod.getSelectitvityAsString());
		}
	}
}
