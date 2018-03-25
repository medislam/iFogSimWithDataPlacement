package org.fog.jni;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.fog.application.*;
import org.fog.entities.FogDevice;
import org.fog.examples.DataPlacement;
import org.fog.placement.ModuleMapping;



public class GraphPonderation {
	protected int [] LP_prod;
	protected int [] LS_cons;
	protected int [] arc_ponderation;
	protected int [] node_ponderation;
	
	private int nb_hgw;
	private int nb_lpop;
	private int nb_rpop;
	private int nb_dc;
	
	public GraphPonderation(int nb_hgw, int nb_lpop, int nb_rpop, int nb_dc, Application application, Map<String, String> moduleToHostMap) throws IOException{
		this.nb_hgw = nb_hgw;
		this.nb_lpop = nb_lpop;
		this.nb_rpop = nb_rpop;
		this.nb_dc = nb_dc;
		this.initialise_LP_prod_LS_cons(application, moduleToHostMap);
	}
	
	private void initialise_LP_prod_LS_cons(Application application, Map<String, String> moduleToHostMap) throws IOException{
		
		int index_LP_prod=0, index_LS_cons = 0;
		AppEdge edge;
		List<String> destinations;
		Map<String, AppEdge> edgeMap = application.getEdgeMap();
		String devName;
		FogDevice fogdev;
		
		int LP_prod_length = getLP_prod_Length();
		int LS_cons_length = getLS_cons_Length(application);
		
		System.out.println("LP_prod length ="+LP_prod_length);
		System.out.println("LS_cons length ="+LS_cons_length);
		
		
		
		LP_prod = new int[LP_prod_length];
		LS_cons = new int[LS_cons_length];
		
		/* adding datacenters */
		for(int dc=0;dc<nb_dc;dc++){
			edge = edgeMap.get("TempDC"+dc);
			//destinations = edge.getDestination();
			LP_prod[index_LP_prod++] = index_LS_cons;
			
			if(edge ==null){
				LS_cons[index_LS_cons++] = -1;
				continue;
			}
			
			if(edge.getDestination()==null){
				LS_cons[index_LS_cons++] = -1;
				
			}else{	
				/* Add arc destinations (consumers) to LS_cons */
				destinations = edge.getDestination();
				for(String dest : destinations){
					devName = moduleToHostMap.get(dest);
					fogdev = application.getFogDeviceByName(devName);
					LS_cons[index_LS_cons++] = fogdev.getId()-3;
					
				}
			}
		}
		
		/* adding rpops */
		for(int rpop=0;rpop<nb_rpop;rpop++){
			edge = edgeMap.get("TempRPOP"+rpop);
			destinations = edge.getDestination();
			LP_prod[index_LP_prod++] = index_LS_cons;
			
			if(destinations.isEmpty()){
				LS_cons[index_LS_cons++] = -1;
			}else{
								
				/* Add arc destinations (consumers) to LS_cons */
				for(String dest : destinations){
					devName = moduleToHostMap.get(dest);
					fogdev = application.getFogDeviceByName(devName);
					LS_cons[index_LS_cons++] = fogdev.getId()-3;
					
				}
			}
		}
		
		/* adding lpops */
		for(int lpop=0;lpop<nb_lpop;lpop++){
			edge = edgeMap.get("TempLPOP"+lpop);
			destinations = edge.getDestination();
			LP_prod[index_LP_prod++] = index_LS_cons;
			
			if(destinations.isEmpty()){
				LS_cons[index_LS_cons++] = -1;
			}else{
								
				/* Add arc destinations (consumers) to LS_cons */
				for(String dest : destinations){
					devName = moduleToHostMap.get(dest);
					fogdev = application.getFogDeviceByName(devName);
					LS_cons[index_LS_cons++] = fogdev.getId()-3;
					
				}
			}
		}
		
		/* adding hgw */
		for(int hgw=0;hgw<nb_hgw;hgw++){
			edge = edgeMap.get("TempHGW"+hgw);
			destinations = edge.getDestination();
			LP_prod[index_LP_prod++] = index_LS_cons;
			
			if(destinations.isEmpty()){
				LS_cons[index_LS_cons++] = -1;
			}else{
								
				/* Add arc destinations (consumers) to LS_cons */
				for(String dest : destinations){
					devName = moduleToHostMap.get(dest);
					fogdev = application.getFogDeviceByName(devName);
					LS_cons[index_LS_cons++] = fogdev.getId()-3;
					
				}
			}
		}
		
		/* add index of the end in LS_cons table */
		LP_prod[index_LP_prod++] = index_LS_cons;
	}
	
