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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.query.QueryException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Iterator that retrieve all users of the current wiki interested in the notifications emails at the specified
 * interval.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = NotificationUserIterator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NotificationUserIterator implements Iterator<DocumentReference>
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private IntervalUsersManager usersManager;

    @Inject
    private Logger logger;

    private Iterator<DocumentReference> iterator;
    private NotificationEmailInterval interval;

    /**
     * Initialize the user iterator.
     * 
     * @param interval the interval that users must have configured
     */
    public void initialize(NotificationEmailInterval interval)
    {
        this.interval = interval;
        try {
            this.iterator =
                this.usersManager.getUsers(interval, this.wikiDescriptorManager.getCurrentWikiId()).iterator();
        } catch (QueryException e) {
            this.logger.warn("Failed to retrieve the notification users. Root error [{}]",
                ExceptionUtils.getRootCauseMessage(e));

            // Create an empty iterator
            this.iterator = Collections.emptyIterator();
        }
    }

    @Override
    public boolean hasNext()
    {
        return this.iterator.hasNext();
    }

    @Override
    public DocumentReference next()
    {
        return this.iterator.next();
    }

    /**
     * @return the interval the iterator is set for.
     * @since 15.6RC1
     */
    public NotificationEmailInterval getInterval()
    {
        return this.interval;
    }
}
