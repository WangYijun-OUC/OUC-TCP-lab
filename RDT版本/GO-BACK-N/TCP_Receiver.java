/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence = 1;//用于记录当前待接收的包序号，注意包序号不完全是
	private int expectedSequence = 0;  // 用于记录期望收到的seq

	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int currentACK = (recvPack.getTcpH().getTh_seq() - 1) / 100;  // 当前包的seq
			if (expectedSequence == currentACK) {  // 当前收到的包就是期望的包
				//生成ACK报文段（设置确认号）
				tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());  // 设置确认号为收到的TCP分组的seq
				ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());  // 新建一个TCP分组（ACK），发往发送方
				tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));  // 设置ACK的校验位

				reply(ackPack);  // 回复ACK报文段

				// 将接收到的正确有序的数据插入 data 队列，准备交付
				dataQueue.add(recvPack.getTcpS().getData());

				expectedSequence += 1;  // 更新期望收到的包的seq		
			} else {  // 收到失序的包，返回已确认的最大序号分组的确认
				//生成ACK报文段（设置确认号）
				tcpH.setTh_ack((expectedSequence - 1) * 100 + 1);  // 设置确认号为已确认的最大序号
				ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());  // 新建一个TCP分组（ACK），发往发送方
				tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));  // 设置ACK的校验位

				reply(ackPack);  // 回复ACK报文段
			}
//			//将接收到的正确有序的数据插入data队列，准备交付 RDT2.0
//			if(recvPack.getTcpH().getTh_seq() != sequence){
//				//RDT2.1 只有返回值为ACK时才会插入队列
//				dataQueue.add(recvPack.getTcpS().getData());				
//				sequence = recvPack.getTcpH().getTh_seq();
//			}else{
//				System.out.println("收到重复包，序列号为：" + sequence);
//			}
			
			
//		}else{
//			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
//			System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
//			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
//			tcpH.setTh_ack(sequence);
//			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
//			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
//			//回复ACK报文段
//			reply(ackPack);
		}
		
		System.out.println();
		
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte) 7);	//eFlag=0，信道无错误
				
		//发送数据报
		client.send(replyPack);
	}
	
}
