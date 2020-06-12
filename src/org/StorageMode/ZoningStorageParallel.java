package org.StorageMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.Results.SaveResults;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.Parallel.CloudSimParallel;
import org.fog.Parallel.ThreadInZone;
import org.fog.application.Application;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.ConsProdMatrix;
import org.fog.gui.lpFileConstuction.DataSizeVector;
import org.fog.gui.lpFileConstuction.FreeCapacityVector;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.gui.lpFileConstuction.MakeLPFileInZone;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.utils.TimeKeeper;

public class ZoningStorageParallel {
	
	public ZoningStorageParallel(){
		
	}
	
	public void sim() throws Exception{
		System.out.println("----------------------------------------------------------");
		System.out.println(DataPlacement.ZoningStorageParallel);
		Log.writeInLogFile("DataPlacement","----------------------------------------------------------");
		Log.writeInLogFile("DataPlacement", DataPlacement.ZoningStorageParallel);

		for (int nb_z : DataPlacement.nb_zones_list) {
			DataPlacement.nb_zone = nb_z;
			if (DataPlacement.nb_zone == 1 || DataPlacement.nb_zone == 0 || DataPlacement.nb_RPOP % DataPlacement.nb_zone != 0) {
				System.out.println("Can't zoning RPOPs, must choose another zoning! nb_zone:"+DataPlacement.nb_zone+"  nb_RPOP:"+DataPlacement.nb_RPOP);
				System.exit(0);
			}
			
			org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW, "consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+DataPlacement.ZoningStorage+ "		nb_zones:"+ DataPlacement.nb_zone);
			//CloudSim.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag);
			CloudSimParallel cloudsimparallel = new CloudSimParallel();
			cloudsimparallel.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag, cloudsimparallel);
			
			String appId = "Data_Placement"; // identifier of the application
			FogBroker broker = new FogBroker("broker", cloudsimparallel);
			System.out.println("Creating of the Fog devices!");
			Log.writeInLogFile("DataPlacement","Creating of the Fog devices!");
			DataPlacement.createFogDevices(broker.getId(), appId, cloudsimparallel);

			System.out.println("Creating of Sensors and Actuators!");
			Log.writeInLogFile("DataPlacement","Creating of Sensors and Actuators!");
			DataPlacement.createSensorsAndActuators(broker.getId(), appId, cloudsimparallel);

			/* Module deployment */
			System.out.println("Creating and Adding modules to devices");
			Log.writeInLogFile("DataPlacement","Creating and Adding modules to devices");
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
			moduleMapping.addModulesToFogDevices();
			moduleMapping.setModuleToHostMap();

			Application application = DataPlacement.createApplication(appId,broker.getId());
			application.setUserId(broker.getId());
			application.setFogDeviceList(DataPlacement.fogDevices);

			System.out.println("Controller!");
			Log.writeInLogFile("DataPlacement", "Controller!");
			Controller controller = new Controller("master-controller",DataPlacement.fogDevices, DataPlacement.sensors, DataPlacement.actuators, moduleMapping, cloudsimparallel);
			controller.submitApplication(application, 0, cloudsimparallel);

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			System.out.println("Basis Delay Matrix computing!");
			BasisDelayMatrix delayMatrix = new BasisDelayMatrix(DataPlacement.fogDevices, DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP,DataPlacement.nb_Service_RPOP, DataPlacement.nb_Service_DC, DataPlacement.nb_HGW, DataPlacement.nb_LPOP,
					DataPlacement.nb_RPOP, DataPlacement.nb_DC, application);
			
							

			System.out.println("Loading ....");
			Log.writeInLogFile("DataPlacement", "Loading ....");
			application.loadApplicationEdges();
			application.loadTupleMappingFraction();
			BasisDelayMatrix.loadBasisDelayMatrix();
			System.out.println("Loaded");
			Log.writeInLogFile("DataPlacement", "Loaded");
			
			DataPlacement.nb_externCons = 0;
			
			DataAllocation dataAllocation = new DataAllocation();
			//BasisDelayMatrix.printBasisDelayMatrix();
			application.printFogDevices();
			
			// la table des threads
			ThreadInZone [] tabThread = new ThreadInZone[DataPlacement.nb_zone];
			
			//for (int zone = 0; zone < DataPlacement.nb_zone; zone++) {
			for (int zone = 0; zone < 1; zone++) {
				Map<String, List<Integer>> zoneDevises = getZoneDevMap(zone, application);
				
				ThreadInZone thrZone = new ThreadInZone(application, delayMatrix, zone, dataAllocation, zoneDevises, cloudsimparallel);
				thrZone.start();
				tabThread[zone] = thrZone;
				System.out.println("Le thread zone :"+zone+" est démarré.");
				

				
			}
			
			//for (int zone = 0; zone < DataPlacement.nb_zone; zone++) {
			for (int zone = 0; zone < 1; zone++) {
				// attendre la fin du traitement
				ThreadInZone thrZone = tabThread[zone];
				thrZone.join();
				System.out.println("Le thread zone :"+zone+" est fini son traitement.");
				
			}
			
			//DataAllocation.printDataAllocationMap(application);
			//org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd+"\n"+DataPlacement.storageMode+"\n"+DataAllocation.dataAllocationStats(application));
			
			//org.fog.examples.Log.writeDataAllocationStatsExternZoneCons(DataPlacement.nb_HGW,DataPlacement.storageMode+DataPlacement.nb_zone+"\n Nb extern cons :"+DataPlacement.nb_externCons);

			
//			cloudsimparallel.startSimulation(cloudsimparallel);
//			cloudsimparallel.stopSimulation(cloudsimparallel);
			
			System.out.println("End of simulation!");

			System.out.println(DataPlacement.storageMode);
			System.out.println("Read Latency:"+ LatencyStats.getOverall_read_Latency());
			System.out.println("Write Latency:"+ LatencyStats.getOverall_write_Latency());
			System.out.println("Overall Latency:"+ LatencyStats.getOverall_Latency());

			Log.writeInLogFile("DataPlacement", DataPlacement.storageMode);
			Log.writeInLogFile("DataPlacement", "Read Latency:"+ LatencyStats.getOverall_read_Latency());
			Log.writeInLogFile("DataPlacement", "Write Latency:"+ LatencyStats.getOverall_write_Latency());
			Log.writeInLogFile("DataPlacement", "Overall Latency:"+ LatencyStats.getOverall_Latency());
			SaveResults.saveLatencyTimes(DataPlacement.nb_DataCons_By_DataProd, DataPlacement.storageMode,DataPlacement.nb_zone, 
					LatencyStats.getOverall_read_Latency(),
					LatencyStats.getOverall_write_Latency(),
					LatencyStats.getOverall_Latency());

			LatencyStats.reset_Overall_Letency();
			LatencyStats.reset_Overall_write_Letency();
			LatencyStats.reset_Overall_read_Letency();

			System.out.println("VRGame finished!");

			DataPlacement.fogDevices.clear();
			DataPlacement.sensors.clear();
			DataPlacement.actuators.clear();
			System.gc();

		}
		
	}
	
	private Map<String, List<Integer>> getZoneDevMap(int zone, Application application) {
		Map<String, List<Integer>> zoneDeviceMap = new HashMap<String, List<Integer>>();

		List<Integer> rpops = new ArrayList<>();
		List<Integer> lpops = new ArrayList<>();
		List<Integer> hgws = new ArrayList<>();

		int nb_rpopInZone = DataPlacement.nb_RPOP / DataPlacement.nb_zone;

		int firstRPOPId = zone * nb_rpopInZone + DataPlacement.nb_DC + 3;
		int lastRPOPId = (zone + 1) * nb_rpopInZone + DataPlacement.nb_DC + 3;

		for (int i = firstRPOPId; i < lastRPOPId; i++) {
			// System.out.println("-------------------");
			// System.out.println("RPOP:"+i);
			rpops.add(i);
			FogDevice RPOP = application.getFogDeviceById(i);
			List<Integer> lpopIds = RPOP.getChildrenIds();
			lpops.addAll(lpopIds);
			// System.out.println("ChildrenLPOPsIds:"+lpopIds);
			for (Integer lpopId : lpopIds) {
				// System.out.println("LPOP:"+lpopId);
				FogDevice LPOP = application.getFogDeviceById(lpopId);
				List<Integer> hgwIds = LPOP.getChildrenIds();
				hgws.addAll(hgwIds);
				// System.out.println("ChlidrenHGWIds:"+hgwIds);
			}
		}
		zoneDeviceMap.put("hgws", hgws);
		zoneDeviceMap.put("lpops", lpops);
		zoneDeviceMap.put("rpops", rpops);
		return zoneDeviceMap;
	}
}
