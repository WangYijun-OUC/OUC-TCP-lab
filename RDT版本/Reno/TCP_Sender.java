/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

import java.util.TimerTask;

public class TCP_Sender extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 1;
	
	// 重传任务
	private My_UDT_RetransTask task;
	// 发送窗口
	private SendWindow windows = new SendWindow(this.client);
	
	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}
	
	// RDT3.0
	//UDT_Timer timer;
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);		
		//更新带有checksum的TCP 报文头		
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//flag = 0;
		
		// RDT3.0
		//timer = new UDT_Timer();	// 设置计时器
		//UDT_RetransTask reTran = new UDT_RetransTask(client, tcpPack);	// 设置重传任务
		
		// 每隔3s执行一次重传，直到收到ACK为止
		//timer.schedule(reTran, 3000, 3000);
		
		// 窗口中的包都发完了，空转
		if (this.windows.isFull()) {
			this.flag = 0;
			System.out.println("********************");
			System.out.println("window is full!!!");
			System.out.println("********************");
		}
		//等待ACK报文
		while (this.flag == 0);
		
		try {
			this.windows.putPacket(this.tcpPack.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		//发送TCP数据报
		udt_send(tcpPack);
		
		//等待ACK报文
		//waitACK();
		//while (flag==0);
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		//0.信道无差错
		//1.只出错
		//2.只丢包
		//3.只延迟
		//4.出错 / 丢包
		//5.出错 / 延迟
		//6.丢包 / 延迟
		//7.出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte) 7);		
		
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//需要修改
	public void waitACK() {

	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum.computeChkSum(recvPack)==recvPack.getTcpH().getTh_sum()){
			// 收到正确的ACK
			System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
			System.out.println();
			this.windows.rcv((recvPack.getTcpH().getTh_ack() - 1) / 100);
			if (!windows.isFull()) {
				this.flag = 1;
			}
		}
	}
}
	

