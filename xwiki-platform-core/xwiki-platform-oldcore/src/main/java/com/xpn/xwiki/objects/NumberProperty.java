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

import java.lang.reflect.ParameterizedType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.component.util.ReflectionUtils;

public abstract class NumberProperty<N extends Number> extends BaseProperty
{
    private static final long serialVersionUID = 1L;

    private N value;

    private Class<N> numberClass;

    public NumberProperty()
    {
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(NumberProperty.class, getClass());
        this.numberClass = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[0]);
    }

    @Override
    public Object getValue()
    {
        return this.value;
    }

    @Override
    public void setValue(Object value)
    {
        N number = convert(value);

        setValueDirty(value);
        this.value = number;
    }

    private N convert(Object value)
    {
        N number = null;

        if (value != null) {
            if (this.numberClass == value.getClass()) {
                number = (N) value;
            } else {
                if (this.numberClass == Double.class) {
                    if (value instanceof Number) {
                        number = (N) (Double) ((Number) value).doubleValue();
                    } else {
                        number = (N) Double.valueOf(value.toString());
                    }
                } else if (this.numberClass == Float.class) {
                    if (value instanceof Number) {
                        number = (N) (Float) ((Number) value).floatValue();
                    } else {
                        number = (N) Float.valueOf(value.toString());
                    }
                } else if (this.numberClass == Integer.class) {
                    if (value instanceof Number) {
                        number = (N) (Integer) ((Number) value).intValue();
                    } else {
                        number = (N) Integer.valueOf(value.toString());
                    }
                } else if (this.numberClass == Long.class) {
                    if (value instanceof Number) {
                        number = (N) (Long) ((Number) value).longValue();
                    } else {
                        number = (N) Long.valueOf(value.toString());
                    }
                }
            }
        }

        return number;
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
        if (obj == null) {
            return false;
        }

        return new EqualsBuilder().appendSuper(super.equals(obj)).append(getValue(), ((NumberProperty) obj).getValue())
            .isEquals();
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
