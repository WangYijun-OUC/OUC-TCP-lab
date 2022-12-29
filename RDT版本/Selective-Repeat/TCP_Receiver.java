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
	private RcvWindow window = new RcvWindow(this.client);
	
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
			int currentACK = -1;  // 当前包的seq 
			try {
				currentACK = this.window.rcv(recvPack.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

			if (currentACK != -1) {
				//生成ACK报文段（设置确认号）
				tcpH.setTh_ack(currentACK * 100 + 1);  // 设置确认号为收到的TCP分组的seq
				ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());  // 新建一个TCP分组（ACK），发往发送方
				tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));  // 设置ACK的校验位

				reply(ackPack);  // 回复ACK报文段
			}
		}
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		// 修改为窗口交付
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
