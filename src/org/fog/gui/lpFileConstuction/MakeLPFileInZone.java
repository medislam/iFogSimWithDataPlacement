package org.fog.gui.lpFileConstuction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.fog.examples.DataPlacement;

public class MakeLPFileInZone{
	public  static int nb_DataHost;
	public  static int nb_DataProd;
	public  static int nb_DataCons;
	
	public  static int base;
	
	private static final int nb_thread = 32;
	
	public  static float [][] write_basis_laenty; 
	public  static float [][] read_basis_laenty;
	
	public  static int [][] consProd_matrix;
	
	public  static long [] freeCapacity;
	public  static float [] dataSize;
	
	private float [][] factors;
	
	
	public MakeLPFileInZone(int nb_GW,int zone) throws IOException, InterruptedException{
		intializeDataActorInZone(nb_GW,zone);
		initializeWriteLatencyMatrixInZone(nb_GW,zone);
		initializeReadLatencyMatrixInZone(nb_GW,zone);
		initializeConsProdMatrixInZone(nb_GW,zone);
		initializeFreeCapacityInZone(nb_GW,zone);
		initializeDataSizeInZone(nb_GW,zone);
		
		contructionLpFileInZone(nb_GW,zone);
		//printWriteLatency();
		//printReadLatency();
		//printConsProdMatrix();
		//printFreeCapacity();
		//printDataSize();
	}

	private boolean contructionLpFileInZone(int nb_GW,int zone) throws IOException, InterruptedException{
		equationInZone(nb_GW,zone);
		constraintsInZone(nb_GW,zone);
		boundsInZone(nb_GW,zone);
		generalInZone(nb_GW,zone);
		endInZone(nb_GW,zone);
		
		return true;
	}
	
