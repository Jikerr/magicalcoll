package org.zhdev.socket.server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import org.zhdev.socket.utils.JSONObject;
//import org.zhdev.socket.utils.JSONArray;

import org.zhdev.socket.entity.BaseSocketMessageRequest;
import org.zhdev.socket.entity.BaseSocketMessageResponse;
import org.zhdev.socket.entity.Users;
import org.zhdev.socket.entity.MessageBean;
import org.zhdev.socket.utils.DateUtils;

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
                        System.out.println(new Date().getTime() + "五秒一次的心跳包");
                        try {
                            BaseSocketMessageResponse response = new BaseSocketMessageResponse();
                            response.setFromUser(getSystemUser());
                            response.setFromClientId("0");
                            response.setMsgDate(DateUtils.getNowDate());
                            response.setHandlerEvent(Constants.HandlerEvent.HREATBEAT_DATA);
                            response.setCode(200);
                            response.setMsgDateStamp(DateUtils.getNowDateStamp());
                            JSONObject responseBody = new JSONObject();
                            responseBody.put("data", "It is hreatbeat data.");
                            response.setResponseBody(responseBody);
                            response.setToClientId(user.getClientId());
                            response.setToUser(user);
                            response.setResponseActionType("null-hreatbeat-data");
                            //如果发送失败 , 抛出IO异常,该发送方法认定为发送失败 , 从处理线程池中删除该客户端
                            reponseMsg(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 5000, 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void reponseMsg(BaseSocketMessageResponse response){
            System.out.print("响应消息.");
            try {
                dos.writeObject(response);
            } catch (IOException e) {//尝试发送,如果失败
                clients.remove(user);//删除当前线程
                heartbeatTimer.cancel();//关闭心跳包的发送
            }
        }

        public void requestMsg(BaseSocketMessageRequest request){
            System.out.print("请求消息.");
            try {
                dos.writeObject(request);
            } catch (IOException e) {
                clients.remove(user);
                heartbeatTimer.cancel();
            }
        }


        public void send(MessageBean messageBean) {
            try {
                dos.writeObject(messageBean);
            } catch (IOException e) {//尝试发送,如果失败
                clients.remove(user);//删除当前线程
                heartbeatTimer.cancel();//关闭心跳包的发送
            }
        }

        public void getOnlineList() {
            try {
                System.out.println("获取在线列表....");
                Set<Users> onLineSet = clients.keySet();
                JSONArray onLineJsonArray = new JSONArray();
                for (Users onLineUser : onLineSet) {
                    JSONObject userJsonObj = new JSONObject(onLineUser);
                    onLineJsonArray.put(userJsonObj);
                }
                BaseSocketMessageResponse response = new BaseSocketMessageResponse();
                response.setFromUser(getSystemUser());
                response.setFromClientId("0");
                response.setMsgDate(DateUtils.getNowDate());
                response.setHandlerEvent(Constants.HandlerEvent.GET_ONLINE_LIST_RESULT);
                response.setCode(200);
                response.setMsgDateStamp(DateUtils.getNowDateStamp());
                JSONObject responseBody = new JSONObject();
                responseBody.put("data", onLineJsonArray);
                response.setResponseBody(responseBody);
                response.setToClientId(user.getClientId());
                response.setToUser(user);
                response.setResponseActionType(Constants.Action.GET_ONLINE_LIST);
                //如果发送失败 , 抛出IO异常,该发送方法认定为发送失败 , 从处理线程池中删除该客户端
                reponseMsg(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void login(BaseSocketMessageRequest request) {
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
                    JSONObject parameters = request.getRequestParameters();

                    String deviceType = parameters.getString("deviceType");
                    String userName = parameters.getString("userName");

                    user.setDeviceType(deviceType);
                    user.setUserName(userName);

                    setUser(user);//设置当前线程的用户对象
                    responseMsg = "login is success";
                    code = 200;
                }

                BaseSocketMessageResponse response = new BaseSocketMessageResponse();
                response.setFromUser(getSystemUser());
                response.setFromClientId("0");
                response.setMsgDate(DateUtils.getNowDate());
                response.setHandlerEvent(Constants.HandlerEvent.LOGIN_RESPONSE);
                response.setCode(200);
                response.setMsgDateStamp(DateUtils.getNowDateStamp());
                JSONObject responseBody = new JSONObject();
                responseBody.put("msg", responseMsg);
                responseBody.put("code", code);
                response.setResponseBody(responseBody);
                response.setToClientId(user.getClientId());
                response.setToUser(user);
                response.setResponseActionType(Constants.Action.LOGIN);
                //如果发送失败 , 抛出IO异常,该发送方法认定为发送失败 , 从处理线程池中删除该客户端
                reponseMsg(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void forwardCommandToUser(BaseSocketMessageRequest request){
            try {
                String toClientId = request.getToClientId();
                String fromClientId = request.getFromClientId();
                JSONObject parameters = request.getRequestParameters();

                String command = parameters.getString("command");
                String remark = parameters.getString("remark");

                ServiceClient serviceClient = null;//找到要发送的用户线程
                for (Map.Entry<Users, ServiceClient> entry : clients.entrySet()) {
                    Users userTemp = entry.getKey();//每个user
                    String userClientId = userTemp.getClientId();
                    if (toClientId.equals(userClientId)) {
                        serviceClient = entry.getValue();
                        break;
                    }
                }
                if (serviceClient == null) {
                    BaseSocketMessageResponse response = new BaseSocketMessageResponse();
                    response.setFromUser(request.getFromUser());
                    response.setFromClientId(request.getFromClientId());
                    response.setMsgDate(DateUtils.getNowDate());
                    response.setHandlerEvent(Constants.HandlerEvent.MESSAGE_FORWARD_USER_OFFLINE);
                    response.setCode(200);
                    response.setMsgDateStamp(DateUtils.getNowDateStamp());
                    response.setToClientId(request.getToClientId());
                    response.setToUser(request.getToUser());
                    response.setResponseActionType(Constants.Action.FORWARD_COMMAND_TOUSER);
                    //如果发送失败 , 抛出IO异常,该发送方法认定为发送失败 , 从处理线程池中删除该客户端
                    reponseMsg(response);

                } else {
                    BaseSocketMessageResponse forwardResponse = new BaseSocketMessageResponse();
                    forwardResponse.setFromUser(request.getFromUser());
                    forwardResponse.setFromClientId(request.getFromClientId());
                    forwardResponse.setMsgDate(DateUtils.getNowDate());
                    forwardResponse.setHandlerEvent(Constants.HandlerEvent.FROM_USER_COMMAND);
                    forwardResponse.setCode(200);
                    forwardResponse.setMsgDateStamp(DateUtils.getNowDateStamp());
                    JSONObject forWardresponseBody = new JSONObject();
                    forWardresponseBody.put("command",command );
                    forWardresponseBody.put("remark", remark);
                    forwardResponse.setResponseBody(forWardresponseBody);
                    forwardResponse.setToClientId(user.getClientId());
                    forwardResponse.setToUser(user);
                    forwardResponse.setResponseActionType(Constants.Action.FORWARD_COMMAND_TOUSER);
                    //使用对应的用户线程响应消息
                    serviceClient.reponseMsg(forwardResponse);

                    //响应给请求用户表达请求成功 ,
                    BaseSocketMessageResponse response = new BaseSocketMessageResponse();
                    response.setFromUser(request.getFromUser());
                    response.setFromClientId(request.getFromClientId());
                    response.setMsgDate(DateUtils.getNowDate());
                    response.setHandlerEvent(Constants.HandlerEvent.MESSAGE_FORWARD_SUCCESS);
                    response.setCode(200);
                    response.setMsgDateStamp(DateUtils.getNowDateStamp());
                    response.setToClientId(user.getClientId());
                    response.setToUser(user);
                    response.setResponseActionType(Constants.Action.FORWARD_COMMAND_TOUSER);
                    reponseMsg(response);

                }

            } catch (JSONException jsonE) {
                jsonE.printStackTrace();
            }
        }


        public void run() {
            try {
                while (bConnected) {
                    //String str = dis.readUTF();
                    BaseSocketMessageRequest request = (BaseSocketMessageRequest) dis.readObject();
                    System.out.println("客户端请求处理类型:" + request.getActionType());
                    System.out.println("客户端请求处理参数:" + request.getRequestParameters().toString());
                    String actionType = request.getActionType();

                    switch (actionType) {
                        case Constants.Action.LOGIN:
                            login(request);
                            break;
                        case Constants.Action.GET_ONLINE_LIST:
                            getOnlineList();
                            break;
                        case Constants.Action.FORWARD_COMMAND_TOUSER://转发命令到指定用户
                            forwardCommandToUser(request);
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
