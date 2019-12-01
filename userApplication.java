import ithakimodem.*;
import java.io.*; // Importing java.io package in order to use its fuctionality
import java.util.ArrayList; // import the ArrayList class

/*
*
* Δίκτυα Υπολογιστών I
*
* Experimental Virtual Lab
*
* Java virtual modem communications seed code
* 
* Author :Michael Karatzas
* AEM:9137
* 
* 
*
*/


public class userApplication {
public static void main(String[] param) {
	
//Creating my modem and giving parameters
Modem myModem;
myModem=new Modem(80000);
myModem.setTimeout(2000);
myModem.open("ithaki");

//setting the request code strings
String echo_request_code="E1307\r";
String image1_request_code="M4289\r";
String image2_request_code="G9090\r";
String gps_request_code="P4678";
String ack_result_code="Q5404\r";
String nack_result_code="R7200\r";



FileOutputStream ErrorFreeImage=null;
FileOutputStream ImageWithErrors=null;
FileOutputStream GPSImage=null;
FileOutputStream GPSTrack=null;
FileOutputStream EchoPacketsTimes=null;
FileOutputStream AckAndNackPacketsTimes=null;
FileOutputStream AckAndNackPacketsStats=null;
FileOutputStream retrasmissionsFreq=null;

//Initializing the streams to the files ,in which the data will be stored
try {
	ErrorFreeImage = new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/E1.jpeg");
	ImageWithErrors = new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/E2.jpeg");
	GPSImage=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/M1.jpeg");
	GPSTrack=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/GPSTRack.txt");
	EchoPacketsTimes=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/EchoPacketsTime.csv");
	AckAndNackPacketsTimes=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/NackAndAckPacketsTimes.csv");
	AckAndNackPacketsStats=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/NackAndAckPacketsStats.csv");
	retrasmissionsFreq=new FileOutputStream("/home/mikalaki/eclipse-workspace/ComputerNetworks1Project/src/files/RetrasmissionsFrequency.csv");
}catch(IOException x) {
	System.out.println("Exception!!!! Cannot initialize the output streams to the output files!! They remain as null (0)" );	
	
}

//Printing the welcoming message to the console
getTheWelcomeMessage(myModem );


//Requesting, printing the echoPackets and getting their times into a csv
EchoRequest(myModem, echo_request_code,EchoPacketsTimes);	  



//Requesting and getting The Image 1( without errors) as E1.jpeg
ImageRequest(myModem ,image1_request_code, ErrorFreeImage );

//Requesting and getting The Image 2( with errors)	as E2.jpeg
ImageRequest(myModem ,image2_request_code, ImageWithErrors);

//Requesting and getting the GPS Track - getting 5 points to and request the M1.jpeg imge from Google maps
GPSRequest(myModem ,gps_request_code,GPSImage , GPSTrack);

//requesting and getting the packets, getting the time of right packets, and stats about the packets
ACKandNACKPackets(myModem, ack_result_code,nack_result_code,AckAndNackPacketsTimes,AckAndNackPacketsStats ,retrasmissionsFreq);


}

private static void ACKandNACKPackets(Modem modem, String ack_code ,String nack_code,FileOutputStream responseTimesOut,FileOutputStream stats,FileOutputStream retrasmissionsFreq) {	
	
	// 4 minutes is 4*60 seconds is 240000 miliseconds .The loop runs for at least 4minutes + 15seconds ==255000 milliseconds.
	int function_time=255000;
		
	String encryptedMessage="";
	String FCSstring="";
	String Packet=""; 
	
	int numberOfAckRequests=0;
	int numberOfNackRequests=0;	
	int numberOfPacketRetransmissions=0;
	
	/*
	 * The array using to represent the frequency 
	 * (for number of a Packet transmissions k ,
	 * numberOfRetrasmissionsFrequency[k] is used to store its frequency.
	 * MAXIMUM 9 Retrasmissions
	 */
	int numberOfRetrasmissionsFrequency []=new int[10];
	
	//initializing the array with zeros
	for(int i=0;i<numberOfRetrasmissionsFrequency.length;i++ ){
		numberOfRetrasmissionsFrequency[i]=0;		
	}

	

	
	int encryptedCharsXorProduct=0;
	int FCSint=0;

	
	int ResponseTime=0;
	int endTime=0;
	//Delay is due to packets with errors.
	int delay=0;
	//StartOfFuctionTime is used in order to get the response times for at least 4 minutes!!
	int startOfFunctionTime=((int)java.lang.System.currentTimeMillis());


	try {	
		stats.write(("The ARQ request fuction starts at :" + String.valueOf(startOfFunctionTime)).getBytes())
		
		;}
	catch(IOException x) {System.out.println("Unable to write the start time at the stats file.");}
	
	//As a receiver our terminal stars with and ACK code request
	int startOfPacketTime=((int)java.lang.System.currentTimeMillis());	
	modem.write(ack_code.getBytes() ); 	
	numberOfAckRequests++;
	
	
	for(;;) {	
		
		//emptying the strings used for a packet.
		encryptedMessage="";
		FCSstring="";
		Packet="";
		encryptedCharsXorProduct=0;	
		
		//getting a packet, the statement is used in order not to have exception of length .
	    while (Packet.length()<5 || (!(Packet.substring(Packet.length() - 5).equals("PSTOP"))) )	{
	    	
	    	Packet=Packet+((char)modem.read());				
	    	

	    }
	    
	    //The time of a Packet ends exactly when the packet is received, all its characters are read
	    endTime=(int)java.lang.System.currentTimeMillis();
	    ResponseTime=endTime-startOfPacketTime;
		
	    //getting the FCSstring and the encryptedMessage from the packet using split
		String[] substrings = Packet.split(" ");	
		
		FCSstring=substrings[5];			 
		encryptedMessage=substrings[4].substring(1, substrings[4].length()-1);
		
	    System.out.println(); 
	    
	    //printing the packet in the console
	    System.out.println(Packet);	   
	    
	    //getting the xor product of the given encrypted chars one by one
	    for(int i=0; i<encryptedMessage.length();i++) {encryptedCharsXorProduct= encryptedCharsXorProduct ^ (int) encryptedMessage.charAt(i);}
	    	    
	    FCSint = Integer.parseInt(FCSstring);
	    
	    //Getting the response time of only right packages
	    if(FCSint==encryptedCharsXorProduct)  {
	    	System.out.println("Response time:" +   (ResponseTime + delay )    + " ms");
	    	
	    	try {
	    		
	    	//getting the time of succesfull downloads of the packets
	    	responseTimesOut.write(String.valueOf(ResponseTime + delay ).getBytes());
	    	responseTimesOut.write('\n');
	    	}catch(IOException x) {System.out.println("Unable to write Nack and Ack packets time into the file.");}
	    	
	    
	    	//increasing the number of transmissions frequency when there is an ack request and the current packet retransmissions are over.
	    	numberOfRetrasmissionsFrequency[ numberOfPacketRetransmissions ]++;
	    	
	    	
	    	//Stopping the loop when the set function time is passed
	    	if( ((int)java.lang.System.currentTimeMillis()-startOfFunctionTime)> function_time    ) {break;}
	    	
	    	//The time when a request starts(startOfPacketTime) is counted exactly before the request of the packet
	    	startOfPacketTime=((int)java.lang.System.currentTimeMillis());
	    	modem.write(ack_code.getBytes() );
	    	
	    
	    	numberOfAckRequests++;
	    	delay=0;

	    	numberOfPacketRetransmissions=0;
	    }
	    else {	    	
	    	
	    	delay=delay+ResponseTime;
	    	
	    	
	    	if( ((int)java.lang.System.currentTimeMillis()-startOfFunctionTime)> function_time    ) {
	    		numberOfRetrasmissionsFrequency[ numberOfPacketRetransmissions ]++;
	    		break;}
	    	
	    	//The time when a request starts(startOfPacketTime) is counted exactly before the request of the packet
	    	startOfPacketTime=((int)java.lang.System.currentTimeMillis());	       	
	    	modem.write(nack_code.getBytes() );
	
	    	numberOfNackRequests++;
	    	numberOfPacketRetransmissions++;
	    	System.out.println("Number of Packet retransmission:" + numberOfPacketRetransmissions);	
	    }    
	    
	}   
	
	System.out.println("Ack Requests:" + numberOfAckRequests);	
	System.out.println("Nack Requests :" + numberOfNackRequests);
	System.out.println("time of execution :" + (   ((int)java.lang.System.currentTimeMillis())  -startOfFunctionTime  ));
	
	
	//printing the retransmissions frequency of a packet
	for(int i=0;i<numberOfRetrasmissionsFrequency.length;i++)
	{
		System.out.println("We had :" + (i) + " retrasmissions " + numberOfRetrasmissionsFrequency[i]+ " times");
		
	}	
	
	//Printing ARQ packets stats
	try {    	
		
		stats.write(      ("Number of Packets received with Ack requests:" + String.valueOf(numberOfAckRequests)).getBytes()     );
		stats.write(("\n").getBytes());
		stats.write(      ("Number of Packets received with Nack requests:" + String.valueOf(numberOfNackRequests)).getBytes()     );
		
		stats.write(("\n \n \n").getBytes());
		stats.write(("################################################################\n").getBytes());
		stats.write(("################# FREQUENCY OF RETRASMISSIONS: #################\n").getBytes());
		stats.write(("################################################################\n").getBytes());
		
		
		//printing the retransmissions frequency of a packet to the stats file
		for(int i=0;i<numberOfRetrasmissionsFrequency.length;i++)
		{
			stats.write(("We had :" + (i+1) + " retrasmissions " + numberOfRetrasmissionsFrequency[i]+ " times").getBytes());
			stats.write(("\n").getBytes());
		}	
		
		//printing the retransmissions frequency of a packet to the  frequency file
		for(int i=0;i<numberOfRetrasmissionsFrequency.length;i++)
		{
			retrasmissionsFreq.write(String.valueOf(numberOfRetrasmissionsFrequency[i]).getBytes());
			retrasmissionsFreq.write(("\n").getBytes());
		}	
		
		
	
	}catch(IOException x) 
	{System.out.println("Unable to write number of right packets and packets with errors into the file.");}
}



private static void GPSRequest(Modem modem, String gps_request_code,FileOutputStream imgout,FileOutputStream txtout) {
	//sending the gps request code together with the R=XPPPPLL code to the server ITHAKI	
    modem.write((gps_request_code+ "R=1010099\r").getBytes() ); 
    //I use the R=1010099(R=ΧPPPPLL) code , that means I follow the route X=1
    //starting from the trace PPPP=0100 and getting LL=99 traces   
 
    int c=0;
    String gpsTracking="";    
    
    //printing the GPS tracking Spots in the console and save it on a string
    while ((c =modem.read()) != -1) {
		System.out.print((char)c);
		gpsTracking=gpsTracking+(char)c;		
	}	 
    
    //writing the gps Tracks into a txt file
    try {
    	txtout.write(gpsTracking.getBytes());
    	txtout.close();    	
    }catch (IOException x) {
    	System.out.println("Exception When trying to write the text at GPStrack.txt file !" );	
    } 
    
    //Function GetTheTString() produce the string of T's getting the track the programm got from the request with R (R=1010099\r)
    //and we send it with the gps_request_code as a new request to get the image with the spots
    modem.write(    (gps_request_code + GetTheTString(modem, gpsTracking )).getBytes()       );    
	try {							
		while ((c =modem.read()) != -1) {
			imgout.write((byte)c);
			
		}			
		imgout.close();				
	}catch (IOException x) {
		System.out.println("Exception When trying to write the bytes at the GPS Image file :" + imgout.toString());	
		
		}	
    
    
}

//This function produces the string of T's
private static String GetTheTString(Modem modem, String tracking) {
	//Initializing the string of T's variable
	String Tstring="";
	
	String [] decimalLatitudes=new String[99];
	String [] decimalLongtitudes=new String[99];
	
	//Using split to get the spots of GPS tracking in NMEA $GPGGA form
	String[] spots = tracking.split("\n");

	for(int i=1;i<(spots.length-1);i++)
	{			
		//"Spliting" the gps traces into smaller parts using the , seperator in order to get the coordinates(latitude and longtitude)
		String[] parts = spots[i].split(",");	
		
		//By splitting a gps trace into parts, parts[2] gives as the latitude and parts[4] the longtitude 
		decimalLatitudes[i-1]=NMEACordinateslToDMS(   Double.parseDouble(parts[2])  );
		decimalLongtitudes[i-1]=NMEACordinateslToDMS(  Double.parseDouble(parts[4])  );

	}
	//creating the string of T's
	Tstring= (   "T="+ decimalLongtitudes[0] +decimalLatitudes[0] 
				+"T="+ decimalLongtitudes[20]+decimalLatitudes[20] 
				+"T="+ decimalLongtitudes[40]+decimalLatitudes[40] 
				+"T="+ decimalLongtitudes[60]+decimalLatitudes[60] 
				+"T="+ decimalLongtitudes[80]+decimalLatitudes[80] + "\r");		
	
	return Tstring;
	
}

//function to transform the given form of NMEA coordinates (decimal) to DMS 
private static String NMEACordinateslToDMS(double coordinate) {
	
    String result = null;
    
    //getting the degrees in the requisite format 
    double GivenDegs = coordinate / 100;    
    int degrees = (int) GivenDegs;
    
    //with these lines of code we get the minutes in the requisite format 
    double decMinutesSeconds = ((GivenDegs - degrees)) / .60;
    double minuteValue = decMinutesSeconds * 60;    
    int mins = (int) minuteValue;
    
    double secs = (minuteValue - mins) * 60;
    
    //creating the string of the transformed coordinates in requisite format
    result = String.valueOf(degrees)  + String.valueOf(mins) + String.format("%.0f", secs);
    
    return result;
}


private static void ImageRequest(Modem modem, String image_request_code ,FileOutputStream out) {
  
    //Sending the image request code to the server ITHAKI
    modem.write(  image_request_code.getBytes() );  			
	try {				
			int c;				
			while ((c =modem.read()) != -1) {
				out.write((byte)c);
				
			}			
			out.close();				
		}catch (IOException x) {
			System.out.println("Exception When trying to write the bytes a the Image file :" + out.toString());	
			
			}		
	
}


//Function to get the times of echopackets
private static void EchoRequest(Modem modem, String echo_request_code,FileOutputStream echoPacketsTime) {	
	
	int k=0;
	String EchoPacket= new String();	
																			
	
	int responseTime=0;
	
	//use startOfFuctionTime in order to get the response times for at least 4 minutes!!
	int startOfFuctionTime=(int)java.lang.System.currentTimeMillis();
	int endTime= startOfFuctionTime;
	
	
	int startOfPacketTime=(int)java.lang.System.currentTimeMillis();
	//write() out of the loop cause one write sends one echo packet
	modem.write(echo_request_code.getBytes() );//Start time is immediatly when the request is sent to the server
	
	
	for(;;){		
				
			if ((k=modem.read())==-1) break;			
			EchoPacket=EchoPacket+((char)k);	

			if(EchoPacket.length()>5)
				if((EchoPacket.substring(EchoPacket.length() - 5).equals("PSTOP"))) {			
					
					//setting our endTime of the server response , the time when all the characters of an echo packet are received
					endTime=	(int)java.lang.System.currentTimeMillis();
					
					responseTime=endTime-startOfPacketTime;				
					
					//getting the echoPacket and its time printed					
					System.out.print(EchoPacket);
					//getting a message about how long it took the userApplication to execute a certain EchoPacket
					System.out.println("\nThe server response time of  EchoPacket is: " + responseTime+" ms");
				    try {
				    	echoPacketsTime.write(String.valueOf(responseTime).getBytes()  ); //casting at int in order to be writable
				    	echoPacketsTime.write('\n' );
				    }catch (IOException x) {
				    	System.out.println("Exception When trying to write the times of Echo packets time file !" );	
				    }			
					
					
					//Emptying the echoPacket string in order to get refilled by the execution of the program with the next echoPacket
					EchoPacket="";	
					
					
					
					
					//counting the time of a completed Echo Request from the moment of the call/ 4 minutes is 4*60 seconds is 240000 miliseconds so I run the loop for at least 4minutes + 15seconds ==255000 milliseconds							
					if(((startOfPacketTime=(int)java.lang.System.currentTimeMillis())-startOfFuctionTime)>255000) 
						{
						  try {
						    	echoPacketsTime.close();
						    }catch (IOException x) {
						    	System.out.println("Cannot close the Echo packets time file !" );	
						    }	
						  break;
						}
					modem.write((echo_request_code).getBytes() );	
			
				}	

	}

}

private static void getTheWelcomeMessage(Modem modem) {
	int k;
	for (;;) {
		try {
			k=modem.read();
			if (k==-1) break;
			System.out.print((char)k);
		} 	catch (Exception x) {
				break;
			}
	}
	
}

//Demo function.
public void demo() {
int k;
Modem modem;
modem=new Modem();
modem.setSpeed(10000);
modem.setTimeout(2000);
modem.open("ithaki");
for (;;) {
	try {
		k=modem.read();
		if (k==-1) break;
		System.out.print((char)k);
	} 	catch (Exception x) {
			break;
		}
}
//NOTE:Break endless loop by catching sequence "\r\n\n\n".
//NOTE:Stop program execution when "NO CARRIER" is detected.
//NOTE:A time-out option will enhance program behavior.
//NOTE:Continue with further Java code.
//NOTE:Enjoy :)
modem.close();
}






}