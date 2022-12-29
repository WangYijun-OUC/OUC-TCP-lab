package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

public class RcvWindow {
	private Client client;
    private int size = 16;
    private int base = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[size];
    //private int counts = 0;
	public int expextedseq=0;//期待的序号
	//public LinkedList<TCP_PACKET>packets = new LinkedList<TCP_PACKET>();//缓存列表
	 Queue<int[]> dataQueue = new LinkedList<int[]>();//提交数据
	
	public RcvWindow(Client client){
		this.client=client;
	}

	public int rcv(TCP_PACKET rcvpacket) {
		//如果大于等于期待的seq，则将数据包缓存。
		int CurrentAck=(rcvpacket.getTcpH().getTh_seq()-1)/100;

        if (CurrentAck < base) {  // [rcvbase-N, rcvbase-1]
            if (base - size <= CurrentAck && CurrentAck <= base - 1) {
                return CurrentAck;  // 对于失序分组也要返回ACK
            }
        } else if (CurrentAck <= base + size - 1) {  // [rcvbase-N, rcvbase+N-1]
            packets[CurrentAck - base] = rcvpacket;  // 对于正确分组，加入窗口中.

            if (CurrentAck == base) {  // 接受到的分组位于窗口左沿

                // 滑动窗口
                slid();

                // 交付数据
                if (dataQueue.size() >= 20 || base == 1000) {
                    deliver_data();
                }

            }

            return CurrentAck;  // 返回ACK
        }

        return -1;  // 错误返回-1
	}

	public void slid() {
		int leftMoveIndex = 0;  // 用于记录窗口左移到的位置：最小未收到数据包处
        while (leftMoveIndex <= size - 1 && packets[leftMoveIndex] != null) {
            leftMoveIndex ++;
        }

        for (int i = 0; i < leftMoveIndex; i++) {  // 将已接收到的分组加入交付队列
            dataQueue.add(packets[i].getTcpS().getData());
        }

        for (int i = 0; leftMoveIndex + i < size; i++) {  // 剩余位置的包左移
            packets[i] = packets[leftMoveIndex + i];
        }

        for (int i = size - (leftMoveIndex); i < size; i++) {  // 将左移的包原来位置处置空
            packets[i] = null;
        }

        base += leftMoveIndex;  // 移动窗口左沿
		
	}

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
}
