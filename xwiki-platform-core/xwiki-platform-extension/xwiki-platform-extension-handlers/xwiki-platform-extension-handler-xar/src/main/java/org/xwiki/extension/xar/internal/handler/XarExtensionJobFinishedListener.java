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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishedEvent;
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
    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new JobFinishedEvent(InstallJob.JOBTYPE),
        new JobFinishedEvent(UninstallJob.JOBTYPE));

    @Inject
    private Execution execution;

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

    @Override
    public void onEvent(Event event, Object o, Object o1)
    {
        JobFinishedEvent jobFinishedEvent = (JobFinishedEvent) event;

        if (!jobFinishedEvent.getRequest().isRemote()) {
            ExecutionContext context = this.execution.getContext();

            if (context != null) {
                Map<String, Map<XarEntry, XarInstalledExtension>> previousXAREntries =
                    (Map<String, Map<XarEntry, XarInstalledExtension>>) context
                        .getProperty(XarExtensionHandler.CONTEXTKEY_PREVIOUSXARPAGES);
                Map<String, Map<XarEntry, LocalExtension>> nextXAREntries =
                    (Map<String, Map<XarEntry, LocalExtension>>) context
                        .getProperty(XarExtensionHandler.CONTEXTKEY_XARPAGES);

                if (nextXAREntries != null) {
                    Map<XarEntry, LocalExtension> rootNextPages = nextXAREntries.get(null);
                    if (rootNextPages == null) {
                        rootNextPages = Collections.emptyMap();
                    }

                    for (Map.Entry<String, Map<XarEntry, XarInstalledExtension>> entry : previousXAREntries.entrySet()) {
                        Map<XarEntry, XarInstalledExtension> previousPages = entry.getValue();
                        Map<XarEntry, LocalExtension> nextPages =
                            entry.getKey() != null ? nextXAREntries.get(entry.getKey()) : rootNextPages;
                        if (nextPages == null) {
                            nextPages = Collections.emptyMap();
                        }

                        Map<XarEntry, XarInstalledExtension> previousPagesToDelete =
                            new HashMap<XarEntry, XarInstalledExtension>(previousPages);
                        for (XarEntry previousPage : previousPages.keySet()) {
                            if (nextPages.containsKey(previousPage) || rootNextPages.containsKey(previousPage)) {
                                previousPagesToDelete.remove(previousPage);
                            }
                        }
                        previousXAREntries.put(entry.getKey(), previousPagesToDelete);
                    }

                    for (Map.Entry<String, Map<XarEntry, XarInstalledExtension>> previousWikiEntry : previousXAREntries
                        .entrySet()) {
                        if (!previousWikiEntry.getValue().isEmpty()) {
                            try {
                                this.packagerProvider.get().unimportPages(
                                    previousWikiEntry.getValue().keySet(),
                                    createPackageConfiguration(jobFinishedEvent.getRequest(),
                                        previousWikiEntry.getKey()));
                            } catch (Exception e) {
                                this.logger.warn(
                                    "Exception when cleaning pages removed since previous xar extension version", e);
                            }
                        }
                    }

                    context.setProperty(XarExtensionHandler.CONTEXTKEY_PREVIOUSXARPAGES, null);
                    context.setProperty(XarExtensionHandler.CONTEXTKEY_XARPAGES, null);
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
