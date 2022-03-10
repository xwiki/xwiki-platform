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

import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A condition which is true if the property value is equals to one of the passed values.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public class InQueryCondition extends AbstractPropertyQueryCondition
{
    private final List<Object> values;

    /**
     * @param reversed true if the condition should be reversed
     * @param property the name of the property
     * @param values the values to compare to the property value
     */
    public InQueryCondition(boolean reversed, String property, List<Object> values)
    {
        super(reversed, property);

        this.values = values;
    }

    /**
     * @param reversed true if the condition should be reversed
     * @param property the name of the property
     * @param custom true if the property is a custom parameter
     * @param values the values to compare to the property value
     * @since 13.9RC1
     */
    public InQueryCondition(boolean reversed, String property, boolean custom, List<Object> values)
    {
        super(reversed, property, custom);

        this.values = values;
    }

    /**
     * @param reversed true if the condition should be reversed
     * @param property the name of the property
     * @param custom true if it's a custom event reversed
     * @param customType the type in which that property was stored
     * @param values the values to compare to the property value
     * @since 14.2RC1
     */
    public InQueryCondition(boolean reversed, String property, boolean custom, Type customType, List<Object> values)
    {
        super(reversed, property, custom, customType);

        this.values = values;
    }

    /**
     * @return the values
     */
    public List<Object> getValues()
    {
        return this.values;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getValues());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof InQueryCondition) {
            InQueryCondition condition = (InQueryCondition) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getValues(), condition.getValues());

            return builder.build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("values", getValues());

        return builder.build();
    }
}
