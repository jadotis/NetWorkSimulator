import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
	/*
	 * Predefined Constants (static member variables):
	 *
	 * int MAXDATASIZE : the maximum size of the Message data and Packet payload
	 *
	 * int A : a predefined integer that represents entity A int B : a
	 * predefined integer that represents entity B
	 *
	 * Predefined Member Methods:
	 *
	 * void stopTimer(int entity): Stops the timer running at "entity" [A or B]
	 * void startTimer(int entity, double increment): Starts a timer running at
	 * "entity" [A or B], which will expire in "increment" time units, causing
	 * the interrupt handler to be called. You should only call this with A.
	 * void toLayer3(int callingEntity, Packet p) Puts the packet "p" into the
	 * network from "callingEntity" [A or B] void toLayer5(String dataSent)
	 * Passes "dataSent" up to layer 5 double getTime() Returns the current time
	 * in the simulator. Might be useful for debugging. int getTraceLevel()
	 * Returns TraceLevel void printEventList() Prints the current event list to
	 * stdout. Might be useful for debugging, but probably not.
	 * Predefined Classes:
	 *
	 * Message: Used to encapsulate a message coming from layer 5 Constructor:
	 * Message(String inputData): creates a new Message containing "inputData"
	 * Methods: boolean setData(String inputData): sets an existing Message's
	 * data to "inputData" returns true on success, false otherwise String
	 * getData(): returns the data contained in the message Packet: Used to
	 * encapsulate a packet Constructors: Packet (Packet p): creates a new
	 * Packet that is a copy of "p" Packet (int seq, int ack, int check, String
	 * newPayload) creates a new Packet with a sequence field of "seq", an ack
	 * field of "ack", a checksum field of "check", and a payload of
	 * "newPayload" Packet (int seq, int ack, int check) chreate a new Packet
	 * with a sequence field of "seq", an ack field of "ack", a checksum field
	 * of "check", and an empty payload Methods: boolean setSeqnum(int n) sets
	 * the Packet's sequence field to "n" returns true on success, false
	 * otherwise boolean setAcknum(int n) sets the Packet's ack field to "n"
	 * returns true on success, false otherwise boolean setChecksum(int n) sets
	 * the Packet's checksum to "n" returns true on success, false otherwise
	 * boolean setPayload(String newPayload) sets the Packet's payload to
	 * "newPayload" returns true on success, false otherwise int getSeqnum()
	 * returns the contents of the Packet's sequence field int getAcknum()
	 * returns the contents of the Packet's ack field int getChecksum() returns
	 * the checksum of the Packet int getPayload() returns the Packet's payload
	 *
	 */

	/*
	 * Please use the following variables in your routines. int WindowSize : the
	 * window size double RxmtInterval : the retransmission timeout int
	 * LimitSeqNo : when sequence number reaches this value, it wraps around
	 */

	public static final int FirstSeqNo = 0;
	private int WindowSize;
	private double RxmtInterval;
	private int LimitSeqNo;
			/* Begin data Collection */
	private ArrayList<Double> timeSent = new ArrayList<Double>();
	private ArrayList<Double> timeReceived = new ArrayList<Double>();
	private int packetsCorrupted;
	private int packetsSent;
	private int succesfullyReceivedPackets;
		/* End the data Collection */
		/* Data Structures for holding data */
	private Queue<Packet> unconfirmedPackets;
	private Queue<Packet> queuedpackets;
	private Hashtable<Integer, Boolean> packetsConfirmed = new Hashtable<>();
	private Queue<Packet> outOfOrderPackets;
		/*      End       */
		/* VARIABLES FOR KEEPING TRACK OF PACKETS */
	private int nextSequenceNumber = 0;
	private int seqNum = FirstSeqNo;
	private int acksNeeded = WindowSize;
	private int packetsPending = 0;
	private boolean duplicates;     //Checks for packets that have already been received.
	private int expectedSequenceNumber = seqNum;
		/*      End       */
			// This is the constructor. Don't touch!
	public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, int seed,
			int winsize, double delay) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
		WindowSize = winsize;
		LimitSeqNo = winsize * 2;
		RxmtInterval = delay;
	}

	// This routine will be called whenever the upper layer at the sender [A]
	// has a message to send. It is the job of your protocol to insure that
	// the data in such a message is delivered in-order, and correctly, to
	// the receiving upper layer.

	protected void aOutput(Message message)
	{
		if (seqNum == LimitSeqNo) //Wrapped for messages
		{
			seqNum = 0;
		}
		Packet sendingPacket = new Packet(seqNum++, -1, 0, message.getData());
		sendingPacket = initializeChecksum(sendingPacket);
		if (packetsPending < WindowSize)
		{
			if (!queuedpackets.isEmpty())
			{
				packetsSent++;
				Packet nextToSend = queuedpackets.remove();
				packetsPending++;
				sendPacket(nextToSend);
				toLayer3(A, nextToSend);
				queuedpackets.add(sendingPacket);
			}
			else
			{
				packetsPending++;
				sendPacket(sendingPacket);
				packetsSent++;
				toLayer3(A, sendingPacket);
			}
		}
		else
		{
			queuedpackets.add(sendingPacket);
		}

	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by a B-side procedure)
	// arrives at the A-side. "packet" is the (possibly corrupted) packet
	// sent from the B-side.
	protected void aInput(Packet packet)
	{
		succesfullyReceivedPackets++;
		if (makeChecksum(packet))
		{
			acceptPacket(packet);
			if (packet.getAcknum() >= nextSequenceNumber)
			{
				double time = getTime();
				int indexer = packet.getAcknum() - nextSequenceNumber + 1;
				for (int i = 0; i < indexer; i++)
				{
					timeReceived.add(time);
				}
				nextSequenceNumber = packet.getAcknum() + 1;
				if (nextSequenceNumber == LimitSeqNo) //Wrapper.
				{
					nextSequenceNumber = 0;
				}
			}
		}
		else if(!makeChecksum(packet))
		{
			packetsCorrupted++;
		}
		else
		{
			packetsCorrupted++;
		}
	}
	// This routine will be called when A's timer expires (thus generating a
	// timer interrupt). You'll probably want to use this routine to control
	// the retransmission of packets. See startTimer() and stopTimer(), above,
	// for how the timer is started and stopped.
	protected void aTimerInterrupt()
	{
		//Iterate through the list of packets
		//if we find the one that is the "active" sequence 
		//Send that MOFO
		for (Packet packets : unconfirmedPackets)
		{
			if (packets.getSeqnum() == nextSequenceNumber)
			{
				startTimer(A, RxmtInterval);
				toLayer3(A, packets);
				packetsSent++;
				return;
			}
			else if(packets.getSeqnum() != nextSequenceNumber)
			{

				//What do we do in the else case?
				//Nothing?
			}
		}
	}

	// This routine will be called once, before any of your other A-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity A).
	protected void aInit()
	{
		//Initializes the two variables on the A Side 
		unconfirmedPackets = new ConcurrentLinkedQueue<Packet>();
		//To keep track of what packets have been sent
		queuedpackets = new ConcurrentLinkedQueue<Packet>();
		//Keeps track of the packets that have entered A but havent 
		//yet been sent yet.
	}

	// This routine will be called whenever a packet sent from the B-side
	// (i.e. as a result of a toLayer3() being done by an A-side procedure)
	// arrives at the B-side. "packet" is the (possibly corrupted) packet
	// sent from the A-side.
	protected void bInput(Packet packet)
	{
		succesfullyReceivedPackets++;
		if (expectedSequenceNumber == LimitSeqNo) //Handles the WRAP AROUND CASE
		{
			if (duplicates)
			{
				duplicates = false;
			}
			else
			{
				duplicates = true;
			}
			expectedSequenceNumber = 0;
		}
		if (makeChecksum(packet))
		{
			if (packetsConfirmed.get(evaluateChecksum(packet)) != null)
			{
				return;
			}
			else
			{
				packetsConfirmed.put(evaluateChecksum(packet), true);
			}
			if (packet.getSeqnum() != expectedSequenceNumber)
			{
				outOfOrderPackets.add(packet);
			}
			else
			{
				toLayer5(packet.getPayload());
				expectedSequenceNumber++;
				while (!outOfOrderPackets.isEmpty())
				{
					if (expectedSequenceNumber == LimitSeqNo)
					{
						if (duplicates) 
						{
							duplicates = false;
						}
						else
						{
							duplicates = true;
						}
						expectedSequenceNumber = 0;
					}
					if (outOfOrderPackets.peek().getSeqnum() == expectedSequenceNumber)
					{
						packet = outOfOrderPackets.remove();
						toLayer5(packet.getPayload());
						expectedSequenceNumber++;
					}
					else
					{
						break;
					}
				}
			}
			Packet ACK = new Packet(-1, expectedSequenceNumber - 1, -1); 
			//^ABOVE MAKES THE CUMULATIVE ACKNOWLEDGMENT
			ACK = initializeChecksum(ACK);
			packetsSent++;
			toLayer3(B, ACK);
		}
		else
		{
			packetsCorrupted++;
		}
	}

	// This routine will be called once, before any of your other B-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity B).
	protected void bInit()
	{
	// set up priority queue
		outOfOrderPackets = new PriorityQueue<Packet>(new Comparator<Packet>()
		 {
//Intitialize a priority queue and the method for comparison.
			@Override                             
			public int compare(Packet first, Packet next) 
			{
				if (first.getSeqnum() > next.getSeqnum()) 
				{
					return 1;
				} 
				else 
				{
					return -1;
				}}});
	}

	
	protected void Simulation_done() 
	{
/*
	From here down is formatting and the manipulation of the data that was collected in the
	simulation above. 
	The RTT may be slightly flawed as often sometimes there are packets that never get printed
	to the outfile as the outfile does not account for packets that are queued in the 
	queuedPackets that has not been yet pushed to the outfile.
*/
		System.out.println("\n   *********Statistics*********\n");
		System.out.println("         RECEIVED PACKETS:    " + succesfullyReceivedPackets);
		System.out.println("         SENT PACKETS:        " + packetsSent);
		System.out.println("         CORRUPTED PACKETS:   " + packetsCorrupted);
		System.out.println("         PACKETS LOST:        " + (packetsSent - succesfullyReceivedPackets));
		double averageRTT = calcRTT(timeSent, timeReceived);
		System.out.println("\n   *********RTT Statistics*********\n");
		System.out.println("         THE AVERAGE RTT:     " + averageRTT + "\n\n");
		System.out.println("Exiting the simulator .... \n");

	}

