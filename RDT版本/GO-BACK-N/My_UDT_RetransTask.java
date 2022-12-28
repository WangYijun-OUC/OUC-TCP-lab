package com.ouc.tcp.test;

import java.util.TimerTask;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

public class My_UDT_RetransTask extends TimerTask{
		// 构造超时传送数据包
		private Client senderClient;
		public int size=20;//窗口大小
//			private TCP_PACKET reTransPacket;
		private TCP_PACKET[] packets;  // 维护窗口内包的数组
		
		public My_UDT_RetransTask(Client client, TCP_PACKET packet[]){
			super();
			this.senderClient = client;
			this.packets = packet;
		}
		
		public void run() {
			System.out.println("超时重发包");
			for (int i = 0; i < packets.length; i ++ )
	        {
	            if (packets[i] == null) {  // 如果没有包则跳出循环
	                break;
	            } else {  // 逐一递交各个包
	                senderClient.send(packets[i]);
	            }
	        }
		}
}
