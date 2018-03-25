package org.fog.gui.lpFileConstuction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;











import org.cloudbus.cloudsim.DataCloudTags;
import org.fog.application.AppEdge;
import org.fog.examples.DataPlacement;
import org.fog.jni.GraphPartitioning;

/**
 * 
 * @author KGZB8091
 * Class that relies each dataProd with a list that contains its dataProd
 */

public class ConsProdMatrix {
	public  Map<String, List<String>> consProdMap = new HashMap<String, List<String>>();
	public Map<String,AppEdge> edgesMap;
	
	public int [][] consProdMatrix= null;

	public int nb_ServiceHGW;
	public int nb_ServiceLPOP;
	public int nb_ServiceRPOP;
	public int nb_ServiceDC;

	public ConsProdMatrix(Map<String,AppEdge> edgesMap, int nb_ServiceHGW, int nb_ServiceLPOP, int nb_ServiceRPOP, int nb_ServiceDC){
		setEdgeList(edgesMap);
		setNb_HGW(nb_ServiceHGW);
		setNb_LPOP(nb_ServiceLPOP);
		setNb_RPOP(nb_ServiceRPOP);
		setNb_DC(nb_ServiceDC);
		AllocateConsProdMap();
		rempMatrixConsProd();
	}
	
	private void setNb_DC(int nb_ServiceDC) {
		this.nb_ServiceDC=nb_ServiceDC;
	}
	
	private void setNb_RPOP(int nb_ServiceRPOP) {
		this.nb_ServiceRPOP=nb_ServiceRPOP;
	}
	
	private void setNb_LPOP(int nb_ServiceLPOP) {
		this.nb_ServiceLPOP=nb_ServiceLPOP;
	}
	
	private void setNb_HGW(int nb_ServiceHGW) {
		this.nb_ServiceHGW=nb_ServiceHGW;
	}

	private void setEdgeList(Map<String,AppEdge> edgesMap) {
		this.edgesMap=edgesMap;
	}

	public void AllocateConsProdMap(){
    	for(String key : edgesMap.keySet()){
    		if(key.startsWith("TempHGW") || key.startsWith("TempLPOP") || key.startsWith("TempRPOP") ){
	    		List<String> l = new ArrayList<String>();
	    		l.addAll(edgesMap.get(key).getDestination());
	    		consProdMap.put(key, l); 
    		}
    	}
    }

    public void printConsProdMap(){
    	System.out.println("\n\nConsProd Map:");
    	
    	for(String key : consProdMap.keySet())
    		System.out.println("DataProd:"+key+"  DataConsList"+ consProdMap.get(key));
    	
    }
    
    public void rempMatrixConsProd(){
    	consProdMatrix = new int[nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP][nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP+nb_ServiceDC];
    	for(String key : consProdMap.keySet()){
    		int dataProdIndex = getDataProdIndex(key);
    		for(String destModule : consProdMap.get(key)){
    			consProdMatrix[dataProdIndex][getDataConsIndex(destModule)]=1;
    		}
    		
    	}
    	
    }
 
    private int getDataProdIndex(String key) {
    	if(key.contains("HGW")){
    		return Integer.parseInt(key.substring(7));
		}else if(key.contains("LPOP")){
			return Integer.parseInt(key.substring(8))+nb_ServiceHGW;
		}else if(key.contains("RPOP")){
			return Integer.parseInt(key.substring(8))+nb_ServiceHGW+nb_ServiceLPOP;
		}
		return -1;
	}

	private int getDataConsIndex(String modName) {
		if(modName.contains("HGW")){
			return Integer.parseInt(modName.substring(10));
			
		}else if(modName.contains("LPOP")){
			return Integer.parseInt(modName.substring(11))+nb_ServiceHGW;
			
		}else if(modName.contains("RPOP")){
			return Integer.parseInt(modName.substring(11))+nb_ServiceHGW+nb_ServiceLPOP;
			
		}else if(modName.contains("DC")){
			return Integer.parseInt(modName.substring(9))+nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP;
			
		}
		return -1;
	}

