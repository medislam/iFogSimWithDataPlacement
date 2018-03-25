package org.fog.gui.lpFileConstuction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.entities.FogDevice;
import org.fog.examples.DataPlacement;
import org.fog.jni.GraphPartitioning;

public class FreeCapacityVector {
	public Map<String,Long> freeCapacity = new HashMap<String,Long>();
	public int nb_HGW;
	public int nb_LPOP;
	public int nb_RPOP;
	public int nb_DC;
	
	
	public FreeCapacityVector(List<FogDevice> fogDevices, int nb_HGW, int nb_LPOP, int nb_RPOP, int nb_DC){
		setFreeCapacity(fogDevices);
		setNb_HGW(nb_HGW);
		setNb_LPOP(nb_LPOP);
		setNb_RPOP(nb_RPOP);
		setNb_DC(nb_DC);
	}
	
	private void setNb_DC(int nb_DC) {
		this.nb_DC=nb_DC;
	}
	
	private void setNb_RPOP(int nb_RPOP) {
		this.nb_RPOP=nb_RPOP;
	}
	
	private void setNb_LPOP(int nb_LPOP) {
		this.nb_LPOP=nb_LPOP;
	}
	
	private void setNb_HGW(int nb_HGW) {
		this.nb_HGW=nb_HGW;
	}
	
	public Map<String,Long> getFreeCapacity(){
		return this.freeCapacity;
	}
	
	public void setFreeCapacity(List<FogDevice> fogDevices){
		for(FogDevice fogdev : fogDevices){
			freeCapacity.put(fogdev.getName(),(long)1000000000);
		}
	}
	
	public void generateFreeCapacityFile() throws IOException{
		FileWriter fichier = new FileWriter(nb_HGW+"freeCapacity_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			for(int i=0;i<nb_HGW;i++){
				fw.write(freeCapacity.get("HGW"+i)+"\t");
			}
			
			for(int i=0;i<nb_LPOP;i++){
				fw.write(freeCapacity.get("LPOP"+i)+"\t");
			}
			
			for(int i=0;i<nb_RPOP;i++){
				fw.write(freeCapacity.get("RPOP"+i)+"\t");
			}
			
			for(int i=0;i<nb_DC;i++){
				fw.write(freeCapacity.get("DC"+i)+"\t");
			}
			fw.close();
			
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void generateFreeCapacityFileInZone(int zone, Map<String, List<Integer>> zoneDevises) throws IOException{
		String file = nb_HGW+"freeCapacityZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
    	FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			for(int i=0;i<zoneDevises.get("hgws").size();i++){
				int index = i + zoneDevises.get("hgws").size()*zone;
				fw.write(freeCapacity.get("HGW"+index)+"\t");
			}
			
			for(int i=0;i<zoneDevises.get("lpops").size();i++){
				int index = i + zoneDevises.get("lpops").size()*zone;
				fw.write(freeCapacity.get("LPOP"+index)+"\t");
			}
			
			for(int i=0;i<zoneDevises.get("rpops").size();i++){
				int index = i + zoneDevises.get("rpops").size()*zone;
				fw.write(freeCapacity.get("RPOP"+index)+"\t");
			}

			fw.close();
			
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void generateFreeCapacityFileInPartition(int partition, GraphPartitioning gp) throws IOException{
		String file = nb_HGW+"freeCapacityPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt";
		List<Integer> HGW_list = gp.getHGWInPartition(partition);
		List<Integer> LPOP_list = gp.getLPOPInPartition(partition);
		List<Integer> RPOP_list = gp.getRPOPInPartition(partition);
		List<Integer> DC_list = gp.getDCInPartition(partition);
		
		
    	FileWriter fichier = new FileWriter(file);
		try{
			BufferedWriter fw = new BufferedWriter(fichier);
			for(int hgw: HGW_list){
				int index = hgw -nb_DC - nb_RPOP - nb_LPOP;
				fw.write(freeCapacity.get("HGW"+index)+"\t");
			}
			
			for(int lpop: LPOP_list){
				int index = lpop -nb_DC - nb_RPOP;
				fw.write(freeCapacity.get("LPOP"+index)+"\t");
			}
			
			for(int rpop: RPOP_list){
				int index = rpop -nb_DC;
				fw.write(freeCapacity.get("RPOP"+index)+"\t");
			}
			

			for(int dc: DC_list){
				int index = dc;
				fw.write(freeCapacity.get("RPOP"+index)+"\t");
			}

			fw.close();
			
		}catch (FileNotFoundException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		for(String key : freeCapacity.keySet()) {
			buffer.append(freeCapacity.get(key)+"\n");
		}
		return buffer.toString();
	}

}
