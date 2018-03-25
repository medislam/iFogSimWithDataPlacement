package org.fog.gui.lpFileConstuction;



import java.io.IOException;

import org.fog.examples.Log;



public class FindFactorsThreadInPartition extends Thread{
	
	private  float [][] fact;
	private int begin;
	private int end;
	public FindFactorsThreadInPartition(String name, int begin, int end){
		super(name);
		this.begin=begin;
		this.end=end;
	}
	
	
	public float[][] getFactInPartition(){
		return this.fact;
	}
	
	public void run(){
		int x;
		float [][] fac = new float [end-begin][MakeLPFileInPartition.nb_DataHost];
		int indLine = 0;
//		try {
//			//Log.writeLog("Thread "+super.getName()+"   begin:"+begin+"    end:"+end);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		for(int i=begin;i<end;i++){
			x=(int) (((float)MakeLPFileInPartition.dataSize[i])/((float)MakeLPFileInPartition.base));
			if((MakeLPFileInPartition.dataSize[i])%(MakeLPFileInPartition.base)!=0){
				x++;
			}

			for (int j = 0; j < MakeLPFileInPartition.nb_DataHost;j++){
				float readDelay = 0 ;
				float writeDelay = (MakeLPFileInPartition.write_basis_laenty[j][i])*x;
				for (int k = 0; k < MakeLPFileInPartition.nb_DataCons;k++){
					readDelay = readDelay + (x * MakeLPFileInPartition.read_basis_laenty[j][k] * MakeLPFileInPartition.consProd_matrix[i][k]);
				}
				fac[indLine][j]=readDelay + writeDelay;	
			}
			indLine++;
		}
		fact = fac;
	}
	
}


















