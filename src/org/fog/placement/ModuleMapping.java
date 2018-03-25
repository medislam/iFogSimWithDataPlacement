package org.fog.placement;

import java.util.HashMap;
import java.util.Map;

import org.fog.entities.FogDevice;
import org.fog.examples.DataPlacement;

public class ModuleMapping {
	/**
	 * Mapping from node name to list of <moduleName, numInstances> of instances to be launched on node
	 */
	
	protected static Map<String, Map<String, Integer>> moduleMapping;
	
	public static Map<String, String> moduleToHostMap;
	
	public static ModuleMapping createModuleMapping(){
		return new ModuleMapping();
	}

	public Map<String, Map<String, Integer>> getModuleMapping() {
		return moduleMapping;
	}
	
	public void setModuleMapping(Map<String, Map<String, Integer>> moduleMapping) {
		this.moduleMapping = moduleMapping;
	}
	
	public Map<String, String> getModuleToHostMap(){
		return moduleToHostMap;
	}
	
	public void setModuleToHostMap() {
		// TODO Auto-generated method stub
		System.out.println("Set the module to host Mapping");
		Map<String,String> map = new HashMap<String,String>();
		for(String hostName:moduleMapping.keySet()){
			for(String moduleName:moduleMapping.get(hostName).keySet()){
				map.put(moduleName, hostName);
			}
		}
		moduleToHostMap = map;
	}

	public ModuleMapping(){
		setModuleMapping(new HashMap<String, Map<String, Integer>>());
	}
	
	/**
	 * Add 1 instance of module moduleName to device deviceName
	 * @param moduleName
	 * @param deviceName
	 */
	
	public void addModuleToDevice(String moduleName, String deviceName){
		addModuleToDevice(moduleName, deviceName, 1);
	}
	
	/**
	 * Add <b>instanceCount</b> number of instances of module <b>moduleName</b> to <b>device deviceName</b>
	 * @param moduleName
	 * @param deviceName
	 * @param instanceCount
	 */
	
	public void addModulesToFogDevices() {
		FogDevice device = null;
		// String chosedDev ;
		// List<String> nonChosedDC = new ArrayList<String>();
		// List<String> nonChosedRPOP = new ArrayList<String>();
		// List<String> nonChosedLPOP = new ArrayList<String>();

		/* Initialization */
		// for(FogDevice dev : fogDevices){
		// if(dev.getName().startsWith("DC")){
		// nonChosedDC.add(dev.getName());
		// }else if(dev.getName().startsWith("RPOP")){
		// nonChosedRPOP.add(dev.getName());
		// }else if(dev.getName().startsWith("LPOP")){
		// nonChosedLPOP.add(dev.getName());
		// }
		// }

		/* Module deployment at DCs */
		for (int dc = 0; dc < DataPlacement.nb_Service_DC; dc++) {
			// chosedDev = choseDevice(nonChosedDC);
			// nonChosedDC.remove(chosedDev);
			// moduleMapping.addModuleToDevice("serviceDC"+dc, chosedDev, 1);
			addModuleToDevice("serviceDC" + dc, "DC" + dc, 1);
			// //System.out.println("Add "+"serviceDC"+dc+" to  "+device.getName());

		}

		/* Module deployment at RPOPs */
		for (int RPOP = 0; RPOP < DataPlacement.nb_Service_RPOP; RPOP++) {
			// chosedDev = choseDevice(nonChosedRPOP);
			// nonChosedRPOP.remove(chosedDev);
			// moduleMapping.addModuleToDevice("serviceRPOP"+(int) (RPOP),
			// chosedDev, 1);
			addModuleToDevice("serviceRPOP" + RPOP,"RPOP" + RPOP, 1);
			// //System.out.println("Add "+"serviceRPOP"+(int)
			// (RPOP)+" to  "+device.getName());
		}

		/* Module deployment at LPOPs */
		for (int LPOP = 0; LPOP < DataPlacement.nb_Service_LPOP; LPOP++) {
			// chosedDev = choseDevice(nonChosedLPOP);
			// nonChosedLPOP.remove(chosedDev);
			// moduleMapping.addModuleToDevice("serviceLPOP"+(int) (LPOP),
			// chosedDev, 1);
			addModuleToDevice("serviceLPOP" + LPOP,"LPOP" + LPOP, 1);
			// //System.out.println("Add "+"serviceLPOP"+(int)
			// (LPOP)+" to  "+device.getName());
		}

		/* Module deployment at HGWs */
		for (int HGW = 0; HGW < DataPlacement.nb_Service_HGW; HGW++) {
			// device = fogDevices.get(HGW+nb_DC+nb_RPOP+nb_LPOP);
			// moduleMapping.addModuleToDevice("serviceHGW"+(int) (HGW),
			// device.getName(), 1);
			addModuleToDevice("serviceHGW" + HGW, "HGW" + HGW, 1);
			// //System.out.println("Add "+"serviceHGW"+(int)
			// (HGW)+" to  "+device.getName());
		}

	}
	
	public void addModuleToDevice(String moduleName, String deviceName, int instanceCount){
		if(!getModuleMapping().containsKey(deviceName))
			getModuleMapping().put(deviceName, new HashMap<String, Integer>());
		if(!getModuleMapping().get(deviceName).containsKey(moduleName))
			getModuleMapping().get(deviceName).put(moduleName, instanceCount);
	}
	
	
	public static String getDeviceHostModule(String moduleName){
		if(moduleToHostMap.containsKey(moduleName)){
			return moduleToHostMap.get(moduleName);
		}
		
		return null;
	}
	
	public static void printModuleMapping(){
		System.out.println("Module Mapping");
		for(String key : moduleMapping.keySet()){
			System.out.println("Device "+key+"   implements modules "+moduleMapping.get(key));
		}
		System.out.println();
	}
}
