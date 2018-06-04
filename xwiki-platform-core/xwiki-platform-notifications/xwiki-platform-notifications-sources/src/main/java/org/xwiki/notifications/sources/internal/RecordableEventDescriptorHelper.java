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
package org.xwiki.notifications.sources.internal;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Helper to get (and cache) the recordable event descriptors enabled in the context of a given user.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@Component(roles = RecordableEventDescriptorHelper.class)
@Singleton
public class RecordableEventDescriptorHelper
{
    private static final String CONTEXT_KEY = "RecordableEventDescriptorHelperCache_";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private Execution execution;

    private boolean isGlobalUser(DocumentReference user)
    {
        return wikiDescriptorManager.getMainWikiId().equals(user.getWikiReference().getName());
    }

    /**
     * @param eventType an event type
     * @param user the user for who we are computing the notifications
     * @return either or not the event type has a corresponding descriptor in the context of the given user
     * @throws EventStreamException if an error occurs
     */
    public boolean hasDescriptor(String eventType, DocumentReference user) throws EventStreamException
    {
        return getRecordableEventDescriptor(user).stream()
                .anyMatch(descriptor -> StringUtils.equals(eventType, descriptor.getEventType()));
    }

    private Collection<RecordableEventDescriptor> getRecordableEventDescriptor(DocumentReference user)
            throws EventStreamException
    {
        final String contextKey = CONTEXT_KEY + user;

        ExecutionContext context = execution.getContext();
        if (context.hasProperty(contextKey)) {
            return (Collection<RecordableEventDescriptor>) context.getProperty(contextKey);
        }

        Collection<RecordableEventDescriptor> descriptors
                = recordableEventDescriptorManager.getRecordableEventDescriptors(isGlobalUser(user));
        context.setProperty(contextKey, descriptors);

        return descriptors;
    }
}
