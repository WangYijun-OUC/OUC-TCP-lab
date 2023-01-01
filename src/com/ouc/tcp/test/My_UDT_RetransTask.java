package com.ouc.tcp.test;

import java.util.TimerTask;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

public class My_UDT_RetransTask extends TimerTask{
		// 构造超时传送数据包
		private Client senderClient;
//		public int size=20;//窗口大小
//		private TCP_PACKET reTransPacket;
//		private TCP_PACKET packets;  // 维护窗口内包的数组
		private SendWindow window;
		
		public My_UDT_RetransTask(Client client, SendWindow window){
			super();
			this.senderClient = client;
			this.window = window;
		}
		
		public void run() {
			this.window.multiDecrease();
			this.window.retrand();
		}
}
