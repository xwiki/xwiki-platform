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
package org.xwiki.rest.jersey.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.rest.XWikiRestComponent;

/**
 * Reload Jersey when a XWikiResource component is uninstalled/installed.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Component
@Named(JerseyReloadListener.HINT)
@Singleton
public class JerseyReloadListener extends AbstractEventListener
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "org.xwiki.rest.jersey.internal.JerseyReloadListener";

    private static final String RESTART = "rest.restart";

    private static final JobStartedEvent PARENT = new JobStartedEvent();

    @Inject
    private Logger logger;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Execution execution;

    @Inject
    private JerseyServletContainer container;

    /**
     * The default constructor.
     */
    public JerseyReloadListener()
    {
        super(HINT, new ComponentDescriptorAddedEvent(XWikiRestComponent.class),
            new ComponentDescriptorRemovedEvent(XWikiRestComponent.class), new JobFinishedEvent("install"),
            new JobFinishedEvent("uninstall"));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ComponentDescriptorEvent) {
            onComponentDescriptorEvent();
        } else {
            flush();
        }
    }

    private void onComponentDescriptorEvent()
    {
        if (this.observationContext.isIn(PARENT)) {
            // If in a job, just remember to restart at the end
            restartLater();
        } else {
            // If not in a job, restart right away
            restartNow();
        }
    }

    private void restartLater()
    {
        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            context.setProperty(RESTART, true);
        }
    }

    private void restartNow()
    {
        try {
            // Restart Jersey service
            this.container.restart();

            // Clean restart marker
            ExecutionContext context = this.execution.getContext();
            if (context != null) {
                context.removeProperty(RESTART);
            }
        } catch (Exception e) {
            this.logger.error("Failed to restart the JAX-RS application", e);
        }
    }

    private void flush()
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null && context.hasProperty(RESTART)) {
            restartNow();
        }
    }
}
