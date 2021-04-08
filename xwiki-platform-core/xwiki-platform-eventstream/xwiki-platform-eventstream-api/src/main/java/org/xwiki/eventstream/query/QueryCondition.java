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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Filter the result of the search.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public class QueryCondition
{
    private final boolean reversed;

    /**
     * @param reversed true if the condition should be reversed
     */
    public QueryCondition(boolean reversed)
    {
        this.reversed = reversed;
    }

    /**
     * @return true of the condition should be reversed
     */
    public boolean isReversed()
    {
        return this.reversed;
    }

    @Override
    public int hashCode()
    {
        return isReversed() ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof QueryCondition) {
            QueryCondition condition = (QueryCondition) obj;

            return isReversed() == condition.isReversed();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("reversed", isReversed());

        return builder.build();
    }
}
