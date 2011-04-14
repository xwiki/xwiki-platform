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
 * @version $Id$
 */
public class WikiManagerException extends PluginException
{
    /**
     * Error when trying to use provided user that does not exists.
     * <p>
     * TODO : move in XWikiException
     */
    public static final int ERROR_XWIKI_USERDOESNOTEXIST = 50091;

    // //////

    /**
     * Wiki Manager plugin error identifier.
     */
    public static final int MODULE_PLUGIN_WIKIMANAGER = 50;

    /**
     * Error when trying to create wiki descriptor that already exists.
     */
    public static final int ERROR_WM_WIKIALREADYEXISTS = 50033;

    /**
     * Error when trying to use a provided wiki descriptor that does not exist.
     */
    public static final int ERROR_WM_WIKIDOESNOTEXISTS = 50034;

    /**
     * Error when trying to update the database/schema.
     */
    public static final int ERROR_WM_UPDATEDATABASE = 50035;

    /**
     * Error when trying to create a wiki with a forbidden provided name.
     */
    public static final int ERROR_WM_WIKINAMEFORBIDDEN = 50036;

    /**
     * Error when trying to execute action that need xwiki engine to be in virtual mode and is not.
     */
    public static final int ERROR_WM_XWIKINOTVIRTUAL = 50037;

    /**
     * Error when trying to get an attached XAR package that does not exists.
     */
    public static final int ERROR_WM_PACKAGEDOESNOTEXISTS = 50038;

    /**
     * Error when trying to load a XAR package file in a list of {@link com.xpn.xwiki.doc.XWikiDocument}.
     */
    public static final int ERROR_WM_PACKAGEIMPORT = 50039;

    /**
     * Error when trying to insert in wiki a loaded XAR package list of {@link com.xpn.xwiki.doc.XWikiDocument}.
     */
    public static final int ERROR_WM_PACKAGEINSTALL = 50040;

    /**
     * Error when trying to get a provided wiki alias that does not exist.
     * 
     * @since 1.1
     */
    public static final int ERROR_WM_WIKIALIASDOESNOTEXISTS = 50041;

    // //////

    /**
     * Needed to identify the version of this code when serializing/deserializing (since Exception is Serializable).
     * Note that the value needs to be modified whenever a non transient field is added or removed in this class.
     */
    private static final long serialVersionUID = -6451750749104331619L;

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
     * Create an WikiManagerException. Replace any parameters found in the <code>message</code> by the passed
     * <code>args</code> parameters. The format is the one used by {@link java.text.MessageFormat}.
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
     * @return unique instance of the default WikiManagerException.
     */
    public static WikiManagerException getDefaultException()
    {
        return DEFAULT_EXCEPTION;
    }
}
