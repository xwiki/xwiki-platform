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
package com.xpn.xwiki.internal.filter.output;

import org.xwiki.filter.instance.output.EntityInstanceOutputProperties;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class UserInstanceOutputProperties extends EntityInstanceOutputProperties
{
    /**
     * @see #getGroupPrefix()
     */
    private String groupPrefix = "";

    /**
     * @see #getGroupSuffix()
     */
    private String groupSuffix = "";

    /**
     * @return the prefix to add before each new group name
     */
    @PropertyName("Group name prefix")
    @PropertyDescription("The prefix to add before each new group name")
    public String getGroupPrefix()
    {
        return this.groupPrefix;
    }

    /**
     * @param groupPrefix The prefix to add before each new group name
     */
    public void setGroupPrefix(String groupPrefix)
    {
        this.groupPrefix = groupPrefix;
    }

    /**
     * @return The suffix to add after each new group name
     */
    @PropertyName("Group name suffix")
    @PropertyDescription("The suffix to add after each new group name")
    public String getGroupSuffix()
    {
        return this.groupSuffix;
    }

    /**
     * @param groupSuffix The prefix to add before each new group
     */
    public void setGroupSuffix(String groupSuffix)
    {
        this.groupSuffix = groupSuffix;
    }
}
