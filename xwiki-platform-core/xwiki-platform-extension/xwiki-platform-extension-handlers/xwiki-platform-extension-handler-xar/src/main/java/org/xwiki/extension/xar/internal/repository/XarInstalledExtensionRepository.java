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
package org.xwiki.extension.xar.internal.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.installed.AbstractInstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.xar.XarException;

/**
 * Local repository proxy for XAR extensions.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named(XarExtensionHandler.TYPE)
public class XarInstalledExtensionRepository extends AbstractInstalledExtensionRepository<XarInstalledExtension>
    implements InstalledExtensionRepository, Initializable
{
    private static final List<Event> EVENTS = Arrays.<Event>asList(new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient ObservationManager observation;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultExtensionRepositoryDescriptor(XarExtensionHandler.TYPE, XarExtensionHandler.TYPE,
            this.installedRepository.getDescriptor().getURI()));

        loadExtensions();

        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                LocalExtension extension = (LocalExtension) source;
                if (extension.getType().equals(XarExtensionHandler.TYPE)) {
                    updateXarExtension(extension);

                    if (data != null) {
                        for (InstalledExtension installedExtension : (Collection<InstalledExtension>) data) {
                            updateXarExtension(installedExtension);
                        }
                    }
                }
            }

            @Override
            public String getName()
            {
                return XarInstalledExtensionRepository.class.getName();
            }

            @Override
            public List<Event> getEvents()
            {
                return EVENTS;
            }
        });
    }

    private void updateXarExtension(LocalExtension extension)
    {
        if (this.extensions.containsKey(extension.getId())) {
            if (!(extension instanceof InstalledExtension)) {
                removeXarExtension(extension.getId());
            }
        } else {
            if (extension instanceof InstalledExtension) {
                try {
                    addXarExtension((InstalledExtension) extension);
                } catch (Exception e) {
                    this.logger.error("Failed to parse extension [" + extension + "]", e);
                }
            }
        }
    }

    private void addXarExtension(InstalledExtension extension) throws IOException, XarException
    {
        XarInstalledExtension xarExtension = new XarInstalledExtension(extension, this);

        addCachedExtension(xarExtension);
    }

    private void removeXarExtension(ExtensionId extensionId)
    {
        removeCachedExtension(this.extensions.get(extensionId));
    }

    private void loadExtensions()
    {
        for (InstalledExtension localExtension : this.installedRepository.getInstalledExtensions()) {
            if (localExtension.getType().equalsIgnoreCase(XarExtensionHandler.TYPE)) {
                try {
                    addXarExtension(localExtension);
                } catch (Exception e) {
                    this.logger.error("Failed to parse extension [" + localExtension + "]", e);
                }
            }
        }
    }

    // InstalledExtensionRepository

    @Override
    public InstalledExtension getInstalledExtension(String id, String namespace)
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        if (extension != null) {
            if (extension.getType().equals(XarExtensionHandler.TYPE)) {
                extension = this.extensions.get(extension.getId());
            } else {
                extension = null;
            }
        }

        return extension;
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency,
        Map<String, Object> properties) throws InstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void uninstallExtension(InstalledExtension extension, String namespace) throws UninstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String id, String namespace) throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        return extension.getType().equals(XarExtensionHandler.TYPE) ? this.installedRepository.getBackwardDependencies(
            id, namespace) : null;
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionId);

        return extension.getType().equals(XarExtensionHandler.TYPE) ? this.installedRepository
            .getBackwardDependencies(extensionId) : null;
    }
}
