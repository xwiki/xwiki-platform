package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 8 mai 2007
 * Time: 16:38:24
 * To change this template use File | Settings | File Templates.
 */
public class XWikiGWTException extends Exception implements IsSerializable {
    private String message;
    private String fullMessage;
    private int code;
    private int module;

    public XWikiGWTException() {
        super();
    }

    public XWikiGWTException(String message, String fullMessage, int code, int module) {
        super();
        this.setMessage(message);
        this.setFullMessage(fullMessage);
        this.setCode(code);
        this.setModule(module);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getModule() {
        return module;
    }

    public void setModule(int module) {
        this.module = module;
    }
}
