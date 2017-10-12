package org.zhdev.socket.entity;

import java.io.Serializable;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 16:28 2017/10/9
 * @Modified By :
 */
public class UserClient implements Serializable {

    private static final long serialVersionUID = 5527951479250104454L;

    private String clientCode;
    private String userName;
    private String pwd;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
}
