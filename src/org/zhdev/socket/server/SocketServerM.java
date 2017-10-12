package org.zhdev.socket.server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zhdev.socket.entity.Users;
import org.zhdev.socket.entity.MessageBean;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 16:14 2017/10/9
 * @Modified By :
 */
public class SocketServerM {

    boolean started = false;
    ServerSocket ss = null;
    //List<ServiceClient> clients = new ArrayList<ServiceClient>();

    private ConcurrentMap<Users, ServiceClient> clients = new ConcurrentHashMap<Users, ServiceClient>();

    public static void main(String[] args) {
        new SocketServerM().start();
    }

    public Users getSystemUser() {
        Users systemUser = new Users();
        systemUser.setClientId("0");
        systemUser.setUserName("system");
        systemUser.setDeviceType("server");
        return systemUser;
    }


    public void start() {
        try {
            ss = new ServerSocket(8888);
            started = true;
            System.out.println("端口已开启,占用8888端口号....");
        } catch (BindException e) {
            System.out.println("端口使用中....");
            System.out.println("请关掉相关程序并重新运行服务器！");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (started) {
                Socket s = ss.accept();
                ServiceClient serviceClient = new ServiceClient(s);//创建服务器线程
                System.out.println("a client connected!");

                Thread threadTemp = new Thread(serviceClient);//启动线程 , 返回线程实例
                threadTemp.start();

                Users user = new Users();//创建用户实体
                user.setClientId(String.valueOf(threadTemp.getId()));//设置用户ClientId , (线程号)
                serviceClient.setUser(user);//设置线程内部用户对象

                clients.put(user, serviceClient);//维护在在线的列表中
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServiceClient implements Runnable {//服务客户端线程
        private Socket s;
        private ObjectInputStream dis = null;
        private ObjectOutputStream dos = null;
        private boolean bConnected = false;
        private Timer heartbeatTimer = new Timer();
        //private String clientId;
        private Users user;

        public Users getUser() {
            return user;
        }

        public void setUser(Users user) {
            this.user = user;
        }

        public ServiceClient(Socket s) {
            this.s = s;
            try {
                dis = new ObjectInputStream(s.getInputStream());
                dos = new ObjectOutputStream(s.getOutputStream());
                bConnected = true;

                //要求登录信息
                /*MessageBean messageBean = new MessageBean();
                Date nowDate = new Date();
                messageBean.setMsgContent(String.valueOf(nowDate.getTime()));
                messageBean.setMsgType("system-To-login");
                messageBean.setMsgDate(nowDate);*/

                //返回它的预登录用户信息
                /*Users userTemp = new Users();
                userTemp.setClientId(super.toString());
                messageBean.setUser(userTemp);*/

                //this.user = userTemp;//设置预登陆模板信息
                //send(messageBean);//通知客户端服务器需要你的用户信息

                heartbeatTimer.schedule(new TimerTask() {
                    public void run() {
                        //只有你客户端补全了信息之后,才会发送心跳包
                        //if(null!=user.getUserName() && !"".equals(user.getUserName())){
                        System.out.println(new Date().getTime() + "五秒一次的心跳包");
                        MessageBean messageBean = new MessageBean();
                        Date nowDate = new Date();
                        messageBean.setMsgContent(String.valueOf(nowDate.getTime()));
                        messageBean.setMsgType("heartbeat-data-Timer");
                        //messageBean.setToClientId(super.);
                        messageBean.setFromClientId("0");
                        messageBean.setUser(getSystemUser());
                        send(messageBean);
                        //如果发送失败 , 抛出IO异常,该发送方法认定为发送失败 , 从处理线程池中删除该客户端
                        // }
                    }
                }, 5000, 5000);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(MessageBean messageBean) {
            try {
                dos.writeObject(messageBean);
            } catch (IOException e) {//尝试发送,如果失败
                clients.remove(user);//删除当前线程
                heartbeatTimer.cancel();//关闭心跳包的发送
                System.out.println("对方退出了！我从List里面去掉了！");
            }
        }

        public void getOnlineList() {
            try {
                System.out.println("获取在线列表....");
                Set<Users> onLineSet = clients.keySet();
                JSONObject responseJson = new JSONObject();
                JSONArray onLineJsonArray = new JSONArray();
                for (Users onLineUser : onLineSet) {
                    JSONObject userJsonObj = new JSONObject(onLineUser);
                    onLineJsonArray.put(userJsonObj);
                }
                responseJson.put("data", onLineJsonArray);
                MessageBean response = new MessageBean();
                response.setMsgType("getOnlineListResult");
                response.setMsgContent(responseJson.toString());
                response.setToClientId(user.getClientId());
                response.setFromClientId("0");
                response.setUser(getSystemUser());
                send(response);//返还给客户端
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void login(MessageBean request) {
            try {
                String responseMsg = "login is failed!";
                int code = 400;
                Users user = null;
                for (Map.Entry<Users, ServiceClient> entry : clients.entrySet()) {
                    if (this.equals(entry.getValue())) {
                        user = entry.getKey();
                    }
                }
                if (user != null) {
                    String msgContent = request.getMsgContent();
                    JSONObject contentJsonObj = null;
                    contentJsonObj = new JSONObject(msgContent);
                    String deviceType = contentJsonObj.getString("deviceType");
                    String userName = contentJsonObj.getString("userName");
                    user.setDeviceType(deviceType);
                    user.setUserName(userName);
                    setUser(user);
                    responseMsg = "login is success";
                    code = 200;
                }
                MessageBean response = new MessageBean();
                JSONObject responseContentJson = new JSONObject();
                responseContentJson.put("msg", responseMsg);
                responseContentJson.put("code", code);
                response.setMsgContent(responseContentJson.toString());
                response.setMsgType("loginResult");
                response.setToClientId(user.getClientId());
                response.setFromClientId("0");
                response.setUser(getSystemUser());
                send(response);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                while (bConnected) {
                    //String str = dis.readUTF();
                    MessageBean messageBean = (MessageBean) dis.readObject();
                    System.out.println("客户端消息:" + messageBean.getMsgContent());
                    String msgType = messageBean.getMsgType();

                    switch (msgType) {
                        case "login":
                            login(messageBean);
                            break;
                        case "getOnlineList":
                            getOnlineList();
                            break;

                        case "ForwardCommandToUser"://转发命令到指定用户
                            try {
                                String toClientId = messageBean.getToClientId();
                                String fromClientId = messageBean.getFromClientId();
                                String msgContent = messageBean.getMsgContent();

                                JSONObject msgContentJsonObj = new JSONObject(msgContent);
                                String command = msgContentJsonObj.getString("command");
                                String remark = msgContentJsonObj.getString("remark");

                                ServiceClient serviceClient = null;//找到要发送的用户线程
                                for (Map.Entry<Users, ServiceClient> entry : clients.entrySet()) {
                                    Users userTemp = entry.getKey();//每个user
                                    String userClientId = userTemp.getClientId();
                                    if(toClientId.equals(userClientId)){
                                        serviceClient = entry.getValue();
                                        break;
                                    }
                                }
                                if(serviceClient==null){
                                    //错误返回
                                    MessageBean errorResponseMsgBean = new MessageBean();
                                    errorResponseMsgBean.setToClientId(toClientId);
                                    errorResponseMsgBean.setFromClientId("0");
                                    errorResponseMsgBean.setMsgContent("对方可能已经下线");
                                    errorResponseMsgBean.setMsgType("UserOffline");
                                    errorResponseMsgBean.setUser(getSystemUser());
                                    send(errorResponseMsgBean);//告知发送失败 , 对方可能已经下线了2
                                }else{
                                    //发送到目的地
                                    serviceClient.send(messageBean);//这里要使用目的地的线程发送
                                    //告知转发成功 , 告知已经转发成功
                                    MessageBean errorResponseMsgBean = new MessageBean();
                                    errorResponseMsgBean.setToClientId(toClientId);
                                    errorResponseMsgBean.setFromClientId("0");
                                    errorResponseMsgBean.setMsgContent("命令成功送达!");
                                    errorResponseMsgBean.setMsgType("MessageForwardSuccess");
                                    errorResponseMsgBean.setUser(getSystemUser());
                                    send(errorResponseMsgBean);//告知发送失败 , 对方可能已经下线了
                                }

                            } catch (JSONException jsonE) {
                                jsonE.printStackTrace();
                            }


                            break;

                        default:
                            break;
                    }

                   /* String msgType = messageBean.getMsgType();
                    String msgContent = messageBean.getMsgContent();
                    Users responseUsers = messageBean.getUser();*/



                    /*if(null!=msgType && "client-login".equals(msgType)){
                        //如果客户端表明了用户名和设备类型
                        if(null!=responseUsers.getUserName() && null!=responseUsers.getDeviceType()){
                            this.user = responseUsers;//更新原先的预登陆模板用户信息
                            clients.put(this.user,this);//维护在在线的列表中

                            MessageBean toClientLoginSuccess =new MessageBean();
                            toClientLoginSuccess.setMsgType("system-To-loginSuccess");
                            toClientLoginSuccess.setMsgContent("is logined");
                            toClientLoginSuccess.setMsgDate(new Date());
                            toClientLoginSuccess.setFromClientId("0");//系统发出的消息客户端ID为0
                            toClientLoginSuccess.setToClientId(this.user.getClientId());
                            toClientLoginSuccess.setUser(getSystemUser());//获取系统用户信息

                            this.send(messageBean);//通知其在服务器端维护信息成功

                        }
                    }*/

                    /*for (int i = 0; i < clients.size(); i++) {
                        ServiceClient c = clients.get(i);
                        c.send(messageBean);//读到客户端发送过来的数据 , 转发给所有客户端
                    }*/
                }
            } catch (EOFException e) {
                System.out.println("Client closed!");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dis != null)
                        dis.close();
                    if (dos != null)
                        dos.close();
                    if (s != null) {
                        s.close();
                    }

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
