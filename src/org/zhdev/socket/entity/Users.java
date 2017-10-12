package org.zhdev.socket.entity;

import java.io.Serializable;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 14:32 2017/10/10
 * @Modified By :
 */
public class Users implements Serializable{

    private static final long serialVersionUID = 2892647702635955529L;

    private String userName;
    private String deviceType;
    private String clientId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
