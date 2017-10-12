package org.zhdev.socket.client;

import org.zhdev.socket.entity.MessageBean;
import org.zhdev.socket.server.SocketServerM;
import sun.plugin2.message.Message;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Scanner;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 16:25 2017/10/9
 * @Modified By :
 */
public class Client {

    ObjectOutputStream dos = null;
    ObjectInputStream dis = null;

    private boolean bConnected = false;
    private Socket clientSocket;
    private Thread receviceThreadHandle;
    //private OutputStream outputStream;
    //private PrintWriter printWriter;
    //private BufferedReader br;
    //private InputStream is;

    public void connectServer(String host, int port) throws IOException {
        //1.创建客户端 ,连接到指定host和端口
        clientSocket = new Socket(host, port);
        //2.获取输出流，向服务器端发送信息
        dos = new ObjectOutputStream(clientSocket.getOutputStream());
        //outputStream = clientSocket.getOutputStream();//字节输出流
        //printWriter = new PrintWriter(outputStream);//将输出流包装为打印流
        //3.获取输入流，并读取服务器端的响应信息
        dis = new ObjectInputStream(clientSocket.getInputStream());
        //is=clientSocket.getInputStream();
        //br=new BufferedReader(new InputStreamReader(is));

        System.out.println("~~~~~~~~连接成功~~~~~~~~!");
        bConnected = true;
    }

    public void startReceviceData() {
        //4,启动接收线程
        ReceviceThread receviceThread = new ReceviceThread();
        System.out.println("recevice thread is started...");
        receviceThreadHandle = new Thread(receviceThread);
        receviceThreadHandle.start();
    }

    public void stopReceviceData() {

    }


    public void sendMessageToServer(MessageBean msg) {
        //printWriter.write(msg);
        //printWriter.flush();
        try {
            dos.writeObject(msg);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdownSocket() {
        //关闭资源
        try {
            bConnected = false;
            dos.close();
            dis.close();
            //printWriter.close();
            //outputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ReceviceThread implements Runnable {
        @Override
        public void run() {
            String info = null;
            try {
                while (bConnected) {
                    //String str = dis.readUTF();
                    MessageBean msg = (MessageBean) dis.readObject();
                    //if(str.indexOf("heartbeatData")==-1){
                    System.out.println("我是客户端，服务器说：" + msg.getMsgContent());
                    //}
                }
                System.out.println("接收线程已经停止...");
                /*while((info=br.readLine())!=null){
                    System.out.println("我是客户端，服务器说："+info);
                }*/
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.connectServer("localhost", 8888);
            client.startReceviceData();

            while (true) {
                System.out.println("请输入要发送到服务器的字符 : ");
                Scanner sc = new Scanner(System.in);
                String inputString = sc.next();
                if (null != inputString && !"".equals(inputString)) {
                    if (inputString.equals("exit")) {
                        break;
                    }
                    MessageBean msg = new MessageBean();
                    msg.setMsgContent("我是客户端 : "+new Date().getTime());
                    client.sendMessageToServer(msg);
                }
            }
            client.shutdownSocket();
            System.out.println("程序结束...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
