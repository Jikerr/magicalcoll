package org.zhdev.socket.entity;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 15:51 2017/10/13
 * @Modified By :
 */
public class SocketMessageResponse implements Serializable {

    private static final long serialVersionUID = -3491319648103195155L;
    private String responseActionType;//响应哪个action


    private String handlerEvent;//处理事件
    private JSONObject responseBody;//响应主体
    private Integer code;//响应代码 200 成功 500 服务器错误 404 不接受的请求类型

    private String toClientId;//发送到哪个客户端ID
    private String fromClientId;//来自哪个客户端ID
    private Users toUser;//发送到哪个用户
    private Users fromUser;//来自哪个用户
    private String msgDate;//消息时间
    private long msgDateStamp;//消息时间戳

}
