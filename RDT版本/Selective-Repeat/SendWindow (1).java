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
	private UDT_Timer[] timers = new UDT_Timer[size];  // 存储计时器

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
        timers[nextseq] = new UDT_Timer();  // 为新放入窗口内的包增加计时器
        My_UDT_RetransTask task = new My_UDT_RetransTask(client, packet);	// 设置重传任务
        timers[nextseq].schedule(task, 3000, 3000);	// 每隔3s执行一次重传，直到收到ACK为止
        nextseq++;  // 更新窗口的插入位置
    }

	
	public void rcv(int CurrentAck){
		if (base <= CurrentAck && CurrentAck < base + size) {  // 如果收到的ACK在窗口范围内
			if (timers[CurrentAck - base] == null) {  // 表示接收到重复ACK，什么也不做
                return;
            }

            timers[CurrentAck - base].cancel();  	// 终止计时器
            timers[CurrentAck - base] = null;  		// 删除计时器

            if (CurrentAck == base) {  // 接收到的ACK位于窗口左沿，则要移动窗口
                int leftMoveIndex = 0;  // 窗口左沿应该移动到的位置：最小未ACK的分组
                while (leftMoveIndex + 1 <= nextseq && timers[leftMoveIndex] == null) {
                    leftMoveIndex ++;
                }


                for (int i = 0; leftMoveIndex + i < size; i++) {  // 将窗口内的包左移
                    packets[i] = packets[leftMoveIndex + i];
                    timers[i] = timers[leftMoveIndex + i];
                }

                for (int i = size - (leftMoveIndex); i < size; i++) {  // 清空已左移的包原来所在位置处的包和计时器
                    packets[i] = null;
                    timers[i] = null;
                }

                base += leftMoveIndex;  // 移动窗口左沿至leftMoveIndex处
                nextseq -= leftMoveIndex;  // 移动下一个插入包的位置

            }
	    }

	}
}


