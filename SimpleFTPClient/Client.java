//package client_final;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Client {

	public static int next_seq_num = 0;
	public static DatagramSocket listenSocket;
	public static DatagramSocket sendSocket;
	public static int sent_seq_num;
	public static int rec_seq_num;
	public static String ip_address;
	public static int port;
	public static int mss;
	public static int mssCount;
	public static ArrayList<byte[]> bytes = new ArrayList<byte[]>();
	public static Timer t = new Timer();
	public static int windowSize;
	public static Retransmit retransmit = new Retransmit();
	public static int bytesLeft = 0;
	public static long startTime = 0;
	public static long endTime = 0;

	public static void main(String args[]) throws IOException{

		Util u = new Util();


		ip_address = args[0];
		port = Integer.parseInt(args[1]);
		String filename = args[2];
		windowSize = Integer.parseInt(args[3]);
		mss = Integer.parseInt(args[4]);

		listenSocket = new DatagramSocket(5678);
		Receiver r = new Receiver();
		r.start();


		DatagramPacket packet;
		sendSocket = new DatagramSocket();
		File f = new File(filename);
		FileInputStream fs = new FileInputStream(f);

		long fileSize = f.length();
		mssCount = (int)fileSize/mss;
		bytesLeft = (int)fileSize%mss;


		int k;
		for(k=0;k<mssCount;k++){
			byte[] bytesRead = new byte[mss];
			fs.read(bytesRead, 0, mss);
			bytes.add(bytesRead);
		}
		if(k==mssCount){
			byte[] bytesRead1 = new byte[bytesLeft];
			fs.read(bytesRead1, 0, bytesLeft);
			bytes.add(bytesRead1);
		}

		final ByteArrayOutputStream baos=new ByteArrayOutputStream();
		final DataOutputStream daos=new DataOutputStream(baos);
		daos.writeInt(mss);
		byte[] mssArray=baos.toByteArray();
		DatagramPacket p = new DatagramPacket (mssArray,mssArray.length,InetAddress.getByName(ip_address),port);
		sendSocket.send(p);
		final ByteArrayOutputStream baos1=new ByteArrayOutputStream();
		final DataOutputStream daos1=new DataOutputStream(baos1);
		daos1.writeInt(mssCount);
		byte[] mssCountArray = baos1.toByteArray();
		DatagramPacket p1 = new DatagramPacket (mssCountArray,mssCountArray.length,InetAddress.getByName(ip_address),port);
		sendSocket.send(p1);

		final ByteArrayOutputStream baos2=new ByteArrayOutputStream();
		final DataOutputStream daos2=new DataOutputStream(baos2);
		daos2.writeInt(bytesLeft);
		byte[] bytesLeftArray = baos2.toByteArray();

		DatagramPacket p2 = new DatagramPacket (bytesLeftArray,bytesLeftArray.length,InetAddress.getByName(ip_address),port);
		sendSocket.send(p2);

		startTime = System.nanoTime();
		for(k = 0;k<windowSize;k++){

			byte[] bytesRead = new byte[mss];


			for(int j=0;j<mss;j++){

				bytesRead[j] = bytes.get(k)[j];
			}
			u.rdt_send(bytesRead, mss, sendSocket,k, ip_address,port);
			sent_seq_num = k;
		}


		t.schedule(retransmit, 2000);


	}
	public static class Receiver extends Thread{
		public void run(){

			Util u = new Util();

			while(true){

				byte[] receiveBuf = new byte[8];
				DatagramPacket receivePacket = new DatagramPacket(receiveBuf,receiveBuf.length);

				try {
					listenSocket.receive(receivePacket);
				} catch (IOException e) {
				}

				byte[] receiveSeqnum = new byte[4];

				for(int i=0;i<4;i++){
					receiveSeqnum[i] = receiveBuf[i];
				}

				rec_seq_num = java.nio.ByteBuffer.wrap(receiveSeqnum).getInt();


				if(rec_seq_num == mssCount){

					retransmit.cancel();
					t.cancel();
					endTime = System.nanoTime();
					System.out.println("The transfer time is" + (endTime - startTime));
					System.exit(0);
				}

				if(rec_seq_num == next_seq_num){
					synchronized(t){
						retransmit.cancel();
						t.cancel();
						t.purge();
					}
					byte[] sendBuf1 = new byte[bytesLeft];
					byte[] sendBuf = new byte[mss];
					if (next_seq_num == (mssCount-1)){
						sendBuf1 = bytes.get(mssCount);
					}
					else
					{
						sendBuf = bytes.get(rec_seq_num+1);
					}
					try {
						if (next_seq_num != (mssCount-1)){
							u.rdt_send(sendBuf, mss, sendSocket, (rec_seq_num+1), ip_address, port);
						}
						else
						{
							u.rdt_send(sendBuf1, bytesLeft, sendSocket, (rec_seq_num+1), ip_address, port);
						}
						Timer t = new Timer();
						retransmit = new Retransmit();
						t.schedule(retransmit, 3000);

					} catch (IOException e) {
					}

					next_seq_num++;


				}



			}
		}




	}

	public static class Retransmit extends TimerTask{

		@Override
		public void run() {

			retransmit.cancel();
			t.cancel();
			t.purge();

			byte[] sendBuf = new byte[mss];
			Util u = new Util();
			System.out.println("Timeout, Sequence number = " + (rec_seq_num+1));
			for(int i=rec_seq_num+1;i<=(rec_seq_num+windowSize) && i<=mssCount;i++){

				sendBuf = bytes.get(i);

				try {
					u.rdt_send(sendBuf, mss, sendSocket, i, ip_address, port);

				} catch (IOException e) {
				}


			}
			Timer t = new Timer();
			retransmit = new Retransmit(); 
			t.schedule(retransmit, 4000);



		}



	}




}
