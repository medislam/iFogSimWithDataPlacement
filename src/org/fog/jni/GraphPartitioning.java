package org.fog.jni;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.examples.DataPlacement;

public class GraphPartitioning {
	
	private int [] LP; 
	private int [] LS;
	
	private int [][] graphMatrix;
	
	private int nb_nodes;
	private int nb_arcs;
	
	private int nb_HGW;
	private int nb_LPOP;
	private int nb_RPOP;
	private int nb_DC;
	
	private Map<Integer, List<Integer>> devicesPartition = null;
	
 	public GraphPartitioning(int nb_hgw, int nb_lpop, int nb_rpop, int nb_dc, int nb_source_vertice, int nb_arc, float [][]dis){
		nb_nodes = nb_hgw+nb_lpop+nb_rpop+nb_dc;
		nb_HGW = nb_hgw;
		nb_LPOP = nb_lpop;
		nb_RPOP = nb_rpop;
		nb_DC = nb_dc;
		init_LP_LS( nb_nodes,  nb_source_vertice,  nb_arc, dis);
	}
	
	public void constructInputFileGaphPartitioning(int [] verticePonderationTab, int [] arcPonderationTab) throws IOException {
		
		constrauctGraphMatrix(arcPonderationTab);
		System.out.println("Graph Matrix");
		//printMatrix(graphMatrix);
		System.out.println();
		
		FileWriter fichier = new FileWriter(verticePonderationTab.length+".graph");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			fw.write(nb_nodes+" "+ nb_arcs+"  011");
			System.out.println("verticePonderationTab.length:"+verticePonderationTab.length);
			
			for(int i=0; i <nb_nodes;i++){
				fw.write("\n"+(int)(verticePonderationTab[i]+1)+" ");
				for(int j=0; j <nb_nodes;j++){
					if(graphMatrix[i][j]!=0){
						fw.write((int)(j+1)+" "+graphMatrix[i][j]+" ");
					}
				}
			}
			
			fw.close();
			graphMatrix = null;
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private void init_LP_LS(int n, int nb_source_vertice, int nb_arc, float [][] dis){
		int i,j,indx_ls;
		
		LP = new int [nb_source_vertice];
		LS =  new int [nb_arc];

		indx_ls=0;i=0;
		/* End of index in LP*/
		LP[nb_source_vertice-1]=nb_arc;

		while(i < n && indx_ls < nb_arc){
			LP[i]=indx_ls;
			// non-oriented Graph
			for(j=i+1;j<n;j++){

				if(dis[i][j]!=Float.MAX_VALUE){
					LS[indx_ls]=j;
					++indx_ls;
				}
			}
			++i;
		}
		
	}

	public void constrauctGraphMatrix(int [] arcPonderationTab){
		graphMatrix = new int [nb_nodes][nb_nodes];

		nb_arcs = arcPonderationTab.length;
		for(int i=0;i<LP.length-1;i++){
			int index_beg = LP[i];
			int index_end = LP[i+1];
			
			int source_node = i;
			
			for(int index=index_beg;index<index_end; index++){
				int dest_node = LS[index];
				graphMatrix[source_node][dest_node] = arcPonderationTab[index]+1;
				graphMatrix[dest_node][source_node] = arcPonderationTab[index]+1;
			}
			
		}
	}
	
	public String partitioningCommande(int nb_nodes, int nb_partition, int ncuts){
		StringBuffer output = new StringBuffer();
		
		Process p;
		 try{
			 String command= "gpmetis "+"-ncuts="+ncuts+" "+nb_nodes+".graph "+nb_partition;
			 p = Runtime.getRuntime().exec(command);
			 p.waitFor();
			 
			 BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			 String line = "";
			 
			 while((line = reader.readLine())!= null){
				 output.append(line + "\n");
			 }
			 
			 
		 } catch (Exception e){
			 e.printStackTrace();
		 }
		 
		 return output.toString();
	}
	
	public void setPartitionDevices(int nb_partitions) throws FileNotFoundException {
		devicesPartition = new HashMap<Integer, List<Integer>>();
	
		FileReader fichier = new FileReader(nb_nodes+".graph.part."+nb_partitions);
		BufferedReader in = null;
		try{
			in = new BufferedReader(fichier);
			String line =null;
			int row =0;

			while((line = in.readLine()) != null){
				if(devicesPartition.containsKey(Integer.valueOf(line))){
					devicesPartition.get(Integer.valueOf(line)).add(row);

				}else{
					List<Integer> list = new ArrayList<>();
					list.add(row);
					devicesPartition.put(Integer.valueOf(line),list);
				}
				row++;
			}
			
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void printPartitonDevicesMap(){
		System.out.println("Zone devices map!");
		for(int key : devicesPartition.keySet()){
			System.out.println("zone:"+key+"\tdevices:"+devicesPartition.get(key));
		}
	}
	
	public Map<Integer, List<Integer>> getPartitionDevices(){
		return this.devicesPartition;
	}
	
	public List<Integer> getDCInPartition(int partition) {
		// TODO Auto-generated method stub
		List<Integer> listDC = new ArrayList<Integer>();

		for(int elem : devicesPartition.get(partition)){
			if(elem < nb_DC){
				listDC.add(elem);
			}
		}
		
		return listDC;
	}

	public List<Integer> getRPOPInPartition(int partition) {
		// TODO Auto-generated method stub
		List<Integer> listRPOP = new ArrayList<Integer>();
		
		for(int elem : devicesPartition.get(partition)){
			if(elem >= nb_DC && elem < nb_RPOP+nb_DC){
				listRPOP.add(elem);
			}
		}
		
		return listRPOP;
	}

	public List<Integer> getLPOPInPartition(int partition) {
		// TODO Auto-generated method stub
		List<Integer> listLPOP = new ArrayList<Integer>();
		
		for(int elem : devicesPartition.get(partition)){
			if(elem >= nb_DC+nb_RPOP && elem < nb_LPOP+nb_RPOP+nb_DC){
				listLPOP.add(elem);
			}
		}
		
		return listLPOP;
	}

	public List<Integer> getHGWInPartition(int partition) {
		// TODO Auto-generated method stub
		List<Integer> listHGW = new ArrayList<Integer>();

		for(int elem : devicesPartition.get(partition)){
			if(elem >= nb_DC+nb_RPOP+nb_LPOP && elem < nb_LPOP+nb_RPOP+nb_DC+nb_HGW){
				listHGW.add(elem);
			}
		}
		
		return listHGW;
	}
	
 	public void printMatrix(int [][] matrix){
		
		System.out.print("\t");
		for(int k = 0; k< matrix.length; k++){
			System.out.print(k+"\t");
		}
		
		for(int i=0; i<matrix.length; i++){
			System.out.print("\n"+i+"\t");
			for(int j=0; j< matrix.length; j++){
				System.out.print(matrix[i][j]+"\t");
			}
		}
	}

 	public boolean checkNullDataConsInPatrtition(int partition){

 		if(getDCInPartition(partition).size()==0 && getRPOPInPartition(partition).size()==0 && getLPOPInPartition(partition).size()==0){
 			return true;
 		}
 		
 		return false;
 	}

 	public boolean checkNullPatrtition(int partition){

 		if(devicesPartition.get(partition) ==null){
 			return true;
 		}
 		
 		return false;
 	}

	public void writeInformation(int nb_partitions) throws IOException {
		// TODO Auto-generated method stub
		String log="Nb_DataCons/DataProd = "+DataPlacement.nb_DataCons_By_DataProd;
		for(int partition = 0; partition<nb_partitions; partition++){
			
			if(checkNullPatrtition(partition)) continue;
			log = "\nNumber of devices in partition:"+partition+"\t is "+devicesPartition.get(partition).size();
			log+= "\nNumber of HGW is "+getHGWInPartition(partition).size();
			log+= "\nNumber of LPOP is "+getLPOPInPartition(partition).size();
			log+= "\nNumber of RPOP is "+getRPOPInPartition(partition).size();
			log+= "\nNumber of DC is "+getDCInPartition(partition).size();
			log+= "\n--------------------------------------------------------------------------------\n";
			org.fog.examples.Log.writePartitionDevicesInformation(nb_HGW, log);
		}
		
		log = "*******************************************************************************";
		org.fog.examples.Log.writePartitionDevicesInformation(nb_HGW, log);
		
		for(int partition = 0; partition<nb_partitions; partition++){
			log = "Devices in partition:"+partition;
			if(checkNullPatrtition(partition)) continue;
			log+= "\nHGW is  :"+getHGWInPartition(partition);
			log+= "\nLPOP is :"+getLPOPInPartition(partition);
			log+= "\nRPOP is :"+getRPOPInPartition(partition);
			log+= "\nDC is   :"+getDCInPartition(partition);
			log+= "\n--------------------------------------------------------------------------------\n";
			org.fog.examples.Log.writePartitionDevicesInformation(nb_HGW, log);
		}
		
	}
 	
//	private void selectFogDevice(int partition) {
//		// TODO Auto-generated method stub
//		int dest;
//		int fognode;
//		if(partition==0){
//			dest = partition+1;
//		}else{
//			dest = partition-1;
//		}
//		boolean cond = false;
//		
//		while(cond){
//			if(getLPOPInPartition(dest).size()>=2){
//				fognode= getLPOPInPartition(dest).get(0);
//				
//				cond = true;
//				
//			}else if(getLPOPInPartition(dest).size()>=2){
//				fognode= getLPOPInPartition(dest).get(0);
//				cond = true;
//			}else if(getRPOPInPartition(dest).size()>=2){
//				fognode= getLPOPInPartition(dest).get(0);
//				cond = true;
//			}else if(getDCInPartition(dest).size()>=2){
//				fognode= getLPOPInPartition(dest).get(0);
//				cond = true;
//			}
//			dest++;
//		}
//		
//		
//
//	}
 		
}
