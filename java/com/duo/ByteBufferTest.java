package com.duo;

import java.nio.ByteBuffer;

public class ByteBufferTest {

    public static void main(String[] args) {
        String str = "abc";

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());

        System.out.println("---------put-----------");
        byteBuffer.put(str.getBytes());
        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());

        System.out.println("---------flip-----------");
        byteBuffer.flip();
        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());

        System.out.println("---------get-----------");
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes);
        System.out.println(new String(bytes));
        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());

        System.out.println("---------rewind-----------");
        byteBuffer.rewind();
        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());

        System.out.println("---------clear-----------");
        byteBuffer.clear();
        System.out.println(byteBuffer.capacity());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.position());
        System.out.println(byteBuffer.get());
    }
}
