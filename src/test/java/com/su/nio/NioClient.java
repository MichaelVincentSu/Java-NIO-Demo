package com.su.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class NioClient {
    private final ByteBuffer sendBuffer=ByteBuffer.allocate(1024);
    private final ByteBuffer receiveBuffer=ByteBuffer.allocate(1024);
    private Selector selector;

    public void init() throws IOException {
        //1.创建selector
        selector = Selector.open();
        //2.创建SocketChannel
        SocketChannel socketChannel = SocketChannel.open();
        //3.设置非阻塞
        socketChannel.configureBlocking(false);
        //4.配置连接的地址
        socketChannel.connect(new InetSocketAddress("localhost", 9090));
        //5.注册channel
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        listen();
    }

    public void listen() throws IOException {
        while (selector.select() > 0 ){

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()){
                SelectionKey key = it.next();
                it.remove();

                if (key.isReadable()) {
                    receive(key);
                }
                // 实际上只要注册了关心写操作，这个操作就一直被激活
                if (key.isWritable()) {
                    send(key);
                }
            }

        }
    }

    private void send(SelectionKey key)throws IOException{
        SocketChannel socketChannel=(SocketChannel)key.channel();
        synchronized(sendBuffer){
            sendBuffer.flip(); //设置写
            while(sendBuffer.hasRemaining()){
                socketChannel.write(sendBuffer);
            }
            sendBuffer.compact();
        }
    }
    private void receive(SelectionKey key)throws IOException{
        SocketChannel socketChannel=(SocketChannel)key.channel();
        socketChannel.read(receiveBuffer);
        receiveBuffer.flip();
        String receiveData= Charset.forName("UTF-8").decode(receiveBuffer).toString();

        System.out.println("receive server message:"+receiveData);
        receiveBuffer.clear();
    }

    private void receiveFromUser() {
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(System.in));
        try{
            String msg;
            while ((msg = bufferedReader.readLine()) != null){
                synchronized(sendBuffer){
                    sendBuffer.put((msg+"\r\n").getBytes());
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NioClient nioClient = new NioClient();
        try {
            nioClient.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}