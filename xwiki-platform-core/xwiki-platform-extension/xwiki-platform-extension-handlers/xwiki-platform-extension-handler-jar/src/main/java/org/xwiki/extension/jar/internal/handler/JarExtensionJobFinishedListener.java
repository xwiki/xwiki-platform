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

package org.xwiki.extension.jar.internal.handler;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.handler.ExtensionInitializer;
import org.xwiki.extension.job.Request;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.event.JobFinishedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Listen to job finished events to properly refresh extension ClassLoader when an uninstall job has been executed.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("JarExtensionJobFinishedListener")
public class JarExtensionJobFinishedListener implements EventListener
{
    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Collections.<Event> singletonList(new JobFinishedEvent());

    /**
     * Jar extension ClassLoader that will be properly refreshed.
     */
    @Inject
    private JarExtensionClassLoader jarExtensionClassLoader;

    /**
     * Extension initializer used to reinstall extensions in a new ClassLoader
     */
    @Inject
    private ExtensionInitializer extensionInitializer;


    @Override
    public String getName()
    {
        return "JarExtensionJobFinishedListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object o, Object o1)
    {
        JobFinishedEvent jobEvent = (JobFinishedEvent) event;
        Request request = jobEvent.getRequest();

        if (request instanceof UninstallRequest) {
            UninstallRequest uninstallRequest = (UninstallRequest) request;
            if (uninstallRequest.hasNamespaces()) {
                for (String namespace : uninstallRequest.getNamespaces()) {
                    jarExtensionClassLoader.dropURLClassLoader(namespace);
                    extensionInitializer.initialize(namespace,  "jar");
                }
            } else {
                jarExtensionClassLoader.dropURLClassLoaders();
                extensionInitializer.initialize(null, "jar");
            }
        }
    }
}
