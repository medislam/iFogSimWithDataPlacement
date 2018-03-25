package org.fog.cplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import org.fog.examples.DataPlacement;

import ilog.concert.*;
import ilog.cplex.*;


public class CallCplex {
	String cplexFileName;
	int nb_DataProd;
	int nb_DataHost;
	
	public CallCplex(String cplexFileName, int nb_DataProd, int nb_DataHost){
		setCplexFileName(cplexFileName);
		this.nb_DataProd=nb_DataProd;
		this.nb_DataHost=nb_DataHost;
	}
	
	private void setCplexFileName(String cplexFileName) {
		this.cplexFileName=cplexFileName;
	}

	public boolean problemSolving(int nb_HGW) throws IOException {
		try {
			
			long begin_t, end_t, importing_t, solving_t;

			IloCplex cplex = new IloCplex();
						
			cplex.setParam(IloCplex.IntParam.WorkMem, 1024*20);
			System.out.println("Work memory ="+cplex.getParam(IloCplex.IntParam.WorkMem));
			
			cplex.setParam(IloCplex.IntParam.VarSel, 4);
			cplex.setParam(IloCplex.BooleanParam.MemoryEmphasis, true);
			cplex.setParam(IloCplex.DoubleParam.TreLim, 2048*60);
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
			Calendar calendar = Calendar.getInstance();

			begin_t = Calendar.getInstance().getTimeInMillis();
		
			System.out.println("Importing the LP file...");

			cplex.importModel(cplexFileName);
			end_t = Calendar.getInstance().getTimeInMillis();
			
			IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
			
			importing_t = end_t - begin_t;
			
			begin_t = Calendar.getInstance().getTimeInMillis();
			System.out.println("Solving the problem...");
			if(cplex.solve()){
				end_t = Calendar.getInstance().getTimeInMillis();
				solving_t = end_t - begin_t;
				org.fog.examples.Log.writeSolvingTime(nb_HGW,"Solving Time:"+String.valueOf(solving_t));
				System.out.println("The problem is well solving");
				
				double objval = cplex.getObjValue();
				System.out.println("Objective ="+objval);
		
				double [] x = cplex.getValues(lp);
				
				
				FileWriter fichier = new FileWriter(nb_HGW+"TimeStats_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
				FileWriter fichier2 = new FileWriter(nb_HGW+"Solution_"+DataPlacement.nb_DataCons_By_DataProd+".txt");

				try{
					BufferedWriter fw = new BufferedWriter(fichier);
					
					fw.write("Objective:"+objval+"\n");
					fw.write("Importing Time:"+importing_t+"\n");
					fw.write("Solving Time:"+solving_t);

					BufferedWriter fw2 = new BufferedWriter(fichier2);
					
					for(int i=0;i<nb_DataProd;i++){
						for(int j=0;j<nb_DataHost;j++){
							fw2.write(String.valueOf(x[(i*nb_DataHost+j)])+"\t");
						}
						fw2.write("\n");
					}			
					fw2.close();		
					fw.close();
				}catch (FileNotFoundException e){
					e.printStackTrace();
				}catch (IOException e){
					e.printStackTrace();
				}
				
				
				System.out.println(cplex.getStatus());
				
				cplex.end();
				return true;
			}else{
				System.out.println("Problem doesn't solving!, may there is insuffusent work memory!");
			}
			cplex.end();
			} catch (IloException e) {
				System.err.println("Concert exception caught: " + e);
			}
		return false;
		
	}

	public boolean problemSolvingInZone(int nb_HGW, int zone) throws IOException {
		try {
			
			long begin_t, end_t, importing_t, solving_t;

			IloCplex cplex = new IloCplex();
						
			cplex.setParam(IloCplex.IntParam.WorkMem, 1024*20);
			System.out.println("Work memory ="+cplex.getParam(IloCplex.IntParam.WorkMem));
			
			cplex.setParam(IloCplex.IntParam.VarSel, 4);
			cplex.setParam(IloCplex.BooleanParam.MemoryEmphasis, true);
			cplex.setParam(IloCplex.DoubleParam.TreLim, 2048*60);
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
			
			Calendar calendar = Calendar.getInstance();
			begin_t = Calendar.getInstance().getTimeInMillis();
		
			System.out.println("Importing the LP file zone "+zone+"...");

			cplex.importModel(cplexFileName);
			end_t = Calendar.getInstance().getTimeInMillis();
			
			IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
			
			importing_t = end_t - begin_t;
			
			begin_t = Calendar.getInstance().getTimeInMillis();
			System.out.println("Solving the problem  zone "+zone+"...");
			if(cplex.solve()){
				end_t = Calendar.getInstance().getTimeInMillis();
				solving_t = end_t - begin_t;
				org.fog.examples.Log.writeSolvingTime(nb_HGW,"				Solving Time:"+String.valueOf(solving_t)+"	zone:"+zone);
				System.out.println("The problem is well solving zone "+zone);
				
				double objval = cplex.getObjValue();
				System.out.println("Objective zone "+zone+" = "+objval);
		
				double [] x = cplex.getValues(lp);
				
				
				FileWriter fichier = new FileWriter(nb_HGW+"TimeStatsZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
				FileWriter fichier2 = new FileWriter(nb_HGW+"SolutionZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");

				try{
					BufferedWriter fw = new BufferedWriter(fichier);
					
					fw.write("Objective:"+objval+"\n");
					fw.write("Importing Time:"+importing_t+"\n");
					fw.write("Solving Time:"+solving_t);

					BufferedWriter fw2 = new BufferedWriter(fichier2);
					
					for(int i=0;i<nb_DataProd;i++){
						for(int j=0;j<nb_DataHost;j++){
							//System.out.println("Name:"+lp.getNumVar(i*nb_DataHost+j)+"     Val:"+x[(i*nb_DataHost+j)]);
							fw2.write(String.valueOf(x[(i*nb_DataHost+j)])+"\t");
						}
						fw2.write("\n");
					}			
					fw2.close();		
					fw.close();
				}catch (FileNotFoundException e){
					e.printStackTrace();
				}catch (IOException e){
					e.printStackTrace();
				}
				
				
				System.out.println(cplex.getStatus());
				
				cplex.end();
				return true;
			}else{
				System.out.println("Problem doesn't solving!, may there is insuffusent work memory!  zone "+zone);
			}
			cplex.end();
			} catch (IloException e) {
				System.err.println("Concert exception caught: " + e);
			}
		return false;
		
	}

	public boolean problemSolvingInPartition(int nb_HGW, int partition) throws IOException {
		try {
			
			long begin_t, end_t, importing_t, solving_t;

			IloCplex cplex = new IloCplex();
						
			cplex.setParam(IloCplex.IntParam.WorkMem, 1024*20);
			System.out.println("Work memory ="+cplex.getParam(IloCplex.IntParam.WorkMem));
			
			cplex.setParam(IloCplex.IntParam.VarSel, 4);
			cplex.setParam(IloCplex.BooleanParam.MemoryEmphasis, true);
			cplex.setParam(IloCplex.DoubleParam.TreLim, 2048*60);
			cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
			Calendar calendar = Calendar.getInstance();
			
			begin_t = Calendar.getInstance().getTimeInMillis();
		
			System.out.println("Importing the LP file partition:"+partition+"...");

			cplex.importModel(cplexFileName);
			end_t = Calendar.getInstance().getTimeInMillis();
			
			IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
			
			importing_t = end_t - begin_t;
			
			begin_t = Calendar.getInstance().getTimeInMillis();
			System.out.println("Solving the problem  partition: "+partition+"...");
			if(cplex.solve()){
				end_t = Calendar.getInstance().getTimeInMillis();
				solving_t = end_t - begin_t;
				org.fog.examples.Log.writeSolvingTime(nb_HGW,"				Solving Time:"+String.valueOf(solving_t)+"	partition:"+partition);
				System.out.println("The problem is well solving partition: "+partition);
				
				double objval = cplex.getObjValue();
				System.out.println("Objective partiton: "+partition+" = "+objval);
		
				double [] x = cplex.getValues(lp);
				
				
				FileWriter fichier = new FileWriter(nb_HGW+"TimeStatsPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
				FileWriter fichier2 = new FileWriter(nb_HGW+"SolutionPartition"+partition+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");

				try{
					BufferedWriter fw = new BufferedWriter(fichier);
					
					fw.write("Objective in partiton:"+partition+"\tis:"+objval+"\n");
					fw.write("Importing Time in partiton:"+partition+"\tis:"+importing_t+"\n");
					fw.write("Solving Time in partiton:"+partition+"\tis:"+solving_t);

					BufferedWriter fw2 = new BufferedWriter(fichier2);
					
					for(int i=0;i<nb_DataProd;i++){
						for(int j=0;j<nb_DataHost;j++){
							//System.out.println("Name:"+lp.getNumVar(i*nb_DataHost+j)+"     Val:"+x[(i*nb_DataHost+j)]);
							fw2.write(String.valueOf(x[(i*nb_DataHost+j)])+"\t");
						}
						fw2.write("\n");
					}			
					fw2.close();		
					fw.close();
				}catch (FileNotFoundException e){
					e.printStackTrace();
				}catch (IOException e){
					e.printStackTrace();
				}
				
				
				System.out.println(cplex.getStatus());
				
				cplex.end();
				return true;
			}else{
				System.out.println("Problem doesn't solving!, may there is insuffusent work memory!  partition: "+partition);
			}
			cplex.end();
			} catch (IloException e) {
				System.err.println("Concert exception caught: " + e);
			}
		return false;
		
	}
	
}
