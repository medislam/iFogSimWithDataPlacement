/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.core;

/**
 * Contains various static command tags that indicate a type of action that needs to be undertaken
 * by CloudSim entities when they receive or send events. <b>NOTE:</b> To avoid conflicts with other
 * tags, CloudSim reserves negative numbers, 0 - 299, and 9600.
 * 
 * @author Manzur Murshed
 * @author Rajkumar Buyya
 * @author Anthony Sulistio
 * @since CloudSim Toolkit 1.0
 */
public class CloudSimTags {

	/** Starting constant value for cloud-related tags **/
	private static final int BASE = 0;

	/** Starting constant value for network-related tags **/
	private static final int NETBASE = 100;

	/** Denotes boolean <tt>true</tt> in <tt>int</tt> value */
	public static final int TRUE = 1;

	/** Denotes boolean <tt>false</tt> in <tt>int</tt> value */
	public static final int FALSE = 0;

	/** Denotes the default baud rate for some CloudSim entities */
	public static final int DEFAULT_BAUD_RATE = 9600;

	/** Schedules an entity without any delay */
	public static final double SCHEDULE_NOW = 0.0;

	/** Denotes the end of simulation */
	public static final int END_OF_SIMULATION = -1;

	/**
	 * Denotes an abrupt end of simulation. That is, one event of this type is enough for
	 * {@link CloudSimShutdown} to trigger the end of the simulation
	 */
	public static final int ABRUPT_END_OF_SIMULATION = -2;

	/**
	 * Denotes insignificant simulation entity or time. This tag will not be used for identification
	 * purposes.
	 */
	public static final int INSIGNIFICANT = BASE + 0;

	/** Sends an Experiment object between UserEntity and Broker entity */
	public static final int EXPERIMENT = BASE + 1;

	/**
	 * Denotes a grid resource to be registered. This tag is normally used between
	 * CloudInformationService and CloudResouce entity.
	 */
	public static final int REGISTER_RESOURCE = BASE + 2;

	/**
	 * Denotes a grid resource, that can support advance reservation, to be registered. This tag is
	 * normally used between CloudInformationService and CloudResouce entity.
	 */
	public static final int REGISTER_RESOURCE_AR = BASE + 3;

	/**
	 * Denotes a list of all hostList, including the ones that can support advance reservation. This
	 * tag is normally used between CloudInformationService and CloudSim entity.
	 */
	public static final int RESOURCE_LIST = BASE + 4;

	/**
	 * Denotes a list of hostList that only support advance reservation. This tag is normally used
	 * between CloudInformationService and CloudSim entity.
	 */
	public static final int RESOURCE_AR_LIST = BASE + 5;

	/**
	 * Denotes grid resource characteristics information. This tag is normally used between CloudSim
	 * and CloudResource entity.
	 */
	public static final int RESOURCE_CHARACTERISTICS = BASE + 6;

	/**
	 * Denotes grid resource allocation policy. This tag is normally used between CloudSim and
	 * CloudResource entity.
	 */
	public static final int RESOURCE_DYNAMICS = BASE + 7;

	/**
	 * Denotes a request to get the total number of Processing Elements (PEs) of a resource. This
	 * tag is normally used between CloudSim and CloudResource entity.
	 */
	public static final int RESOURCE_NUM_PE = BASE + 8;

	/**
	 * Denotes a request to get the total number of free Processing Elements (PEs) of a resource.
	 * This tag is normally used between CloudSim and CloudResource entity.
	 */
	public static final int RESOURCE_NUM_FREE_PE = BASE + 9;

	/**
	 * Denotes a request to record events for statistical purposes. This tag is normally used
	 * between CloudSim and CloudStatistics entity.
	 */
	public static final int RECORD_STATISTICS = BASE + 10;

	/** Denotes a request to get a statistical list. */
	public static final int RETURN_STAT_LIST = BASE + 11;

	/**
	 * Denotes a request to send an Accumulator object based on category into an event scheduler.
	 * This tag is normally used between ReportWriter and CloudStatistics entity.
	 */
	public static final int RETURN_ACC_STATISTICS_BY_CATEGORY = BASE + 12;

	/**
	 * Denotes a request to register a CloudResource entity to a regional CloudInformationService
	 * (GIS) entity
	 */
	public static final int REGISTER_REGIONAL_GIS = BASE + 13;

	/**
	 * Denotes a request to get a list of other regional GIS entities from the system GIS entity
	 */
	public static final int REQUEST_REGIONAL_GIS = BASE + 14;

	/**
	 * Denotes request for grid resource characteristics information. This tag is normally used
	 * between CloudSim and CloudResource entity.
	 */
	public static final int RESOURCE_CHARACTERISTICS_REQUEST = BASE + 15;

