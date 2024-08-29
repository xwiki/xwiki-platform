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
package org.xwiki.extension.security.internal.listener;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.internal.ExtensionSecurityScheduler;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

import static org.xwiki.extension.index.internal.job.ExtensionIndexJob.JOB_TYPE;

/**
 * Starts the {@link ExtensionSecurityScheduler} in charge of fetching known security vulnerabilities for installed
 * extensions once the extension indexing is done.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Named(ExtensionSecurityInitializerListener.NAME)
@Singleton
public class ExtensionSecurityInitializerListener extends AbstractLocalEventListener
{
    /**
     * The name of the event listener (and its component hint).
     */
    public static final String NAME = "ExtensionSecurityInitializerListener";

    @Inject
    private Provider<ExtensionSecurityScheduler> schedulerProvider;

    /**
     * Default constructor.
     */
    public ExtensionSecurityInitializerListener()
    {
        super(NAME, new JobFinishedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        // Clustering related note: since the JobFinishedEvent is only sent by node, the scheduler will also be stared 
        // on a single node. Therefore 
        if (event instanceof JobFinishedEvent
            && Objects.equals(((JobFinishedEvent) event).getJobType(), JOB_TYPE))
        {
            this.schedulerProvider.get().start();
        }
    }
}
