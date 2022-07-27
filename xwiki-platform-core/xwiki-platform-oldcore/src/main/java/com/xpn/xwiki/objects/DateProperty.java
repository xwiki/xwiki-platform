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

import java.util.Date;

import org.xwiki.xar.internal.property.DateXarObjectPropertySerializer;

/**
 * Represents a date property.
 *
 * @version $Id$
 */
public class DateProperty extends BaseProperty implements Cloneable
{
    private static final long serialVersionUID = 1L;

    /**
     * The property value.
     */
    private Date value;

    @Override
    public Object getValue()
    {
        return this.value;
    }

    @Override
    public void setValue(Object value)
    {
        // Make sure to store a Date and not some extended Date or it's going to be a nightmare to compare between
        // them
        Date date = (Date) value;
        if (date != null && date.getClass() != Date.class) {
            date = new Date(((Date) value).getTime());
        }

        setValueDirty(date);
        this.value = date;
    }

    @Override
    public String toText()
    {
        return DateXarObjectPropertySerializer.serializeDate(getValue());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        // Same Java object, they sure are equal.
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getValue() == null && ((DateProperty) obj).getValue() == null) {
            return true;
        }

        return getValue().equals(((DateProperty) obj).getValue());
    }

    @Override
    public DateProperty clone()
    {
        return (DateProperty) super.clone();
    }

    @Override
    protected void cloneInternal(BaseProperty clone)
    {
        DateProperty property = (DateProperty) clone;

        if (property.value != null) {
            property.value = (Date) property.value.clone();
        }
    }
}
