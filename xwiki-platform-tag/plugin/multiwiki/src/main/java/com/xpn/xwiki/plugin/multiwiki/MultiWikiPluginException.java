package com.xpn.xwiki.plugin.multiwiki;

import com.xpn.xwiki.plugin.PluginException;

public class MultiWikiPluginException extends PluginException
{
    public static final int ERROR_MULTIWIKI_ACCOUNT_ALREADY_EXISTS = 50010;

    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_ACCOUNT = 50011;

    public static final int ERROR_MULTIWIKI_SERVER_ALREADY_EXISTS = 50020;

    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_SERVER = 50021;

    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_SERVER_ADMIN = 50022;

    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_USER_SPACE = 50003;

    public static final int ERROR_MULTIWIKI_CANNOT_USER_ALREADY_EXISTS = 50030;

    public static final int ERROR_MULTIWIKI_CANNOT_CANNOT_VALIDATE_ACCOUNT= 50031;

    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_WIKI = 50032;

    public MultiWikiPluginException(int code, String message)
    {
        super(com.xpn.xwiki.plugin.multiwiki.MultiWikiPlugin.class, code, message);
    }

    public MultiWikiPluginException(int code, String message, Throwable e, Object[] args)
    {
        super(com.xpn.xwiki.plugin.multiwiki.MultiWikiPlugin.class, code, message, e, args);
    }

    public MultiWikiPluginException(int code, String message, Throwable e)
    {
        super(com.xpn.xwiki.plugin.multiwiki.MultiWikiPlugin.class, code, message, e);
    }
}
