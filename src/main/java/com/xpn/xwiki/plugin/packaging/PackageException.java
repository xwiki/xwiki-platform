package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.plugin.PluginException;

/**
 * ===================================================================
 *
 * Copyright (c) 2005 XpertNet, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 */
public class PackageException extends PluginException {
    static String plugName = "Package";
    public static final int ERROR_PACKAGE_UNKNOWN = 1;
    public static final int ERROR_PACKAGE_NODESCRIPTION = 2;

    public PackageException(int code, String message, Throwable e, Object[] args)
    {
        super(plugName, code, message, e, args);
    }

    public PackageException(int code, String message, Throwable e){
        super(plugName, code, message, e);
    }

    public PackageException(int code, String message){
        super(plugName, code, message);
    }

    public PackageException(){
        super();
    }
}
