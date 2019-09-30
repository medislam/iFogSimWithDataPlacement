package org.fog.gui.lpFileConstuction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.network.*;
import org.fog.application.Application;
import org.fog.entities.FogDevice; 
import org.fog.entities.Actuator;
import org.fog.entities.Sensor;
import org.fog.examples.DataPlacement;
import org.fog.jni.GraphPartitioning;
import org.fog.placement.ModuleMapping;

public class BasisDelayMatrix {
	private TopologicalGraph graph = new TopologicalGraph();
	private static final boolean directed = false;
	
	private Application application;
	
	private int nb_ServiceHGW;
	private int nb_ServiceLPOP;
	private int nb_ServiceRPOP;
	private int nb_ServiceDC;
	
	private int nb_HGW;
	private int nb_LPOP;
	private int nb_RPOP;
	private int nb_DC;
	
	public static float[][] mDelayMatrix;
	public static int[][] mFlowMatrix;
	public static float[][] mAdjacenceMatrix;
	
	public BasisDelayMatrix(List<FogDevice> fogDevices, int nb_ServiceHGW, int nb_ServiceLPOP, int nb_ServiceRPOP, int nb_ServiceDC, 
												int nb_HGW, int nb_LPOP, int nb_RPOP, int nb_DC, Application application){
		this.nb_ServiceHGW=nb_ServiceHGW;
		this.nb_ServiceLPOP=nb_ServiceLPOP;
		this.nb_ServiceRPOP=nb_ServiceRPOP;
		this.nb_ServiceDC=nb_ServiceDC;
		
		this.nb_HGW= DataPlacement.nb_HGW;
		this.nb_LPOP= DataPlacement.nb_LPOP;
		this.nb_RPOP= DataPlacement.nb_RPOP;
		this.nb_DC= DataPlacement.nb_DC;
		this.application=application;
	}
	
	public void getDelayMatrix(List<FogDevice> fogDevices){
		TopologicalNode node =null;
		TopologicalLink link =null;
		System.out.println("Graph construction...");
		
		for(FogDevice fogDevice : fogDevices){
			
			node =  new TopologicalNode(fogDevice.getId()-3, fogDevice.getName(),0,0);
			graph.addNode(node);
			
			/* ADD cheldren nodes */
			if(fogDevice.getChildrenIds() != null){
				Map<Integer,Float> childMap = fogDevice.getChildToLatencyMap();
				for(Integer key : childMap.keySet()){
					link = new TopologicalLink(fogDevice.getId()-3,(int) key-3,childMap.get(key),30000);
					graph.addLink(link);
				}
			}
			
//			/* ADD left Link to Graph */
			// the left is the right of the other => to be not added
//			if(fogDevice.getLeftId()!=-1){
//				link = new TopologicalLink(fogDevice.getId()-3,fogDevice.getLeftId()-3,fogDevice.getLeftLatency(),30000);
//				graph.addLink(link);
//			}
//			
			/* ADD Right Link to Graph */
			if(fogDevice.getRightId()!=-1){
				link = new TopologicalLink(fogDevice.getId()-3,fogDevice.getRightId()-3,fogDevice.getRightLatency(),30000);
				graph.addLink(link);
			}
		}
		
		
		//System.out.println(graph.toString());
		System.out.println("Latencies computation...");
		DelayMatrix_Float delayMatrix = new DelayMatrix_Float(graph, directed);
		mDelayMatrix = DelayMatrix_Float.mDelayMatrix;
		mAdjacenceMatrix =  DelayMatrix_Float.mAdjacenceMatrix;
		mFlowMatrix = DelayMatrix_Float.mFlowMatrix;
		
	}
	
