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
package org.xwiki.livedata;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes a live data entry.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveDataEntryDescriptor implements InitializableLiveDataElement
{
    private String idProperty;

    /**
     * @return the name of the property that identifies the live data entry
     */
    public String getIdProperty()
    {
        return idProperty;
    }

    /**
     * Sets the name of the property that identifies the live data entry.
     * 
     * @param idProperty the name of a property of a live data entry
     */
    public void setIdProperty(String idProperty)
    {
        this.idProperty = idProperty;
    }
}
