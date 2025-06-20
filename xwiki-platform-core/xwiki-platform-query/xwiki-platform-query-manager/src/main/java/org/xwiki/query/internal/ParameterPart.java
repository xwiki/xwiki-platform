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
package org.xwiki.query.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a part of a HQL parameter. There can be several parts since we separate literals (ie characters that
 * will be escaped) vs special characters having a SQL meaning and that won't be escaped (ie {@code _} and {@code %}).
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class ParameterPart
{
    private String value;

    /**
     * @param value the characters for this part
     */
    public ParameterPart(String value)
    {
        this.value = value;
    }

    /**
     * @return the characters for this part
     */
    public String getValue()
    {
        return this.value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParameterPart that = (ParameterPart) o;

        return new EqualsBuilder().append(value, that.value).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(63, 37).append(value).toHashCode();
    }
}
