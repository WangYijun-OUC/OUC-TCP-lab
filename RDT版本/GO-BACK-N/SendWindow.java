package com.ouc.tcp.test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

public class SendWindow {
	public int base = 0;//窗口左沿
	public int nextseq = 0;//指向下一个发送
	public int size=16;//窗口大小

	private Client client;//发送

	private TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包

	private int mapHead=0;
	
	private UDT_Timer timer;
	private My_UDT_RetransTask task;
	
	
	public SendWindow(Client client){
		this.client=client;
	}
	
	// 判断窗口是否已满
	public boolean isFull() {
		return this.size <= this.nextseq;
	}
	
    /*向窗口中加入包*/
    public void putPacket(TCP_PACKET packet) {
        packets[nextseq] = packet;  	// 在窗口的插入位置放入包
        if (nextseq == 0) {  			// 如果在窗口左沿，则要开启计时器
        	timer = new UDT_Timer();	// 设置计时器
        	My_UDT_RetransTask task = new My_UDT_RetransTask(client, packets);	// 设置重传任务
        	
        	// 每隔3s执行一次重传，直到收到ACK为止
        	timer.schedule(task, 3000, 3000);
        }

        nextseq++;  // 更新窗口的插入位置
    	
    }
	

	
	public void rcv(int CurrentAck){		
		if (base <= CurrentAck && CurrentAck < base + size) {  // 如果收到的ACK在窗口范围内
            for (int i = 0; CurrentAck - base + 1 + i < size; i++) {  // 将窗口中位于确认的包之后的包整体移动到窗口左沿
                packets[i] = packets[CurrentAck - base + 1 + i];
                packets[CurrentAck - base + 1 + i] = null;
            }

            nextseq -=CurrentAck - base + 1;  // 更新nextIndex
            base = CurrentAck + 1;  // 更新窗口左沿指示的seq

            timer.cancel();  // 停止计时器

            if (nextseq != 0) {  // 窗口中仍有包，则重开计时器
            	timer = new UDT_Timer();	// 设置计时器
            	My_UDT_RetransTask task = new My_UDT_RetransTask(client, packets);	// 设置重传任务
            	
            	// 每隔3s执行一次重传，直到收到ACK为止
            	timer.schedule(task, 3000, 3000);
            }

        }
	}


	
}


