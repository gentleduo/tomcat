package com.duo;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;

public class NioTest {

    @Test
    public void server() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8888));
        serverSocketChannel.configureBlocking(false);

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 当前select上是不是存在可以进行io的key了
        while (selector.select() > 0) {
            // 有哪些可以进行io的key
            Set<SelectionKey> keys =  selector.selectedKeys();

            // 遍历并处理每一个key
            for (SelectionKey key: keys) {
                // 如果是一个可读事件，那么就可以开始读
                if (key.isReadable()) {
                    // 获取当前key所对应的channel
                    SocketChannel channel = (SocketChannel) key.channel();

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    while (channel.read(byteBuffer) != -1) {
                        byteBuffer.clear();
                    }

//                    channel.write("");


                    channel.close();
                }
            }

        }



    }
}