	public void generateBasisWriteDelayFile(int nb_GW) throws IOException {
		System.out.println("Generating Basis Write Delay file...");
		
		FileWriter fichier = new FileWriter(nb_GW+"writeDelay_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			/* nb data hosts */
			int maxRow = nb_HGW+nb_LPOP+nb_RPOP+nb_DC;
			
			/* nb data rods */
			int maxCol = nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP;
			
			for(int row=0;row<(maxRow);row++){
				for(int col=0;col<(maxCol);col++){
					fw.write(String.valueOf(getWriteLatencyOf(row,col))+"\t");
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
	
	public void generateBasisWriteDelayFileInZone(int nb_GW, int zone, Map<String, List<Integer>> zoneDevises) throws IOException {
		
		System.out.println("Generating Basis Write Delay file for zone:"+zone);
		String file = nb_GW+"writeDelayZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		
		FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			
//			System.out.println("\nnb_ZHGW:"+zoneDevises.get("hgws").size());
//			System.out.println("nb_ZLPOP:"+zoneDevises.get("lpops").size());
//			System.out.println("nb_ZRPOP:"+zoneDevises.get("rpops").size()+"\n");
			
			/* data hosts */
			int maxRow = zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()+zoneDevises.get("rpops").size();
			/* data prods */
			int maxCol = zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()+zoneDevises.get("rpops").size();
			
			int row2;
			int col2;
			for(int row=0;row<(maxRow);row++){
				if(row>(zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()-1)){
					//RPOP
					row2 = row + nb_HGW+ nb_LPOP + zoneDevises.get("rpops").size()*zone - zoneDevises.get("hgws").size() -zoneDevises.get("lpops").size();
					//System.out.println("ROW->RPOP:"+row2);
					
				}else if(row >(zoneDevises.get("hgws").size()-1)){
					//LPOP
					row2 = row + nb_HGW + zoneDevises.get("lpops").size()*zone - zoneDevises.get("hgws").size();
					//System.out.println("ROW->LPOP:"+row2);
					
				}else{
					//HGW
					row2 = row + zoneDevises.get("hgws").size()*zone;
					//System.out.println("ROW->HGW:"+row2);
					
				}
				
				for(int col=0;col<(maxCol);col++){
					//System.out.println("col="+col);
					if(col>(zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()-1)){
						//RPOP
						col2 = col + nb_HGW + nb_LPOP + zoneDevises.get("rpops").size()*zone - zoneDevises.get("hgws").size() -zoneDevises.get("lpops").size();
						//System.out.println("COL->RPOP:"+col2);
						
					}else if(col > zoneDevises.get("hgws").size()-1){
						//LPOP
						//System.out.println("nb_hgw:"+nb_HGW+"    zoneDevises.get(lpops).size():"+zoneDevises.get("lpops").size()+"    zone:"+zone);
						col2 = col + nb_HGW + zoneDevises.get("lpops").size()*zone - zoneDevises.get("hgws").size();
						//System.out.println("COL->LPOP:"+col2);
						
					}else{
						//HGW
						col2 = col + zoneDevises.get("hgws").size()*zone;
						//System.out.println("COL->HGW:"+col2);
					}
					
					fw.write(String.valueOf(getWriteLatencyOf(row2,col2))+"\t");
					
					col2 = -1;
				}
				row2 = -1;
				fw.write("\n");
			}
			
			fw.close();			
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	private float getWriteLatencyOf(int row, int col) {
		
		int rowIndex = -1;
		int colIndex = -1;	
		
		if(row<nb_HGW){
			//HGW	dh
			rowIndex = row+nb_DC+nb_RPOP+nb_LPOP +3;
			
		}else if(row<nb_LPOP+nb_HGW){
			//LPOP	dh
			rowIndex = row+nb_DC+nb_RPOP-nb_HGW +3;

		}else if(row<nb_RPOP+nb_LPOP+nb_HGW){
			//RPOP	dh
			rowIndex = row+nb_DC-nb_HGW - nb_LPOP +3;	
			
		}else if(row<nb_DC+nb_RPOP+nb_LPOP+nb_HGW){
			//DC	dh
			rowIndex = row-nb_HGW - nb_LPOP- nb_RPOP +3;
		}
		
		if(col<nb_ServiceHGW){
			//this is a hgw DataProd

			int ind = col;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceHGW"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();
			
			
		}else if(col<nb_ServiceLPOP+nb_ServiceHGW){
			//this is a LPOP DataProd

			int ind = col - nb_ServiceHGW; 
			String deviceName = ModuleMapping.getDeviceHostModule("serviceLPOP"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();

		}else if(col<nb_ServiceRPOP+nb_ServiceLPOP+nb_ServiceHGW){
			//this is a RPOP DataProd

			int ind = col - nb_ServiceHGW - nb_ServiceLPOP;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceRPOP"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();	
		}
		
		
		return getFatestLink(rowIndex,colIndex);
	}
	
	public static float getFatestLink(int src, int dest){
		// -3 because fogIds start by 3 --> 0
		return mDelayMatrix[src-3][dest-3];
	}

	public void generateBasisReadDelayFile(int nb_GW) throws IOException {
		System.out.println("Generating Bais Read Latency");

		FileWriter fichier = new FileWriter(nb_GW+"readDelay_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			for(int row=0;row<(nb_HGW+nb_LPOP+nb_RPOP+nb_DC);row++){
				for(int col=0;col<(nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP+nb_ServiceDC);col++){
					fw.write(String.valueOf(getReadLatencyOf(row,col))+"\t");
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

	public void generateBasisReadDelayFileInZone(int nb_GW,int zone, Map<String, List<Integer>> zoneDevises) throws IOException {
		System.out.println("Generating Basis Read Delay file for zone:"+zone);
		String file = nb_GW+"readDelayZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		
		FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			
//			System.out.println("\nnb_ZHGW:"+zoneDevises.get("hgws").size());
//			System.out.println("nb_ZLPOP:"+zoneDevises.get("lpops").size());
//			System.out.println("nb_ZRPOP:"+zoneDevises.get("rpops").size()+"\n");
//			
			/* DataProd */
			int maxRow = zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()+zoneDevises.get("rpops").size();
			
			/* DataCons */
			int maxCol = zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()+zoneDevises.get("rpops").size();
			int row2;
			int col2;
			for(int row=0;row<(maxRow);row++){
				if(row>(zoneDevises.get("hgws").size()+zoneDevises.get("lpops").size()-1)){
					//RPOP
					row2 = row + nb_HGW + nb_LPOP + zoneDevises.get("rpops").size()*zone - zoneDevises.get("hgws").size() -zoneDevises.get("lpops").size();
					//System.out.println("ROW->RPOP:"+row2);
					
				}else if(row >(zoneDevises.get("hgws").size()-1)){
					//LPOP
					row2 = row + nb_HGW + zoneDevises.get("lpops").size()*zone - zoneDevises.get("hgws").size();
					//System.out.println("ROW->LPOP:"+row2);
					
				}else{
					//HGW
					row2 = row + zoneDevises.get("hgws").size()*zone;
					//System.out.println("ROW->HGW:"+row2);
					
				}
				
				for(int col=0;col<(maxCol);col++){
					//System.out.println("col="+col);
					col2 = -1;
					if(col>(zoneDevises.get("hgws").size() + zoneDevises.get("lpops").size()-1)){
						//RPOP
						col2 = col + nb_HGW + nb_LPOP + zoneDevises.get("rpops").size()*zone - zoneDevises.get("hgws").size() - zoneDevises.get("lpops").size();
						//System.out.println("COL->RPOP:"+col2);
						
					}else if(col>(zoneDevises.get("hgws").size()-1)){
						//LPOP
						//System.out.println("nb_hgw:"+nb_HGW+"    zoneDevises.get(lpops).size():"+zoneDevises.get("lpops").size()+"    zone:"+zone);
						col2 = col + nb_HGW + zoneDevises.get("lpops").size()*zone - zoneDevises.get("hgws").size();
						//System.out.println("COL->LPOP:"+col2);
						
					}else if(col<(zoneDevises.get("hgws").size())){
						//HGW
						//System.out.println("nb_hgw:"+nb_HGW+"    zoneDevises.get(lpops).size():"+zoneDevises.get("lpops").size()+"    zone:"+zone);
						col2 = col + zoneDevises.get("hgws").size()*zone;
						//System.out.println("COL->LPOP:"+col2);
						
					}
					
					fw.write(String.valueOf(getReadLatencyOf(row2,col2))+"\t");
		
				}
				row2 = -1;
				fw.write("\n");
			}

		fw.close();
	}catch (FileNotFoundException e){
		e.printStackTrace();
	}catch (IOException e){
		e.printStackTrace();
	}
	
}
	
	private float getReadLatencyOf(int row, int col) {
		int rowIndex = -1;
		int colIndex = -1;	
		
		if(row<nb_HGW){
			//HGW	dh
			rowIndex = row+nb_DC+nb_RPOP+nb_LPOP +3;
			
		}else if(row<nb_LPOP+nb_HGW){
			//LPOP	dh
			rowIndex = row+nb_DC+nb_RPOP-nb_HGW +3;
			
		}else if(row<nb_RPOP+nb_LPOP+nb_HGW){
			//RPOP	dh
			rowIndex = row+nb_DC-nb_HGW - nb_LPOP +3;	
			
		}else if(row<nb_DC+nb_RPOP+nb_LPOP+nb_HGW){
			//DC	dh
			rowIndex = row-nb_HGW - nb_LPOP- nb_RPOP +3;
		}
		
		if(col<nb_ServiceHGW){
			//this is a HGW DataCons
			int ind = col;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceHGW"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();

		}else if(col<nb_ServiceHGW+nb_ServiceLPOP){
			//this is a LPOP DataCons
			int ind = col - nb_ServiceHGW;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceLPOP"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();
			
		}else if(col<nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP){
			//this is a RPOP DataCons
			int ind = col - nb_ServiceHGW - nb_ServiceLPOP;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceRPOP"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();
			
		}else if(col<nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP+nb_ServiceDC){
			//this is a DC DataCons
			int ind = col - nb_ServiceHGW - nb_ServiceLPOP - nb_ServiceRPOP;
			String deviceName = ModuleMapping.getDeviceHostModule("serviceDC"+ind);
			colIndex = application.getFogDeviceByName(deviceName).getId();
			
		}
		
		return getFatestLink(rowIndex,colIndex);
	}
	
	public void generateBasisWriteDelayFileForPartition(int partition, Map<Integer, List<Integer>> partitionDevices, GraphPartitioning gp) throws IOException {
		// TODO Auto-generated method stub
			
		System.out.println("\nGenerating Basis Write Delay file for partition:"+partition);
		
		List<Integer> HGW_list = gp.getHGWInPartition(partition);
		List<Integer> LPOP_list = gp.getLPOPInPartition(partition);
		List<Integer> RPOP_list = gp.getRPOPInPartition(partition);
		List<Integer> DC_list = gp.getDCInPartition(partition);
		
//		System.out.println("HGW_List:"+HGW_list);
//		System.out.println("LPOPList:"+LPOP_list);
//		System.out.println("RPOP_List:"+RPOP_list);
//		System.out.println("DC_List:"+DC_list);
		
		String file = nb_HGW+"writeDelayPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		
		FileWriter fichier = new FileWriter(file);
		
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			
			/* data hosts */
			List<Integer> DataHsotList = new ArrayList<>();
			
			DataHsotList.addAll(HGW_list);
			DataHsotList.addAll(LPOP_list);
			DataHsotList.addAll(RPOP_list);
			DataHsotList.addAll(DC_list);
			
			/* data prods */
			List<Integer> DataProdList = new ArrayList<>();
			
			DataProdList.addAll(HGW_list);
			DataProdList.addAll(LPOP_list);
			DataProdList.addAll(RPOP_list);

			for(int datahost : DataHsotList){
				for(int dataprod:DataProdList){
					fw.write(String.valueOf(getFatestLink(datahost+3, dataprod+3))+"\t");
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

	public void generateBasisReadDelayFileForPartition(int partition, Map<Integer, List<Integer>> partitionDevices, GraphPartitioning gp) throws IOException {
		// TODO Auto-generated method stub
			
		System.out.println("Generating Basis Read Delay file for partition:"+partition);
		
		List<Integer> HGW_list = gp.getHGWInPartition(partition);
		List<Integer> LPOP_list = gp.getLPOPInPartition(partition);
		List<Integer> RPOP_list = gp.getRPOPInPartition(partition);
		List<Integer> DC_list = gp.getDCInPartition(partition);
		
//		System.out.println("HGW_List:"+HGW_list);
//		System.out.println("LPOPList:"+LPOP_list);
//		System.out.println("RPOP_List:"+RPOP_list);
//		System.out.println("DC_List:"+DC_list);
		
		String file = nb_HGW+"readDelayPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		
		FileWriter fichier = new FileWriter(file);
		
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			
			/* data hosts */
			List<Integer> DataHsotList = new ArrayList<>();
			
			DataHsotList.addAll(HGW_list);
			DataHsotList.addAll(LPOP_list);
			DataHsotList.addAll(RPOP_list);
			DataHsotList.addAll(DC_list);
			
			/* data conss */
			List<Integer> DataConsList = new ArrayList<>();
			
			DataConsList.addAll(HGW_list);
			DataConsList.addAll(LPOP_list);
			DataConsList.addAll(RPOP_list);
			DataConsList.addAll(DC_list);

			for(int datahost : DataHsotList){
				for(int datacons:DataConsList){
					fw.write(String.valueOf(getFatestLink(datahost+3, datacons+3))+"\t");
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
	
	public static void saveBasisDelayMatrix() throws IOException {
		// System.out.println("Generating Basis Latency file ...");
		FileWriter fichier;

		if (DataPlacement.parallel == false) {
			fichier = new FileWriter("latencies/latency" + DataPlacement.nb_HGW+ "sequence.txt");
		} else {
			fichier = new FileWriter("latencies/latency" + DataPlacement.nb_HGW+ "parallel.txt");
		}

		try {
			BufferedWriter fw = new BufferedWriter(fichier);
			int maxRow = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			int maxCol = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			for (int row = 0; row < (maxRow); row++) {
				for (int col = 0; col < (maxCol); col++) {
					fw.write(String.valueOf(BasisDelayMatrix.mDelayMatrix[row][col])
							+ "\t");
				}
				fw.write("\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadBasisDelayMatrix()throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		// System.out.println("Loading BasisDelayMatrix");
		float[][] matrix = new float[DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC]
									[DataPlacement.nb_HGW+ DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC];
		
		FileReader fichier;
		if(new File("latencies/latency" + DataPlacement.nb_HGW+ "parallel.txt").exists()) {
			fichier = new FileReader("latencies/latency" + DataPlacement.nb_HGW+ "parallel.txt");
			
		}else {
			fichier = new FileReader("latencies/latency" + DataPlacement.nb_HGW+ "sequence.txt");
			
		}
		BufferedReader in = null;
		try {
			
			in = new BufferedReader(fichier);
			String line = null;
			int row = 0;
			while ((line = in.readLine()) != null) {
				String[] splited = line.split("\t");
				int col = 0;
				for (String val : splited) {
					matrix[row][col] = Float.valueOf(val);
					col++;
				}
				row++;
			}
			BasisDelayMatrix.mDelayMatrix = matrix;
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void loadmFlowMatrix() throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		// System.out.println("Loading BasisDelayMatrix");
		int[][] matrix = new int[DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC]
								[DataPlacement.nb_HGW+ DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC];
		FileReader fichier = new FileReader("latencies/mFlowMatrix" + DataPlacement.nb_HGW+ ".txt");
		BufferedReader in = null;
		try {
			
			in = new BufferedReader(fichier);
			String line = null;
			int row = 0;
			while ((line = in.readLine()) != null) {
				String[] splited = line.split("\t");
				int col = 0;
				for (String val : splited) {
					matrix[row][col] = Integer.valueOf(val);
					col++;
				}
				row++;
			}
			mFlowMatrix = matrix;
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void loadmAdjacenceMatrix() throws FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		// System.out.println("Loading BasisDelayMatrix");
		float[][] matrix = new float[DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC]
									[DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC];
		FileReader fichier = new FileReader("latencies/mAdjacenceMatrix" + DataPlacement.nb_HGW+ ".txt");
		BufferedReader in = null;
		try {
			
			in = new BufferedReader(fichier);
			String line = null;
			int row = 0;
			while ((line = in.readLine()) != null) {
				String[] splited = line.split("\t");
				int col = 0;
				for (String val : splited) {
					matrix[row][col] = Float.valueOf(val);
					col++;
				}
				row++;
			}
			mAdjacenceMatrix = matrix;
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void savemFlowMatrix() throws IOException {
		// System.out.println("Generating Basis Latency file ...");
		FileWriter fichier;

		if (DataPlacement.parallel == false) {
			fichier = new FileWriter("latencies/mFlowMatrix" + DataPlacement.nb_HGW+ "sequence.txt");
		} else {
			fichier = new FileWriter("latencies/mFlowMatrix" + DataPlacement.nb_HGW+ ".txt");
		}

		try {
			BufferedWriter fw = new BufferedWriter(fichier);
			int maxRow = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			int maxCol = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			for (int row = 0; row < (maxRow); row++) {
				for (int col = 0; col < (maxCol); col++) {
					fw.write(String.valueOf(mFlowMatrix[row][col])+ "\t");
				}
				fw.write("\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void savemAdjacenceMatrix() throws IOException {
		// System.out.println("Generating Basis Latency file ...");
		FileWriter fichier;

		if (DataPlacement.parallel == false) {
			fichier = new FileWriter("latencies/mAdjacenceMatrix" + DataPlacement.nb_HGW+ "sequence.txt");
		} else {
			fichier = new FileWriter("latencies/mAdjacenceMatrix" + DataPlacement.nb_HGW+ ".txt");
		}

		try {
			BufferedWriter fw = new BufferedWriter(fichier);
			int maxRow = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			int maxCol = DataPlacement.nb_HGW + DataPlacement.nb_LPOP + DataPlacement.nb_RPOP + DataPlacement.nb_DC;
			for (int row = 0; row < (maxRow); row++) {
				for (int col = 0; col < (maxCol); col++) {
					fw.write(String.valueOf(mAdjacenceMatrix[row][col])
							+ "\t");
				}
				fw.write("\n");
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
