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

package com.xpn.xwiki.plugin.wikimanager;

import com.xpn.xwiki.plugin.PluginException;

/**
 * Wiki Manager plugin base exception.
 * 
 * @version $Id: $
 */
public class WikiManagerException extends PluginException
{
    // TODO : move in XWikiException
    public static final int ERROR_XWIKI_USER_DOES_NOT_EXIST = 50091;

    // //////

    /**
     * Wiki Manager plugin error identifier.
     */
    public static final int MODULE_PLUGIN_WIKIMANAGER = 50;

    public static final int ERROR_WIKIMANAGER_CANNOT_CREATE_WIKI = 50032;

    public static final int ERROR_WIKIMANAGER_SERVER_DOES_NOT_EXIST = 50034;

    public static final int ERROR_WIKIMANAGER_WIKISERVER_ALREADY_EXISTS = 50020;

    public static final int ERROR_WIKIMANAGER_WIKI_NAME_FORBIDDEN = 50035;

    public static final int ERROR_WIKIMANAGER_XWIKI_NOT_VIRTUAL = 50036;

    // //////

    /**
     * The default WikiManagerException.
     */
    private static final WikiManagerException DEFAULT_EXCEPTION = new WikiManagerException();

    // //////

    /**
     * Create an WikiManagerException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     */
    public WikiManagerException(int code, String message)
    {
        super(WikiManagerPlugin.class, code, message);
    }

    /**
     * Create an WikiManagerException. Replace any parameters found in the <code>message</code> by
     * the passed <code>args</code> parameters. The format is the one used by
     * {@link java.text.MessageFormat}.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     * @param args the array of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     */
    public WikiManagerException(int code, String message, Throwable e, Object[] args)
    {
        super(WikiManagerPlugin.class, code, message, e, args);
    }

    /**
     * Create an WikiManagerException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     */
    public WikiManagerException(int code, String message, Throwable e)
    {
        super(WikiManagerPlugin.class, code, message, e);
    }

    // //////

    /**
     * Create default WikiManagerException.
     */
    private WikiManagerException()
    {
        super(WikiManagerPlugin.class, 0, "No error");
    }

    /**
     * @return unique instance of the default ApplicationManagerException.
     */
    public static WikiManagerException getDefaultException()
    {
        return DEFAULT_EXCEPTION;
    }
}
