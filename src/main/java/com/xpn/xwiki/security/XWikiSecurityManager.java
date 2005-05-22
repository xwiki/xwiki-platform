/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 23 mars 2004
 * Time: 22:35:07
 */

package com.xpn.xwiki.security;

import java.lang.reflect.ReflectPermission;
import java.security.Permission;

public class XWikiSecurityManager extends SecurityManager {
    private boolean enabled = false;

    public XWikiSecurityManager(boolean enabled) {
        this.enabled = enabled;
    }

    public void checkPermission(Permission perm) {
        if (enabled) {
        // Under this security manager we refuse reflect code
        if (perm instanceof ReflectPermission)
         throw new SecurityException();
        }
    }

    public void checkPermission(Permission perm, Object context) {
        if (enabled) {
        // Under this security manager we refuse reflect code
        if (perm instanceof ReflectPermission)
         throw new SecurityException();
        }
    }

}
