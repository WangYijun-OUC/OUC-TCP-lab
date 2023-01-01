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
    //private TCP_PACKET[] packets = new TCP_PACKET[size];
    //private int counts = 0;
	public int expextedseq=0;//期待的序号
	public LinkedList<TCP_PACKET> packets = new LinkedList<TCP_PACKET>();//缓存列表
	 Queue<int[]> dataQueue = new LinkedList<int[]>();//提交数据
	
	public RcvWindow(Client client){
		this.client=client;
	}

	public int rcv(TCP_PACKET rcvpacket) {
		//如果大于等于期待的seq，则将数据包缓存。
		int CurrentAck = (rcvpacket.getTcpH().getTh_seq()-1)/100;
		if(CurrentAck>=this.expextedseq){
			//找到合适的位置存放数据包
			int index=0;
			while(index < this.packets.size() && CurrentAck > (this.packets.get(index).getTcpH().getTh_seq()-1)/100){
				index++;
			}
			
			System.out.println("CurrentACK:"+CurrentAck);
			System.out.println("Index:"+index);
			System.out.println("size:"+this.packets.size());
			
			
			if(index==this.packets.size()||CurrentAck!=(this.packets.get(index).getTcpH().getTh_seq()-1)/100){
				this.packets.add(index,rcvpacket);
				System.out.println("size:"+(this.packets.get(index).getTcpH().getTh_seq()-1)/100);
			}
		}
		
		//滑动窗口
		this.slid();
		System.out.println("expextedseq:"+this.expextedseq);
		
		return this.expextedseq-1;
	}

	public void slid() {
		//第一个数据包收到，就滑动
				while(!this.packets.isEmpty()&&(this.packets.getFirst().getTcpH().getTh_seq()-1)/100==this.expextedseq){
					this.dataQueue.add(this.packets.poll().getTcpS().getData());
					this.expextedseq++;
				}
				//累积到20个包或者到发送结束，向上提交数据
				if(this.dataQueue.size()>=20 || this.expextedseq==1000){
					this.deliver_data();
				}
		
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
