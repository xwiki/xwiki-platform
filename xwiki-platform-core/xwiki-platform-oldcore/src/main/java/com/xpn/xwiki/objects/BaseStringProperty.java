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

import com.google.common.base.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Base string XProperty which all types of string XProperties extend. $Id$
 */
public class BaseStringProperty extends BaseProperty
{
    /** The value of the string. */
    private String value;

    @Override
    public String getValue()
    {
        // A null String does not make much sense (the whole property would not be in the xobject in that case) and we
        // have to make sure something saved as empty string will come back as such in Oracle (which has a very
        // annoying "empty string is stored as null" behavior)
        return this.value != null ? this.value : "";
    }

    @Override
    public void setValue(Object value)
    {
        setValueDirty(value);
        this.value = (String) value;
    }

    @Override
    public String toText()
    {
        return getValue();
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

        if (!(obj instanceof BaseStringProperty)) {
            return false;
        }

        return Objects.equal(this.getValue(), ((BaseStringProperty) obj).getValue());
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getValue());

        return builder.toHashCode();
    }

    @Override
    public BaseStringProperty clone()
    {
        return (BaseStringProperty) super.clone();
    }

    @Override
    protected void cloneInternal(BaseProperty clone)
    {
        BaseStringProperty property = (BaseStringProperty) clone;
        property.setValue(getValue());
    }
}
