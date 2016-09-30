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

package com.xpn.xwiki.plugin.rightsmanager;

import com.xpn.xwiki.plugin.PluginException;

/**
 * Rights Manager plugin base exception.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerException extends PluginException
{
    /**
     * The default RightsManagerException.
     */
    private static final RightsManagerException DEFAULT_EXCEPTION = new RightsManagerException();

    // //////

    /**
     * Create an RightsManagerException.
     *
     * @param code the error code.
     * @param message a literal message about this error.
     */
    public RightsManagerException(int code, String message)
    {
        super(RightsManagerPlugin.class, code, message);
    }

    /**
     * Create an RightsManagerException. Replace any parameters found in the <code>message</code> by the passed
     * <code>args</code> parameters. The format is the one used by {@link java.text.MessageFormat}.
     *
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     * @param args the array of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     */
    public RightsManagerException(int code, String message, Throwable e, Object[] args)
    {
        super(RightsManagerPlugin.class, code, message, e, args);
    }

    /**
     * Create an RightsManagerException.
     *
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     */
    public RightsManagerException(int code, String message, Throwable e)
    {
        super(RightsManagerPlugin.class, code, message, e);
    }

    // //////

    /**
     * Create default RightsManagerException.
     */
    private RightsManagerException()
    {
        super(RightsManagerPlugin.class, 0, "No error");
    }

    /**
     * @return unique instance of the default RightsManagerException.
     */
    public static RightsManagerException getDefaultException()
    {
        return DEFAULT_EXCEPTION;
    }
}
