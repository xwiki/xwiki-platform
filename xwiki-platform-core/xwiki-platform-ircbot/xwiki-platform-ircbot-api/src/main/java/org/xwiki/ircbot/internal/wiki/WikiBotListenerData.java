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
package org.xwiki.ircbot.internal.wiki;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.model.reference.DocumentReference;

/**
 * In-memory information about a Bot Listener.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class WikiBotListenerData extends BotListenerData
{
    /**
     * @see #getReference()
     */
    private DocumentReference botListenerReference;

    /**
     * @param botListenerReference see {@link #getReference()}
     * @param id see {@link #getId()}
     * @param name see {@link #getName()}
     * @param description see {@link #getDescription()}
     */
    public WikiBotListenerData(DocumentReference botListenerReference, String id, String name, String description)
    {
        super(id, name, description, true);
        this.botListenerReference = botListenerReference;
    }

    /**
     * @return the reference to the document containing the Wiki Bot Listener definition
     */
    public DocumentReference getReference()
    {
        return this.botListenerReference;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        WikiBotListenerData rhs = (WikiBotListenerData) object;
        return new EqualsBuilder()
            .appendSuper(super.equals(object))
            .append(getReference(), rhs.getReference())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 17)
            .appendSuper(super.hashCode())
            .append(getReference())
            .toHashCode();
    }
}
