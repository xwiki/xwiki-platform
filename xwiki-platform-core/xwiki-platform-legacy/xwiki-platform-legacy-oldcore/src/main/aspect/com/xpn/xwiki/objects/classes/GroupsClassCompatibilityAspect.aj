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
package com.xpn.xwiki.objects.classes;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.GroupsClass} class.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public privileged aspect GroupsClassCompatibilityAspect
{
    /**
     * @return {@code true} if the list box that is used to select the groups should be filled with all the available
     *         groups, {@code false} otherwise
     * @deprecated this meta property is not used anymore because we changed the default displayer
     */
    @Deprecated(since = "4.3M2")
    public boolean GroupsClass.isUsesList()
    {
        return getIntValue("usesList") == 1;
    }

    /**
     * Sets whether to list all the available groups in the list box used to select the groups.
     *
     * @param usesList {@code true} to fill the list box that is used to select the groups with all the available
     *            groups, {@code false} otherwise
     * @deprecated this meta property is not used anymore because we changed the default displayer
     */
    @Deprecated(since = "4.3M2")
    public void GroupsClass.setUsesList(boolean usesList)
    {
        setIntValue("usesList", usesList ? 1 : 0);
    }
}
