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
package org.xwiki.icon;

/**
 * An icon that have a name and a value to help rendering it.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class Icon
{
    private String value;

    /**
     * Default constructor.
     */
    public Icon()
    {
    }

    /**
     * Constructor.

     * @param value value of the icon
     */
    public Icon(String value)
    {
        this.value = value;
    }

    /**
     * @return the value of the icon
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the value of the icon.
     * @param value the value to set
     */
    public void setValue(String value)
    {
        this.value = value;
    }
}
