/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 01:07:04
 */
package com.xpn.xwiki;

import net.sf.hibernate.JDBCException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

public class XWikiException extends Exception {
    // Module list
    public static final int MODULE_XWIKI = 0;
    public static final int MODULE_XWIKI_CONFIG = 1;
    public static final int MODULE_XWIKI_DOC = 2;
    public static final int MODULE_XWIKI_STORE = 3;
    public static final int MODULE_XWIKI_RENDERING = 4;
    public static final int MODULE_XWIKI_PLUGINS = 5;
    public static final int MODULE_XWIKI_PERLPLUGINS = 6;
    public static final int MODULE_XWIKI_CLASSES = 7;
    public static final int MODULE_XWIKI_USER = 8;
    public static final int MODULE_XWIKI_APP = 10;

    // Error list
    public static final int ERROR_XWIKI_UNKNOWN = 0;

    // Config
    public static final int ERROR_XWIKI_CONFIG_FILENOTFOUND = 1001;
    public static final int ERROR_XWIKI_CONFIG_FORMATERROR = 1002;

    // Doc

    // Store
    public static final int ERROR_XWIKI_STORE_CLASSINVOCATIONERROR = 3001;
    public static final int ERROR_XWIKI_STORE_FILENOTFOUND = 3002;
    public static final int ERROR_XWIKI_STORE_ARCHIVEFORMAT = 3003;
    public static final int ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT = 3004;

    public static final int ERROR_XWIKI_STORE_RCS_SAVING_FILE = 3101;
    public static final int ERROR_XWIKI_STORE_RCS_READING_FILE = 3102;
    public static final int ERROR_XWIKI_STORE_RCS_READING_REVISIONS = 3103;
    public static final int ERROR_XWIKI_STORE_RCS_READING_VERSION = 3104;
    public static final int ERROR_XWIKI_STORE_RCS_SEARCH = 3111;
    public static final int ERROR_XWIKI_STORE_RCS_LOADING_ATTACHMENT = 3221;
    public static final int ERROR_XWIKI_STORE_RCS_SAVING_ATTACHMENT = 3222;
    public static final int ERROR_XWIKI_STORE_RCS_SEARCHING_ATTACHMENT = 3223;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_FILE = 3201;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_FILE = 3202;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS = 3203;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION = 3204;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT = 3211;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT = 3212;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_CLASS = 3221;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS = 3222;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SEARCH = 3223;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT = 3231;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT = 3232;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST = 3233;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT = 3234;


    public static final int ERROR_XWIKI_PERLPLUGIN_START_EXCEPTION = 6001;
    public static final int ERROR_XWIKI_PERLPLUGIN_START = 6002;
    public static final int ERROR_XWIKI_PERLPLUGIN_PERLSERVER_EXCEPTION = 6003;

    public static final int ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST = 7001;
    public static final int ERROR_XWIKI_CLASSES_FIELD_INVALID = 7002;

    public static final int ERROR_XWIKI_USER_INIT = 8001;
    public static final int ERROR_XWIKI_USER_CREATE = 8002;

    public static final int ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST = 10001;
    public static final int ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY = 10002;


    private int module;
    private int code;
    private Throwable exception;
    private Object[] args;
    private String message;

    public XWikiException(int module, int code, String message, Throwable e, Object[] args) {
        setModule(module);
        setCode(code);
        setException(e);
        setArgs(args);
        setMessage(message);
    }

    public XWikiException(int module, int code, String message, Throwable e) {
        this(module, code, message, e, null);
    }

    public XWikiException(int module, int code, String message) {
        this(module, code, message, null, null);
    }

    public int getModule() {
        return module;
    }

    public String getModuleName() {
        return "" + module;
    }

    public void setModule(int module) {
        this.module = module;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage()
    {
        String str = "Error number " + getCode() + " in " + getModuleName() + ": ";

        if (message!=null)
        {
            if (args==null)
                str += message;
            else
            {
                MessageFormat msgFormat = new MessageFormat (message);
                try
                {
                    str += msgFormat.format(args);
                }
                catch (Exception e)
                {
                    str += "Cannot format message " + message + " with args ";
                    for (int i = 0; i< args.length ; i++)
                    {
                        if (i!=0)
                            str += ",";
                        str += args[i];
                    }
                }
            }
        }
        str += "\n";
        if (exception!=null) {
            if (exception instanceof net.sf.hibernate.JDBCException) {
                str += exception.getMessage() + ":\n";
                str += getStackStrace(((JDBCException)exception).getSQLException());
            }
            else {
                str += exception.getMessage() + "\n";
                str += getStackStrace(exception);
            }
        }
        return str;
    }

    public String getStackStrace(Throwable e) {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        e.printStackTrace(pwriter);
        pwriter.flush();
        return swriter.getBuffer().toString();
    }

}