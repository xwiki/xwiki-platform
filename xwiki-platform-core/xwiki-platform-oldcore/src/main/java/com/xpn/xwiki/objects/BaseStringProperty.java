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

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.merge.MergeUtils;

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
        return this.value;
    }

    @Override
    public void setValue(Object value)
    {
        this.value = (String) value;
    }

    @Override
    public String toText()
    {
        String value = getValue();
        if (value != null) {
            return value;
        }

        return "";
    }

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

    @Override
    public BaseStringProperty clone()
    {
        BaseStringProperty property = (BaseStringProperty) super.clone();
        property.setValue(getValue());

        return property;
    }

    @Override
    protected void mergeValue(Object previousValue, Object newValue, MergeResult mergeResult)
    {
        setValue(MergeUtils.mergeString((String) previousValue, (String) newValue, getValue(), mergeResult));
    }
}
