package com.ouc.tcp.test;

import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		
		//构造校验字符段
		String seq = "" + tcpPack.getTcpH().getTh_seq();//确认字段
		String ack = "" + tcpPack.getTcpH().getTh_ack();//ack字段
		String sum = "" + tcpPack.getTcpH().getTh_sum();//校验和字段
		int tcp_Data[] = tcpPack.getTcpS().getData();//数据字段
		
		String _sum = "" + seq + ack;
		for(int i = 0; i < tcp_Data.length; i++){
			_sum += tcp_Data[i];
		}
		
		//用Adler32校验
		byte[] nxt_cheksum = _sum.getBytes();
		Checksum adler = new Adler32();
		adler.reset();
		adler.update(nxt_cheksum, 0, nxt_cheksum.length);
		long ans = adler.getValue();
		checkSum = (short) ans;
		
		//checkSum = (checkSum & 0xffff) + (checkSum >> 16);
		
		return (short) checkSum;
	}
	
}
