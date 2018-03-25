package org.Results;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.fog.examples.DataPlacement;


/**
 * 
 * @author islam
 * this class contains methods to save various simulations results as latency time, cost, energy and so on
 * 
 *
 */


public class SaveResults {
	
	public SaveResults() {
		
	}
	
	public static void saveLatencyTimes(int dataConsPerDataProd, String storageMode, int nb_z, double write, double read,
			double overall) throws IOException {
		
		System.out.println("Saving Latency time information");
		FileWriter fichier = new FileWriter("Stats/latencyStats" + DataPlacement.nb_HGW+"_"+DataPlacement.nb_DataCons_By_DataProd, true);
		try {
			BufferedWriter fw = new BufferedWriter(fichier);

			if (storageMode.equals(DataPlacement.CloudStorage)) {
				fw.write("**********************************************************************************\n");
			}
			fw.write("DataCons/DataProd: " + dataConsPerDataProd + "\n");
			fw.write("StorageMode: " + storageMode + "\n");
			if (nb_z != -1) {
				fw.write("nb_zone: " + nb_z + "\n");
			}
			fw.write("Write latency: " + write + "\n");
			fw.write("Read latency: " + read + "\n");
			fw.write("Overall latency: " + overall + "\n");
			fw.write("----------------------------------------------------------------------------------\n");
			fw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