/*

Helper functions for manipulating the packets.

*/
	private double calcRTT(ArrayList<Double> timesIn,  ArrayList<Double> timesReceived)
	{
		double RTTsum = 0;
		int Times = 0;
		for (int j = 0; j < timesReceived.size(); j++)
		 {
			if (j >= timeSent.size()) 
			{
				//Deals with the results from the Hashtable that are less than 0. (IE -1.0)
				break;
			}
			RTTsum += timesReceived.get(j) - timesIn.get(j);
			Times++;
		}
		return (RTTsum / Times);


	}


	private Packet initializeChecksum(Packet packet) 
	{
		packet.setChecksum(evaluateChecksum(packet));
		return packet;
	}
	private boolean makeChecksum(Packet packet) 
	{
		if (evaluateChecksum(packet) != packet.getChecksum()) 
		{
			return false;
		} 
		else
		{
			return true;
		}
	}
	//Checks the Checksum
	private int evaluateChecksum(Packet packet) 
	{
		int Checksum = packet.getSeqnum() + packet.getAcknum();
		for (int i = 0; i < packet.getPayload().length(); i++) 
		{
			Checksum += packet.getPayload().charAt(i);
		}
		return Checksum;
	}
	// Removes the packet with the Acknum from the unconfirmedPackets
	protected void acceptPacket(Packet packet) 
	{
		for (Packet p2 : unconfirmedPackets) 
		{
			if (p2.getSeqnum() == packet.getAcknum()) 
			{
				unconfirmedPackets.remove(p2);
				if (!queuedpackets.isEmpty()) 
				{
					// send the packets that have been pending and add to the
					// queue
					Packet queued = queuedpackets.remove();
					sendPacket(queued);
					toLayer3(A, queued);
					packetsSent++;
				} 
				else 
				{
					packetsPending--;
				}
			}
		}
	}
//Supplementary method for making sure that the packet was sent
	protected void sendPacket(Packet packet)
	{
		stopTimer(A);
		unconfirmedPackets.add(packet);
		timeSent.add(getTime());
		startTimer(A, RxmtInterval);
	}

}