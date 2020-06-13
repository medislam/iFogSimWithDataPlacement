package org.fog.Parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.cplex.CallCplex;
import org.fog.cplex.DataAllocation;
import org.fog.examples.DataPlacement;
import org.fog.gui.lpFileConstuction.BasisDelayMatrix;
import org.fog.gui.lpFileConstuction.ConsProdMatrix;
import org.fog.gui.lpFileConstuction.DataSizeVector;
import org.fog.gui.lpFileConstuction.FreeCapacityVector;
import org.fog.gui.lpFileConstuction.MakeLPFileInZone;

public class ThreadInZone extends Thread{
	private Application application;
	private BasisDelayMatrix delayMatrix;
	private int zone;
	private DataAllocation dataAllocation;
	private Map<String, List<Integer>> zoneDevises;
	private CloudSimParallel cloudsimparallel;
	
	

	public ThreadInZone(Application application, BasisDelayMatrix delayMatrix, int zone, DataAllocation dataAllocation, Map<String, List<Integer>> zoneDevises, CloudSimParallel cloudsimparallel) {
		// TODO Auto-generated constructor stub
		this.application=application;
		this.delayMatrix=delayMatrix;
		this.zone=zone;
		this.dataAllocation=dataAllocation;
		this.zoneDevises = zoneDevises;
		this.cloudsimparallel = cloudsimparallel;
		//printDevicesInZone();
	}
	
	public void run() {
				
		try {
			
			/* generate write and read basis delay files */
			System.out.println("\n******************Zone " + zone+ " ******************");
			Log.writeInLogFile("DataPlacement","\n******************Zone " + zone+ " ******************");
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
			System.out.println("Making LP file Thread:"+zone);
			Log.writeInLogFile("DataPlacement", "Making LP file...");
			
			//printDevicesInZone();
			
			
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
			
			dataAllocation.setDataAllocationMapInZone(DataPlacement.nb_HGW, zone,zoneDevises, application);
			
			cloudsimparallel.startSimulation(cloudsimparallel);
			cloudsimparallel.stopSimulation(cloudsimparallel);
		
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("il y a un pb dans le run du thread zone :"+zone);
		}

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
	
	private void printDevicesInZone() {
		System.out.println("Print Devices in Zone : "+zone);
		for(String key : zoneDevises.keySet()) {
			System.out.print(key+" : ");
			for(int devId : zoneDevises.get(key)) {
				System.out.print(devId+", ");
			}
			System.out.println();
		}
		
	}
}
