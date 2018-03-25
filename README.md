# iFogSimWithDataPlacement

This is an extension of iFogSim that makes it possible to simulate and evaluate data placement strategies in context of Fog computing and IoT. This extension uses two external tools:
1- Cplex to compute the data placement which is formulated as a Generalized Assignement Problem (GAP)
2- Metis to partition a graph which models the simulated infrastructure into k-partitions. This is in order to conceive several data placement sub problems hence reducing the overall problem complexity.

Next, various configurations and setups to reuse this extension, are shown.
1- Clone this repository in your machine.
2- Install Cplex: 
3- Add the Cplex Jar:
4- Install Metis.
