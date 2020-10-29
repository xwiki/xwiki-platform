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
package org.xwiki.extension.index.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.internal.job.ExtensionIndexJob;
import org.xwiki.extension.index.internal.job.ExtensionIndexRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

/**
 * Start the indexer when XWiki starts.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Named(ExtensionIndexInitializerListener.NAME)
public class ExtensionIndexInitializerListener extends AbstractEventListener
{
    /**
     * The unique name of this event listener.
     */
    public static final String NAME = "ExtensionIndexInitializerListener";

    @Inject
    private JobExecutor jobs;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public ExtensionIndexInitializerListener()
    {
        super(NAME, new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Trigger first indexing
        try {
            this.jobs.execute(ExtensionIndexJob.JOB_TYPE, new ExtensionIndexRequest(true));
        } catch (JobException e) {
            this.logger.error("Failed to start indexing the available extensions", e);
        }
    }
}
