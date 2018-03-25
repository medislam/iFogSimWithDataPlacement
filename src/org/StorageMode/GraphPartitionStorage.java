package org.StorageMode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.Results.SaveResults;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.DelayMatrix_Float;
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
import org.fog.gui.lpFileConstuction.MakeLPFileInPartition;
import org.fog.jni.GraphPartitioning;
import org.fog.jni.GraphPonderation;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.utils.TimeKeeper;

public class GraphPartitionStorage {
	
	public GraphPartitionStorage(){
		
	}

	public void sim() throws Exception{
		System.out.println("----------------------------------------------------------");
		System.out.println(DataPlacement.GraphPartitionStorage);
		Log.writeInLogFile("DataPlacement","----------------------------------------------------------");
		Log.writeInLogFile("DataPlacement", DataPlacement.GraphPartitionStorage);

		for(int nb_part : DataPlacement.nb_partitions_list){
			DataPlacement.nb_partitions = nb_part;
			
			org.fog.examples.Log.writeSolvingTime(DataPlacement.nb_HGW, "consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		storage mode:"+DataPlacement.GraphPartitionStorage+ "		nb_partitions:"+ DataPlacement.nb_partitions);
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
			BasisDelayMatrix delayMatrix = new BasisDelayMatrix(DataPlacement.fogDevices,DataPlacement.nb_Service_HGW, DataPlacement.nb_Service_LPOP,DataPlacement. nb_Service_RPOP,
					DataPlacement.nb_Service_DC, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC, application);

			System.out.println("loading basis matrixes...");
			
			BasisDelayMatrix.loadBasisDelayMatrix();
			BasisDelayMatrix.loadmAdjacenceMatrix();
			BasisDelayMatrix.loadmFlowMatrix();
			loadNbNodesAndNbArcs();
			System.out.println("Loaded");
			
			System.out.println("Loading Infrastructure....");
			Log.writeInLogFile("DataPlacement", "Loading ....");
			application.loadApplicationEdges();
			application.loadTupleMappingFraction();

			
			System.out.println("Loaded");
			Log.writeInLogFile("DataPlacement", "Loaded");

			/*
			 * Graph Ponderation
			 */
			System.out.println("-------------------- Graph ponderation ------------------");
			GraphPonderation gp = new GraphPonderation(DataPlacement.nb_HGW, DataPlacement.nb_LPOP,DataPlacement.nb_RPOP, DataPlacement.nb_DC, application,moduleMapping.getModuleToHostMap());
			Log.writeInLogFile("DataPlacement","-------------------- Graph ponderation ------------------");

			int LPLength = DelayMatrix_Float.mTotalNodeNum - DataPlacement.nb_HGW + 1;
			int LSLength = DelayMatrix_Float.mTotalArcNum;
			double start, end;

			long begin_t = Calendar.getInstance().getTimeInMillis();
			gp.arcPonderation(LPLength, LSLength,delayMatrix.mAdjacenceMatrix,delayMatrix.mFlowMatrix);
			long end_t = Calendar.getInstance().getTimeInMillis();
			
			long period_t = end_t -begin_t;
			
			org.fog.examples.Log.writeGraphPonderationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		ArcPonderationTime:"+String.valueOf(period_t));
			
			
			System.out.println("Arc ponderation table length:"+ gp.getArcPonderation().length);
			System.out.println("Arc ponderation table:");
			// Log.writeInLogFile("DataPlacement",
			// "Arc ponderation table length:"+gp.getArcPonderation().length);
			Log.writeInLogFile("DataPlacement", "Arc ponderation table");
			//gp.printTablePonderation(gp.getArcPonderation());

			begin_t = Calendar.getInstance().getTimeInMillis();
			gp.nodePonderation(DataPlacement.nb_HGW + DataPlacement.nb_RPOP + DataPlacement.nb_LPOP + DataPlacement.nb_DC,application);
			end_t = Calendar.getInstance().getTimeInMillis();
			
			period_t = end_t -begin_t;
			
			org.fog.examples.Log.writeGraphPonderationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		NodePonderationTime:"+String.valueOf(period_t));

			System.out.println("-------------------- Graph partitioning --------------------");
			Log.writeInLogFile("DataPlacement","-------------------- Graph partitioning --------------------");

			GraphPartitioning graphPart = new GraphPartitioning(DataPlacement.nb_HGW,DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC, LPLength, LSLength, delayMatrix.mAdjacenceMatrix);

			System.out.println("Metis input file constructing...");
			Log.writeInLogFile("DataPlacement","Metis input file constructing...");
			graphPart.constructInputFileGaphPartitioning(gp.getNodePonderation(), gp.getArcPonderation());

			System.out.println("Metis commande...");
			Log.writeInLogFile("DataPlacement", "Metis commande...");
			int ncuts = 1;
			
			System.out.println("nb_paritions = "+DataPlacement.nb_partitions);
			begin_t = Calendar.getInstance().getTimeInMillis();
			saveMetisLog(graphPart.partitioningCommande(DataPlacement.nb_HGW + DataPlacement.nb_RPOP+ DataPlacement.nb_LPOP + DataPlacement.nb_DC, DataPlacement.nb_partitions, ncuts));
			end_t = Calendar.getInstance().getTimeInMillis();
			
			period_t = end_t -begin_t;
			org.fog.examples.Log.writeGraphPonderationTime(DataPlacement.nb_HGW,"consProd:"+DataPlacement.nb_DataCons_By_DataProd + "		nb_partitions:"+ DataPlacement.nb_partitions+"		partitionTime:"+String.valueOf(period_t));
			
			
			graphPart.setPartitionDevices(DataPlacement.nb_partitions);
			graphPart.writeInformation(DataPlacement.nb_partitions);

			/* create data allocation map */
			DataAllocation dataAllocation = new DataAllocation();
			
			DataPlacement.nb_externCons = 0;

			for(int partition = 0; partition < DataPlacement.nb_partitions; partition++){
				
				if(graphPart.checkNullPatrtition(partition)){
					continue;
					
				}else if(graphPart.checkNullDataConsInPatrtition(partition)){	
					/* recuperation of data allocation */
					dataAllocation.setDataAllocationMapInPartitionNullDataCons(partition, graphPart);
					// DataAllocation.printDataAllocationMap(application);
				}else{		
				
					List<Integer> HGW_list = graphPart.getHGWInPartition(partition);
					List<Integer> LPOP_list = graphPart.getLPOPInPartition(partition);
					List<Integer> RPOP_list = graphPart.getRPOPInPartition(partition);
					List<Integer> DC_list = graphPart.getDCInPartition(partition);

					int dataHost, dataCons, dataProd;
					dataHost = HGW_list.size() + LPOP_list.size()+ RPOP_list.size() + DC_list.size();
					dataProd = HGW_list.size() + LPOP_list.size()+ RPOP_list.size();
					dataCons = LPOP_list.size() + RPOP_list.size()+ DC_list.size();


					/* generate write and read basis delay files */
					delayMatrix.generateBasisWriteDelayFileForPartition(partition, graphPart.getPartitionDevices(),graphPart);
					delayMatrix.generateBasisReadDelayFileForPartition(partition, graphPart.getPartitionDevices(),graphPart);

					/* generate Data Size vector */
					System.out.println("Generating of Data Size for parition:"+ partition);
					Log.writeInLogFile("DataPlacement","Generating of Data Size for parition:" + partition);
					DataSizeVector dataSizeVec = new DataSizeVector(application.getEdgeMap(), DataPlacement.nb_Service_HGW,DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, application);
					dataSizeVec.generateDataSizeFileInpartiton(partition,graphPart, DataPlacement.nb_DC);

					// dataSizeVec =null;

					/* generate ConsProd matrix */
					System.out.println("Generating of ConsProdMap for partition:"+ partition);
					Log.writeInLogFile("DataPlacement","Generating of ConsProdMap for partition:"+ partition);
					ConsProdMatrix consProdMap = new ConsProdMatrix(application.getEdgeMap(), DataPlacement.nb_Service_HGW,DataPlacement.nb_Service_LPOP, DataPlacement.nb_Service_RPOP, DataPlacement.nb_Service_DC);
					// consProdMap.printConsProdMap();
					consProdMap.generateConsProdFileInPartition(partition,graphPart);

					/* generate Free Capacity vector */
					System.out.println("Generating of Free Capacity for partition:"+ partition);
					Log.writeInLogFile("DataPlacement","Generating of Free Capacity for partition:"+ partition);
					FreeCapacityVector freeCapacity = new FreeCapacityVector(DataPlacement.fogDevices, DataPlacement.nb_HGW, DataPlacement.nb_LPOP, DataPlacement.nb_RPOP, DataPlacement.nb_DC);
					
					freeCapacity.generateFreeCapacityFileInPartition(partition,graphPart);


					System.out.println("Generating of Data Actors for partition:"+ partition);
					Log.writeInLogFile("DataPlacement","Generating of Data Actors for partition:"+ partition);
					generateDataActorsFileInPartition(partition, graphPart);

					begin_t = Calendar.getInstance().getTimeInMillis();
					System.out.println("Making LP file...");
					Log.writeInLogFile("DataPlacement", "Making LP file...");
					int rien = 1;
					MakeLPFileInPartition mlpf = new MakeLPFileInPartition(DataPlacement.nb_HGW, partition);
					end_t = Calendar.getInstance().getTimeInMillis();
					period_t = end_t - begin_t;
					org.fog.examples.Log.writeProblemFormulationTime(DataPlacement.nb_HGW,"consProd:"+ DataPlacement.nb_DataCons_By_DataProd + "		partition:"+partition+"		Making Cplex File:"+String.valueOf(period_t));

					CallCplex cc = new CallCplex(DataPlacement.nb_HGW + "cplexPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", dataProd, dataHost);

					begin_t = Calendar.getInstance().getTimeInMillis();
					cc.problemSolvingInPartition(DataPlacement.nb_HGW, partition);
					end_t = Calendar.getInstance().getTimeInMillis();
					period_t = end_t - begin_t;
					// org.fog.examples.Log.writeLog(nb_HGW,"				Solving TimeDP:"+String.valueOf(period_t)+"	partition:"+partition);
					/* recuperation of data allocation */
					dataAllocation.setDataAllocationMapInPartition(DataPlacement.nb_HGW,partition, graphPart, application);
					
				}
				
			}
			//DataAllocation.printDataAllocationMap(application);
			org.fog.examples.Log.writeDataAllocationStats(DataPlacement.nb_HGW,"------------------------------------------\n"+ DataPlacement.nb_DataCons_By_DataProd+"\n"+
					DataPlacement.storageMode+"\n"+ DataAllocation.dataAllocationStats(application));
			
			org.fog.examples.Log.writeDataAllocationStatsExternZoneCons(DataPlacement.nb_HGW,"Graph Partition Storage "+DataPlacement.nb_partitions
					+"\n Nb extern cons :"+DataPlacement.nb_externCons);

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

			SaveResults.saveLatencyTimes(DataPlacement.nb_DataCons_By_DataProd, DataPlacement.storageMode,DataPlacement.nb_partitions, 
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
	
	public static void saveNbNodesAndNbArcs(int mTotalNodeNum, int mTotalArcNum) throws IOException {
		// System.out.println("Saving nb nodes and nb arcs");
		FileWriter fichier;

		fichier = new FileWriter("latencies/" + DataPlacement.nb_HGW + "nb_nodes_nb_arcs.txt");

		try {
			BufferedWriter fw = new BufferedWriter(fichier);

			fw.write(mTotalNodeNum + "\n");

			fw.write(mTotalArcNum + "\n");

			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadNbNodesAndNbArcs() throws IOException, InterruptedException {
		FileReader fichier = new FileReader("latencies/" + DataPlacement.nb_HGW+ "nb_nodes_nb_arcs.txt");
		BufferedReader in = null;
		try {
			
			
			in = new BufferedReader(fichier);

			DelayMatrix_Float.mTotalNodeNum = Integer.valueOf(in.readLine());
			DelayMatrix_Float.mTotalArcNum = Integer.valueOf(in.readLine());

			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveMetisLog(String log) throws IOException {
		// TODO Auto-generated method stub
		FileWriter fichier = new FileWriter(
				"Stats/" + DataPlacement.nb_HGW +"_"+DataPlacement.nb_DataCons_By_DataProd+ "Metis_log.txt", true);
		try {
			BufferedWriter fw = new BufferedWriter(fichier);

			fw.write("-------------------------------------------------------------\n");
			fw.write("nb_DataCons_By_DataProd :" + DataPlacement.nb_DataCons_By_DataProd
					+ "\n");
			fw.write(log + "\n");

			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private static void generateDataActorsFileInPartition(int partition, GraphPartitioning gp) {
		// TODO Auto-generated method stub

		List<Integer> HGW_list = gp.getHGWInPartition(partition);
		List<Integer> LPOP_list = gp.getLPOPInPartition(partition);
		List<Integer> RPOP_list = gp.getRPOPInPartition(partition);
		List<Integer> DC_list = gp.getDCInPartition(partition);

		int dataHost, dataCons, dataProd;
		dataHost = HGW_list.size() + LPOP_list.size() + RPOP_list.size()+ DC_list.size();
		dataProd = HGW_list.size() + LPOP_list.size() + RPOP_list.size();
		dataCons = HGW_list.size() + LPOP_list.size() + RPOP_list.size() + DC_list.size();

		File fichier = new File(DataPlacement.nb_HGW + "dataActorsPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
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
