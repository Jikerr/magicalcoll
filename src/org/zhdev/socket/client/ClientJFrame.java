package org.zhdev.socket.client;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 16:26 2017/10/16
 * @Modified By :
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zhdev.socket.entity.BaseSocketMessageRequest;
import org.zhdev.socket.entity.BaseSocketMessageResponse;
import org.zhdev.socket.entity.MessageBean;
import org.zhdev.socket.entity.Users;
import org.zhdev.socket.server.Constants;
import org.zhdev.socket.utils.DateUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class ClientJFrame{

    private JFrame frame;
    private JList userList;
    private JTextArea textArea;
    private JTextField textField;
    private JTextField txt_port;
    private JTextField txt_hostIp;
    private JTextField txt_name;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;
    private JPanel northPanel;
    private JPanel southPanel;
    private JScrollPane rightScroll;
    private JScrollPane leftScroll;
    private JSplitPane centerSplit;

    private DefaultListModel listModel;
    private boolean isConnected = false;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageThread messageThread;// 负责接收消息的线程
    private Map<String, User> onLineUsers = new HashMap<String, User>();// 所有在线用户


    /**************************************/
    ObjectOutputStream dos = null;
    ObjectInputStream dis = null;
    private boolean bConnected = false;
    private Socket clientSocket;
    private Thread receviceThreadHandle;
    private Users user;

    /**************************************/




    // 主方法,程序入口
    public static void main(String[] args) {
        new ClientJFrame();
    }

    // 执行发送
    public void send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = textField.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        //sendMessage(frame.getTitle() + "@" + "ALL" + "@" + message);

        BaseSocketMessageRequest request = new BaseSocketMessageRequest();
        request.setActionType("send.");

        SendThread sendThread = new SendThread(request);
        new Thread(sendThread).start();

        textField.setText(null);
    }

    // 构造方法
    public ClientJFrame() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.blue);
        textField = new JTextField();
        txt_port = new JTextField("8888");
        txt_hostIp = new JTextField("127.0.0.1");
        txt_name = new JTextField("xiaoqiang");
        btn_start = new JButton("连接");
        btn_stop = new JButton("断开");
        btn_send = new JButton("发送");
        listModel = new DefaultListModel();
        userList = new JList(listModel);

        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 7));
        northPanel.add(new JLabel("端口"));
        northPanel.add(txt_port);
        northPanel.add(new JLabel("服务器IP"));
        northPanel.add(txt_hostIp);
        northPanel.add(new JLabel("姓名"));
        northPanel.add(txt_name);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("连接信息"));

        rightScroll = new JScrollPane(textArea);
        rightScroll.setBorder(new TitledBorder("消息显示区"));
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("在线用户"));
        southPanel = new JPanel(new BorderLayout());
        southPanel.add(textField, "Center");
        southPanel.add(btn_send, "East");
        southPanel.setBorder(new TitledBorder("写消息"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
                rightScroll);
        centerSplit.setDividerLocation(100);

        frame = new JFrame("客户机");
        // 更改JFrame的图标：
        //frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600, 400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);

        // 写消息的文本框中按回车键时事件
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        // 单击发送按钮时事件
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // 单击连接按钮时事件
        btn_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int port;
                if (bConnected) {
                    JOptionPane.showMessageDialog(frame, "已处于连接上状态，不要重复连接!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    try {
                        port = Integer.parseInt(txt_port.getText().trim());
                    } catch (NumberFormatException e2) {
                        throw new Exception("端口号不符合要求!端口为整数!");
                    }
                    String hostIp = txt_hostIp.getText().trim();
                    String name = txt_name.getText().trim();
                    if (name.equals("") || hostIp.equals("")) {
                        throw new Exception("姓名、服务器IP不能为空!");
                    }
                    boolean flag = connectServer(port, hostIp, name);
                    if (flag == false) {
                        throw new Exception("与服务器连接失败!");
                    }
                    frame.setTitle(name);
                    JOptionPane.showMessageDialog(frame, "成功连接!");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 单击断开按钮时事件
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!bConnected) {
                    JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    boolean flag = closeConnection();// 断开连接
                    listModel.removeAllElements();
                    if (flag == false) {
                        throw new Exception("断开连接发生异常！");
                    }
                    JOptionPane.showMessageDialog(frame, "成功断开!");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 关闭窗口时事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    closeConnection();// 关闭连接
                }
                System.exit(0);// 退出程序
            }
        });
    }





    /**
     * 连接服务器
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer(int port, String hostIp, String name) {
        // 连接服务器
        try {
           /* socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            // 发送客户端用户基本信息(用户名和ip地址)
            sendMessage(name + "@" + socket.getLocalAddress().toString());
            // 开启接收消息的线程
            messageThread = new MessageThread(reader, textArea);
            messageThread.start();
            isConnected = true;// 已经连接上了*/

            //1.连接成功
            clientSocket = new Socket(hostIp, port);
            dos = new ObjectOutputStream(clientSocket.getOutputStream());
            dis = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("~~~~~~~~连接成功~~~~~~~~!");
            bConnected = true;
            //2.启动接收
            startReceviceData();

            //3.登录
            try {
                BaseSocketMessageRequest loginRequest = new BaseSocketMessageRequest();
                loginRequest.setActionType(Constants.Action.LOGIN);
                loginRequest.setToClientId("0");
                loginRequest.setToUser(new Users());
                loginRequest.setMsgDate(DateUtils.getNowDate());
                loginRequest.setMsgDateStamp(DateUtils.getNowDateStamp());
                //loginRequest.setFromClientId(new Users());
                //loginRequest.setFromUser(user);

                //MessageBean messageBean = new MessageBean();
                //messageBean.setMsgType("Login");
                //messageBean.setToClientId("0");

                JSONObject requestJson = new JSONObject();
                requestJson.put("deviceType", "PC");
                requestJson.put("userName", txt_name.getText());

                loginRequest.setRequestParameters(requestJson);
                sendMessageToServer(loginRequest);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return true;
        } catch (Exception e) {
            textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
                    + "   的服务器连接失败!" + "\r\n");
            isConnected = false;// 未连接上
            return false;
        }
    }

    public void sendMessageToServer(BaseSocketMessageRequest request) {
        SendThread sendThread = new SendThread(request);
        new Thread(sendThread).start();
    }

    public void startReceviceData() {
        //4,启动接收线程
        ReceviceThread receviceThread = new ReceviceThread();
        System.out.println("recevice thread is started...");
        receviceThreadHandle = new Thread(receviceThread);
        receviceThreadHandle.start();
    }

    public void getOnlineList(int index){
        BaseSocketMessageRequest request = new BaseSocketMessageRequest();
        request.setActionType(Constants.Action.GET_ONLINE_LIST);
        request.setFromClientId(user.getClientId());
        request.setFromUser(user);
        request.setMsgDate(DateUtils.getNowDate());
        request.setMsgDateStamp(DateUtils.getNowDateStamp());
        request.setRequestParameters(new JSONObject());
        request.setToClientId("0");
        request.setToUser(new Users());
        sendMessageToServer(request);
    }

    class ReceviceThread implements Runnable {
        @Override
        public void run() {
            String info = null;
            try {
                while (bConnected) {
                    BaseSocketMessageResponse response = (BaseSocketMessageResponse) dis.readObject();

                    String handlerEvent = response.getHandlerEvent();
                    JSONObject responseBody = response.getResponseBody();

                    if (null != handlerEvent && "".equals(handlerEvent)) {
                        System.out.println("from server handler event : " + handlerEvent);
                    }
                    if (null != response.getResponseBody()) {
                        System.out.println("from server response message : " + responseBody.toString());
                    }

                    try {
                        switch (handlerEvent) {
                            case Constants.HandlerEvent.GET_ONLINE_LIST_RESULT :
                                JSONArray onlineList = responseBody.getJSONArray("data");
                                for (int i = 0; i < onlineList.length(); i++) {
                                    JSONObject userJsonObj = onlineList.getJSONObject(i);
                                    String deviceType = userJsonObj.getString("deviceType");
                                    String userName = userJsonObj.getString("userName");
                                    String clientId = userJsonObj.getString("clientId");
                                    userName = userName + "-" + clientId;
                                    //myAdapter.addItemToFirst(userName, deviceType);
                                    //onLineUsers.put(userName, user);
                                    listModel.addElement(userName);
                                }

                                break;
                            case Constants.HandlerEvent.LOGIN_RESPONSE:
                                int code = responseBody.getInt("code");//登录结果识别码
                                user = response.getToUser();//登录成功后把用户对象保存在当前Activity
                                if (code == 200) {//登录成功 获取在线列表
                                    getOnlineList(1);
                                } else {
                                    String loginErrorMsg = responseBody.getString("msg");
                                    textArea.append("登录失败" + "\r\n");
                                }
                                break;
                            case Constants.HandlerEvent.MESSAGE_FORWARD_USER_OFFLINE://用户已经离线通知
                                textArea.append("对方离线,消息接收失败" + "\r\n");
                                break;
                            case Constants.HandlerEvent.MESSAGE_FORWARD_SUCCESS://服务器成功转发消息通知
                                textArea.append("消息(命令)发送成功" + "\r\n");
                                break;
                            case Constants.HandlerEvent.HREATBEAT_DATA:
                                break;
                            default:
                                textArea.append("处理Socket消息中失败!未知的类型!" + "\r\n");
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                System.out.println("接收线程已经停止...");

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    class SendThread implements Runnable {
        private BaseSocketMessageRequest request;

        public SendThread(BaseSocketMessageRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                System.out.println("request server actionType : " + request.getActionType());
                System.out.println("request server parameters : " + request.getRequestParameters());
                dos.writeObject(request);
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 发送消息
     *
     * @param message
     *//*
    public void sendMessage(String message) {
        writer.println(message);
        writer.flush();
    }*/

    /**
     * 客户端主动关闭连接
     */
    @SuppressWarnings("deprecation")
    public synchronized boolean closeConnection() {
            receviceThreadHandle.stop();// 停止接受消息线程
            try {
                bConnected = false;
                dos.close();
                dis.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
    }

    // 不断接收消息的线程
    class MessageThread extends Thread {
        private BufferedReader reader;
        private JTextArea textArea;

        // 接收消息线程的构造方法
        public MessageThread(BufferedReader reader, JTextArea textArea) {
            this.reader = reader;
            this.textArea = textArea;
        }

        // 被动的关闭连接
        public synchronized void closeCon() throws Exception {
            // 清空用户列表
            listModel.removeAllElements();
            // 被动的关闭连接释放资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;// 修改状态为断开
        }

        public void run() {
            String message = "";
            while (true) {
                try {
                    message = reader.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(
                            message, "/@");
                    String command = stringTokenizer.nextToken();// 命令
                    if (command.equals("CLOSE"))// 服务器已关闭命令
                    {
                        textArea.append("服务器已关闭!\r\n");
                        closeCon();// 被动的关闭连接
                        return;// 结束线程
                    } else if (command.equals("ADD")) {// 有用户上线更新在线列表
                        String username = "";
                        String userIp = "";
                        if ((username = stringTokenizer.nextToken()) != null
                                && (userIp = stringTokenizer.nextToken()) != null) {
                            User user = new User(username, userIp);
                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                        }
                    } else if (command.equals("DELETE")) {// 有用户下线更新在线列表
                        String username = stringTokenizer.nextToken();
                        User user = (User) onLineUsers.get(username);
                        onLineUsers.remove(user);
                        listModel.removeElement(username);
                    } else if (command.equals("USERLIST")) {// 加载在线用户列表
                        int size = Integer
                                .parseInt(stringTokenizer.nextToken());
                        String username = null;
                        String userIp = null;
                        for (int i = 0; i < size; i++) {
                            username = stringTokenizer.nextToken();
                            userIp = stringTokenizer.nextToken();
                            User user = new User(username, userIp);
                            onLineUsers.put(username, user);
                            listModel.addElement(username);
                        }
                    } else if (command.equals("MAX")) {// 人数已达上限
                        textArea.append(stringTokenizer.nextToken()
                                + stringTokenizer.nextToken() + "\r\n");
                        closeCon();// 被动的关闭连接
                        JOptionPane.showMessageDialog(frame, "服务器缓冲区已满！", "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;// 结束线程
                    } else {// 普通消息
                        textArea.append(message + "\r\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}