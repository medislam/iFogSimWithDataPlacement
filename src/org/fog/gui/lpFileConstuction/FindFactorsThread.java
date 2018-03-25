package org.fog.gui.lpFileConstuction;



import java.io.IOException;

import org.fog.examples.Log;



public class FindFactorsThread extends Thread{
	
	private  float [][] fact;
	private int begin;
	private int end;
	public FindFactorsThread(String name, int begin, int end){
		super(name);
		this.begin=begin;
		this.end=end;
	}
	
	
	public float[][] getFact(){
		return this.fact;
	}
	
	public void run(){
		int x;
		float [][] fac = new float [end-begin][MakeLPFile.nb_DataHost];
		int indLine = 0;
//		try {
//			//Log.writeLog("Thread "+super.getName()+"   begin:"+begin+"    end:"+end);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		for(int i=begin;i<end;i++){
			x=(int) (((float)MakeLPFile.dataSize[i])/((float)MakeLPFile.base));
			if((MakeLPFile.dataSize[i])%(MakeLPFile.base)!=0){
				x++;
			}

			for (int j = 0; j < MakeLPFile.nb_DataHost;j++){
				float readDelay = 0 ;
				float writeDelay = (MakeLPFile.write_basis_laenty[j][i])*x;
				for (int k = 0; k < MakeLPFile.nb_DataCons;k++){
					readDelay = readDelay + (x * MakeLPFile.read_basis_laenty[j][k] * MakeLPFile.consProd_matrix[i][k]);
				}
				fac[indLine][j]=readDelay + writeDelay;	
			}
			indLine++;
		}
		fact = fac;
	}
	
}


















