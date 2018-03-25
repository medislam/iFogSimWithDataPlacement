/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network;

import java.util.Iterator;

import org.fog.examples.DataPlacement;

/**
 * This class represents an delay-topology storing every distance between connected nodes
 * 
 * @author Thomas Hohnstein
 * @since CloudSim Toolkit 1.0
 */
public class DelayMatrix_Float {

	/**
	 * matrix holding delay information between any two nodes
	 */
	public static float[][] mDelayMatrix = null;
	public static int[][] mFlowMatrix = null;
	public static float[][] mAdjacenceMatrix = null;

	/**
	 * number of nodes in the distance-aware-topology
	 */
	public static int mTotalNodeNum = 0;
	public static int mTotalArcNum = 0;

	/**
	 * private constructor to ensure that only an correct initialized delay-matrix could be created
	 */
	@SuppressWarnings("unused")
	private DelayMatrix_Float() {
	};

	/**
	 * this constructor creates an correct initialized Float-Delay-Matrix
	 * 
	 * @param graph the topological graph as source-information
	 * @param directed true if an directed matrix should be computed, false otherwise
	 */
	public DelayMatrix_Float(TopologicalGraph graph, boolean directed) {

		// lets preinitialize the Delay-Matrix
		System.out.println("Create short path matrix...");
		createDelayMatrix(graph, directed);

		// now its time to calculate all possible connection-delays
		System.out.println("Shortest Path computations...");
		calculateShortestPath();
	}

	/**
	 * @param srcID the id of the source-node
	 * @param destID the id of the destination-node
	 * @return the delay-count between the given two nodes
	 */
	public float getDelay(int srcID, int destID) {
		// check the nodeIDs against internal array-boundarys
		if (srcID > mTotalNodeNum || destID > mTotalNodeNum) {
			throw new ArrayIndexOutOfBoundsException("srcID or destID is higher than highest stored node-ID!");
		}

		return mDelayMatrix[srcID][destID];
	}

	/**
	 * creates all internal necessary network-distance structures from the given graph for
	 * similarity we assume all kommunikation-distances are symmetrical thus leads to an undirected
	 * network
	 * 
	 * @param graph this graph contains all node and link information
	 * @param directed defines to preinitialize an directed or undirected Delay-Matrix!
	 */
	private void createDelayMatrix(TopologicalGraph graph, boolean directed) {

		// number of nodes inside the network
		mTotalNodeNum = graph.getNumberOfNodes();
		mTotalArcNum = graph.getNumberOfLinks();


		mDelayMatrix = new float[mTotalNodeNum][mTotalNodeNum];
		mAdjacenceMatrix = new float[mTotalNodeNum][mTotalNodeNum];
		mFlowMatrix = new int[mTotalNodeNum][mTotalNodeNum];
		// cleanup the complete distance-matrix with "0"s
		for (int row = 0; row < mTotalNodeNum; ++row) {
			for (int col = 0; col < mTotalNodeNum; ++col) {
				mDelayMatrix[row][col] = Float.MAX_VALUE;
				mFlowMatrix[row][col] = -1;
			}
		}

		Iterator<TopologicalLink> itr = graph.getLinkIterator();

		TopologicalLink edge;
		while (itr.hasNext()) {
			edge = itr.next();

			mDelayMatrix[edge.getSrcNodeID()][edge.getDestNodeID()] = edge.getLinkDelay();

			if (!directed) {
				// according to aproximity of symmetry to all kommunication-paths
				mDelayMatrix[edge.getDestNodeID()][edge.getSrcNodeID()] = edge.getLinkDelay();
			}

		}
		mAdjacenceMatrix=mDelayMatrix;
	}

	/**
	 * just calculates all pairs shortest paths
	 */
	
	private void calculateShortestPath() {
		
		if(DataPlacement.parallel==false){
			FloydWarshall_Float floyd = new FloydWarshall_Float();
			System.out.println("Initialize...");
			floyd.initialize(mTotalNodeNum);
			
			System.out.println("allPairsShortestPaths in sequential mode...");
			mDelayMatrix = floyd.allPairsShortestPaths(mDelayMatrix);
		
		}else{
			FloydWarshallFloatPrallel floyd = new FloydWarshallFloatPrallel();
			System.out.println("Initialize...");
			
			System.out.println("allPairsShortestPaths in parallel programming mode...");
			floyd.floyd(mTotalNodeNum, mTotalArcNum, mDelayMatrix,true);
			
			mDelayMatrix = floyd.allPairsShortestPaths();
			mFlowMatrix = floyd.getFlowMatrix();
			
//			System.out.println("\nNew Delay Matrix");
//			floyd.displayArrayFloat(mTotalNodeNum, mDelayMatrix);
//			
//			System.out.println("\nNew Flow Matrix");
//			floyd.displayArrayInt(mTotalNodeNum, mFlowMatrix);
			
		}
		
		
		
	}

	/**
	 * this method just creates an string-output from the internal structures... eg. printsout the
	 * delay-matrix...
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("just a simple printout of the distance-aware-topology-class\n");
		buffer.append("delay-matrix is:\n");

		for (int column = 0; column < mTotalNodeNum; ++column) {
			buffer.append("\t" + column);
		}

		for (int row = 0; row < mTotalNodeNum; ++row) {
			buffer.append("\n" + row);

			for (int col = 0; col < mTotalNodeNum; ++col) {
				if (mDelayMatrix[row][col] == Float.MAX_VALUE) {
					buffer.append("\t" + "-");
				} else {
					buffer.append("\t" + mDelayMatrix[row][col]);
				}
			}
		}

		return buffer.toString();
	}

	public float[][] getDelayMatrix() {
		// TODO Auto-generated method stub
		return mDelayMatrix;
	}
	
	public static int[][] getFlowMatrix(){
		return mFlowMatrix;
	}
	
	public static float[][] getAdjacenceMatrix(){
		return mAdjacenceMatrix;
	}

}
