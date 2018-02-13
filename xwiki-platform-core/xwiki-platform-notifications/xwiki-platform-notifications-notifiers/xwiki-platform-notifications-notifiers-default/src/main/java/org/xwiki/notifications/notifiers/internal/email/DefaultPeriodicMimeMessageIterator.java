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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.sources.NotificationManager;

/**
 * Default implementation of {@link PeriodicMimeMessageIterator}.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultPeriodicMimeMessageIterator extends AbstractMimeMessageIterator
        implements PeriodicMimeMessageIterator
{
    @Inject
    private NotificationManager notificationManager;

    private Date lastTrigger;

    @Override
    public void initialize(NotificationUserIterator userIterator, Map<String, Object> factoryParameters,
            Date lastTrigger, DocumentReference templateReference)
    {
        this.lastTrigger = lastTrigger;
        super.initialize(userIterator, factoryParameters, templateReference);
    }

    protected List<CompositeEvent> retrieveCompositeEventList(DocumentReference user) throws NotificationException
    {
        return notificationManager.getEvents(serializer.serialize(user),
                NotificationFormat.EMAIL, Integer.MAX_VALUE / 4, null,
                lastTrigger, Collections.emptyList());
    }
}
