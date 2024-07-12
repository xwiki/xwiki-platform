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
package org.xwiki.eventstream.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Default implementation of {@link org.xwiki.eventstream.RecordableEventDescriptorManager}.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultRecordableEventDescriptorManager implements RecordableEventDescriptorManager
{
    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public List<RecordableEventDescriptor> getRecordableEventDescriptors(boolean allWikis) throws EventStreamException
    {
        try {
            // We use an hashSet to be sure we won't store the same descriptor twice (in case the same application
            // is installed on several wikis).
            Set<RecordableEventDescriptor> recordableEventDescriptors = new HashSet<>();

            // Load the component from the context component manager (root + wiki + user component managers, etc...)
            recordableEventDescriptors.addAll(contextComponentManager.getInstanceList(RecordableEventDescriptor.class));
            recordableEventDescriptors.addAll(contextComponentManager.getInstanceList(
                    UntypedRecordableEventDescriptor.class));

            // Remove disabled components
            Iterator<RecordableEventDescriptor> iterator = recordableEventDescriptors.iterator();
            while (iterator.hasNext()) {
                RecordableEventDescriptor descriptor = iterator.next();
                if (!descriptor.isEnabled(wikiDescriptorManager.getCurrentWikiId())) {
                    iterator.remove();
                }
            }

            // Maybe add components of the other wikis too
            if (allWikis) {
                for (String wikiId : wikiDescriptorManager.getAllIds()) {
                    recordableEventDescriptors.addAll(getDescriptorsFromWiki(wikiId));
                }
            }

            return new ArrayList<>(recordableEventDescriptors);

        } catch (WikiManagerException | ComponentLookupException e) {
            throw new EventStreamException("Failed to get the list of all Recordable Event Descriptors.", e);
        }
    }

    @Override
    public RecordableEventDescriptor getDescriptorForEventType(String eventType, boolean allWikis)
            throws EventStreamException
    {
        // FIXME: We should cache the descriptors for improving perf when calling multiple times this method.
        return getRecordableEventDescriptors(allWikis).stream().filter(descriptor
            -> eventType.equals(descriptor.getEventType())).findAny().orElse(null);
    }

    private List<RecordableEventDescriptor> getDescriptorsFromWiki(String wikiId)
            throws ComponentLookupException
    {
        Namespace namespace = new WikiNamespace(wikiId);
        ComponentManager wikiComponentManager =
                componentManagerManager.getComponentManager(namespace.serialize(), false);
        if (wikiComponentManager == null) {
            return Collections.emptyList();
        }

        List<RecordableEventDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(wikiComponentManager.getInstanceList(RecordableEventDescriptor.class));
        descriptors.addAll(wikiComponentManager.getInstanceList(UntypedRecordableEventDescriptor.class));

        // Remove disabled components
        Iterator<RecordableEventDescriptor> iterator = descriptors.iterator();
        while (iterator.hasNext()) {
            RecordableEventDescriptor descriptor = iterator.next();
            if (!descriptor.isEnabled(wikiId)) {
                iterator.remove();
            }
        }

        return descriptors;
    }
}
