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
package org.xwiki.ircbot.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * In-memory information about a Bot Listener.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class BotListenerData
{
    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getDescription()
     */
    private String description;

    /**
     * @see #isWikiBotListener()
     */
    private boolean isWikiBotListener;

    /**
     * @param id see {@link #getId()}
     * @param name see {@link #getName()}
     * @param description see {@link #getDescription()}
     */
    public BotListenerData(String id, String name, String description)
    {
        this(id, name, description, false);
    }

    /**
     * @param id see {@link #getId()}
     * @param name see {@link #getName()}
     * @param description see {@link #getDescription()}
     * @param isWikiBotListener see {@link #isWikiBotListener()}
     */
    public BotListenerData(String id, String name, String description, boolean isWikiBotListener)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isWikiBotListener = isWikiBotListener;
    }

    /**
     * @return true if this Bot Listener is defined in a wiki page, false otherwise.
     */
    public boolean isWikiBotListener()
    {
        return this.isWikiBotListener;
    }

    /**
     * @return the id is a serialized Reference (in a compact notation, ie without the wiki part) if the Bot listener
     *         is defined in a wiki page then, the hint of the component otherwise
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return see {@link org.xwiki.ircbot.IRCBotListener#getName()}
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return see {@link org.xwiki.ircbot.IRCBotListener#getDescription()}
     */
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