	/** This tag is used by an entity to send ping requests */
	public static final int INFOPKT_SUBMIT = NETBASE + 5;

	/** This tag is used to return the ping request back to sender */
	public static final int INFOPKT_RETURN = NETBASE + 6;

	/**
	 * Denotes the return of a Cloudlet back to sender. This tag is normally used by CloudResource
	 * entity.
	 */
	public static final int CLOUDLET_RETURN = BASE + 20;

	/**
	 * Denotes the submission of a Cloudlet. This tag is normally used between CloudSim User and
	 * CloudResource entity.
	 */
	public static final int CLOUDLET_SUBMIT = BASE + 21;

	/**
	 * Denotes the submission of a Cloudlet with an acknowledgement. This tag is normally used
	 * between CloudSim User and CloudResource entity.
	 */
	public static final int CLOUDLET_SUBMIT_ACK = BASE + 22;

	/** Cancels a Cloudlet submitted in the CloudResource entity. */
	public static final int CLOUDLET_CANCEL = BASE + 23;

	/** Denotes the status of a Cloudlet. */
	public static final int CLOUDLET_STATUS = BASE + 24;

	/** Pauses a Cloudlet submitted in the CloudResource entity. */
	public static final int CLOUDLET_PAUSE = BASE + 25;

	/**
	 * Pauses a Cloudlet submitted in the CloudResource entity with an acknowledgement.
	 */
	public static final int CLOUDLET_PAUSE_ACK = BASE + 26;

	/** Resumes a Cloudlet submitted in the CloudResource entity. */
	public static final int CLOUDLET_RESUME = BASE + 27;

	/**
	 * Resumes a Cloudlet submitted in the CloudResource entity with an acknowledgement.
	 */
	public static final int CLOUDLET_RESUME_ACK = BASE + 28;

	/** Moves a Cloudlet to another CloudResource entity. */
	public static final int CLOUDLET_MOVE = BASE + 29;

	/**
	 * Moves a Cloudlet to another CloudResource entity with an acknowledgement.
	 */
	public static final int CLOUDLET_MOVE_ACK = BASE + 30;

	/**
	 * Denotes a request to create a new VM in a Datacentre With acknowledgement information sent by
	 * the Datacentre
	 */
	public static final int VM_CREATE = BASE + 31;

	/**
	 * Denotes a request to create a new VM in a Datacentre With acknowledgement information sent by
	 * the Datacentre
	 */
	public static final int VM_CREATE_ACK = BASE + 32;

	/**
	 * Denotes a request to destroy a new VM in a Datacentre
	 */
	public static final int VM_DESTROY = BASE + 33;

	/**
	 * Denotes a request to destroy a new VM in a Datacentre
	 */
	public static final int VM_DESTROY_ACK = BASE + 34;

	/**
	 * Denotes a request to migrate a new VM in a Datacentre
	 */
	public static final int VM_MIGRATE = BASE + 35;

	/**
	 * Denotes a request to migrate a new VM in a Datacentre With acknowledgement information sent
	 * by the Datacentre
	 */
	public static final int VM_MIGRATE_ACK = BASE + 36;

	/**
	 * Denotes an event to send a file from a user to a datacenter
	 */
	public static final int VM_DATA_ADD = BASE + 37;

	/**
	 * Denotes an event to send a file from a user to a datacenter
	 */
	public static final int VM_DATA_ADD_ACK = BASE + 38;

	/**
	 * Denotes an event to remove a file from a datacenter
	 */
	public static final int VM_DATA_DEL = BASE + 39;

	/**
	 * Denotes an event to remove a file from a datacenter
	 */
	public static final int VM_DATA_DEL_ACK = BASE + 40;

	/**
	 * Denotes an internal event generated in a PowerDatacenter
	 */
	public static final int VM_DATACENTER_EVENT = BASE + 41;

	/**
	 * Denotes an internal event generated in a Broker
	 */
	public static final int VM_BROKER_EVENT = BASE + 42;

	public static final int Network_Event_UP = BASE + 43;

	public static final int Network_Event_send = BASE + 44;

	public static final int RESOURCE_Register = BASE + 45;

	public static final int Network_Event_DOWN = BASE + 46;

	public static final int Network_Event_Host = BASE + 47;

	public static final int NextCycle = BASE + 48;

	/** Private Constructor */
	private CloudSimTags() {
		throw new UnsupportedOperationException("CloudSim Tags cannot be instantiated");
	}

