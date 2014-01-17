//package client_final;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Util {
	public long calculateChecksum(byte[] buf) {
		int length = buf.length;
		int i = 0;

		long sum = 0;
		long data;


		while (length > 1) {

			data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
			sum += data;

			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}

			i += 2;
			length -= 2;
		}


		if (length > 0) {

			sum += (buf[i] << 8 & 0xFF00);

			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}


		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;

	}

	public void rdt_send(byte[] buffer, int mss, DatagramSocket s,int seqNum, String ipaddress, int portNum) throws IOException
	{
		int i,j,k;
		byte[] seq_num = new byte[4];
		byte[] seqnum_temp = new byte[4];
		byte [] checksum = new byte[2];
		byte [] udp_field = new BigInteger("0101010101010101",2).toByteArray();
		Integer checksum_value = (int)calculateChecksum(buffer);
		checksum = new BigInteger(checksum_value.toString(), 10).toByteArray();
		seqnum_temp = new BigInteger(new Integer(seqNum).toString(),10).toByteArray();

		Integer length_temp = seqnum_temp.length;
		if (seqnum_temp.length<4)
		{
			Integer append = 4 -seqnum_temp.length;
			for (i=0;i<append;i++)
			{
				seq_num[i] = 0;
			}
			for (j = append,  k=0 ; j<4 && k<length_temp ; j++,k++)
			{
				seq_num[j] = seqnum_temp[k];
			}


		}

		byte[] finalSend = new byte[mss+8];

		for (i=0;i<4;i++)
		{
			finalSend[i] = seq_num[i];

		}
		for (i=4,k=0;i<6 && k<2;i++,k++)
		{
			finalSend[i] = checksum[k];
		}
		for (i=6,k=0;i<8 && k<2 ;i++,k++)
		{
			finalSend[i] = udp_field[k];
		}
		for (i=8,k=0;i<finalSend.length && k< buffer.length;i++,k++)
		{
			finalSend[i] = buffer[k];
		}

		DatagramPacket p = new DatagramPacket (finalSend,finalSend.length,InetAddress.getByName(ipaddress),portNum);
		s.send(p);



	}


}
