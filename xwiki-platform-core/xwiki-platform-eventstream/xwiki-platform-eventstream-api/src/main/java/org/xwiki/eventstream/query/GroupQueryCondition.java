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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A group of conditions.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public class GroupQueryCondition extends QueryCondition
{
    protected final List<QueryCondition> conditions = new ArrayList<>();

    private final boolean or;

    /**
     * @param or true if one of the conditions is enough, if false all conditions must match
     * @param reversed true if the condition should be reversed
     * @param conditions the condition to start with
     */
    public GroupQueryCondition(boolean or, boolean reversed, QueryCondition... conditions)
    {
        super(reversed);

        this.or = or;

        CollectionUtils.addAll(this.conditions, conditions);
    }

    /**
     * @return true if one of the conditions is enough, if false all conditions must match
     */
    public boolean isOr()
    {
        return this.or;
    }

    /**
     * @return the conditions
     */
    public List<QueryCondition> getConditions()
    {
        return this.conditions;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(isOr());
        builder.append(getConditions());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof GroupQueryCondition) {
            GroupQueryCondition group = (GroupQueryCondition) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(isOr(), group.isOr());
            builder.append(getConditions(), group.getConditions());

            return builder.build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("or", isOr());
        builder.append("conditions", getConditions());

        return builder.build();
    }
}
