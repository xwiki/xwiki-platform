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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
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

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "JarHandlerApplicationStartedListener";
    }

    private void loadJarExtension(LocalExtension localExtension, Map<String, Set<LocalExtension>> loadedExtensions)
        throws InstallException
    {
        if (localExtension.getNamespaces() != null) {
            for (String namespace : localExtension.getNamespaces()) {
                loadJarExtension(localExtension, namespace, loadedExtensions);
            }
        } else {
            loadJarExtension(localExtension, null, loadedExtensions);
        }
    }

    private void loadJarExtension(LocalExtension localExtension, String namespace,
        Map<String, Set<LocalExtension>> loadedExtensions) throws InstallException
    {
        Set<LocalExtension> loadedExtensionsInNamespace = loadedExtensions.get(namespace);

        if (loadedExtensionsInNamespace == null) {
            loadedExtensionsInNamespace = new HashSet<LocalExtension>();
            loadedExtensions.put(namespace, loadedExtensionsInNamespace);
        }

        if (!loadedExtensionsInNamespace.contains(localExtension)) {
            for (ExtensionDependency dependency : localExtension.getDependencies()) {
                if (!this.coreExtensionRepository.exists(dependency.getId())) {
                    LocalExtension dependencyExtension =
                        this.localExtensionRepository.getInstalledExtension(dependency.getId(), namespace);
                    loadJarExtension(dependencyExtension, namespace, loadedExtensions);
                }
            }

            this.extensionHandlerManager.install(localExtension, namespace);

            loadedExtensionsInNamespace.add(localExtension);
        }
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        Map<String, Set<LocalExtension>> loadedExtensions = new HashMap<String, Set<LocalExtension>>();

        // Load extensions from local repository
        Collection<LocalExtension> localExtensions = this.localExtensionRepository.getInstalledExtensions();
        for (LocalExtension localExtension : localExtensions) {
            if (localExtension.getType().equals("jar")) {
                try {
                    loadJarExtension(localExtension, loadedExtensions);
                } catch (Exception e) {
                    this.logger.error("Failed to install local extension [" + localExtension + "]", e);
                }
            }
        }
    }
}
