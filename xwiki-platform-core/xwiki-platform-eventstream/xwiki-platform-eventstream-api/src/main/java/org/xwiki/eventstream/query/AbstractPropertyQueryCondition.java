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

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A condition with a property.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public abstract class AbstractPropertyQueryCondition extends QueryCondition
{
    private final String property;

    /**
     * @param reversed true if the condition should be reversed
     * @param property the name of the property
     */
    public AbstractPropertyQueryCondition(boolean reversed, String property)
    {
        super(reversed);

        this.property = property;
    }

    /**
     * @return the property
     */
    public String getProperty()
    {
        return this.property;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getProperty());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (super.equals(obj) && obj instanceof AbstractPropertyQueryCondition) {
            return getProperty().contentEquals(((AbstractPropertyQueryCondition) obj).getProperty());
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("property", getProperty());

        return builder.build();
    }
}
