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

/**
 * In-memory information about a Bot Listener.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class BotListenerData
{
    /**
     * If the Bot listener is defined in a wiki page then the id is a serialized Reference (in a compact notation, ie
     * without the wiki part). Otherwise it's the hint of the component.
     */
    private String id;

    private String name;

    private String description;

    private boolean isWikiBotListener;

    public BotListenerData(String id, String name, String description)
    {
        this(id, name, description, false);
    }

    public BotListenerData(String id, String name, String description, boolean isWikiBotListener)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isWikiBotListener = isWikiBotListener;
    }

    public boolean isWikiBotListener()
    {
        return this.isWikiBotListener;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
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
        BotListenerData rhs = (BotListenerData) object;
        return new EqualsBuilder()
            .append(getId(), rhs.getId())
            .append(getName(), rhs.getName())
            .append(getDescription(), rhs.getDescription())
            .append(isWikiBotListener(), rhs.isWikiBotListener())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 17)
            .append(getId())
            .append(getName())
            .append(getDescription())
            .append(isWikiBotListener())
            .toHashCode();
    }
}
