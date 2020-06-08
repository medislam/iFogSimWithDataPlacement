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
			CloudSim.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag);
			String appId = "Data_Placement"; // identifier of the application
			FogBroker broker = new FogBroker("broker");
			System.out.println("Creating of the Fog devices!");
			Log.writeInLogFile("DataPlacement","Creating of the Fog devices!");
			DataPlacement.createFogDevices(broker.getId(), appId);

			System.out.println("Creating of Sensors and Actuators!");
			Log.writeInLogFile("DataPlacement","Creating of Sensors and Actuators!");
			DataPlacement.createSensorsAndActuators(broker.getId(), appId);

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
			Controller controller = new Controller("master-controller",DataPlacement.fogDevices, DataPlacement.sensors, DataPlacement.actuators, moduleMapping);
			controller.submitApplication(application, 0);

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
	
			for (int zone = 0; zone < DataPlacement.nb_zone; zone++) {
				System.out.println("\n******************Zone " + zone+ " ******************");
				Log.writeInLogFile("DataPlacement","\n******************Zone " + zone+ " ******************");
				Map<String, List<Integer>> zoneDevises = getZoneDevMap(zone, application);

				/* generate write and read basis delay files */
				delayMatrix.generateBasisWriteDelayFileInZone(DataPlacement.nb_HGW, zone,zoneDevises);
				delayMatrix.generateBasisReadDelayFileInZone(DataPlacement.nb_HGW, zone,zoneDevises);

				/* generate Data Size vector */
				System.out.println("Generating of Data Size for zone:"+ zone);
				Log.writeInLogFile("DataPlacement","Generating of Data Size for zone:" + zone);
				DataSizeVector dataSizeVec = new DataSizeVector(application.getEdgeMap(), DataPlacement.nb_Service_HGW,
						DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, application);
				dataSizeVec.generateDataSizeFileInZone(zone, zoneDevises);

				// dataSizeVec =null;

				/* generate ConsProd matrix */
				System.out.println("Generating of ConsProdMap!");
				Log.writeInLogFile("DataPlacement","Generating of ConsProdMap!");
				ConsProdMatrix consProdMap = new ConsProdMatrix(application.getEdgeMap(), DataPlacement.nb_Service_HGW,DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, DataPlacement.nb_Service_DC);
				consProdMap.generateConsProdFileInZone(zone, zoneDevises);

				/* generate Free Capacity vector */
				System.out.println("Generating of Free Capacity for zone:"+ zone);
				Log.writeInLogFile("DataPlacement","Generating of Free Capacity for zone:" + zone);
				FreeCapacityVector freeCapacity = new FreeCapacityVector(DataPlacement.fogDevices, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC);
				freeCapacity.generateFreeCapacityFileInZone(zone, zoneDevises);
				//System.out.println("\n"+freeCapacity.toString());

				System.out.println("Generating of Data Actors for zone:"+ zone);
				Log.writeInLogFile("DataPlacement","Generating of Data Actors for zone:"+ zone);
				generateDataActorsFileInZone(zone, zoneDevises);

				long begin_t = Calendar.getInstance().getTimeInMillis();
				System.out.println("Making LP file...");
				Log.writeInLogFile("DataPlacement", "Making LP file...");
				MakeLPFileInZone mlpf = new MakeLPFileInZone(DataPlacement.nb_HGW, zone);
				long end_t = Calendar.getInstance().getTimeInMillis();
				long period_t = end_t - begin_t;
				org.fog.examples.Log.writeProblemFormulationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		zone:"+zone+"		Making Cplex File:"+String.valueOf(period_t));

				int dataHost, dataCons, dataProd;
				dataHost = zoneDevises.get("hgws").size()+ zoneDevises.get("lpops").size()+ zoneDevises.get("rpops").size();
				dataProd = zoneDevises.get("hgws").size()+ zoneDevises.get("lpops").size()+ zoneDevises.get("rpops").size();
				dataCons = zoneDevises.get("lpops").size()+ zoneDevises.get("rpops").size();

				CallCplex cc = new CallCplex(DataPlacement.nb_HGW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", dataProd, dataHost);

				begin_t = Calendar.getInstance().getTimeInMillis();
				cc.problemSolvingInZone(DataPlacement.nb_HGW, zone);
				end_t = Calendar.getInstance().getTimeInMillis();
				period_t = end_t - begin_t;
				// org.fog.examples.Log.writeLog(nb_HGW,"				Solving TimeDP:"+String.valueOf(period_t)+"	zone:"+zone);

				/* recuperation of data allocation */
				DataAllocation dataAllocation = new DataAllocation();
				dataAllocation.setDataAllocationMapInZone(DataPlacement.nb_HGW, zone,zoneDevises, application);
			}
			
			//DataAllocation.printDataAllocationMap(application);
			org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+
					DataPlacement.nb_DataCons_By_DataProd+"\n"+DataPlacement.storageMode+"\n"+DataAllocation.dataAllocationStats(application));
			
			org.fog.examples.Log.writeDataAllocationStatsExternZoneCons(DataPlacement.nb_HGW,"Zoning Storage "+DataPlacement.nb_zone
					+"\n Nb extern cons :"+DataPlacement.nb_externCons);

			System.out.println("----------------------");

			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			
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

	private void generateDataActorsFileInZone(int zone,Map<String, List<Integer>> zoneDevises) {
		int dataHost, dataCons, dataProd;
		dataHost = zoneDevises.get("hgws").size()
				+ zoneDevises.get("lpops").size()
				+ zoneDevises.get("rpops").size();
		dataProd = zoneDevises.get("hgws").size()
				+ zoneDevises.get("lpops").size()
				+ zoneDevises.get("rpops").size();
		dataCons = zoneDevises.get("hgws").size()
				+ zoneDevises.get("lpops").size()
				+ zoneDevises.get("rpops").size();

		File fichier = new File(DataPlacement.nb_HGW + "dataActorsZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		FileWriter fw;
		try {
			fw = new FileWriter(fichier);
			fw.write(dataHost + "\t");
			fw.write(dataProd + "\t");
			fw.write(dataCons + "\t");
			fw.write(DataPlacement.Basis_Exchange_Unit + "\t");
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
