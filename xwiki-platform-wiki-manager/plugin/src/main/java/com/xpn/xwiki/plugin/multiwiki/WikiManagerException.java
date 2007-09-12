/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors.
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

package com.xpn.xwiki.plugin.multiwiki;

import com.xpn.xwiki.plugin.PluginException;

public class WikiManagerException extends PluginException
{
    // TODO : move in XWikiException
    public static final int ERROR_XWIKI_USER_DOES_NOT_EXIST = 50091;
    
    ////////

    public static final int MODULE_PLUGIN_MULTIWIKI = 50;
    
    public static final int ERROR_MULTIWIKI_CANNOT_CREATE_WIKI = 50032;
    
    public static final int ERROR_MULTIWIKI_SERVER_DOES_NOT_EXIST = 50034;
    public static final int ERROR_MULTIWIKI_WIKISERVER_ALREADY_EXISTS = 50020;
    
    public static final int ERROR_MULTIWIKI_WIKI_NAME_FORBIDDEN = 50035;
    
    ////////
    
    public WikiManagerException(int code, String message)
    {
        super(WikiManagerPlugin.class, code, message);
    }

    public WikiManagerException(int code, String message, Throwable e, Object[] args)
    {
        super(WikiManagerPlugin.class, code, message, e, args);
    }

    public WikiManagerException(int code, String message, Throwable e)
    {
        super(WikiManagerPlugin.class, code, message, e);
    }
    
    ////////
    
    private WikiManagerException()
    {
        super(WikiManagerPlugin.class, 0, "No error");
    }
    
    private static final WikiManagerException _defaultException = new WikiManagerException();
    
    public static WikiManagerException getDefaultException()
    {
        return _defaultException;
    }
}
