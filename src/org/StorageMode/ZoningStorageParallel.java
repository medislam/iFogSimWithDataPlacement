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
import org.cloudbus.cloudsim.core.CloudInformationService;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimShutdown;
import org.fog.Parallel.CloudSimParallel;
import org.fog.Parallel.ThreadInZone;
import org.fog.application.Application;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
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
			
			CloudSimParallel [] tabCloudSimParallel = new CloudSimParallel[DataPlacement.nb_zone];
			String appId = "Data_Placement"; // identifier of the application
			
			CloudSimParallel initcloudsimparallel = new CloudSimParallel(0);
			initcloudsimparallel.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag, initcloudsimparallel);
			
			System.out.println("add Broker to initcloudsimparallel ");
			FogBroker broker = new FogBroker("broker", initcloudsimparallel);
			initcloudsimparallel.addEntity2(broker);
			
			System.out.println("Creating of the Fog devices!");
			DataPlacement.createFogDevices(broker.getId(), appId, initcloudsimparallel);
			setFogDevicesIds(initcloudsimparallel);
			
			System.out.println("Creating of Sensors and Actuators!");
			Log.writeInLogFile("DataPlacement","Creating of Sensors and Actuators!");
			DataPlacement.createSensorsAndActuators(broker.getId(), appId, initcloudsimparallel);
			setSensorsAndActuatorsIds(initcloudsimparallel);
			

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
			Controller controller = new Controller("master-controller",DataPlacement.fogDevices, DataPlacement.sensors, DataPlacement.actuators, moduleMapping, initcloudsimparallel);
			controller.submitApplication(application, 0, initcloudsimparallel);

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
			
			// la table des threads
			ThreadInZone [] tabThread = new ThreadInZone[DataPlacement.nb_zone];
			CloudSimParallel cloudsimparallel;
			
			
			
			initcloudsimparallel.addEntity2(controller);
			//application.printFogDevices();
			
			CloudInformationService cis = (CloudInformationService) initcloudsimparallel.getEntityByName("CloudInformationService");
			
			CloudSimShutdown shutdown = (CloudSimShutdown) initcloudsimparallel.getEntityByName("CloudSimShutdown");
			
			for(int zone = 0; zone < DataPlacement.nb_zone; zone++) {
				
				if(zone == 0) {
					cloudsimparallel = initcloudsimparallel;
					cloudsimparallel.printEntities();
					cloudsimparallel.deleleInitiaEntities();
					
					
				}else {
					cloudsimparallel = new CloudSimParallel(zone);
					cloudsimparallel.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag, cloudsimparallel);
					cloudsimparallel.addEntityToEntities(shutdown);
					cloudsimparallel.addEntityToEntities(cis);
					cloudsimparallel.addEntityToEntities(broker);
					cloudsimparallel.addEntityToEntities(controller);
					
					
				}
				
				tabCloudSimParallel[zone] = cloudsimparallel;
				
				Map<String, List<Integer>> zoneDevises = getZoneDevMap2(zone, application, cloudsimparallel);
				cloudsimparallel.addEntitiesIdList();
				
				
				ThreadInZone thrZone = new ThreadInZone(application, delayMatrix, zone, dataAllocation, zoneDevises, cloudsimparallel);
				thrZone.start();
				tabThread[zone] = thrZone;
				System.out.println("Le thread zone :"+zone+" est démarré.");
				
				
			}
			
