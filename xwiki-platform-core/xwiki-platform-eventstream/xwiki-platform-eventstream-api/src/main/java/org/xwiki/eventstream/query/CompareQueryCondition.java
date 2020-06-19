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
package org.xwiki.eventstream.query;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A comparison between a property and a passed value.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public class CompareQueryCondition extends AbstractPropertyQueryCondition
{
    /**
     * The type of comparison.
     * 
     * @version $Id$
     */
    public enum CompareType
    {
        /**
         * The property value is greater than the passed value.
         */
        GREATER,

        /**
         * The property value is lower than the passed value.
         */
        LESS,

        /**
         * The property value is greater or equals to the passed value.
         */
        GREATER_OR_EQUALS,

        /**
         * The property value is lower or equals to the passed value.
         */
        LESS_OR_EQUALS,

        /**
         * The property value is equals to the passed value.
         */
        EQUALS
    }

    private final Object value;

    private final CompareType type;

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @param type the type of comparison
     */
    public CompareQueryCondition(String property, Object value, CompareType type)
    {
        this(property, value, type, false);
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @param type the type of comparison
     * @param reversed true if the condition should be reversed
     */
    public CompareQueryCondition(String property, Object value, CompareType type, boolean reversed)
    {
        super(reversed, property);

        this.value = value;
        this.type = type;
    }

    /**
     * @return the value the property should be equal to
     */
    public Object getValue()
    {
        return this.value;
    }

    /**
     * @return the type the type of comparison
     */
    public CompareType getType()
    {
        return this.type;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getValue());
        builder.append(getType());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof CompareQueryCondition) {
            CompareQueryCondition compare = (CompareQueryCondition) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getValue(), compare.getValue());
            builder.append(getType(), compare.getType());

            return builder.build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("value", getValue());
        builder.append("type", getType());

        return builder.build();
    }
}
