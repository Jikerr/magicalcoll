package org.zhdev.socket.client;

/**
 * @Author: zh
 * @Description :
 * @Date Created in 16:28 2017/10/16
 * @Modified By :
 */
//用户信息类
public class User{
    private String name;
    private String ip;

    public User(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}