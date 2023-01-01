package com.ouc.tcp.test;

import java.util.Arrays;
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
	public int size=20;//窗口大小
	private int cwnd=1;//拥塞窗口
	private int ssthresh=16;//门限
	private int count=0;// 拥塞避免：cwnd=cwnd+count/cwnd。每次对count++，当count==cwnd，即一轮后，cwnd++
	private Client client;//发送
	private int dupseq=-1,dupseqcount=1;//快重传，重复确认的序号和重复包数量
	
	private Map<Integer,TCP_PACKET> packets =  new LinkedHashMap<Integer, TCP_PACKET>();//储存数据包
	private int mapHead=0;
	
	private int[] cwndChangeList = new int[1000];  // 用于记录 cwnd 变化
    private int cwndChangeListIndex = 0; // 记录数组下标
    private int[] ssthreshChangeList = new int[1000];  // 用于记录 ssthresh 变化
    private int ssthreshChangeListIndex = 0;  // 记录数组下标
    
	//private TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包
	//private UDT_Timer[] timers = new UDT_Timer[size];  // 存储计时器

	private UDT_Timer timer;
	private My_UDT_RetransTask task;
	
	
	public SendWindow(Client client){
		this.client=client;
	}
	
	// 判断窗口是否已满
	public boolean isFull() {
		return this.cwnd <= this.packets.size();
	}
	
    /*向窗口中加入包*/
    public void putPacket(TCP_PACKET packet) {
// SR
//        packets[nextseq] = packet;  	// 在窗口的插入位置放入包
//        timers[nextseq] = new UDT_Timer();  // 为新放入窗口内的包增加计时器
//        My_UDT_RetransTask task = new My_UDT_RetransTask(client, packet);	// 设置重传任务
//        timers[nextseq].schedule(task, 3000, 3000);	// 每隔3s执行一次重传，直到收到ACK为止
//        nextseq++;  // 更新窗口的插入位置
        
// GBN
//        if (nextseq == 0) {  			// 如果在窗口左沿，则要开启计时器
//        	timer = new UDT_Timer();	// 设置计时器
//        	My_UDT_RetransTask task = new My_UDT_RetransTask(client, packets);	// 设置重传任务
//        	
//        	// 每隔3s执行一次重传，直到收到ACK为止
//        	timer.schedule(task, 3000, 3000);
//        }
		int CurrentSeq=(packet.getTcpH().getTh_seq()-1)/100;
		this.packets.put(CurrentSeq, packet);
		
		if(this.timer==null) {
			this.timer = new UDT_Timer();	// 设置计时器
			this.task = new My_UDT_RetransTask(client, this);	// 设置重传任务
			this.timer.schedule(task, 3000,3000);
		}
		
		// 如果是最后一个分组，则输出记录数组
        if (CurrentSeq == 999) {
            System.out.println("***** Change List Show *****");
            System.out.println(Arrays.toString(cwndChangeList));
            System.out.println(Arrays.toString(ssthreshChangeList));
            System.out.println("***** Show End *****");
        } else if (CurrentSeq == 0) {
            cwndChangeList[cwndChangeListIndex ++] = cwnd;
            ssthreshChangeList[ssthreshChangeListIndex ++] = ssthresh;
        }
    }
    
    /*在 cwnd ssthresh 和 ack 变化列表中加入新值*/
    public void appendChange(int currentSequence) {
        cwndChangeList[cwndChangeListIndex ++] = cwnd;
        ssthreshChangeList[ssthreshChangeListIndex ++] = ssthresh;
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
		System.out.println("*******************");
		System.out.println("cwnd is：" + this.cwnd);
		System.out.println("ssthresh is：" + this.ssthresh);
		System.out.println();
		
		if(CurrentAck==this.dupseq){
			// 如果说收到的包的序号和前一个包的序号相同，就需要对重复包的个数进行累加，如果累加个数超过三个就要实施快重传、快恢复
			this.dupseqcount++;
			if(this.dupseqcount==4){
				
				// 快重传 
				TCP_PACKET packet=this.packets.get(CurrentAck+1);
				// 连续收到3个对上一个包的ACK 获取下一个应该重传的包的ACK
				if(packet!=null){
					System.out.println("连续收到3个ACK");
					System.out.println("执行快重传");
					System.out.println();
					
					this.client.send(packet);
					
					if(this.timer!=null){
						this.timer.cancel();
					}
					this.timer = new UDT_Timer();
					this.task = new My_UDT_RetransTask(client, this);
					this.timer.schedule(task, 3000,3000);
				}
				// 执行快恢复
				fastRecovery();
			}
			appendChange(CurrentAck);
		}else{
			// 没有重复ACK
			
			// 收到新的ACK 清除前面的包
			for(int i=this.mapHead;i<=CurrentAck;i++){
				this.packets.remove(i);
			}
			
			// 链表的起始位置为当前的下一个
			this.mapHead=CurrentAck+1;
			
			// 清空计时器
			if(this.timer!=null){
				this.timer.cancel();
			}
			
			// 如果窗口中仍有分组 重开计时器
			if(this.packets.size()!=0){
				this.timer= new UDT_Timer();
				this.task = new My_UDT_RetransTask(client, this);
				this.timer.schedule(task, 3000,3000);
			}
			
			this.dupseq = CurrentAck;	// dupseq记录当前ACK 在下一个包到来后 相当于之前的ACK
			this.dupseqcount=1;			// 重置为1
			
			// 慢开始
			if(this.cwnd < this.ssthresh){
				System.out.println("********慢开始********");
				System.out.println("cwnd:"+this.cwnd+"--->"+(this.cwnd+1));
				this.cwnd++;	// 每收到一个ACK cwnd++
				
				appendChange(CurrentAck);
			}else{
				// 拥塞避免
				this.count++;
				if(count>=this.cwnd){
					this.count=this.count-this.cwnd;
					System.out.println("*******拥塞避免*********");
					System.out.println("cwnd:"+this.cwnd+"--->"+(this.cwnd+1));
					this.cwnd++;
					appendChange(CurrentAck);
				}
			}
		}
	}

	private void fastRecovery() {
		System.out.println("*******************");
		System.out.println("快恢复阶段");
		System.out.println("*******************");
		
		this.ssthresh = this.cwnd/2;
		if(this.ssthresh < 2)
			// ssthresh 不得小于2
			this.ssthresh = 2;
		// Tahoe cwnd 变为 1
		System.out.println("cwnd:"+this.cwnd+"--->"+this.ssthresh);
		//this.cwnd = 1;
		this.cwnd = this.ssthresh;
		
		
		System.out.println("ssthresh is :"+this.ssthresh);
	}

	public void multiDecrease() {
		System.out.println("*******************");
		System.out.println("乘法减小");
		System.out.println("*******************");
		System.out.println("cwnd is :"+this.cwnd);
		System.out.println("ssthresh is :"+this.ssthresh);
		
		this.ssthresh=this.cwnd/2;
		if(this.ssthresh<2)
			this.ssthresh=2;
		System.out.println("cwnd:"+this.cwnd+"--->1");
		this.cwnd=1;
		
		System.out.println("ssthresh is :"+this.ssthresh);
	}

	public void retrand() {
		System.out.println("*******************");
		System.out.println("开始重传");
		System.out.println("*******************");
		
		this.timer.cancel();
		for(int i=this.mapHead,t=0;t<this.packets.size();t++,i++){
			TCP_PACKET packet=this.packets.get(i);
			if(packet!=null){
				System.out.println("retrand:   "+(packet.getTcpH().getTh_seq()-1)/100);
				this.client.send(packet);
			}
		}
		if(this.packets.size()!=0){
			this.timer= new UDT_Timer();
			this.task = new My_UDT_RetransTask(client, this);
			this.timer.schedule(task, 3000,3000);
		}
	}

	
}


