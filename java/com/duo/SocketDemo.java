package com.duo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketDemo {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket("localhost",8080);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(createMixHttp().getBytes());
            byte[] bytes = new byte[2 * 1024];
            int len;
            while ((len = inputStream.read(bytes, 0, bytes.length)) > 0) {
                System.out.println(new String(bytes, 0, len));
            }

        } catch (IOException ioException) {
            System.out.println(ioException.getStackTrace());
        }
    }

    public static String createPostHttp() {

        StringBuilder stringBuilder = new StringBuilder();
        //请求行
        stringBuilder.append("POST /webapp_archive/hello HTTP/1.1\r\n");
        //请求头
        stringBuilder.append("Content-Type: application/json\r\n");
        stringBuilder.append("User-Agent: PostmanRuntime/7.26.8\r\n");
        stringBuilder.append("Accept: */*\r\n");
        stringBuilder.append("Cache-Control: no-cache\r\n");
        stringBuilder.append("Postman-Token: bef87ec2-6523-4f73-b20b-5c522274a719\r\n");
        stringBuilder.append("Host: localhost:8080\r\n");
        stringBuilder.append("Accept-Encoding: gzip, deflate, br\r\n");
        stringBuilder.append("Connection: keep-alive\r\n");
        stringBuilder.append("Content-Length: 33\r\n");
        //结束请求头
        stringBuilder.append("\r\n");
        //请求体
        stringBuilder.append("{\"sex\":\"man\",\"password\":\"123456\"}");
        return stringBuilder.toString();
    }


    public static String createGetHttp() {

        StringBuilder stringBuilder = new StringBuilder();
        //请求行
        stringBuilder.append("GET /webapp_archive/hello HTTP/1.1\r\n");
        //请求头
        stringBuilder.append("User-Agent: PostmanRuntime/7.26.8\r\n");
        stringBuilder.append("Accept: */*\r\n");
        stringBuilder.append("Cache-Control: no-cache\r\n");
        stringBuilder.append("Postman-Token: a8842d2d-de76-4a8f-8134-f9450e4afa60\r\n");
        stringBuilder.append("Host: localhost:8080\r\n");
        stringBuilder.append("Accept-Encoding: gzip, deflate, br\r\n");
        stringBuilder.append("Connection: keep-alive\r\n");
        stringBuilder.append("\r\n");
        return stringBuilder.toString();
    }

    public static String createMixHttp() {

        StringBuilder stringBuilder = new StringBuilder();
        //POST请求行
        stringBuilder.append("POST /webapp_archive/hello HTTP/1.1\r\n");
        //POST请求头
        stringBuilder.append("Content-Type: application/json\r\n");
        stringBuilder.append("User-Agent: PostmanRuntime/7.26.8\r\n");
        stringBuilder.append("Accept: */*\r\n");
        stringBuilder.append("Cache-Control: no-cache\r\n");
        stringBuilder.append("Postman-Token: bef87ec2-6523-4f73-b20b-5c522274a719\r\n");
        stringBuilder.append("Host: localhost:8080\r\n");
        stringBuilder.append("Accept-Encoding: gzip, deflate, br\r\n");
        stringBuilder.append("Connection: keep-alive\r\n");
        stringBuilder.append("Content-Length: 33\r\n");
        //POST结束请求头
        stringBuilder.append("\r\n");
        //POST请求体
        stringBuilder.append("{\"sex\":\"man\",\"password\":\"123456\"}");

        //GET请求行
        stringBuilder.append("GET /webapp_archive/hello HTTP/1.1\r\n");
        //GET请求头
        stringBuilder.append("User-Agent: PostmanRuntime/7.26.8\r\n");
        stringBuilder.append("Accept: */*\r\n");
        stringBuilder.append("Cache-Control: no-cache\r\n");
        stringBuilder.append("Postman-Token: a8842d2d-de76-4a8f-8134-f9450e4afa60\r\n");
        stringBuilder.append("Host: localhost:8080\r\n");
        stringBuilder.append("Accept-Encoding: gzip, deflate, br\r\n");
        stringBuilder.append("Connection: keep-alive\r\n");
        //GET结束请求头
        stringBuilder.append("\r\n");

        return stringBuilder.toString();

    }
}
