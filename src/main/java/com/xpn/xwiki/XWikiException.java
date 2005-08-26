/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 01:07:04
 */
package com.xpn.xwiki;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import javax.servlet.ServletException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.MethodInvocationException;
import org.hibernate.JDBCException;

public class XWikiException extends Exception {

    private static final Log log = LogFactory.getLog(XWikiException.class);

    private int module;
    private int code;
    private Throwable exception;
    private Object[] args;
    private String message;

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
    public static final int MODULE_XWIKI_ACCESS = 9;
    public static final int MODULE_XWIKI_EMAIL = 10;
    public static final int MODULE_XWIKI_APP = 11;
    public static final int MODULE_XWIKI_EXPORT = 12;
    public static final int MODULE_XWIKI_DIFF = 13;

    public static final int MODULE_PLUGIN_LASZLO = 21;

    // Error list
    public static final int ERROR_XWIKI_UNKNOWN = 0;
    public static final int ERROR_XWIKI_NOT_IMPLEMENTED = 1;
    public static final int ERROR_XWIKI_DOES_NOT_EXIST = 2;
    public static final int ERROR_XWIKI_INIT_FAILED = 3;
    public static final int ERROR_XWIKI_MKDIR = 4;

    // Config
    public static final int ERROR_XWIKI_CONFIG_FILENOTFOUND = 1001;
    public static final int ERROR_XWIKI_CONFIG_FORMATERROR = 1002;

    // Doc
    public static final int ERROR_XWIKI_DOC_EXPORT = 2001;
    public static final int ERROR_DOC_XML_PARSING = 2002;
    public static final int ERROR_DOC_RCS_PARSING = 2003;

    // Store
    public static final int ERROR_XWIKI_STORE_CLASSINVOCATIONERROR = 3001;
    public static final int ERROR_XWIKI_STORE_FILENOTFOUND = 3002;
    public static final int ERROR_XWIKI_STORE_ARCHIVEFORMAT = 3003;
    public static final int ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT = 3004;

    public static final int ERROR_XWIKI_STORE_RCS_SAVING_FILE = 3101;
    public static final int ERROR_XWIKI_STORE_RCS_READING_FILE = 3102;
    public static final int ERROR_XWIKI_STORE_RCS_DELETING_FILE = 3103;
    public static final int ERROR_XWIKI_STORE_RCS_READING_REVISIONS = 3103;
    public static final int ERROR_XWIKI_STORE_RCS_READING_VERSION = 3104;
    public static final int ERROR_XWIKI_STORE_RCS_SEARCH = 3111;
    public static final int ERROR_XWIKI_STORE_RCS_LOADING_ATTACHMENT = 3221;
    public static final int ERROR_XWIKI_STORE_RCS_SAVING_ATTACHMENT = 3222;
    public static final int ERROR_XWIKI_STORE_RCS_SEARCHING_ATTACHMENT = 3223;
    public static final int ERROR_XWIKI_STORE_RCS_DELETING_ATTACHMENT = 3224;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC = 3201;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_DOC = 3202;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC = 3203;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC = 3204;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS = 3203;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION = 3204;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT = 3211;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT = 3212;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT = 3213;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_CLASS = 3221;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_CLASS = 3222;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SEARCH = 3223;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT = 3231;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT = 3232;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT = 3233;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST = 3234;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT = 3235;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC = 3236;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE = 3301;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE = 3401;

    public static final int ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION = 4001;
    public static final int ERROR_XWIKI_RENDERING_GROOVY_EXCEPTION = 4002;
    
    public static final int ERROR_XWIKI_PERLPLUGIN_START_EXCEPTION = 6001;
    public static final int ERROR_XWIKI_PERLPLUGIN_START = 6002;
    public static final int ERROR_XWIKI_PERLPLUGIN_PERLSERVER_EXCEPTION = 6003;

    public static final int ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST = 7001;
    public static final int ERROR_XWIKI_CLASSES_FIELD_INVALID = 7002;
    public static final int ERROR_XWIKI_CLASSES_DIFF = 7003;
    public static final int ERROR_XWIKI_CLASSES_CUSTOMCLASSINVOCATIONERROR = 7004;
    public static final int ERROR_XWIKI_CLASSES_PROPERTY_CLASS_INSTANCIATION = 7005;


    public static final int ERROR_XWIKI_USER_INIT = 8001;
    public static final int ERROR_XWIKI_USER_CREATE = 8002;
    public static final int ERROR_XWIKI_USER_INACTIVE = 8003;
    
    public static final int ERROR_XWIKI_ACCESS_DENIED = 9001;
    public static final int ERROR_XWIKI_ACCESS_TOKEN_INVALID = 9002;

    public static final int ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG = 10001;
    public static final int ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL = 10002;
    public static final int ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL = 10003;
    public static final int ERROR_XWIKI_EMAIL_CONNECT_FAILED = 10004;
    public static final int ERROR_XWIKI_EMAIL_LOGIN_FAILED = 10005;
    public static final int ERROR_XWIKI_EMAIL_SEND_FAILED = 10006;

