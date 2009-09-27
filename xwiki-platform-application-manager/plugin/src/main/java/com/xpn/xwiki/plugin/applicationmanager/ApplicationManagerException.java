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

package com.xpn.xwiki.plugin.applicationmanager;

import com.xpn.xwiki.plugin.PluginException;

/**
 * Application Manager plugin base exception.
 * 
 * @version $Id$
 */
public class ApplicationManagerException extends PluginException
{
    /**
     * Application Manager plugin error identifier.
     */
    public static final int MODULE_PLUGIN_APPLICATIONMANAGER = 60;

    /**
     * Error when trying to create application descriptor that already exist in the database.
     */
    public static final int ERROR_AM_APPDOCALREADYEXISTS = 60010;

    /**
     * Error when trying to get application descriptor that does not exist in the database.
     */
    public static final int ERROR_AM_DOESNOTEXIST = 60011;

    /**
     * The default ApplicationManagerException.
     */
    private static final ApplicationManagerException DEFAULT_EXCEPTION = new ApplicationManagerException();

    // //////

    /**
     * Create an ApplicationManagerException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     */
    public ApplicationManagerException(int code, String message)
    {
        super(ApplicationManagerPlugin.class, code, message);
    }

    /**
     * Create an ApplicationManagerException. Replace any parameters found in the <code>message</code> by the passed
     * <code>args</code> parameters. The format is the one used by {@link java.text.MessageFormat}.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     * @param args the array of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     */
    public ApplicationManagerException(int code, String message, Throwable e, Object[] args)
    {
        super(ApplicationManagerPlugin.class, code, message, e, args);
    }

    /**
     * Create an ApplicationManagerException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     */
    public ApplicationManagerException(int code, String message, Throwable e)
    {
        super(ApplicationManagerPlugin.class, code, message, e);
    }

    // //////

    /**
     * Create default ApplicationManagerException.
     */
    private ApplicationManagerException()
    {
        super(ApplicationManagerPlugin.class, 0, "No error");
    }

    /**
     * @return unique instance of the default ApplicationManagerException.
     */
    public static ApplicationManagerException getDefaultException()
    {
        return DEFAULT_EXCEPTION;
    }
}
