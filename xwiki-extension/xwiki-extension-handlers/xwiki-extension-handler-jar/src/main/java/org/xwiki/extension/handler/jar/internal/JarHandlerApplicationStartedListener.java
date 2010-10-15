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
package org.xwiki.extension.handler.jar.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

@Component("ExtensionManagerApplicationStarted")
public class JarHandlerApplicationStartedListener extends AbstractLogEnabled implements EventListener
{
    private static final List<Event> EVENTS = Collections.<Event> singletonList(new ApplicationStartedEvent());

    @Requirement
    private CoreExtensionRepository coreExtensionRepository;

    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    @Requirement
    private ExtensionHandlerManager extensionHandlerManager;

    public List<Event> getEvents()
    {
        return EVENTS;
    }

    public String getName()
    {
        return "ExtensionManagerApplicationStarted";
    }

    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        Map<String, Boolean> loadedExtension = new HashMap<String, Boolean>();

        // Load extensions from local repository
        List<LocalExtension> localExtensions = this.localExtensionRepository.getLocalExtensions();
        for (LocalExtension localExtension : localExtensions) {
            if (localExtension.getType().equals("jar")) {
                try {
                    loadExtension(localExtension, loadedExtension);
                } catch (Exception e) {
                    getLogger().error("Failed to install local extension [" + localExtension + "]", e);
                }
            }
        }
    }

    private void loadExtension(LocalExtension localExtension, Map<String, Boolean> loadedExtension)
        throws InstallException
    {
        Boolean loaded = loadedExtension.get(localExtension.getId());

        if (loaded == null) {
            for (ExtensionDependency dependency : localExtension.getDependencies()) {
                try {
                    loadExtension(dependency.getId(), loadedExtension);
                } catch (InstallException e) {
                    loadedExtension.put(localExtension.getId(), false);
                    throw new InstallException("Failed to load extension [" + localExtension
                        + "]: impossible to load dependency [" + dependency.getId() + "]", e);
                }
            }
        } else if (!loaded) {
            throw new InstallException("Extension [" + localExtension + "] has not been loaded");
        }

        if (loadedExtension.containsKey(localExtension)) {
            this.extensionHandlerManager.install(localExtension);
        }

        loadedExtension.put(localExtension.getId(), true);
    }

    private void loadExtension(String localExtensionId, Map<String, Boolean> loadedExtension) throws InstallException
    {
        LocalExtension localExtension = this.localExtensionRepository.getLocalExtension(localExtensionId);

        if (localExtension == null) {
            if (this.coreExtensionRepository.exists(localExtensionId)) {
                loadedExtension.put(localExtensionId, true);
            } else {
                loadedExtension.put(localExtensionId, false);
                throw new InstallException("Failed to load extension [" + localExtensionId + "]: does not exists");
            }
        }
    }
}
