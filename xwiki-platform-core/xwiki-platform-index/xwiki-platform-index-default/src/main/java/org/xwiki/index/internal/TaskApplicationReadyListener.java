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
package org.xwiki.index.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.index.TaskManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Listen for the application to be ready before starting the {@link DefaultTasksManager}. Note that this class is only
 * useful to start the thread of the {@link DefaultTasksManager} when the application is ready and does nothing if
 * another implementation of {@link TaskManager} is injected instead.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Component
@Singleton
@Named("org.xwiki.index.internal.TaskApplicationReadyListener")
public class TaskApplicationReadyListener extends AbstractEventListener implements Initializable
{
    @Inject
    private TaskManager taskManager;

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor, initialize the listener with its name and the listened event ({@link
     * ApplicationReadyEvent}).
     */
    public TaskApplicationReadyListener()
    {
        super("TaskApplicationReadyListener", new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // In case of ApplicationReadyEvent (when the wiki starts) and the implementation of type DefaultTasksManager.
        if (this.taskManager instanceof DefaultTasksManager) {
            ((DefaultTasksManager) this.taskManager).startThread();
        }
    }

    @Override
    public void initialize()
    {
        // If the application is already initialized we start the threads immediately (e.g. in case of extension
        // install) and the implementation of type DefaultTasksManager.
        if (this.contextProvider.get() != null && this.taskManager instanceof DefaultTasksManager) {
            ((DefaultTasksManager) this.taskManager).startThread();
        }
    }
}
