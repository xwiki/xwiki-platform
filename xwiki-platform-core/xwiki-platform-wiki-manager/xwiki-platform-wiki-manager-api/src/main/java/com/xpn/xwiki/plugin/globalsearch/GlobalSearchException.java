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

package com.xpn.xwiki.plugin.globalsearch;

import com.xpn.xwiki.plugin.PluginException;

/**
 * Global Search plugin base exception.
 * 
 * @version $Id$
 */
public class GlobalSearchException extends PluginException
{
    /**
     * Global Search plugin error identifier.
     */
    public static final int MODULE_PLUGIN_GLOABLSEARCH = 60;

    /**
     * Error when trying to create wiki descriptor that already exists.
     */
    public static final int ERROR_FS_CANT_ACCESS_FIELD = 60000;

    // //////

    /**
     * The default GlobalSearchException.
     */
    private static final GlobalSearchException DEFAULT_EXCEPTION = new GlobalSearchException();

    // //////

    /**
     * Create an GlobalSearchException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     */
    public GlobalSearchException(int code, String message)
    {
        super(GlobalSearchPlugin.class, code, message);
    }

    /**
     * Create an GlobalSearchException. Replace any parameters found in the <code>message</code> by the passed
     * <code>args</code> parameters. The format is the one used by {@link java.text.MessageFormat}.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     * @param args the array of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     */
    public GlobalSearchException(int code, String message, Throwable e, Object[] args)
    {
        super(GlobalSearchPlugin.class, code, message, e, args);
    }

    /**
     * Create an GlobalSearchException.
     * 
     * @param code the error code.
     * @param message a literal message about this error.
     * @param e the exception this exception wrap.
     */
    public GlobalSearchException(int code, String message, Throwable e)
    {
        super(GlobalSearchPlugin.class, code, message, e);
    }

    // //////

    /**
     * Create default GlobalSearchException.
     */
    private GlobalSearchException()
    {
        super(GlobalSearchPlugin.class, 0, "No error");
    }

    /**
     * @return unique instance of the default GlobalSearchException.
     */
    public static GlobalSearchException getDefaultException()
    {
        return DEFAULT_EXCEPTION;
    }
}