	public void generateConsProdFile() throws IOException{
		rempMatrixConsProd();
		FileWriter fichier = new FileWriter(nb_ServiceHGW+"consProd_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			for(int row=0;row<(nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP);row++){
				for(int col=0;col<(nb_ServiceHGW+nb_ServiceLPOP+nb_ServiceRPOP+nb_ServiceDC);col++){
					fw.write(consProdMatrix[row][col]+"\t");	
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
	
	public void generateConsProdFileInZone(int zone, Map<String, List<Integer>> zoneDevices) throws IOException{
		String file = nb_ServiceHGW+"consProdZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			
			int maxNbDataProd = zoneDevices.get("hgws").size()
								+ zoneDevices.get("lpops").size()
								+ zoneDevices.get("rpops").size();
			
			int maxNbDataCons = zoneDevices.get("hgws").size()
								+zoneDevices.get("lpops").size()
								+zoneDevices.get("rpops").size();
			int dp2;
			int dc2;
			
			for(int dp=0;dp<(maxNbDataProd);dp++){
				if(dp>(zoneDevices.get("hgws").size()+zoneDevices.get("lpops").size()-1)){
					//TempRPOP
					dp2 = dp + nb_ServiceHGW + nb_ServiceLPOP + zoneDevices.get("rpops").size()*zone - zoneDevices.get("hgws").size() - zoneDevices.get("lpops").size();
					//System.out.println("ROW->RPOP:"+row2);
					
				}else if(dp >(zoneDevices.get("hgws").size()-1)){
					//TempLPOP
					dp2 = dp + nb_ServiceHGW + zoneDevices.get("lpops").size()*zone - zoneDevices.get("hgws").size();
					//System.out.println("ROW->LPOP:"+row2);
					
				}else{
					//TempHGW
					dp2 = dp + zoneDevices.get("hgws").size()*zone;
					//System.out.println("ROW->HGW:"+row2);
					
				}
				
				for(int dc=0;dc<(maxNbDataCons);dc++){
					//System.out.println("col="+col);
					if(dc>(zoneDevices.get("hgws").size()+zoneDevices.get("lpops").size()-1)){
						//RPOP
						dc2 = dc + nb_ServiceHGW + nb_ServiceLPOP + zoneDevices.get("rpops").size()*zone - zoneDevices.get("hgws").size() - zoneDevices.get("lpops").size();
						//System.out.println("COL->RPOP:"+col2);
						
					}else if(dc>(zoneDevices.get("hgws").size()-1)){
						//LPOP
						//System.out.println("nb_hgw:"+nb_HGW+"    zoneDevises.get(lpops).size():"+zoneDevises.get("lpops").size()+"    zone:"+zone);
						dc2 = dc + nb_ServiceHGW + zoneDevices.get("lpops").size()*zone - zoneDevices.get("hgws").size();
						//System.out.println("COL->LPOP:"+col2);
						
					}else {
						//HGW
						dc2 = dc + zoneDevices.get("hgws").size()*zone;
					}
					fw.write(consProdMatrix[dp2][dc2]+"\t");
					dc2 = -1;
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
	
	public void generateConsProdFileInPartition(int partition, GraphPartitioning gp) throws IOException{
		String file = nb_ServiceHGW+"consProdPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		
		List<Integer> HGW_list = gp.getHGWInPartition(partition);
		List<Integer> LPOP_list = gp.getLPOPInPartition(partition);
		List<Integer> RPOP_list = gp.getRPOPInPartition(partition);
		List<Integer> DC_list = gp.getDCInPartition(partition);
	
		
		/* data conss */
		List<Integer> DataConsList = new ArrayList<>();
		
		DataConsList.addAll(HGW_list);
		DataConsList.addAll(LPOP_list);
		DataConsList.addAll(RPOP_list);
		DataConsList.addAll(DC_list);
		
		/* data prods */
		List<Integer> DataProdList = new ArrayList<>();
		
		DataProdList.addAll(HGW_list);
		DataProdList.addAll(LPOP_list);
		DataProdList.addAll(RPOP_list);
		
		FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			int dp,dc; 
			
			for(int dataprod : DataProdList){
				dp=-1;
				if(dataprod > nb_ServiceDC + nb_ServiceRPOP + nb_ServiceLPOP -1){
					//HGW dataprod
					dp = dataprod - nb_ServiceDC - nb_ServiceRPOP - nb_ServiceLPOP;
					
				}else if(dataprod >  nb_ServiceDC + nb_ServiceRPOP -1){
					//LPOP dataprod
					dp = dataprod - nb_ServiceDC - nb_ServiceRPOP + nb_ServiceHGW;
					
				}else if(dataprod >  nb_ServiceDC -1){
					//RPOP dataprod
					dp = dataprod - nb_ServiceDC + nb_ServiceHGW + nb_ServiceLPOP;
					
				}				
				
				for(int datacons : DataConsList){
					dc=-1;
					
					if(datacons >  nb_ServiceDC + nb_ServiceRPOP + nb_ServiceLPOP -1){
						//HGW datacons
						dc = datacons - nb_ServiceDC - nb_ServiceRPOP - nb_ServiceLPOP;
						
					}else if(datacons >  nb_ServiceDC + nb_ServiceRPOP -1){
						//LPOP datacons
						dc = datacons - nb_ServiceDC - nb_ServiceRPOP + nb_ServiceHGW;
						
					}else if(datacons >  nb_ServiceDC-1){
						//RPOP datacons
						dc = datacons - nb_ServiceDC + nb_ServiceHGW + nb_ServiceLPOP ;
						
					}else if(datacons <  nb_ServiceDC){
						//DC datacons
						dc = datacons + nb_ServiceHGW + nb_ServiceLPOP + nb_ServiceRPOP;
						
					}
					
					
					fw.write(consProdMatrix[dp][dc]+"\t");
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
}
