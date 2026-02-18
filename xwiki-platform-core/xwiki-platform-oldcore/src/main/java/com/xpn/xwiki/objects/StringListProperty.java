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
package com.xpn.xwiki.objects;

import org.xwiki.stability.Unstable;

import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Property defining a list stored in a string.
 *
 * @version $Id$
 */
public class StringListProperty extends ListProperty
{
    /**
     * The type used as a hint to find the property.
     * @since 18.2.0RC1
     */
    @Unstable
    public static final String PROPERTY_TYPE = "StringList";

    private static final long serialVersionUID = 1L;

    @Override
    public Object getCustomMappingValue()
    {
        return getTextValue();
    }

    /**
     * This method is called by Hibernate to set the raw value read from the database. Check the xwiki.hbm.xml file.
     *
     * @param value the string value retrieved from the database
     */
    public void setTextValue(String value)
    {
        setList(ListClass.getListFromString(value));
    }

    @Override
    public String getPropertyType()
    {
        return PROPERTY_TYPE;
    }
}
