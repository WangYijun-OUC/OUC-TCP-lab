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
//	private int cwnd=1;//拥塞窗口
//	private int ssthresh=16;//门限
//	private int count=0;// 拥塞避免：cwnd=cwnd+count/cwnd。每次对count++，当count==cwnd，即一轮后，cwnd++
	private Client client;//发送
	//private int dupseq=-1,dupseqcount=1;//快重传，重复确认的序号和重复包数量
	private TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包
//	private Map<Integer,TCP_PACKET> packets =  new LinkedHashMap<Integer, TCP_PACKET>();//储存数据包
	private int mapHead=0;
	
	private UDT_Timer timer;
	private My_UDT_RetransTask task;
	
	
	public SendWindow(Client client){
		this.client=client;
	}
	
	// 判断窗口是否已满
	public boolean isFull() {
		//return this.cwnd <= this.packets.size();
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
	
//	public void send(TCP_PACKET tcpPack) {
//		int CurrentSeq=(tcpPack.getTcpH().getTh_seq()-1)/100;
//		this.packets.put(CurrentSeq, tcpPack);
//		
//		if(this.timer==null) {
//			this.timer=new UDT_Timer();
//			this.task = new OverTimerTask(this);
//			this.timer.schedule(task, 3000,3000);
//		}
//		
//	}
	
	public void rcv(int CurrentAck){
//		System.out.println("*******************");
//		System.out.println("now cwnd is："+this.cwnd);
//		System.out.println("now ssthresh is："+this.ssthresh);
//		if(CurrentAck==this.dupseq){//如果说收到的包的序号和前一个包的序号相同，就需要对重复包的个数进行累加，如果累加个数超过三个就要实施快恢复
//			this.dupseqcount++;
//			if(this.dupseqcount==4){
//				TCP_PACKET packet=this.packets.get(CurrentAck+1);
//				if(packet!=null){
//					this.client.send(packet);
//					
//					if(this.timer!=null){
//						this.timer.cancel();
//					}
//					this.timer = new UDT_Timer();
//					//this.task = new OverTimerTask(this);
//					//this.timer.schedule(task, 3000,3000);
//				}
//				//fastRecovery();
//			}
//		}else{//这个应该是没有出现包重复的现象
//			
//			for(int i=this.mapHead;i<=CurrentAck;i++){
//				this.packets.remove(i);
//			}
//			this.mapHead=CurrentAck+1;
//			
//			if(this.timer!=null){
//				this.timer.cancel();
//			}
//			
//			if(this.packets.size()!=0){
//				this.timer= new UDT_Timer();
//				//this.task = new OverTimerTask(this);
//				//this.timer.schedule(task, 3000,3000);
//			}
//			
//			this.dupseq=CurrentAck;
//			this.dupseqcount=1;
//			
//			if(this.cwnd<this.ssthresh){
//				this.cwnd++;
//			}else{
//				this.count++;
//				if(count>=this.cwnd){
//					this.count=this.count-this.cwnd;
//					this.cwnd++;
//				}
//			}
//		}
		
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

//	private void fastRecovery() {
//		System.out.println("*******************");
//		System.out.println("this is fastRecovery");
//		System.out.println("*******************");
//		
//		this.ssthresh=this.cwnd/2;
//		if(this.ssthresh<2)
//			this.ssthresh=2;
//		this.cwnd=this.ssthresh;
//		
//		System.out.println("the cwnd is :"+this.cwnd);
//		System.out.println("the ssthresh is :"+this.ssthresh);
//	}

//	public void multiDecrease() {
//		System.out.println("*******************");
//		System.out.println("this is multiDecrease");
//		System.out.println("*******************");
//		System.out.println("the cwnd is :"+this.cwnd);
//		System.out.println("the ssthresh is :"+this.ssthresh);
//		
//		this.ssthresh=this.cwnd/2;
//		if(this.ssthresh<2)
//			this.ssthresh=2;
//		this.cwnd=1;
//		
//		System.out.println("the cwnd is :"+this.cwnd);
//		System.out.println("the ssthresh is :"+this.ssthresh);
//	}

//	public void retrand() {
//		System.out.println("*******************");
//		System.out.println("this is retrand");
//		System.out.println("*******************");
//		
//		this.timer.cancel();
//		for(int i=this.mapHead,t=0;t<this.packets.size();t++,i++){
//			TCP_PACKET packet=this.packets.get(i);
//			if(packet!=null){
//				System.out.println("retrand:   "+(packet.getTcpH().getTh_seq()-1)/100);
//				this.client.send(packet);
//			}
//		}
//		if(this.packets.size()!=0){
//			this.timer= new UDT_Timer();
//			//this.task = new OverTimerTask(this);
//			//this.timer.schedule(task, 3000,3000);
//		}
//	}

	
}


