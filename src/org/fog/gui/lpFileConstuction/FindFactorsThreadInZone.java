package org.fog.gui.lpFileConstuction;



import java.io.IOException;

import org.fog.examples.Log;



public class FindFactorsThreadInZone extends Thread{
	
	private  float [][] fact;
	private int begin;
	private int end;
	public FindFactorsThreadInZone(String name, int begin, int end){
		super(name);
		this.begin=begin;
		this.end=end;
	}
	
	
	public float[][] getFactInZone(){
		return this.fact;
	}
	
	public void run(){
		int x;
		float [][] fac = new float [end-begin][MakeLPFileInZone.nb_DataHost];
		int indLine = 0;
//		try {
//			//Log.writeLog("Thread "+super.getName()+"   begin:"+begin+"    end:"+end);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		for(int i=begin;i<end;i++){
			x=(int) (((float)MakeLPFileInZone.dataSize[i])/((float)MakeLPFileInZone.base));
			if((MakeLPFileInZone.dataSize[i])%(MakeLPFileInZone.base)!=0){
				x++;
			}

			for (int j = 0; j < MakeLPFileInZone.nb_DataHost;j++){
				float readDelay = 0 ;
				float writeDelay = (MakeLPFileInZone.write_basis_laenty[j][i])*x;
				for (int k = 0; k < MakeLPFileInZone.nb_DataCons;k++){
					readDelay = readDelay + (x * MakeLPFileInZone.read_basis_laenty[j][k] * MakeLPFileInZone.consProd_matrix[i][k]);
				}
				fac[indLine][j]=readDelay + writeDelay;	
			}
			indLine++;
		}
		fact = fac;
	}
	
}


















