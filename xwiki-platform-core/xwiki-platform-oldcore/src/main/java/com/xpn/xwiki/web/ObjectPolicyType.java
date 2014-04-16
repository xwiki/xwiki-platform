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

package com.xpn.xwiki.web;

/**
 * Enumeration of supported object policy types.
 * 
 * @version $Id$
 */
public enum ObjectPolicyType
{
    /** Only update objects. */
    UPDATE("update"),
    
    /** Update and/or create objects. */
    UPDATE_OR_CREATE("updateOrCreate");
    
    /** Name that is used in HTTP parameters to specify the object policy. */
    private final String name;
    
    /**
     * @param name The string name corresponding to the object policy type.
     */
    ObjectPolicyType(String name) {
        this.name = name;
    }
    
    /**
     * @return The string name corresponding to the object policy type.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * @param name The string name corresponding to the object policy type.
     * @return The ObjectPolicyType corresponding to the parameter 'name'.
     */
    public static ObjectPolicyType forName(String name) {
        for (ObjectPolicyType type : values()) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        return null;
    }
}
