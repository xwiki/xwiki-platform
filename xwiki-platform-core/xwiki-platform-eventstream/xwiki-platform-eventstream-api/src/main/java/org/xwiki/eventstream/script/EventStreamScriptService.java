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
package org.xwiki.eventstream.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script services for the Event Stream Module.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named("eventstream")
@Unstable
public class EventStreamScriptService implements ScriptService
{
    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    /**
     * @return the list of the available RecordableEventDescriptors
     * @throws EventStreamException if an error happen
     */
    public List<RecordableEventDescriptor> getAllRecordableEventDescriptors() throws EventStreamException
    {
        return recordableEventDescriptorManager.getAllRecordableEventDescriptorsAllWikis();
    }
}
