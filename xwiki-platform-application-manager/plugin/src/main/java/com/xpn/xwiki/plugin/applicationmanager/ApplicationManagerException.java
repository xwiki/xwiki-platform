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

public class ApplicationManagerException extends PluginException
{
	public static final int MODULE_PLUGIN_APPLICATIONMANAGER = 60;
	
    public static final int ERROR_APPLICATIONMANAGER_APPDOC_ALREADY_EXISTS = 60010;
    
    public static final int ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST = 60011;
    
    ////////
    
    public ApplicationManagerException(int code, String message)
    {
        super(ApplicationManagerPlugin.class, code, message);
    }

    public ApplicationManagerException(int code, String message, Throwable e, Object[] args)
    {
        super(ApplicationManagerPlugin.class, code, message, e, args);
    }

    public ApplicationManagerException(int code, String message, Throwable e)
    {
        super(ApplicationManagerPlugin.class, code, message, e);
    }
    
    ////////
    
    private ApplicationManagerException()
    {
        super(ApplicationManagerPlugin.class, 0, "No error");
    }
    
    private static final ApplicationManagerException _defaultException = new ApplicationManagerException();
    
    public static ApplicationManagerException getDefaultException()
    {
        return _defaultException;
    }
}
