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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

@Component
@Singleton
@Named("JarHandlerApplicationStartedListener")
public class JarHandlerApplicationStartedListener implements EventListener
{
    private static final List<Event> EVENTS = Collections.<Event> singletonList(new ApplicationStartedEvent());

    @Inject
    private LocalExtensionRepository localExtensionRepository;

    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    public List<Event> getEvents()
    {
        return EVENTS;
    }

    public String getName()
    {
        return "JarHandlerApplicationStartedListener";
    }

    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        // Load extensions from local repository
        Collection<LocalExtension> localExtensions = this.localExtensionRepository.getLocalExtensions();
        for (LocalExtension localExtension : localExtensions) {
            if (localExtension.getType().equals("jar")) {
                if (localExtension.isInstalled()) {
                    try {
                        if (localExtension.getNamespaces() != null) {
                            for (String wiki : localExtension.getNamespaces()) {
                                this.extensionHandlerManager.install(localExtension, wiki);
                            }
                        } else {
                            this.extensionHandlerManager.install(localExtension, null);
                        }
                    } catch (Exception e) {
                        this.logger.error("Failed to install local extension [" + localExtension + "]", e);
                    }
                }
            }
        }
    }
}
