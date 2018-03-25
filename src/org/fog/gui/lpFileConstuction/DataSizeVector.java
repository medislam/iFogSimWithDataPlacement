package org.fog.gui.lpFileConstuction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.examples.DataPlacement;
import org.fog.jni.GraphPartitioning;
import org.fog.utils.Config;

/**
 * 
 * @author KGZB8091
 *
 *
 *DataSizeVector class relies each DataProd (service/Sensor) with it output data size
 */

public class DataSizeVector {
	public  Map<String, Double> dataSizeVec = new HashMap<String, Double>();
	
	public int nb_ServiceHGW;
	public int nb_ServiceLPOP;
	public int nb_ServiceRPOP;
	
	public Map<String,AppEdge> edgesMap;
	
	public DataSizeVector(Map<String,AppEdge> edgesMap, int sHGW, int sLpop, int sRpop, Application application){
		setEdgeList(edgesMap);
		nb_ServiceHGW=sHGW;
		nb_ServiceLPOP=sLpop;
		nb_ServiceRPOP=sRpop;
		
		AllocateDataSize(application);
		
	}
	
	private void setEdgeList(Map<String,AppEdge> edgesMap) {
		this.edgesMap=edgesMap;
	}
  
    public void AllocateDataSize(Application application){
    	//System.out.println("Selectivity map:");
    	//application.printSelectivityMap();
    	
    	int nb_TupleOccirence = Integer.valueOf((int) (Config.MAX_SIMULATION_TIME / (DataPlacement.SNR_TRANSMISSION_TIME * 10 )))-1;
    	
    	for(String key : edgesMap.keySet()){
    		if(key.startsWith("TempSNR")||key.startsWith("TempAct"))
    			continue;
    		
	    	if(key.startsWith("TempHGW")){
	    		addDataSize(key, edgesMap.get(key).getTupleNwLength() * nb_TupleOccirence);
	    			
	    	}else if(application.mapSelectivity.containsKey(key)){
	    		//System.out.println("contains:"+key);
	    		
	    		if(application.mapSelectivity.get(key).get(0).contains("HGW")){
	    			addDataSize(key, edgesMap.get(key).getTupleNwLength() * nb_TupleOccirence);
	    		}else{
	    			if(application.mapSelectivity.containsKey(application.mapSelectivity.get(key).get(0))){
		    			addDataSize(key, edgesMap.get(key).getTupleNwLength() * nb_TupleOccirence);
		    		}else
		    			addDataSize(key, 0);
	    		}
	    	}else{
	    		addDataSize(key, 0);
	    	}
    	}
    }

	private void addDataSize(String key, double value){
    		dataSizeVec.put(key, value);
    }
    
    public void generateDataSizeFile() throws IOException{
		FileWriter fichier = new FileWriter(nb_ServiceHGW+"dataSize_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);

			for(int i=0;i<nb_ServiceHGW;i++){
				fw.write(dataSizeVec.get("TempHGW"+i)+"\t");
			}
			
			for(int i=0;i<nb_ServiceLPOP;i++){
				fw.write(dataSizeVec.get("TempLPOP"+i)+"\t");
			}
			
			for(int i=0;i<nb_ServiceRPOP;i++){
				fw.write(dataSizeVec.get("TempRPOP"+i)+"\t");
			}
			
			fw.close();
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
    
    public void generateDataSizeFileInZone(int zone, Map<String, List<Integer>> zoneDevises) throws IOException{
		String file = nb_ServiceHGW+"dataSizeZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
    	FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);

			for(int i=0;i<zoneDevises.get("hgws").size();i++){
				int index = i + zoneDevises.get("hgws").size()*zone;
				fw.write(dataSizeVec.get("TempHGW"+index)+"\t");
			}
			
			for(int i=0;i<zoneDevises.get("lpops").size();i++){
				int index = i + zoneDevises.get("lpops").size()*zone;
				fw.write(dataSizeVec.get("TempLPOP"+index)+"\t");
			}
			
			for(int i=0;i<zoneDevises.get("rpops").size();i++){
				int index = i + zoneDevises.get("rpops").size() * zone;
				fw.write(dataSizeVec.get("TempRPOP"+index)+"\t");
			}
			
			fw.close();
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
    
    public void generateDataSizeFileInpartiton(int partition, GraphPartitioning gp, int nb_DC) throws IOException{
		String file = nb_ServiceHGW+"dataSizePartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
    	FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);

			for(int hgw : gp.getHGWInPartition(partition)){
				int index = hgw - nb_DC - nb_ServiceRPOP - nb_ServiceLPOP ;
				fw.write(dataSizeVec.get("TempHGW"+index)+"\t");
			}
			
			for(int lpop : gp.getLPOPInPartition(partition)){
				int index = lpop - nb_DC - nb_ServiceRPOP;
				fw.write(dataSizeVec.get("TempLPOP"+index)+"\t");
			}
			
			for(int rpop : gp.getRPOPInPartition(partition)){
				int index = rpop - nb_DC;
				fw.write(dataSizeVec.get("TempRPOP"+index)+"\t");
			}
			
			fw.close();
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
    
    public void printDataSizeVec(){
    	System.out.println("Data Size Vector:");
    	
    	for(String key : dataSizeVec.keySet())
    		System.out.println("Tuple:"+key+"  DataSize:"+ dataSizeVec.get(key));
    	
    }
}
