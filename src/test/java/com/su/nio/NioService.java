package com.su.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * NIO服务端
 */
public class NioService {
    private ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024);
    private ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024);
    private Selector selector;

    /**
     * 初始化服务端
     */
    public void init() throws IOException {
        //1.创建selector
        selector = Selector.open();
        //2.通过ServiceSocketChannel创建Channel通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //3.为channel绑定监听端口
        serverSocketChannel.socket().bind(new InetSocketAddress(9090));
        //4.设置channel为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //5.将channel注册到selector上 监听连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //6.循环等待新接入的连接
        listen();
    }

    private void listen() throws IOException {

        // block api
        while(selector.select()>0){

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                // 新连接
                if(selectionKey.isAcceptable()){
                    System.out.println("isAcceptable");
                    ServerSocketChannel server = (ServerSocketChannel)selectionKey.channel();

                    // 新注册channel
                    SocketChannel socketChannel  = server.accept();
                    if(socketChannel==null){
                        continue;
                    }
                    socketChannel.configureBlocking(false);
                    // 注意！这里和阻塞io的区别非常大，在编码层面之前的等待输入已经变成了注册事件，这样我们就可以在等待的时候做别的事情，
                    // 比如监听更多的socket连接，也就是之前说了一个线程监听多个socket连接。这也是在编码的时候最直观的感受
                    socketChannel.register(selector, SelectionKey.OP_READ| SelectionKey.OP_WRITE);


                    ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                    buffer.put("hi new channel".getBytes());
                    buffer.flip();
                    socketChannel.write(buffer);
                }

                // 服务端关心的可读，意味着有数据从client传来了，根据不同的需要进行读取，然后返回
                if(selectionKey.isReadable()){
                    System.out.println("isReadable");
                    SocketChannel socketChannel = (SocketChannel)selectionKey.channel();

                    readBuffer.clear();
                    socketChannel.read(readBuffer);
                    readBuffer.flip();

                    String receiveData= Charset.forName("UTF-8").decode(readBuffer).toString();
                    System.out.println("receiveData:"+receiveData);

                    // 把读到的数据绑定到key中
                    selectionKey.attach("server message echo:"+receiveData);
                }

                // 实际上服务端不在意这个，这个写入应该是client端关心的，这只是个demo,顺便试一下selectionKey的attach方法
                if(selectionKey.isWritable()){
                    SocketChannel socketChannel = (SocketChannel)selectionKey.channel();

                    String message = (String) selectionKey.attachment();
                    if(message==null){
                        continue;
                    }
                    selectionKey.attach(null);

                    writeBuffer.clear();
                    writeBuffer.put(message.getBytes());
                    writeBuffer.flip();
                    while(writeBuffer.hasRemaining()){
                        socketChannel.write(writeBuffer);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        NioService nioService = new NioService();
        nioService.init();
    }
}