//			for (int zone = 0; zone < DataPlacement.nb_zone; zone++) {
//				// attendre la fin du traitement
//				cloudsimparallel = tabCloudSimParallel[zone];
//				cloudsimparallel.printEntities();
//			}

			
			for (int zone = 0; zone < DataPlacement.nb_zone; zone++) {
				// attendre la fin du traitement
				ThreadInZone thrZone = tabThread[zone];
				thrZone.join();
				System.out.println("Le thread zone :"+zone+" est fini son traitement.");
				
			}
			
			//DataAllocation.printDataAllocationMap(application);
			//org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd+"\n"+DataPlacement.storageMode+"\n"+DataAllocation.dataAllocationStats(application));
			
			//org.fog.examples.Log.writeDataAllocationStatsExternZoneCons(DataPlacement.nb_HGW,DataPlacement.storageMode+DataPlacement.nb_zone+"\n Nb extern cons :"+DataPlacement.nb_externCons);

			
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
	
	private void setFogDevicesIds(CloudSimParallel initcloudsimparallel) {
		// TODO Auto-generated method stub
		
		for(FogDevice fogdev : DataPlacement.fogDevices) {
			initcloudsimparallel.addEntity2(fogdev);
		}
		
	}
	
	private void setSensorsAndActuatorsIds(CloudSimParallel initcloudsimparallel) {
		// TODO Auto-generated method stub
		
		for(Sensor snr : DataPlacement.sensors) {
			initcloudsimparallel.addEntity2(snr);
		}
		
		for(Actuator act : DataPlacement.actuators) {
			initcloudsimparallel.addEntity2(act);
		}
		
	}

	private Map<String, List<Integer>> getZoneDevMap2(int zone, Application application, CloudSimParallel cloudSimParallel) {
		System.out.println("Getting Entities for Zone:"+zone);
		
		Map<String, List<Integer>> zoneDeviceMap = new HashMap<String, List<Integer>>();
		
		List<Integer> dcs = new ArrayList<>();
		List<Integer> rpops = new ArrayList<>();
		List<Integer> lpops = new ArrayList<>();
		List<Integer> hgws = new ArrayList<>();
		List<Integer> snrs = new ArrayList<>();
		List<Integer> acts = new ArrayList<>();


		int nb_dcInZone = DataPlacement.nb_DC / DataPlacement.nb_zone;
		int nb_rpopInZone = DataPlacement.nb_RPOP / DataPlacement.nb_zone;
		int nb_lpopInZone = DataPlacement.nb_LPOP / DataPlacement.nb_zone;
		int nb_hgwInZone = DataPlacement.nb_HGW / DataPlacement.nb_zone;
		
		int firstDCId = zone * nb_dcInZone;
		int lastDCId = (zone + 1) * nb_dcInZone;
		
		for(int dc_id=firstDCId;dc_id<lastDCId;dc_id++) {
			
			FogDevice dc = DataPlacement.fogDeviceByNameMap.get("DC"+dc_id);
			dcs.add(dc.getId());
			cloudSimParallel.addEntityToEntities(dc);
		}
		
		int firstrpopId = zone * nb_rpopInZone;
		int lastrpopId = (zone + 1) * nb_rpopInZone;
		
		for(int rpop_id=firstrpopId;rpop_id<lastrpopId;rpop_id++) {
			
			FogDevice rpop = DataPlacement.fogDeviceByNameMap.get("RPOP"+rpop_id);
			rpops.add(rpop.getId());
			cloudSimParallel.addEntityToEntities(rpop);
			
		}
		
		
		int firstlpopId = zone * nb_lpopInZone;
		int lastlpopId = (zone + 1) * nb_lpopInZone;
		
		for(int lpop_id=firstlpopId;lpop_id<lastlpopId;lpop_id++) {
			
			FogDevice lpop = DataPlacement.fogDeviceByNameMap.get("LPOP"+lpop_id);
			lpops.add(lpop.getId());
			cloudSimParallel.addEntityToEntities(lpop);
		}
		
		int firsthgwId = zone * nb_hgwInZone;
		int lasthgwId = (zone + 1) * nb_hgwInZone;
		
		for(int hgw_id=firsthgwId;hgw_id<lasthgwId;hgw_id++) {
			
			FogDevice hgw = DataPlacement.fogDeviceByNameMap.get("HGW"+hgw_id);
			hgws.add(hgw.getId());
			cloudSimParallel.addEntityToEntities(hgw);
		}

			
		int nb_snrInZone = (DataPlacement.nb_HGW / DataPlacement.nb_zone) * DataPlacement.nb_SnrPerHGW ;
		
		int firstsnrId =  zone * nb_snrInZone;
		int lastsnrId = (zone + 1) * nb_snrInZone;
		for(int snr_id=firstsnrId;snr_id<lastsnrId;snr_id++) { 
			Sensor snr = DataPlacement.sensorsByNameMap.get("s-"+snr_id);
			snrs.add(snr.getId());
			cloudSimParallel.addEntityToEntities(snr);
		}
		
		
		int nb_actInZone = (DataPlacement.nb_HGW / DataPlacement.nb_zone) * DataPlacement.nb_ActPerHGW;
		
		int firstactId =  zone * nb_actInZone;
		int lastactId = (zone + 1) * nb_actInZone;
		for(int act_id=firstactId;act_id<lastactId;act_id++) { 
			Actuator act = DataPlacement.actuatorsByNameMap.get("a-"+act_id);
			acts.add(act.getId());
			cloudSimParallel.addEntityToEntities(act);
		}
		
		zoneDeviceMap.put("acts", acts);
		zoneDeviceMap.put("snrs", snrs);
		zoneDeviceMap.put("hgws", hgws);
		zoneDeviceMap.put("lpops", lpops);
		zoneDeviceMap.put("rpops", rpops);
		zoneDeviceMap.put("dcs", dcs);
		
		return zoneDeviceMap;
	}
}
