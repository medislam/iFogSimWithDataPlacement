package org.StorageMode;

import java.util.Calendar;

import org.Results.SaveResults;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.DelayMatrix_Float;
import org.fog.application.Application;
import org.fog.entities.FogBroker;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.utils.TimeKeeper;

public class CloudStorage {
	
	public CloudStorage(){
	}

	public void sim() throws Exception {
		// TODO Auto-generated method stub
		long begin_t, end_t, period_t;
		System.out.println("----------------------------------------------------------");
		System.out.println(DataPlacement.CloudStorage);
		Log.writeInLogFile("DataPlacement","----------------------------------------------------------");
		Log.writeInLogFile("DataPlacement", DataPlacement.CloudStorage);

		CloudSim.init(DataPlacement.num_user, DataPlacement.calendar, DataPlacement.trace_flag);
		String appId = "Data_Placement"; // identifier of the application
		FogBroker broker = new FogBroker("broker");
		System.out.println("Creating of the Fog devices!");
		Log.writeInLogFile("DataPlacement", "Creating of the Fog devices!");
		DataPlacement.createFogDevices(broker.getId(), appId);

		System.out.println("Creating of Sensors and Actuators!");
		Log.writeInLogFile("DataPlacement", "Creating of Sensors and Actuators!");
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
		Log.writeInLogFile("DataPlacement","Basis Delay Matrix computing!");
		BasisDelayMatrix delayMatrix = new BasisDelayMatrix(DataPlacement.fogDevices,DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP,DataPlacement.nb_Service_DC, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC,application);
		
		/*
		 * generate basis delay matrix
		 */
		if (DataPlacement.nb_DataCons_By_DataProd == 1) {
			begin_t = Calendar.getInstance().getTimeInMillis();
			delayMatrix.getDelayMatrix(DataPlacement.fogDevices);

			end_t = Calendar.getInstance().getTimeInMillis();
			
			period_t = end_t - begin_t;
			
			org.fog.examples.Log.writeShortPathTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+DataPlacement.storageMode+"		short path time (floyd):"+String.valueOf(period_t));

			BasisDelayMatrix.saveBasisDelayMatrix();
			BasisDelayMatrix.savemFlowMatrix();
			BasisDelayMatrix.savemAdjacenceMatrix();

			GraphPartitionStorage.saveNbNodesAndNbArcs(DelayMatrix_Float.mTotalNodeNum, DelayMatrix_Float.mTotalArcNum);

		} else {
			BasisDelayMatrix.loadBasisDelayMatrix();
		}
		
		
		/*
		 * Connecting the application modules (vertices) in the
		 * application model (directed graph) with edges
		 */
		application.addAppEdgesToApplication();

		/*
		 * Defining the input-output relationships (represented by
		 * selectivity) of the application modules.
		 */
		application.addTupleMappingFraction();

		
		// loadBasisDelayMatrix(delayMatrix);

		/* saving the configurations */
		System.out.println("Saving infrastructure ...");
		Log.writeInLogFile("DataPlacement", "Saving infrastructure ...");
		Application.saveApplicationEdges();
		application.saveTupleMappingFraction();
		System.out.println("End of saving");
		Log.writeInLogFile("DataPlacement", "End of saving");
		
		
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

		SaveResults.saveLatencyTimes(DataPlacement.nb_DataCons_By_DataProd, DataPlacement.storageMode, -1, LatencyStats.getOverall_read_Latency(), LatencyStats.getOverall_write_Latency(), LatencyStats.getOverall_Latency());

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
