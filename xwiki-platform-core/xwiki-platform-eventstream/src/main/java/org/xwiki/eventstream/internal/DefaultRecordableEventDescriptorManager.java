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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
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
    private ComponentManager componentManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ModelContext modelContext;

    @Override
    public List<RecordableEventDescriptor> getAllRecordableEventDescriptors() throws EventStreamException
    {
        try {
            return componentManager.getInstanceList(RecordableEventDescriptor.class);
        } catch (ComponentLookupException e) {
            throw new EventStreamException("Failed to retrieve the list of RecordableEventDescriptor.", e);
        }
    }

    @Override
    public List<RecordableEventDescriptor> getAllRecordableEventDescriptorsAllWikis() throws EventStreamException
    {
        if (isMainWiki()) {
            // We use an hashSet to be sure we won't store the same descriptor twice (in case the same application
            // is installed on several wikis).
            Set<RecordableEventDescriptor> recordableEventDescriptors = new HashSet<>();

            try {
                for (String wikiId : wikiDescriptorManager.getAllIds()) {
                    modelContext.setCurrentEntityReference(new WikiReference(wikiId));
                    for (RecordableEventDescriptor recordableEventDescriptor : getAllRecordableEventDescriptors()) {
                        recordableEventDescriptors.add(recordableEventDescriptor);
                    }
                }
            } catch (WikiManagerException e) {
                throw new EventStreamException("Failed to get the list of all Recordable Event Descriptors.", e);
            } finally {
                modelContext.setCurrentEntityReference(new WikiReference(wikiDescriptorManager.getMainWikiId()));
            }

            return new ArrayList<>(recordableEventDescriptors);
        }

        return getAllRecordableEventDescriptors();
    }

    private boolean isMainWiki()
    {
        return wikiDescriptorManager.getMainWikiId() != null
                && wikiDescriptorManager.getMainWikiId().equals(wikiDescriptorManager.getCurrentWikiId());
    }
}
