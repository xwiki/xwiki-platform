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
 package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

/**
 * Add a backward compatibility layer to the {@link Utils} class.
 * 
 * @version $Id: UtilsCompatibilityAspect.aj 10166 2008-06-09 12:50:40Z sdumitriu $
 */
public privileged aspect UtilsCompatibilityAspect
{
    /**
     * Lookup a XWiki component by role and hint.
     * 
     * @param role the class (aka role) that the component implements
     * @param hint a value to differentiate different component implementations for the same role
     * @return the component's instance
     */
	public static Object Utils.getComponent(String role, String hint)
    {
        try {
            return getComponent(Utils.class.getClassLoader().loadClass(role), hint);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load component [" + role + "] for hint [" + hint + "]", e);
        }
    }

    /**
     * Lookup a XWiki component by role (uses the default hint).
     * 
     * @param role the class (aka role) that the component implements
     * @return the component's instance
     */
    public static Object Utils.getComponent(String role)
    {
        return getComponent(role, "default");
    }
}
