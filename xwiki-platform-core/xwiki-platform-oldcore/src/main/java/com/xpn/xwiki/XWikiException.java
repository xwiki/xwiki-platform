/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki;

import java.text.MessageFormat;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XWikiException extends Exception
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiException.class);

    private int module;

    private int code;

    private Object[] args;

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

    public static final int MODULE_XWIKI_GROOVY = 14;

    public static final int MODULE_XWIKI_NOTIFICATION = 15;

    public static final int MODULE_XWIKI_CACHE = 16;

    public static final int MODULE_XWIKI_CONTENT = 17;

    public static final int MODULE_XWIKI_XMLRPC = 18;

    public static final int MODULE_XWIKI_GWT_API = 19;

    // Error list
    public static final int ERROR_XWIKI_UNKNOWN = 0;

    public static final int ERROR_XWIKI_NOT_IMPLEMENTED = 1;

    public static final int ERROR_XWIKI_DOES_NOT_EXIST = 2;

    public static final int ERROR_XWIKI_INIT_FAILED = 3;

    public static final int ERROR_XWIKI_MKDIR = 4;

    public static final int ERROR_XWIKI_DOC_CUSTOMDOCINVOCATIONERROR = 5;

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

    public static final int ERROR_XWIKI_STORE_MIGRATION = 3005;

    public static final int ERROR_XWIKI_STORE_MISC = 3006;

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

    public static final int ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION = 3205;

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

    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETE_DATABASE = 3402;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE = 3403;

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

    public static final int ERROR_XWIKI_CLASSES_PROPERTY_CLASS_IN_METACLASS = 7006;

    public static final int ERROR_XWIKI_CLASSES_CANNOT_PREPARE_CUSTOM_DISPLAY = 7007;

    public static final int ERROR_XWIKI_USER_INIT = 8001;

    public static final int ERROR_XWIKI_USER_CREATE = 8002;

    public static final int ERROR_XWIKI_USER_INACTIVE = 8003;

    public static final int ERROR_XWIKI_ACCESS_DENIED = 9001;

    public static final int ERROR_XWIKI_ACCESS_TOKEN_INVALID = 9002;

    public static final int ERROR_XWIKI_ACCESS_EXO_EXCEPTION_USERS = 9003;

    public static final int ERROR_XWIKI_ACCESS_EXO_EXCEPTION_GROUPS = 9004;

    public static final int ERROR_XWIKI_ACCESS_EXO_EXCEPTION_ADDING_USERS = 9005;

    public static final int ERROR_XWIKI_ACCESS_EXO_EXCEPTION_LISTING_USERS = 9006;

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

    public static final int ERROR_XWIKI_APP_FILE_EXCEPTION_MAXSIZE = 11013;

    public static final int ERROR_XWIKI_APP_JAVA_HEAP_SPACE = 11014;

    public static final int ERROR_XWIKI_APP_EXPORT = 11015;

    public static final int ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE = 11016;

    public static final int ERROR_XWIKI_EXPORT_XSL_FILE_NOT_FOUND = 12001;

    public static final int ERROR_XWIKI_EXPORT_PDF_FOP_FAILED = 12002;

    public static final int ERROR_XWIKI_EXPORT_XSL_FAILED = 12003;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_LOCK = 13006;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_LOCK = 13007;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_LOCK = 13008;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING = 13009;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED = 13010;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_LINKS = 13011;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_SAVING_LINKS = 13012;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_DELETING_LINKS = 13013;

    public static final int ERROR_XWIKI_STORE_HIBERNATE_LOADING_BACKLINKS = 13014;

    public static final int ERROR_XWIKI_DIFF_CONTENT_ERROR = 13021;

    public static final int ERROR_XWIKI_DIFF_RENDERED_ERROR = 13022;

    public static final int ERROR_XWIKI_DIFF_METADATA_ERROR = 13023;

    public static final int ERROR_XWIKI_DIFF_CLASS_ERROR = 13024;

    public static final int ERROR_XWIKI_DIFF_OBJECT_ERROR = 13025;

    public static final int ERROR_XWIKI_DIFF_ATTACHMENT_ERROR = 13026;

    public static final int ERROR_XWIKI_DIFF_XML_ERROR = 13027;

    public static final int ERROR_XWIKI_STORE_SEARCH_NOTIMPL = 13200;

    public static final int ERROR_XWIKI_GROOVY_COMPILE_FAILED = 14001;

    public static final int ERROR_XWIKI_GROOVY_EXECUTION_FAILED = 14002;

    public static final int ERROR_XWIKI_NOTIFICATION = 15001;

    public static final int ERROR_CACHE_INITIALIZING = 16001;

    // Content errors
    public static final int ERROR_XWIKI_CONTENT_LINK_INVALID_TARGET = 22000;

    public static final int ERROR_XWIKI_CONTENT_LINK_INVALID_URI = 22001;

    public XWikiException(int module, int code, String message, Throwable e, Object[] args)
    {
        super(message, e);

        setModule(module);
        setCode(code);
        setArgs(args);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getMessage(), e);
        }
    }

    public XWikiException(int module, int code, String message, Throwable e)
    {
        this(module, code, message, e, null);
    }

    public XWikiException(int module, int code, String message)
    {
        this(module, code, message, null, null);
    }

    public XWikiException()
    {
    }

    public int getModule()
    {
        return this.module;
    }

    public String getModuleName()
    {
        return String.valueOf(this.module);
    }

    public void setModule(int module)
    {
        this.module = module;
    }

    public int getCode()
    {
        return this.code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    /**
     * @deprecated since 5.2M2, use {@link #getCause()} instead
     */
    @Deprecated
    public Throwable getException()
    {
        return getCause();
    }

    /**
     * @deprecated since 5.2M2, use {@link #initCause(Throwable)} instead
     */
    @Deprecated
    public void setException(Throwable exception)
    {
        initCause(exception);
    }

    public Object[] getArgs()
    {
        return this.args;
    }

    public void setArgs(Object[] args)
    {
        this.args = args;
    }

    @Override
    public String getMessage()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("Error number ");
        buffer.append(getCode());
        buffer.append(" in ");
        buffer.append(getModuleName());

        String message = super.getMessage();
        if (message != null) {
            buffer.append(": ");
            if (this.args == null) {
                buffer.append(message);
            } else {
                MessageFormat msgFormat = new MessageFormat(message);
                try {
                    buffer.append(msgFormat.format(this.args));
                } catch (Exception e) {
                    buffer.append("Cannot format message [" + message + "] with args ");
                    for (int i = 0; i < this.args.length; i++) {
                        if (i != 0) {
                            buffer.append(",");
                        }
                        buffer.append(this.args[i]);
                    }
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Has no effect.
     *
     * @deprecated since 5.2M2, the message should be passed to the constructor
     */
    @Deprecated
    public void setMessage(String message)
    {

    }

    public String getFullMessage()
    {
        StringBuilder buffer = new StringBuilder(getMessage());
        buffer.append("\n");
        buffer.append(getStackTraceAsString());
        buffer.append("\n");

        return buffer.toString();
    }

    public String getStackTraceAsString()
    {
        return ExceptionUtils.getStackTrace(this);
    }

    public String getStackTraceAsString(Throwable e)
    {
        return ExceptionUtils.getStackTrace(e);
    }
}
