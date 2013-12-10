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

import java.io.IOException;
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
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wikistream.xar.internal.XarEntry;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private Provider<XWikiContext> xcontextProvider;

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
                XarExtensionPlan xarExtensionPlan =
                    (XarExtensionPlan) context.getProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN);

                if (xarExtensionPlan != null) {
                    try {
                        Map<String, Map<XarEntry, XarExtensionPlanEntry>> previousXAREntries =
                            xarExtensionPlan.previousXAREntries;
                        Map<String, Map<XarEntry, LocalExtension>> nextXAREntries = xarExtensionPlan.nextXAREntries;

                        Map<XarEntry, LocalExtension> rootNextPages = nextXAREntries.get(null);
                        if (rootNextPages == null) {
                            rootNextPages = Collections.emptyMap();
                        }

                        XWikiContext xcontext = this.xcontextProvider.get();

                        Packager packager = this.packagerProvider.get();

                        for (Map.Entry<String, Map<XarEntry, XarExtensionPlanEntry>> wikiEntry : previousXAREntries
                            .entrySet()) {
                            String wiki = wikiEntry.getKey();
                            WikiReference wikiReference = wiki != null ? new WikiReference(wiki) : null;
                            Map<XarEntry, XarExtensionPlanEntry> previousPages = wikiEntry.getValue();
                            Map<XarEntry, LocalExtension> nextPages =
                                wiki != null ? nextXAREntries.get(wiki) : rootNextPages;
                            if (nextPages == null) {
                                nextPages = Collections.emptyMap();
                            }

                            Map<XarEntry, XarExtensionPlanEntry> previousPagesToDelete =
                                new HashMap<XarEntry, XarExtensionPlanEntry>(previousPages);
                            for (Map.Entry<XarEntry, XarExtensionPlanEntry> pageEntry : previousPages.entrySet()) {
                                XarEntry previousPage = pageEntry.getKey();
                                XarExtensionPlanEntry xarPlanEntry = pageEntry.getValue();

                                if (nextPages.containsKey(previousPage) || rootNextPages.containsKey(previousPage)) {
                                    previousPagesToDelete.remove(previousPage);
                                } else {
                                    DocumentReference documentReference =
                                        new DocumentReference(previousPage.getReference(), wikiReference);
                                    XWikiDocument currentDocument;
                                    try {
                                        currentDocument = xcontext.getWiki().getDocument(documentReference, xcontext);
                                    } catch (Exception e) {
                                        this.logger.error("Failed to get document [{}]", documentReference, e);
                                        // Lets be safe and skip removing that page
                                        previousPagesToDelete.remove(previousPage);
                                        continue;
                                    }

                                    if (currentDocument.isNew()) {
                                        // Current already removed
                                        previousPagesToDelete.remove(previousPage);
                                        continue;
                                    }

                                    XWikiDocument previousDocument;
                                    try {
                                        previousDocument =
                                            packager.getXWikiDocument(wikiReference, previousPage.getReference(),
                                                xarPlanEntry.xarFile);
                                    } catch (Exception e) {
                                        this.logger.error("Failed to get document [{}] from XAR [{}]",
                                            documentReference, xarPlanEntry.xarFile, e);
                                        // Lets be safe and skip removing that page
                                        previousPagesToDelete.remove(previousPage);
                                        continue;
                                    }

                                    try {
                                        currentDocument.loadAttachments(xcontext);
                                        // XXX: conflict between current and new
                                        if (!currentDocument.equalsData(previousDocument)) {
                                            previousPagesToDelete.remove(previousPage);
                                        }
                                    } catch (Exception e) {
                                        this.logger.error("Failed to load attachments", e);
                                        // Lets be safe and skip removing that page
                                        previousPagesToDelete.remove(previousPage);
                                        continue;
                                    }
                                }
                            }
                            previousXAREntries.put(wikiEntry.getKey(), previousPagesToDelete);
                        }

                        for (Map.Entry<String, Map<XarEntry, XarExtensionPlanEntry>> previousWikiEntry : previousXAREntries
                            .entrySet()) {
                            if (!previousWikiEntry.getValue().isEmpty()) {
                                try {
                                    packager.unimportPages(
                                        previousWikiEntry.getValue().keySet(),
                                        createPackageConfiguration(jobFinishedEvent.getRequest(),
                                            previousWikiEntry.getKey()));
                                } catch (Exception e) {
                                    this.logger
                                        .warn(
                                            "Exception when cleaning pages removed since previous xar extension version",
                                            e);
                                }
                            }
                        }
                    } finally {
                        // Cleanup extension plan
                        try {
                            xarExtensionPlan.close();
                        } catch (IOException e) {
                            this.logger.error("Failed to close XAR extension plan", e);
                        }
                        context.setProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN, null);
                    }
                }
            }
        }
    }

    private PackageConfiguration createPackageConfiguration(Request request, String wiki)
    {
        PackageConfiguration configuration = new PackageConfiguration();

        configuration.setInteractive(false);
        configuration.setUser(XarExtensionHandler.getRequestUserReference(XarExtensionHandler.PROPERTY_USERREFERENCE,
            request));
        configuration.setWiki(wiki);
        configuration.setLogEnabled(true);
        configuration.setSkipMandatorytDocuments(true);

        return configuration;
    }
}
