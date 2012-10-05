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
package org.xwiki.extension.xar.internal.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Listen to job finished events to properly clean pages after upgrades.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Singleton
@Named("XarExtensionJobFinishedListener")
public class XarExtensionJobFinishedListener implements EventListener
{
    private static final class UpgradedExtensionCollection
    {
        public Map<String, Set<XarEntry>> previousEntries;

        public Map<String, Set<XarEntry>> newEntries;

        public void addPrevious(String namespace, Collection<XarEntry> previousEntries)
        {
            if (this.previousEntries == null) {
                this.previousEntries = new HashMap<String, Set<XarEntry>>();
            }

            Set<XarEntry> entries = this.previousEntries.get(namespace);
            if (entries == null) {
                entries = new HashSet<XarEntry>();
                this.previousEntries.put(namespace, entries);
            }

            entries.addAll(previousEntries);
        }

        public void addNew(String namespace, Collection<XarEntry> newEntries)
        {
            if (this.newEntries == null) {
                this.newEntries = new HashMap<String, Set<XarEntry>>();
            }

            Set<XarEntry> entries = this.newEntries.get(namespace);
            if (entries == null) {
                entries = new HashSet<XarEntry>();
                this.newEntries.put(namespace, entries);
            }

            entries.addAll(newEntries);
        }
    }

    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Arrays.asList(new ExtensionUpgradedEvent(), new JobStartedEvent(
        InstallJob.JOBTYPE), new JobFinishedEvent(InstallJob.JOBTYPE));

    @Inject
    private Execution execution;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private Provider<InstalledExtensionRepository> xarRepositoryProvider;

    @Inject
    private Provider<Packager> packagerProvider;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "XarExtensionJobFinishedListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    private void pushUpgradeLevel()
    {
        Stack<UpgradedExtensionCollection> extensions = getUpgradedExtensionCollectionStack(true);

        extensions.push(null);
    }

    private UpgradedExtensionCollection popUninstallLevel()
    {
        Stack<UpgradedExtensionCollection> extensions = getUpgradedExtensionCollectionStack(false);

        if (extensions != null) {
            return extensions.pop();
        }

        return null;
    }

    private Stack<UpgradedExtensionCollection> getUpgradedExtensionCollectionStack(boolean create)
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null) {
            Stack<UpgradedExtensionCollection> extensions =
                (Stack<UpgradedExtensionCollection>) context.getProperty("extension.xar.upgradedExtensions");

            if (extensions == null && create) {
                extensions = new Stack<UpgradedExtensionCollection>();
                context.setProperty("extension.xar.upgradedExtensions", extensions);
            }

            return extensions;
        }

        return null;
    }

    private UpgradedExtensionCollection getCurrentUpgradedExtensionCollection(boolean create)
    {
        Stack<UpgradedExtensionCollection> extensions = getUpgradedExtensionCollectionStack(false);

        if (extensions != null) {
            UpgradedExtensionCollection collection = extensions.peek();

            if (collection == null) {
                collection = new UpgradedExtensionCollection();
                extensions.set(extensions.size() - 1, collection);
            }

            return collection;
        }

        return null;
    }

    @Override
    public void onEvent(Event event, Object o, Object o1)
    {
        if (event instanceof ExtensionUpgradedEvent) {
            onExtensionUpgradedEvent((ExtensionUpgradedEvent) event, (InstalledExtension) o, (InstalledExtension) o1);
        } else if (event instanceof JobStartedEvent) {
            onJobStartedEvent(event);
        } else {
            onJobFinishedEvent((JobFinishedEvent) event);
        }
    }

    private List<XarEntry> getPages(InstalledExtension extension) throws IOException
    {
        List<XarEntry> pages;

        if (extension instanceof XarInstalledExtension) {
            pages = ((XarInstalledExtension) extension).getPages();
        } else {
            XarInstalledExtension xarExtension =
                (XarInstalledExtension) this.xarRepositoryProvider.get().getInstalledExtension(extension.getId());
            if (xarExtension == null) {
                pages = this.packagerProvider.get().getEntries(new File(extension.getFile().getAbsolutePath()));
            } else {
                pages = xarExtension.getPages();
            }
        }

        return pages;
    }

    private void onExtensionUpgradedEvent(ExtensionUpgradedEvent event, InstalledExtension newExtension,
        InstalledExtension previousExtension)
    {
        UpgradedExtensionCollection upgradedPages = getCurrentUpgradedExtensionCollection(true);

        try {
            upgradedPages.addPrevious(XarExtensionHandler.getWikiFromNamespace(event.getNamespace()),
                getPages(previousExtension));
        } catch (Exception e) {
            this.logger.error("Failed to get pages from xar extension [{}]", previousExtension.getId(), e);
        }

        try {
            upgradedPages
                .addNew(XarExtensionHandler.getWikiFromNamespace(event.getNamespace()), getPages(newExtension));
        } catch (Exception e) {
            this.logger.error("Failed to get pages from xar extension [{}]", newExtension.getId(), e);
        }
    }

    private void onJobStartedEvent(Event event)
    {
        pushUpgradeLevel();
    }

    private void onJobFinishedEvent(JobFinishedEvent event)
    {
        UpgradedExtensionCollection collection = popUninstallLevel();

        if (collection != null && collection.previousEntries != null) {
            if (collection.newEntries != null) {
                for (Map.Entry<String, Set<XarEntry>> newWikiEntry : collection.newEntries.entrySet()) {
                    Set<XarEntry> previousEntries = collection.previousEntries.get(newWikiEntry.getKey());
                    if (previousEntries != null) {
                        for (XarEntry newEntry : newWikiEntry.getValue()) {
                            previousEntries.remove(newEntry);
                        }
                    }
                }
            }

            for (Map.Entry<String, Set<XarEntry>> previousWikiEntry : collection.previousEntries.entrySet()) {
                try {
                    this.packagerProvider.get().unimportPages(previousWikiEntry.getValue(),
                        createPackageConfiguration(event.getRequest(), previousWikiEntry.getKey()));
                } catch (Exception e) {
                    this.logger.warn("Exception when cleaning pages removed since previous xar extension version", e);
                }
            }
        }
    }

    private PackageConfiguration createPackageConfiguration(Request request, String wiki)
    {
        DefaultPackageConfiguration configuration = new DefaultPackageConfiguration();

        configuration.setInteractive(false);
        configuration.setUser(XarExtensionHandler.getRequestUserReference(XarExtensionHandler.PROPERTY_USERREFERENCE,
            request));
        configuration.setWiki(wiki);
        configuration.setLogEnabled(true);

        return configuration;
    }
}
