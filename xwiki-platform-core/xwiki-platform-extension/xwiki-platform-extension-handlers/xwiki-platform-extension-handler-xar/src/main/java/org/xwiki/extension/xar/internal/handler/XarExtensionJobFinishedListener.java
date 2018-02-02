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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.question.CleanPagesQuestion;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.xar.XarEntry;

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
    private static final List<Event> EVENTS =
        Arrays.<Event>asList(new JobFinishingEvent(InstallJob.JOBTYPE), new JobFinishingEvent(UninstallJob.JOBTYPE));

    @Inject
    private Execution execution;

    @Inject
    private Provider<Packager> packagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarRepository;

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
    public void onEvent(Event event, Object source, Object data)
    {
        JobFinishingEvent jobFinishingEvent = (JobFinishingEvent) event;

        if (!jobFinishingEvent.getRequest().isRemote()) {
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

                        // Get pages to delete

                        Set<DocumentReference> pagesToDelete = new HashSet<DocumentReference>();

                        for (Map.Entry<String, Map<XarEntry, XarExtensionPlanEntry>> previousWikiEntry : previousXAREntries
                            .entrySet()) {
                            if (!previousWikiEntry.getValue().isEmpty()) {
                                try {
                                    List<DocumentReference> references =
                                        packager.getDocumentReferences(previousWikiEntry.getValue().keySet(),
                                            createPackageConfiguration(jobFinishingEvent.getRequest(),
                                                previousWikiEntry.getKey()));

                                    for (DocumentReference reference : references) {
                                        // Ignore document that are part of other installed extensions (don't even
                                        // propose to enable them)
                                        if (((XarInstalledExtensionRepository) this.xarRepository)
                                            .getXarInstalledExtensions(reference).isEmpty()) {
                                            pagesToDelete.add(reference);
                                        }
                                    }
                                } catch (Exception e) {
                                    this.logger.warn(
                                        "Exception when cleaning pages removed since previous xar extension version",
                                        e);
                                }
                            }
                        }

                        // Create cleanup question

                        CleanPagesQuestion question = new CleanPagesQuestion(pagesToDelete);

                        Map<DocumentReference, Boolean> pages = question.getPages();

                        // Remove pages which are in the next XAR packages
                        for (DocumentReference previousReference : pagesToDelete) {
                            if (xarExtensionPlan.containsNewPage(previousReference)) {
                                pages.remove(previousReference);
                            }
                        }

                        // Deal with conflicts before sending the question

                        for (Map.Entry<DocumentReference, Boolean> entry : pages.entrySet()) {
                            DocumentReference reference = entry.getKey();

                            // Get current
                            XWikiDocument currentDocument;
                            try {
                                currentDocument = xcontext.getWiki().getDocument(reference, xcontext);
                            } catch (Exception e) {
                                this.logger.error("Failed to get document [{}]", reference, e);
                                // Lets be safe and skip removing that page
                                pages.put(reference, false);
                                continue;
                            }
                            if (currentDocument.isNew()) {
                                // Current already removed
                                pages.put(reference, false);
                                continue;
                            }

                            // Get previous
                            XWikiDocument previousDocument;
                            try {
                                previousDocument = xarExtensionPlan.getPreviousXWikiDocument(reference, packager);
                            } catch (Exception e) {
                                this.logger.error("Failed to get previous version of document [{}]", reference, e);
                                // Lets be safe and skip removing that page
                                pages.put(reference, false);
                                continue;
                            }

                            // Compare previous and current
                            try {
                                currentDocument.loadAttachmentsContentSafe(xcontext);
                                if (!currentDocument.equalsData(previousDocument)) {
                                    // conflict between current and new
                                    pages.put(reference, false);
                                }
                            } catch (Exception e) {
                                this.logger.error("Failed to load attachments", e);
                                // Lets be safe and skip removing that page
                                pages.put(reference, false);
                                continue;
                            }
                        }

                        // Ask confirmation
                        if (!pages.isEmpty() && jobFinishingEvent.getRequest().isInteractive()) {
                            try {
                                ((Job) source).getStatus().ask(question);
                            } catch (InterruptedException e) {
                                this.logger.warn("The thread has been interrupted", e);

                                // The thread has been interrupted, do nothing
                                return;
                            }
                        }

                        // Delete pages

                        PackageConfiguration configuration = createPackageConfiguration(jobFinishingEvent.getRequest());

                        for (Map.Entry<DocumentReference, Boolean> entry : pages.entrySet()) {
                            if (entry.getValue()) {
                                packager.deleteDocument(entry.getKey(), configuration);
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

    private PackageConfiguration createPackageConfiguration(Request request)
    {
        return createPackageConfiguration(request, null);
    }

    private PackageConfiguration createPackageConfiguration(Request request, String wiki)
    {
        PackageConfiguration configuration = new PackageConfiguration();

        configuration.setInteractive(false);
        configuration.setUser(
            XarExtensionHandler.getRequestUserReference(AbstractExtensionValidator.PROPERTY_USERREFERENCE, request));
        configuration.setWiki(wiki);
        configuration.setVerbose(request.isVerbose());
        configuration.setSkipMandatorytDocuments(true);

        return configuration;
    }
}
