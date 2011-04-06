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
 *
 */

package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.plugin.PluginException;

public class PackageException extends PluginException
{
    static String plugName = "Package";

    public static final int ERROR_PACKAGE_UNKNOWN = 1;

    public static final int ERROR_PACKAGE_NODESCRIPTION = 2;

    public static final int ERROR_PACKAGE_INVALID_FILTER = 3;

    public PackageException(int code, String message, Throwable e, Object[] args)
    {
        super(plugName, code, message, e, args);
    }

    public PackageException(int code, String message, Throwable e)
    {
        super(plugName, code, message, e);
    }

    public PackageException(int code, String message)
    {
        super(plugName, code, message);
    }

    public PackageException()
    {
        super();
    }
}