    public static final int ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST = 11001;
    public static final int ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY = 11002;
    public static final int ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND = 11003;
    public static final int ERROR_XWIKI_APP_CREATE_USER = 11004;
    public static final int ERROR_XWIKI_APP_VALIDATE_USER = 11005;
    public static final int ERROR_XWIKI_APP_INVALID_CHARS = 11006;
    public static final int ERROR_XWIKI_APP_URL_EXCEPTION = 11007;
    public static final int ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION = 11008;
    public static final int ERROR_XWIKI_APP_UPLOAD_FILE_EXCEPTION = 11009;
    public static final int ERROR_XWIKI_APP_REDIRECT_EXCEPTION = 11010;
    public static final int ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION = 11011;
    public static final int ERROR_XWIKI_APP_SERVICE_NOT_FOUND = 11012;
    public static final int ERROR_XWIKI_EXPORT_XSL_FILE_NOT_FOUND = 12001;
    public static final int ERROR_XWIKI_EXPORT_PDF_FOP_FAILED = 12002;
    public static final int ERROR_XWIKI_EXPORT_XSL_FAILED = 12003;
    public static final int ERROR_XWIKI_DIFF_CONTENT_ERROR = 13001;
    public static final int ERROR_XWIKI_DIFF_RENDERED_ERROR = 13002;
    public static final int ERROR_XWIKI_DIFF_METADATA_ERROR = 13003;
    public static final int ERROR_XWIKI_DIFF_CLASS_ERROR = 13004;
    public static final int ERROR_XWIKI_DIFF_OBJECT_ERROR = 13005;
    public static final int ERROR_XWIKI_DIFF_XML_ERROR = 13005;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_LOCK = 13006;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_LOCK = 13007;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_LOCK = 13008;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING = 13009;
    public static final int ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED = 13010;
    public static final int ERROR_LASZLO_INVALID_XML = 21001;
    public static final int ERROR_LASZLO_INVALID_DOTDOT = 21002;

    public XWikiException(int module, int code, String message, Throwable e, Object[] args) {
        setModule(module);
        setCode(code);
        setException(e);
        setArgs(args);
        setMessage(message);
        if (log.isTraceEnabled())
            log.trace(getMessage(), e);
    }

    public XWikiException(int module, int code, String message, Throwable e) {
        this(module, code, message, e, null);
    }

    public XWikiException(int module, int code, String message) {
        this(module, code, message, null, null);
    }

    public XWikiException() {
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
        StringBuffer buffer = new StringBuffer();
        buffer.append("Error number ");
        buffer.append(getCode());
        buffer.append(" in ");
        buffer.append(getModuleName());
        buffer.append(": ");

        if (message!=null)
        {
            if (args==null)
                buffer.append(message);
            else
            {
                MessageFormat msgFormat = new MessageFormat (message);
                try
                {
                    buffer.append(msgFormat.format(args));
                }
                catch (Exception e)
                {
                    buffer.append("Cannot format message " + message + " with args ");
                    for (int i = 0; i< args.length ; i++)
                    {
                        if (i!=0)
                            buffer.append(",");
                        buffer.append(args[i]);
                    }
                }
            }
        }

        if (exception!=null) {
             buffer.append("\nWrapped Exception: ");
             buffer.append(exception.getMessage());
        }
        return buffer.toString();
    }

    public String getFullMessage()
    {
        StringBuffer buffer = new StringBuffer(getMessage());
        buffer.append("\n");
        buffer.append(getStackTraceAsString());
        return buffer.toString();
    }

    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        if (exception!=null) {
            s.write("\n\nWrapped Exception:\n\n");
            if (exception instanceof org.hibernate.JDBCException) {
                (((JDBCException)exception).getSQLException()).printStackTrace(s);
            } else if (exception instanceof MethodInvocationException) {
                (((MethodInvocationException)exception).getWrappedThrowable()).printStackTrace(s);
            } else if (exception instanceof ServletException) {
                (((ServletException)exception).getRootCause()).printStackTrace(s);
            } else if (exception instanceof TransformerException) {
                (((TransformerException)exception).getCause()).printStackTrace(s);
            } else {
                exception.printStackTrace(s);
            }
        }
    }

    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        if (exception!=null) {
            s.print("\n\nWrapped Exception:\n\n");
            if (exception instanceof org.hibernate.JDBCException) {
                (((JDBCException)exception).getSQLException()).printStackTrace(s);
            } else if (exception instanceof MethodInvocationException) {
                (((MethodInvocationException)exception).getWrappedThrowable()).printStackTrace(s);
            } else if (exception instanceof ServletException) {
                (((ServletException)exception).getRootCause()).printStackTrace(s);
            } else if (exception instanceof TransformerException) {
                (((TransformerException)exception).getCause()).printStackTrace(s);
            } else {
                exception.printStackTrace(s);
            }
        }
    }

    public String getStackTraceAsString() {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        printStackTrace(pwriter);
        pwriter.flush();
        return swriter.getBuffer().toString();
    }

    public String getStackTraceAsString(Throwable e) {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        e.printStackTrace(pwriter);
        pwriter.flush();
        return swriter.getBuffer().toString();
    }

}