	private boolean checkSolution(){
		long fc=0;
		long ds=0;
		
		for(int i=0;i<nb_DataHost; i++){
			fc=+freeCapacity[i];
		}
		
		for(int j=0;j<nb_DataProd; j++){
			ds=(long) +dataSize[j];
		}
		
		if(ds>fc){
			//System.out.println("There is no solution! insufusent storage capacity");
			return false;
		}
		return true;
	}
	
		
	private void equationInZone(int nb_GW, int zone) throws IOException, InterruptedException{
		FindFactorsThreadInZone thrd;
		
		if(nb_DataProd<nb_thread*nb_thread){
			//just one thread
			//System.out.println("Nb Thread is :"+1);
			int begin=0;
			int end=nb_DataProd;
			thrd = new FindFactorsThreadInZone("1", begin, end);
			thrd.start();
			////System.out.println("Thread "+1+"  is started!");
			thrd.join();
			////System.out.println("Thread "+1+"  is joined!");
			factors = thrd.getFactInZone();
		}else{
			int plane = (int)nb_DataProd/(int)nb_thread;
			FindFactorsThreadInZone [] threadTable = new FindFactorsThreadInZone[nb_thread];
			//System.out.println("Nb Threads is :"+nb_thread);
			float [][] fac = new float [nb_DataProd][nb_DataHost];
			
			for(int nb_thrd=0;nb_thrd<nb_thread;nb_thrd++){
				int begin = nb_thrd * plane;
				int end;
				if(nb_thrd!=nb_thread-1){
					end = (nb_thrd+1)*plane;
				}else{
					end = nb_DataProd;
				}
				thrd = new FindFactorsThreadInZone(String.valueOf(nb_thrd), begin, end);
				threadTable[nb_thrd] = thrd;
				thrd.start();
				
				////System.out.println("Thread "+nb_thrd+"  is started!");
			}
			
			for(int nb_thrd=0;nb_thrd<nb_thread;nb_thrd++){
				thrd = threadTable[nb_thrd];
				thrd.join();
				////System.out.println("Thread "+nb_thrd+"  is joined!");
			}
			
			for(int nb_thrd=0;nb_thrd<nb_thread;nb_thrd++){
				int begin = nb_thrd * plane;
				int end;
				if(nb_thrd!=nb_thread-1){
					end = (nb_thrd+1)*plane;
				}else{
					end = nb_DataProd;
				}
				thrd = threadTable[nb_thrd];
				int indLine =0;
				//System.out.println("Factors of :"+nb_thrd);
				
				for(int row = begin; row<end;row++){
					for(int col=0;col<nb_DataHost;col++){
						fac[row][col] = thrd.getFactInZone()[indLine][col];
						//fac[row][col] = 300;
					}
					indLine++;
				}
			}
			factors=fac;
		}
		
		FileWriter lpFile = new FileWriter(nb_GW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp");
		try {
			BufferedWriter fw = new BufferedWriter(lpFile);
			fw.write("Minimize\n");
			for (int i = 0; i < nb_DataProd; i++){
				for (int j = 0; j < nb_DataHost; j++){
					fw.write(factors[i][j]+" x"+i+"_"+j+" + ");
				}
			 }

			fw.write("\n");
			
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void constraintsInZone(int nb_GW,int zone) throws IOException{
		FileWriter lpFile = new FileWriter(nb_GW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", true);
		try {
			BufferedWriter fw = new BufferedWriter(lpFile);
			fw.write("Subject To\n");
			
			for (int i = 0; i < nb_DataProd;i++){
				fw.write("  ");
		 		for (int j = 0; j < nb_DataHost; j++){
		 			fw.write("x"+i+"_"+j);		 
		 			if (j<(nb_DataHost-1)){
		 				fw.write(" + ");
		 			}
		 		} 
		 		fw.write(" = 1\n");
		 	 }
			
			for (int j = 0; j < nb_DataHost;j++){
				fw.write("  ");
		 		for (int i = 0; i < nb_DataProd; i++){
		 			fw.write(dataSize[i]+" x"+i+"_"+j);		 
		 			if (i<(nb_DataProd-1)){
		 				fw.write(" + ");
		 			}

		 		}
		 		fw.write(" <= "+freeCapacity[j]+"\n");
		 	}
			
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	private void boundsInZone(int nb_GW,int zone) throws IOException{
		FileWriter lpFile = new FileWriter(nb_GW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", true);
		try {
			BufferedWriter fw = new BufferedWriter(lpFile);
			fw.write("Bounds\n");

			for(int i=0;i<nb_DataProd;i++){
				for(int j=0;j<nb_DataHost;j++){
					fw.write("0 <= x"+i+"_"+j+" <=1\n");
				}
			}

			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void generalInZone(int nb_GW,int zone) throws IOException{
		FileWriter lpFile = new FileWriter(nb_GW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", true);
		try {
			BufferedWriter fw = new BufferedWriter(lpFile);
			
			fw.write("General\n");
			for(int i=0;i<nb_DataProd;i++){
				for(int j=0;j<nb_DataHost;j++){
					fw.write("x"+i+"_"+j+"\n");
				}
			}
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void endInZone(int nb_GW,int zone) throws IOException{
		FileWriter lpFile = new FileWriter(nb_GW+"cplexZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".lp", true);
		try {
			BufferedWriter fw = new BufferedWriter(lpFile);
			fw.write("End\n");
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void intializeDataActorInZone(int nb_GW,int zone) throws FileNotFoundException{
		FileReader fichier = new FileReader(nb_GW+"dataActorsZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line = in.readLine();
			String[] splited = line.split("\t");
			
			nb_DataHost = Integer.valueOf(splited[0]);
			nb_DataProd = Integer.valueOf(splited[1]);
			nb_DataCons = Integer.valueOf(splited[2]);
			base = Integer.valueOf(splited[3]);
			
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println("nb_DataHost:"+nb_DataHost);
		//System.out.println("nb_DataProd:"+nb_DataProd);
		//System.out.println("nb_DataCons:"+nb_DataCons);
		//System.out.println("Exchange unit:"+base);
		
	}
	
	private void initializeWriteLatencyMatrixInZone(int nb_GW,int zone) throws FileNotFoundException{
		float [][] writeDelay = new float [nb_DataHost][nb_DataProd];
		
		FileReader fichier = new FileReader(nb_GW+"writeDelayZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line =null;
			int row = 0;
	
			while((line = in.readLine()) != null){
				String[] splited = line.split("\t");
				int col = 0;

				for(String val : splited){
					writeDelay[row][col] = Float.valueOf(val);
					col++;
				}
				row++;
			}
			in.close();
			
			write_basis_laenty = writeDelay;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private void initializeReadLatencyMatrixInZone(int nb_GW,int zone) throws FileNotFoundException{
		FileReader fichier = new FileReader(nb_GW+"readDelayZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		float [][] readDelay = new float [nb_DataHost][nb_DataCons];
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line =null;
			int row = 0;
	
			while((line = in.readLine()) != null){
				String[] splited = line.split("\t");
				int col = 0;
				for(String val : splited){
					readDelay[row][col] = Float.valueOf(val);
					col++;
				}
				row++;
			}
			in.close();
			read_basis_laenty = readDelay;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeConsProdMatrixInZone(int nb_GW,int zone) throws FileNotFoundException{
		FileReader fichier = new FileReader(nb_GW+"consProdZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		int [][] consProd = new int [nb_DataProd][nb_DataCons];
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line =null;
			int row = 0;
	
			while((line = in.readLine()) != null){
				String[] splited = line.split("\t");
				int col = 0;
				for(String val : splited){
					if(Integer.valueOf(val) == 1){
						consProd[row][col] = 1;
					}else{
						consProd[row][col] = 0;
					}	
					col++;
				}
				row++;
			}
			in.close();
			consProd_matrix = consProd;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	private void initializeFreeCapacityInZone(int nb_GW,int zone) throws FileNotFoundException{
		FileReader fichier = new FileReader(nb_GW+"freeCapacityZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		long [] free = new long [nb_DataHost];
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line = in.readLine();
	
			String[] splited = line.split("\t");
			int i =0;
			for(String val : splited){
				free[i]= Long.valueOf(val);
				i++;
			}

			in.close();
			freeCapacity = free;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeDataSizeInZone(int nb_GW,int zone) throws FileNotFoundException{
		FileReader fichier = new FileReader(nb_GW+"dataSizeZone"+zone+"_"+DataPlacement.nb_DataCons_By_DataProd+".txt");
		float [] size = new float [nb_DataProd];
		BufferedReader in = null;
		try{
			in = new BufferedReader (fichier);
			String line = in.readLine();
			String[] splited = line.split("\t");
			int i =0;
			for(String val : splited){
				if(val.equals("null")){
					size[i]= 0;
				}else{
					size[i]= Float.valueOf(val);
				}
				i++;
			}
			in.close();
			
			dataSize = size;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printWriteLatency(){
		//System.out.println("\nprintWriteLatency");
		for(int i=0;i<nb_DataProd;i++){
			for(int j=0;j<nb_DataHost;j++){
				//System.out.print(write_basis_laenty[i][j]+"\t");
			}
			//System.out.println();
		}
	}
	
	private void printReadLatency(){
		//System.out.println("\nprintReadLatency");
		for(int i=0;i<nb_DataCons;i++){
			for(int j=0;j<nb_DataHost;j++){
				//System.out.print(read_basis_laenty[i][j]+"\t");
			}
			//System.out.println();
		}
	}
	
	private void printConsProdMatrix(){
		//System.out.println("\nprintConsProdMatrix");
		for(int i=0;i<nb_DataProd;i++){
			for(int j=0;j<nb_DataCons;j++){
				//System.out.print(consProd_matrix[i][j]+"\t");
			}
			//System.out.println();
		}
	}
	
	private void printFreeCapacity(){
		//System.out.println("\nprintFreeCapacity");
		for(int i=0;i<nb_DataHost;i++){
			//System.out.print(freeCapacity[i]+"\t");
		}
		//System.out.println();
	}
	
	private void printDataSize(){
		//System.out.println("\nprintDataSize");
		for(int i=0;i<nb_DataProd;i++){
			//System.out.print(dataSize[i]+"\t");
		}
		//System.out.println();
	}
}

