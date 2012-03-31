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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Local repository proxy for XAR extensions.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named("xar")
public class XarInstalledExtensionRepository extends AbstractExtensionRepository implements
    InstalledExtensionRepository, Initializable
{
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private Packager packager;

    @Inject
    private ObservationManager observation;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    private Map<ExtensionId, XarInstalledExtension> extensions =
        new ConcurrentHashMap<ExtensionId, XarInstalledExtension>();

    @Override
    public void initialize() throws InitializationException
    {
        setId(new ExtensionRepositoryId("xar", "xar", this.installedRepository.getId().getURI()));

        loadExtensions();

        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object arg1, Object arg2)
            {
                LocalExtension extension = (LocalExtension) arg1;
                if (extension.getType().equals("xar")) {
                    updateXarExtension(extension);
                }

                if (arg2 != null) {
                    updateXarExtension((LocalExtension) arg2);
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
                } catch (IOException e) {
                    this.logger.error("Failed to parse extension [" + extension + "]", e);
                }
            }
        }
    }

    private void addXarExtension(InstalledExtension extension) throws IOException
    {
        XarInstalledExtension xarExtension = new XarInstalledExtension(extension, this, this.packager);

        this.extensions.put(extension.getId(), xarExtension);
    }

    private void removeXarExtension(ExtensionId extensionId)
    {
        this.extensions.remove(extensionId);
    }

    private void loadExtensions()
    {
        for (InstalledExtension localExtension : this.installedRepository.getInstalledExtensions()) {
            if (localExtension.getType().equalsIgnoreCase("xar")) {
                try {
                    addXarExtension(localExtension);
                } catch (IOException e) {
                    this.logger.error("Failed to parse extension [" + localExtension + "]", e);
                }
            }
        }
    }

    // ExtensionRepository

    @Override
    public InstalledExtension resolve(ExtensionId extensionId) throws ResolveException
    {
        InstalledExtension extension = this.extensions.get(extensionId);

        if (extension == null) {
            throw new ResolveException("Extension [" + extensionId + "] does not exists or is not a xar extension");
        }

        return extension;
    }

    @Override
    public InstalledExtension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionDependency);
        extension = this.extensions.get(extension.getId());

        if (extension == null) {
            throw new ResolveException("Extension [" + extensionDependency
                + "] does not exists or is not a xar extension");
        }

        return extension;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        return this.installedRepository.resolveVersions(id, offset, nb);
    }

    // LocalExtensionRepository

    @Override
    public int countExtensions()
    {
        return this.extensions.size();
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions(String namespace)
    {
        List<InstalledExtension> installedExtensions = new ArrayList<InstalledExtension>(extensions.size());
        for (InstalledExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled(namespace)) {
                installedExtensions.add(localExtension);
            }
        }

        return installedExtensions;
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions()
    {
        return (Collection<InstalledExtension>) this.extensions;
    }

    @Override
    public InstalledExtension getInstalledExtension(String id, String namespace)
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        if (extension.getType().equals("xar")) {
            extension = this.extensions.get(extension.getId());
        } else {
            extension = null;
        }

        return extension;
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency)
        throws InstallException
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

        return extension.getType().equals("xar") ? this.installedRepository.getBackwardDependencies(id, namespace)
            : null;
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionId);

        return extension.getType().equals("xar") ? this.installedRepository.getBackwardDependencies(extensionId) : null;
    }

    @Override
    public IterableResult<Extension> search(String pattern, int offset, int nb) throws SearchException
    {
        Pattern patternMatcher = Pattern.compile(".*" + pattern + ".*");

        List<Extension> result = new ArrayList<Extension>();

        for (XarInstalledExtension extension : this.extensions.values()) {
            if (patternMatcher.matcher(extension.getId().getId()).matches()
                || patternMatcher.matcher(extension.getDescription()).matches()
                || patternMatcher.matcher(extension.getSummary()).matches()
                || patternMatcher.matcher(extension.getName()).matches()
                || patternMatcher.matcher(extension.getFeatures().toString()).matches()) {
                result.add(extension);
            }
        }

        return new CollectionIterableResult<Extension>(this.extensions.size(), offset, result);
    }
}
