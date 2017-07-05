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
package org.xwiki.notifications.internal.email.live;

import java.util.Arrays;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.internal.email.AbstractMimeMessageIterator;
import org.xwiki.notifications.internal.email.NotificationUserIterator;

/**
 * MimeMessageIterator for sending live mail notifications.
 */
@Component(roles = LiveMimeMessageIterator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveMimeMessageIterator extends AbstractMimeMessageIterator
{
    private List<CompositeEvent> compositeEvents;

    /**
     * Initialize the iterator.
     *
     * @param userIterator iterator that returns all users
     * @param event the event that has to be sent to every user
     * @param templateReference reference to the mail template
     */
    public void initialize(NotificationUserIterator userIterator, CompositeEvent event,
            DocumentReference templateReference)
    {
        this.compositeEvents = Arrays.asList(event);
        super.initialize(userIterator, templateReference);
    }

    protected List<CompositeEvent> retrieveCompositeEventList(DocumentReference user) throws NotificationException
    {
        return this.compositeEvents;
    }
}