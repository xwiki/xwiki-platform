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

package com.xpn.xwiki.objects;


/**
 * Base string XProperty which all types of string XProperties extend.
 *
 * $Id$
 */
public class BaseStringProperty extends BaseProperty
{
    /** The value of the string. */
    private String value;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#getValue()
     */
    @Override
    public String getValue()
    {
        return this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object value)
    {
        this.value = (String) value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#toText()
     */
    @Override
    public String toText()
    {
        String value = getValue();
        if (value != null) {
            return value;
        }

        return "";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        // Same Java object, they sure are equal
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if ((getValue() == null) && (((BaseStringProperty) obj).getValue() == null)) {
            return true;
        }

        return getValue().equals(((BaseStringProperty) obj).getValue());
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#clone()
     */
    @Override
    public Object clone()
    {
        BaseStringProperty property = (BaseStringProperty) super.clone();
        property.setValue(getValue());

        return property;
    }
}
