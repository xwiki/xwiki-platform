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

public abstract class NumberProperty extends BaseProperty
{
    private Number value;

    public NumberProperty()
    {
    }

    @Override
    public Object getValue()
    {
        return this.value;
    }

    @Override
    public void setValue(Object value)
    {
        setValueDirty(value);
        this.value = (Number) value;
    }

    @Override
    public String toText()
    {
        Number nb = (Number) getValue();
        return (nb == null) ? "" : nb.toString();
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

        if ((getValue() == null) && (((NumberProperty) obj).getValue() == null)) {
            return true;
        }

        return getValue().equals(((NumberProperty) obj).getValue());
    }

    @Override
    public NumberProperty clone()
    {
        return (NumberProperty) super.clone();
    }

    @Override
    protected void cloneInternal(BaseProperty clone)
    {
        NumberProperty property = (NumberProperty) clone;
        property.setValue(getValue());
    }

}
