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
package org.xwiki.index;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Provides the operations to interact with the task consumer from the scripts.
 *
 * @version $Id$
 * @since 14.2
 */
@Named("taskConsumer")
@Component
@Singleton
public class TaskConsumerScriptService implements ScriptService
{
    @Inject
    private TaskManager taskManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @return the count of queued tasks, grouped by task type
     */
    public Map<String, Long> getQueueSizePerType()
    {
        return this.taskManager.getQueueSizePerType(this.wikiDescriptorManager.getCurrentWikiId());
    }
}
