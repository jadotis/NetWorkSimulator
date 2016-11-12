#Welcome to the README for the Network Simulator:

In order to compile the files, simply type "make" at a terminal.
This will run the commands:
javac Project.java
java Project

In addition this will run the file.

------------------------------------------------------------------------------
Upon Running you will be prompted for parameters to the simulator:
-Feel free to have fun:
Recommended:
nsim = 10+
loss =  0.2
corrupt = 0.2
delay = 10 (HIGHLY RECOMMENDED)
Interval = 15 (HIGHLY RECOMMENDED)
trace = 0,1,2 (Values correspond to the Event details printed out)
		(2 being a complete trace).


The results of the packets will be printed to a "OutputFile".
------------------------------------------------------------------------------
Features of the Simulator:

-For a Finite State Machine: Please Email jadotis@bu.edu
-The simulator will randomly generate packets a-z of 15 characters.
Depending on the parameters input, the packets generated may be corupted by 
the simulator. Corrupted packets will start with the "?" character and corrupt
acknowledgements will start have corrupted Sequence and/or Acknowledgement 
values.
-Upon exiting the simulator a printout of the traces (With a trace value = 2)
will print out the events. The statistics will always be printed out and will
calculate the messages lost, corrupted, sent, and received. In addition, the 
RoundTripTime will be calculated as well. This should be a value that is 
directly proportional to the amount of messages lost/corrupted.

------------------------------------------------------------------------------
Contact:
James Otis
jadotis@bu.edu
Boston University
Computer Science 2016

