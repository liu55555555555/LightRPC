package com.Lrpc.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

public class IdGenerator {

    // 起始时间戳
    private static final long START_STAMP = DateUtils.get("2023-01-01").getTime();
    // 机房号占用的比特的大小：最大为5比特
    public static final long DATA_CENTER_BIT = 5L;
    // 机器号占用的大小
    public static final long MACHINE_BIT = 5L;
    // 序列号占用的大小
    public static final long SEQUENCE_BIT = 12L;

    // 最大值 这里不使用纯数学计算最大值：Math.pow(2,5) -1，我们使用位运算计算最大值，因为位运算比纯数学计算效率更高，所以生成随机id值的效率更高
    public static final long DATA_CENTER_MAX = ~(-1L << DATA_CENTER_BIT);// 相当于：(2^5)-1，代表机房号从0开始最大到31
    public static final long MACHINE_MAX = ~(-1L << MACHINE_BIT);
    public static final long SEQUENCE_MAX = ~(-1L << SEQUENCE_BIT);// 相当于：(2^12)-1

    //最后咱们是要把这些信息都拼在一起，我们可以先左移在或运算，最后的结果：
    // 时间戳(42) 机房号(5) 机器号(5) 序列号(12)
    // 101010101010101010101010101010101010 101010 101010 101010101010
    public static final long TIMESTAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;// 时间戳左移22位
    public static final long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;// 机房号左移17位
    public static final long MACHINE_LEFT = SEQUENCE_BIT; // 机器号左移12位

    private long dataCenterId;
    private long machineId;
    private LongAdder sequenceId = new LongAdder();
    // 处理时钟回拨问题:当前的机器的时间相较于专门的时间机器变快了，当前机器就要回拨到与时间机器相同的时间，导致之后生成的时间戳重复。所以我们要记录一下上一次生成的时间戳。
    private long lastTimeStamp = -1L;

    public IdGenerator(long dataCenterId, long machineId) {
        // 判断传进来的参数是否合法
        if(dataCenterId > DATA_CENTER_MAX || dataCenterId < 0 || machineId > MACHINE_MAX || machineId < 0){
            throw new IllegalArgumentException("参数不合法");
        }

        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }


    //todo :改进加synchronized锁的方案
    public synchronized long getId(){
        // 获取当前时间戳
        long nowTimestamp = System.currentTimeMillis();

        long timestamp = nowTimestamp - START_STAMP;


        // 判断时钟回拨
        if(timestamp < lastTimeStamp){
            throw new RuntimeException("您的服务器进行了时钟回拨");
        }

        // sequenceId 需要做一些处理，如果是同一个时间节点，要自增。如果自增的时候发现序列号达到最大值了，那么还需要重置序列号，并获取下一个时间戳。
        if(timestamp==lastTimeStamp){
            sequenceId.increment();
            // 注意这里不能直接重置sequenceId值，因为这里面sequenceId自增本来就是要解决同一个时间戳下会生成多个序列号，如果不自增那么整个在此时间戳下的id值就重复的问题，但是如果直接重置sequenceId的话，在当前时间戳还没有改变的情况下可能会产生重复的id值。
            if(sequenceId.sum() >= SEQUENCE_MAX){
                timestamp = nextTimeStamp();
                sequenceId.reset();
            }
        }else {
            sequenceId.reset();
        }

        lastTimeStamp = timestamp;

        long sequence = sequenceId.sum(); // sequenceId.sum()获取当前累加到的数值,也就是获取当前的数值
        return timestamp<<TIMESTAMP_LEFT | dataCenterId<<DATA_CENTER_LEFT | machineId<<MACHINE_LEFT | sequence;
    }



    public long nextTimeStamp(){
        long timestamp = System.currentTimeMillis() - START_STAMP;
        while (timestamp <= lastTimeStamp){
            timestamp = System.currentTimeMillis() - START_STAMP;
        }
        return timestamp;
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new IdGenerator(1,2);
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> System.out.println(idGenerator.getId())).start();
        }
    }

}

