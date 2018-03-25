package org.StorageMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import org.Results.SaveResults;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.entities.FogBroker;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.ConsProdMatrix;
import org.fog.gui.lpFileConstuction.DataSizeVector;
import org.fog.gui.lpFileConstuction.FreeCapacityVector;
import org.fog.gui.lpFileConstuction.LatencyStats;
import org.fog.gui.lpFileConstuction.MakeLPFile;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.utils.TimeKeeper;

public class FogStorage {
	
	public FogStorage(){
		
	}

	public void sim() throws Exception{
		System.out.println("----------------------------------------------------------");
		System.out.println(DataPlacement.FogStorage);
		Log.writeInLogFile("DataPlacement","----------------------------------------------------------");
		Log.writeInLogFile("DataPlacement", DataPlacement.FogStorage);

		org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW,"----------------------------------------------------------------------");
		org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW, "consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+ DataPlacement.FogStorage);
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
		Log.writeInLogFile("DataPlacement","Basis Delay Matrix computing!");
		BasisDelayMatrix delayMatrix = new BasisDelayMatrix(DataPlacement.fogDevices,DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP,
				DataPlacement.nb_Service_DC, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC, application);
		
//		delayMatrix.getDelayMatrix(fogDevices);
//		saveBasisDelayMatrix(delayMatrix);
//		savemFlowMatrix(delayMatrix);
//		savemAdjacenceMatrix(delayMatrix);
//		saveNbNodesAndNbArcs(DelayMatrix_Float.mTotalNodeNum, DelayMatrix_Float.mTotalArcNum);

	
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

		
		BasisDelayMatrix.loadBasisDelayMatrix();

		/* saving the configurations */
		System.out.println("Saving infrastructure ...");
		Log.writeInLogFile("DataPlacement", "Saving infrastructure ...");
		Application.saveApplicationEdges();
		application.saveTupleMappingFraction();
		System.out.println("End of saving");
		Log.writeInLogFile("DataPlacement", "End of saving");

		
		
		System.out.println("Loading ....");
		Log.writeInLogFile("DataPlacement", "Loading ....");
		//loadApplicationEdges(application);
		//loadTupleMappingFraction(application);
		//loadBasisDelayMatrix(delayMatrix);
		System.out.println("Loaded");
		Log.writeInLogFile("DataPlacement", "Loaded");
		

		/* generate write and read basis delay files */
		delayMatrix.generateBasisWriteDelayFile(DataPlacement.nb_HGW);
		delayMatrix.generateBasisReadDelayFile(DataPlacement.nb_HGW);

		/* generate Data Size vector */
		System.out.println("Generating of Data Size!");
		Log.writeInLogFile("DataPlacement", "Generating of Data Size!");
		DataSizeVector dataSizeVec = new DataSizeVector(application.getEdgeMap(), DataPlacement.nb_Service_HGW,DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, application);
		dataSizeVec.generateDataSizeFile();

		/* generate ConsProd matrix */
		System.out.println("Generating of ConsProdMap!");
		Log.writeInLogFile("DataPlacement","Generating of ConsProdMap!");
		ConsProdMatrix consProdMap = new ConsProdMatrix(application.getEdgeMap(),DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, DataPlacement.nb_Service_DC);
		consProdMap.generateConsProdFile();

		/* generate Free Capacity vector */
		System.out.println("Generating of Free Capacity!");
		Log.writeInLogFile("DataPlacement","Generating of Free Capacity!");
		FreeCapacityVector freeCapacity = new FreeCapacityVector(DataPlacement.fogDevices, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC);
		freeCapacity.generateFreeCapacityFile();
		// //System.out.println("\n"+freeCapacity.toString());

		System.out.println("Generating of Data Actors!");
		Log.writeInLogFile("DataPlacement","Generating of Data Actors!");
		generateDataActorsFile();

		long begin_t = Calendar.getInstance().getTimeInMillis();
		System.out.println("Making LP file...");
		Log.writeInLogFile("DataPlacement", "Making LP file...");
		MakeLPFile mlpf = new MakeLPFile(DataPlacement.nb_HGW);
		long end_t = Calendar.getInstance().getTimeInMillis();
		long period_t = end_t - begin_t;
		org.fog.examples.Log.writeProblemFormulationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+DataPlacement.storageMode+"		Making Cplex File:"+String.valueOf(period_t));

		int dataHost, dataCons, dataProd;
		dataHost = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
		dataProd = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP;
		dataCons = DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP + DataPlacement.nb_Service_DC;

		begin_t = Calendar.getInstance().getTimeInMillis();
		CallCplex cc = new CallCplex(DataPlacement.nb_HGW + "cplex_"+DataPlacement.nb_DataCons_By_DataProd+".lp", dataProd,dataHost);
		cc.problemSolving(DataPlacement.nb_HGW);
		end_t = Calendar.getInstance().getTimeInMillis();
		period_t = end_t - begin_t;
		// org.fog.examples.Log.writeLog(nb_HGW,"Solving TimeDP:"+String.valueOf(period_t));

		/* recuperation of data allocation */
		DataAllocation dataAllocation = new DataAllocation();
		dataAllocation.setDataPlacementMap(DataPlacement.nb_HGW, application);
		org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd+"\n"+
				DataPlacement.storageMode+"\n"+ DataAllocation.dataAllocationStats(application));

		// DataAllocation.printDataAllocationMap(application);
		
		
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

		SaveResults.saveLatencyTimes(DataPlacement.nb_DataCons_By_DataProd, DataPlacement.storageMode, -1,
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
	
	private void generateDataActorsFile() {
		int dataHost, dataCons, dataProd;
		dataHost = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
		dataProd = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP;
		dataCons = DataPlacement.nb_Service_HGW + DataPlacement.nb_Service_LPOP + DataPlacement.nb_Service_RPOP + DataPlacement.nb_Service_DC;

		File fichier = new File(DataPlacement.nb_HGW + "dataActors_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
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