	private int getLS_cons_Length(Application application) {
		// TODO Auto-generated method stub
		int length=nb_dc;
		for(AppEdge edge : application.getEdges()){
			if(edge.getTupleType().startsWith("TempSNR") || edge.getTupleType().startsWith("TempAct"))
				continue;
			length += edge.getDestination().size();
		}
		
		return length;
	}

	private int getLP_prod_Length() {
		// TODO Auto-generated method stub
		return nb_dc+nb_rpop+nb_lpop+nb_hgw+1;
	}

	public void printLP_prod(){
		System.out.println("\n********** LP_prod Table ***********");
		for(int i=0;i<LP_prod.length;i++){
			System.out.print(i+"\t");
		}
		System.out.println();
		for(int j=0;j<nb_dc;j++){
			System.out.print("dc_"+j+"\t");
		}
		
		for(int j=0;j<nb_rpop;j++){
			System.out.print("rpop_"+j+"\t");
		}
		
		for(int j=0;j<nb_lpop;j++){
			System.out.print("lpop_"+j+"\t");
		}
		
		for(int j=0;j<nb_hgw;j++){
			System.out.print("hgw_"+j+"\t");
		}
		System.out.println("end");
		for(int i=0;i<LP_prod.length;i++){
			System.out.print(LP_prod[i]+"\t");
		}
		System.out.println();
	}
	
	public void printLS_cons(){
		System.out.println("\n********** LS_cons Table ***********");
		for(int i=0;i<LS_cons.length;i++){
			System.out.print(i+"\t");
		}
		System.out.println();
		for(int i=0;i<LS_cons.length;i++){
			System.out.print(LS_cons[i]+"\t");
		}
		System.out.println();
	}
	
	/* create Java native method to weight the arcs */
	static { 
		System.load(DataPlacement.FloydPath+"libArcPonderation.so");
		}
	
	private native int [] nativeArcPoderation(int LP_prodLength, int LS_consLength, int [] LP_prod, int [] LS_cons, int LPLength, int LSLength,float[][] dist,int [][] flow);
	
	public void arcPonderation(int LPLength, int LSLength,float[][] adjacence,int [][] flow) throws IOException{
		Log.writeInLogFile("DataPlacement","Arc Ponderation ...");
		arc_ponderation = new int [LSLength];
		
		//printLP_prod();
		//printLS_cons();
		Log.writeInLogFile("DataPlacement","LP Length="+LPLength);
		Log.writeInLogFile("DataPlacement","LS Length="+LSLength+" = "+ arc_ponderation.length);
		
		
		arc_ponderation = nativeArcPoderation(LP_prod.length, LS_cons.length, LP_prod,LS_cons,LPLength, LSLength, adjacence,flow);
		
		//System.out.println("Press a key to continue!");
		//System.in.read();
	}
	
	public void nodePonderation(int nb_nodes, Application application){
		
		node_ponderation = new int [nb_nodes];
		
		Log.writeInLogFile("DataPlacement","Node Ponderation ...");
		// get dataProd (module) -> source of edges 
		// get Module host and host ++
		
		for(AppEdge edge: application.getEdges()){
			if(edge.getTupleType().startsWith("TempSNR") || edge.getTupleType().startsWith("TempAct"))
				continue;
			String moduName = edge.getSource();
			String devName = ModuleMapping.getDeviceHostModule(moduName);
			++node_ponderation[getIndiceIntableNodes(devName)];
			
		}
	}
	
	private int getIndiceIntableNodes(String devName) {
		
		if(devName.startsWith("DC"))
			return Integer.valueOf(devName.substring(2));
		
		if(devName.startsWith("RPOP"))
			return Integer.valueOf(devName.substring(4))+nb_dc;
		
		if(devName.startsWith("LPOP"))
			return Integer.valueOf(devName.substring(4))+nb_dc+nb_rpop;
		
		if(devName.startsWith("HGW"))
			return Integer.valueOf(devName.substring(3))+nb_dc+nb_rpop+nb_lpop;
		
		return -1;
	}

	public int [] getArcPonderation(){
		return arc_ponderation;
	}
	
	public int [] getNodePonderation(){
		return node_ponderation;
	}
	
	public void printTablePonderation(int [] table){
		for(int i : table){
			System.out.print(i+"\t");
		}
		System.out.println();
	}
}
