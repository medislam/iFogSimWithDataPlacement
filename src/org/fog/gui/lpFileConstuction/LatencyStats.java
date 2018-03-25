package org.fog.gui.lpFileConstuction;

public class LatencyStats {
	public static double Overall_read_latency=0;
	public static double Overall_write_latency=0;
	public static double Overall_latency=0;
	
	public static void add_Overall_Letency(double latency){
		Overall_latency= latency;
	}
	public static void add_Overall_read_Letency(double latency){
		Overall_read_latency= latency;
	}
	public static void add_Overall_write_Letency(double latency){
		Overall_write_latency= latency;
	}
	
	public static double getOverall_Latency(){
		return Overall_latency;
	}
	
	public static double getOverall_read_Latency(){
		return Overall_read_latency;
	}
	
	public static double getOverall_write_Latency(){
		return Overall_write_latency;
	}
	
	public static void reset_Overall_Letency(){
		Overall_latency= 0;
	}
	public static void reset_Overall_read_Letency(){
		Overall_read_latency= 0;
	}
	public static void reset_Overall_write_Letency(){
		Overall_write_latency= 0;
	}
	
}