	public static String getTagString(int tag){
		switch (tag){
		case 0:
			return "False or Insignified!";
		case 1:
			return "True or EXPERIMENT!";
		case -1:
			return "END_OF_SIMULATION";
		case -2:
			return "ABRUPT_END_OF_SIMULATION";
		case BASE + 2:
			return "REGISTER_RESOURCE";
		case BASE + 3:
			return "REGISTER_RESOURCE_AR";
		case BASE + 4:
			return "RESOURCE_LIST";
		case BASE + 5:
			return "RESOURCE_AR_LIST";
		case BASE + 6:
			return "RESOURCE_CHARACTERISTICS";
		case BASE + 7:
			return "RESOURCE_DYNAMICS";
		case BASE + 8:
			return "RESOURCE_NUM_PE";
		case BASE + 9:
			return "RESOURCE_NUM_FREE_PE";
		case BASE + 10:
			return "RECORD_STATISTICS";
		case BASE + 11:
			return "RETURN_STAT_LIST";
		case BASE + 12:
			return "RETURN_ACC_STATISTICS_BY_CATEGORY";
		case BASE + 13:
			return "REGISTER_REGIONAL_GIS";
		case BASE + 14:
			return "REQUEST_REGIONAL_GIS";
		case BASE + 15:
			return "RESOURCE_CHARACTERISTICS_REQUEST";
		case NETBASE + 5:
			return "INFOPKT_SUBMIT";
		case NETBASE + 6:
			return "INFOPKT_RETURN";
		case BASE + 20:
			return "CLOUDLET_RETURN";
		case BASE + 21:
			return "CLOUDLET_SUBMIT";
		case BASE + 22:
			return "CLOUDLET_SUBMIT_ACK";
		case BASE + 23:
			return "CLOUDLET_CANCEL";
		case BASE + 24:
			return "CLOUDLET_STATUS";
		case BASE + 25:
			return "CLOUDLET_PAUSE";
		case BASE + 27:
			return "CLOUDLET_RESUME";
		case BASE + 28:
			return "CLOUDLET_RESUME_ACK";
		case BASE + 29:
			return "CLOUDLET_MOVE";
		case BASE + 30:
			return "CLOUDLET_MOVE_ACK";
		case BASE + 31:
			return "VM_CREATE";
		case BASE + 32:
			return "VM_CREATE_ACK";
		case BASE + 33:
			return "VM_DESTROY";
		case BASE + 34:
			return "VM_DESTROY_ACK";
		case BASE + 35:
			return "VM_MIGRATE";
		case BASE + 36:
			return "VM_MIGRATE_ACK";
		case BASE + 37:
			return "VM_DATA_ADD";
		case BASE + 38:
			return "VM_DATA_ADD_ACK";
		case BASE + 39:
			return "VM_DATA_DEL";
		case BASE + 41:
			return "VM_DATACENTER_EVENT";
		case BASE + 42:
			return "VM_BROKER_EVENT";
		case BASE + 43:
			return "Network_Event_UP";
		case BASE + 44:
			return "Network_Event_send";
		case BASE + 45:
			return "RESOURCE_Register";
		case BASE + 46:
			return "Network_Event_DOWN";
		case BASE + 47:
			return "Network_Event_Host";
		case BASE + 48:
			return "NextCycle";	
		case 50:
			return "50";
		case 51:
			return "TUPLE_ARRIVAL";
		case 52:
			return "LAUNCH_MODULE";
		case 53:
			return "RELEASE_OPERATOR";
		case 54:
			return "SENSOR_JOINED";
		case 55:
			return "TUPLE_ACK";
		case 56:
			return "APP_SUBMIT";
		case 57:
			return "CALCULATE_INPUT_RATE";
		case 58:
			return "CALCULATE_UTIL";
		case 59:
			return "UPDATE_RESOURCE_USAGE";
		case 60:
			return "UPDATE_TUPLE_QUEUE";
		case 61:
			return "TUPLE_FINISHED";
		case 62:
			return "ACTIVE_APP_UPDATE";
		case 63:
			return "CONTROLLER_RESOURCE_MANAGE";
		case 64:
			return "ADAPTIVE_OPERATOR_REPLACEMENT";
		case 65:
			return "GET_RESOURCE_USAGE";
		case 66:
			return "RESOURCE_USAGE";
		case 67:
			return "CONTROL_MSG_ARRIVAL";
		case 68:
			return "UPDATE_NORTH_TUPLE_QUEUE";
		case 69:
			return "UPDATE_SOUTH_TUPLE_QUEUE";
		case 70:
			return "ACTUATOR_JOINED";
		case 71:
			return "STOP_SIMULATION";
		case 72:
			return "SEND_PERIODIC_TUPLE";
		case 73:
			return "LAUNCH_MODULE_INSTANCE";
		case 74:
			return "RESOURCE_MGMT or INITIALIZE_SENSOR";
		case 75:
			return "EMIT_TUPLE";
		case 76:
			return "STORAGE_TUPLE";
		case 77:
			return "PROCESS_TUPLE";
		default:	
			return String.valueOf(tag);
		
		}
	}
}
