package org.fog.application.selectivity;

import org.cloudbus.cloudsim.Log;

/**
 * Generates an output tuple for an incoming input tuple with a fixed probability
 * @author Harshit Gupta
 *
 */
public class FractionalSelectivity implements SelectivityModel{

	/**
	 * The fixed probability of output tuple creation per incoming input tuple
	 */
	double selectivity;
	
	int count =1;
	
	double rest =0;
	
	public FractionalSelectivity(double selectivity){
		setSelectivity(selectivity);
	}
	
	@Override
	public double getSelectivity() {
		return selectivity;
	}
	
	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}
	
	@Override
	public boolean canSelect() {
		//System.out.println("Selectivity="+selectivity);
		/* this is for deal with unprecesion float java problem */
		if(rest > 0.0999 && rest < 0.1){
			rest = 0.1;
		}else if(rest > 0.0899 && rest < 0.09){
			rest = 0.09;
		}else if(rest > 0.0799 && rest < 0.08){
			rest = 0.08;
		}else if(rest > 0.0699 && rest < 0.07){
			rest = 0.07;
		}else if(rest > 0.0599 && rest < 0.06){
			rest = 0.06;
		}else if(rest > 0.0499 && rest < 0.05){
			rest = 0.05;
		}else if(rest > 0.0399 && rest < 0.04){
			rest = 0.04;
		}else if(rest > 0.0299 && rest < 0.03){
			rest = 0.03;
		}else if(rest > 0.0199 && rest < 0.02){
			rest = 0.02;
		}else if(rest > 0.0099 && rest < 0.01){
			rest = 0.01;
		}
		
		Log.writeInLogFile("Fraction", "selectivity="+selectivity+"\tcount="+count+"\trest="+rest);
		Log.writeInLogFile("Fraction", "compute="+((selectivity*count)+rest));
		
		if((selectivity*count)+rest >= 0.9999999){
			rest = ((selectivity*count)+rest)%1; 
			count = 1;
			
			
			
			//System.out.println("cout="+count);
			//System.out.println("rest="+rest);
			
			Log.writeInLogFile("Fraction Selectivity", "cout="+count);
			Log.writeInLogFile("Fraction Selectivity", "rest="+rest);
			return true;
		}else{
			count++;
			//System.out.println("cout="+count);
			//System.out.println("rest="+rest);
			
			Log.writeInLogFile("Fraction Selectivity", "cout="+count);
			Log.writeInLogFile("Fraction Selectivity", "rest="+rest);
			return false;
		}
//		if(Math.random() < getSelectivity()) // if the probability condition is satisfied
//			return true;
	}


	@Override
	public double getMeanRate() {
		return getSelectivity(); // the average rate of tuple generation is the fixed probability value
	}
	
	@Override
	public double getMaxRate() {
		return getSelectivity(); // the maximum rate of tuple generation is the fixed probability value
	}
	
	public void resetCount(){
		count=1;
	}
	
}
