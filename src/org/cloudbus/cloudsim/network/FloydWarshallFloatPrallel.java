/*
 * @(#)FloydWarshall.java	ver 1.2  6/20/2005
 *
 * Modified by Weishuai Yang (wyang@cs.binghamton.edu).
 * Originally written by Rahul Simha
 *
 */

package org.cloudbus.cloudsim.network;

import org.fog.examples.DataPlacement;

/**
 * FloydWarshall algorithm to calculate all pairs delay and predecessor matrix.
 * 
 * @author 
 * @author Mohammed Islam NAAS
 * @version 
 * @since 
 */

public class FloydWarshallFloatPrallel {

	private float max_value = Float.MAX_VALUE;

	private int[][] Pk;

	private float[][] arrayResults;

    Boolean lock = new Boolean(true);
    
    int arraySize = -1;

    static { 
    	System.load(DataPlacement.FloydPath+"libFloydWarshallFloatPrallel.so");
    	//System.load("~/Bureau/libFloydWarshallFloatPrallel.so");
    	}

    public void sendArrayResults(float[][] results){
        arraySize = results.length;

        arrayResults = new float[results.length][];
        System.arraycopy(results,0,arrayResults,0,arraySize);
    }
 
    public void sendArrayResultsFlow(int[][] results){
        //arraySize = results.length;

        Pk = new int[results.length][];
        System.arraycopy(results,0,Pk,0,arraySize);
    }

    public native void floyd(int n, int m, float[][] adjMatrix, Boolean lock);

	/**
	 * calculates all pairs delay
	 * 
	 * @param adjMatrix original delay matrix
	 * @return all pairs delay matrix
	 */
	
	public float[][] allPairsShortestPaths(){
		return arrayResults;
	}
		
	/**
	 * gets predecessor matrix
	 * 
	 * @return predecessor matrix
	 */
	
	public int[][] getFlowMatrix(){
		return Pk;
	}

	public void displayArrayFloat(int nb_elements, float[][] tab_dist){
        for(int i=0; i<nb_elements; i++){
            System.out.println();
            for(int j=0; j<nb_elements;j++){
                if(tab_dist[i][j]==Float.MAX_VALUE){
                    System.out.print("Inf\t");
                }else{
                    System.out.print(tab_dist[i][j]+"\t");
                }
                
            }
        }
    }

    public void displayArrayInt(int nb_elements, int[][] tab_dist){
        for(int i=0; i<nb_elements; i++){
            System.out.println();
            for(int j=0; j<nb_elements;j++){
                if(tab_dist[i][j]==Float.MAX_VALUE){
                    System.out.print("Inf\t");
                }else{
                    System.out.print(tab_dist[i][j]+"\t");
                }
                
            }
        }
    }

}
