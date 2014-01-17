//package simpleftpserver;


import simpleftpserver.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Random;



public class SimpleFTPServer {
	public static void main (String args[]) throws IOException
	{
		Integer port1 = Integer.parseInt(args[0]);
		String filename = args[1];
		Integer seq_num = 0;
		DatagramSocket dg = new DatagramSocket(port1);
		DatagramSocket send = new DatagramSocket ();
		double prob = Double.parseDouble(args[2]);


		FileOutputStream fo = new FileOutputStream (filename,true);
		byte[] recvBuf = new byte[4];
		byte[] recvBuf1 = new byte[4];
		byte[] recvBuf2 = new byte[4];
		DatagramPacket r1 = new DatagramPacket (recvBuf,recvBuf.length);
		dg.receive(r1);

		byte[] recievedMss = new byte[4];
		byte[] recievedData_temp = r1.getData();
		if (recievedData_temp.length<4)
		{
			int append = 4 - recievedData_temp.length;
			for (int i = 0 ; i < append; i++)
			{
				recievedMss[i] = 0;
			}
			for (int i = append, j = 0; i<4 && j<recievedData_temp.length; i++,j++)
			{
				recievedMss[i] = recievedData_temp[j];
			}
		}
		else
		{
			recievedMss = r1.getData();
		}
		DatagramPacket r2 = new DatagramPacket (recvBuf1,recvBuf1.length);
		dg.receive(r2);
		recvBuf1 = r2.getData();
		DatagramPacket r3 = new DatagramPacket (recvBuf2,recvBuf2.length);
		dg.receive(r3);
		recvBuf2 = r3.getData();
		Integer mss = 0;
		Integer mssCount = 0;
		Integer bytesLeft = 0;
		mss = java.nio.ByteBuffer.wrap(recievedMss).getInt();
		byte[] buf = new byte[mss+8];
		DatagramPacket packet = new DatagramPacket(buf,buf.length);
		while(true){
			mssCount = java.nio.ByteBuffer.wrap(recvBuf1).getInt();
			bytesLeft = java.nio.ByteBuffer.wrap(recvBuf2).getInt();
			dg.receive(packet);
			buf = packet.getData();
			Random rn =new Random();
			double p = rn.nextDouble();
			byte[] seqnumarray = new byte[4];
			for (int i=0; i<4 ; i++)
			{
				seqnumarray[i] = buf[i];
			}
			Integer sequence_number = java.nio.ByteBuffer.wrap(seqnumarray).getInt();
			if (p > prob)
			{
				if(seq_num.equals(sequence_number)){

					byte[]	data = new byte[buf.length-8];
					for (int i = 8, j = 0; i< buf.length && j< buf.length-8  ; i++,j++)
					{
						data[j] = buf[i];
					}
					Util u = new Util();
					Integer checksum = (int) u.calculateChecksum(data);

					byte [] checksum_data = new byte[2];
					byte [] checksum_temp = new BigInteger(checksum.toString(), 10).toByteArray();
					if (checksum_temp.length < 2)
					{
						Integer append = 2 - checksum_temp.length;

						for (int i=0; i<append; i++)
						{
							checksum_data[i] = 0;
						}
						for (int j = append , k = 0; j<2 && k <checksum_temp.length; j++,k++)
						{
							checksum_data[j] = checksum_temp[k];
						}
					}
					else
					{
						checksum_data = new BigInteger(checksum.toString(), 10).toByteArray();
					}
					Integer invalid = 0;

					for (int i=0 ; i<2 ; i++)
					{

						if (checksum_data[i] != buf[i+4] )
						{
							invalid = 1;
							break;
						}
					}
					if (seq_num.equals(mssCount))
					{
						invalid =0;
					}
					byte[] response = new byte[8];
					if (invalid == 0)
					{
						response[0] = 0;
						response[1] =  0;
						response [2] = 0;
						response [3] = 0;
						for (int i=0; i<4 ; i++)
						{
							response [i] = buf[i];
						}
						response [4] = 0;
						response[5] = 0;
						byte[] temp = new BigInteger("1010101010101010", 2).toByteArray();
						response [6] = temp[0];
						response[7] = temp[1];
					}
					else
					{
					}
					InetAddress ipaddress = packet.getAddress();

					DatagramPacket packet_response = new DatagramPacket (response,response.length,ipaddress,5678);

					send.send(packet_response);

					if (sequence_number.equals(mssCount))
					{	
						fo.write(data,0,(bytesLeft));
						System.exit(0);
					}
					else
					{
						fo.write(data,0,data.length);
					}
					seq_num++;		
				}
			}
			else
			{
				System.out.println("Packet loss, sequence number = " + seq_num);
			}
		}
	}
}
