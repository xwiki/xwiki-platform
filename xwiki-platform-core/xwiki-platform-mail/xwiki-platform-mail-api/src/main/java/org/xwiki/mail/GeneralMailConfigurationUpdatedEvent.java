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
package org.xwiki.mail;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * An event triggered after the general mail configuration has been changed.
 * <p>
 * The event also sends the following parameters:
 * </p>
 * <ul>
 * <li>source: the wiki id (as string) where the configuration has been changed, or {@code null} if the change was on
 * the main wiki and thus affects all wikis.</li>
 * <li>data: {@code null}</li>
 * </ul>
 * <p>This event is intentionally not serializable as it will be triggered on every node of the cluster separately.</p>
 *
 * @since 14.10.15
 * @since 15.5.2
 * @since 15.7RC1
 * @version $Id$
 */
@Unstable
public class GeneralMailConfigurationUpdatedEvent implements Event
{
    private String wikiId;

    /**
     * Default constructor, used to get notified about a mail configuration change in any wiki or to trigger an event
     * for a configuration change on the main wiki.
     */
    public GeneralMailConfigurationUpdatedEvent()
    {
    }

    /**
     * An event for changes that affect the passed wiki id. Used to get notified about a change that affects the
     * specified wiki or to trigger an event on a subwiki.
     *
     * @param wikiId the id of the wiki where the configuration has been changed
     */
    public GeneralMailConfigurationUpdatedEvent(String wikiId)
    {
        this.wikiId = wikiId;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == this) {
            return true;
        }

        boolean isMatching = false;

        if (this.getClass().isAssignableFrom(otherEvent.getClass())) {
            GeneralMailConfigurationUpdatedEvent other = (GeneralMailConfigurationUpdatedEvent) otherEvent;
            isMatching = this.wikiId == null || other.wikiId == null || Objects.equals(this.wikiId, other.wikiId);
        }

        return isMatching;
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

        GeneralMailConfigurationUpdatedEvent that = (GeneralMailConfigurationUpdatedEvent) o;

        return new EqualsBuilder().append(this.wikiId, that.wikiId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.wikiId).toHashCode();
    }
}
