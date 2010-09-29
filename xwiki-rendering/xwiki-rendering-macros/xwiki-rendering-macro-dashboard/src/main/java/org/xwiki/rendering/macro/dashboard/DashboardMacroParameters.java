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
package org.xwiki.rendering.macro.dashboard;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the dashboard macro.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class DashboardMacroParameters
{
    /**
     * The identifier of the layout to be used for this dashboard. If none specified, columns will be used.
     */
    private String layout = "columns";

    /**
     * @return the layout style of this dashboard
     */
    public String getLayout()
    {
        return layout;
    }

    /**
     * @param layout the layout to set
     */
    @PropertyDescription("The identifier of the layout to use for this dashboard (e.g. columns, etc). "
        + "If none specified, columns will be used.")
    public void setLayout(String layout)
    {
        this.layout = layout;
    }
}
