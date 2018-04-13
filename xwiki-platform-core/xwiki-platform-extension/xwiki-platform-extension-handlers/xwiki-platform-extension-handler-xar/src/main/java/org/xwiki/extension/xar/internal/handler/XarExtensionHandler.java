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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.ConflictType;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.extension.xar.question.DefaultConflictActionQuestion;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named(XarExtensionHandler.TYPE)
public class XarExtensionHandler extends AbstractExtensionHandler
{
    public static final String TYPE = "xar";

    private static final TranslationMarker LOG_EXTENSIONPLAN_BEGIN =
        new TranslationMarker("extension.xar.log.extensionplan.begin");

    private static final TranslationMarker LOG_EXTENSIONPLAN_END =
        new TranslationMarker("extension.xar.log.extensionplan.end");

    private static final String CONTEXTKEY_PACKAGECONFIGURATION = "extension.xar.packageconfiguration";

    @Inject
    private Packager packager;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarRepository;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private LocalExtensionRepository localRepository;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to access the execution context.
     */
    @Inject
    private Execution execution;

    protected static DocumentReference getRequestUserReference(String property, Request request)
    {
        Object obj = request.getProperty(property);

        if (obj instanceof DocumentReference) {
            return (DocumentReference) obj;
        }

        return null;
    }

    private void initializePagesIndex(Request request) throws ExtensionException, XarException, IOException
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null && context.getProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN) == null) {
            ExtensionPlan plan = (ExtensionPlan) context.getProperty(AbstractExtensionJob.CONTEXTKEY_PLAN);

            if (plan != null) {
                if (request.isVerbose()) {
                    this.logger.info(LOG_EXTENSIONPLAN_BEGIN, "Preparing XAR extension plan");
                }

                context.setProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN,
                    new XarExtensionPlan(plan, this.xarRepository, this.localRepository));

                if (request.isVerbose()) {
                    this.logger.info(LOG_EXTENSIONPLAN_END, "XAR extension plan ready");
                }
            }
        }
    }

    private XarExtensionPlan getXARExtensionPlan()
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null) {
            return (XarExtensionPlan) context.getProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN);
        }

        return null;
    }

    @Override
    public void install(LocalExtension localExtension, String namespace, Request request) throws InstallException
    {
        // Only import XAR when it's a local order (otherwise it will be imported several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki;
            try {
                wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
            } catch (UnsupportedNamespaceException e) {
                throw new InstallException("Failed to extract wiki id from namespace", e);
            }

            installInternal(localExtension, wiki, request);
        }
    }

    @Override
    public void upgrade(Collection<InstalledExtension> previousLocalExtensions, LocalExtension newLocalExtension,
        String namespace, Request request) throws InstallException
    {
        // Only import XAR when it's a local order (otherwise it will be imported several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki;
            try {
                wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
            } catch (UnsupportedNamespaceException e) {
                throw new InstallException("Failed to extract wiki id from namespace", e);
            }

            // Install new pages
            installInternal(newLocalExtension, wiki, request);
        }
    }

    private void installInternal(LocalExtension newLocalExtension, String wiki, Request request) throws InstallException
    {
        try {
            initializePagesIndex(request);
            initJobPackageConfiguration(request, true);
        } catch (Exception e) {
            throw new InstallException("Failed to initialize extension plan index", e);
        }

        // import xar into wiki (add new version when the page already exists)
        PackageConfiguration configuration = createPackageConfiguration(newLocalExtension, request, wiki);
        try {
            this.packager.importXAR("Install extension [" + newLocalExtension + "]",
                new File(newLocalExtension.getFile().getAbsolutePath()), configuration);
        } catch (Exception e) {
            throw new InstallException("Failed to import xar for extension [" + newLocalExtension + "]", e);
        }
    }

    @Override
    public void uninstall(InstalledExtension installedExtension, String namespace, Request request)
        throws UninstallException
    {
        try {
            initializePagesIndex(request);
            initJobPackageConfiguration(request, false);
        } catch (Exception e) {
            throw new UninstallException("Failed to initialize extension plan index", e);
        }

        // Only remove XAR when it's a local order (otherwise it will be deleted several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            Job currentJob;
            try {
                currentJob = this.componentManager.<JobContext>getInstance(JobContext.class).getCurrentJob();
            } catch (ComponentLookupException e) {
                currentJob = null;
            }

            if (currentJob == null) {
                String wiki;
                try {
                    wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
                } catch (UnsupportedNamespaceException e) {
                    throw new UninstallException("Failed to extract wiki id from namespace", e);
                }

                PackageConfiguration configuration = createPackageConfiguration(null, request, wiki);

                try {
                    XarInstalledExtension xarLocalExtension =
                        (XarInstalledExtension) this.xarRepository.resolve(installedExtension.getId());
                    Collection<XarEntry> pages = xarLocalExtension.getXarPackage().getEntries();
                    this.packager.unimportPages(pages, configuration);
                } catch (Exception e) {
                    // Not supposed to be possible
                    throw new UninstallException(
                        "Failed to get xar extension [" + installedExtension.getId() + "] from xar repository", e);
                }
            } else {
                // The actual delete of pages is done in XarExtensionJobFinishedListener
            }
        }
    }

    private void initJobPackageConfiguration(Request request, boolean defaultConflict) throws InterruptedException
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null && context.getProperty(CONTEXTKEY_PACKAGECONFIGURATION) == null) {
            Job currentJob = null;
            try {
                currentJob = this.componentManager.<JobContext>getInstance(JobContext.class).getCurrentJob();
            } catch (Exception e) {
                this.logger.error("Failed to lookup JobContext, it will be impossible to do interactive install");
            }

            if (currentJob != null) {
                PackageConfiguration configuration = new PackageConfiguration();
                context.setProperty(CONTEXTKEY_PACKAGECONFIGURATION, configuration);

                DocumentReference userReference =
                    getRequestUserReference(AbstractExtensionValidator.PROPERTY_USERREFERENCE, request);

                configuration.setInteractive(request.isInteractive());
                configuration.setUser(userReference);
                configuration.setVerbose(request.isVerbose());
                configuration.setSkipMandatorytDocuments(true);
                configuration.setXarExtensionPlan(getXARExtensionPlan());

                configuration.setJobStatus(currentJob.getStatus());

                // Non blocker conflicts
                configuration.setConflictAction(ConflictType.CURRENT_DELETED,
                    request.getProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_CURRENT_DELETED),
                    GlobalAction.CURRENT);
                configuration.setConflictAction(ConflictType.MERGE_SUCCESS,
                    request.getProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_MERGE_SUCCESS),
                    GlobalAction.MERGED);
                // Blocker conflicts
                configuration.setConflictAction(ConflictType.CURRENT_EXIST,
                    request.getProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_CURRENT_EXIST),
                    configuration.isInteractive() ? GlobalAction.ASK : GlobalAction.NEXT);
                configuration.setConflictAction(ConflictType.MERGE_FAILURE,
                    request.getProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_MERGE_FAILURE),
                    configuration.isInteractive() ? GlobalAction.ASK : GlobalAction.MERGED);

                // If user asked to be asked about conflict behavior
                if (defaultConflict && currentJob.getStatus().getRequest().isInteractive()) {
                    XWikiContext xcontext = xcontextProvider.get();
                    // Make sure the context has the right user
                    xcontext.setUserReference(userReference);
                    int extensionConflictSetup =
                        NumberUtils.toInt(xcontext.getWiki().getUserPreference("extensionConflictSetup", xcontext), 0);

                    if (extensionConflictSetup == 1) {
                        DefaultConflictActionQuestion question = new DefaultConflictActionQuestion(configuration);

                        currentJob.getStatus().ask(question, 1, TimeUnit.HOURS);
                    }
                }
            }
        }
    }

    private PackageConfiguration createPackageConfiguration(LocalExtension extension, Request request, String wiki)
    {
        PackageConfiguration configuration;

        // Search job configuration in the context
        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            configuration = (PackageConfiguration) context.getProperty(CONTEXTKEY_PACKAGECONFIGURATION);
        } else {
            configuration = null;
        }

        // Create a configuration for this extension
        if (configuration != null) {
            configuration = configuration.clone();
        } else {
            configuration = new PackageConfiguration();

            DocumentReference userReference =
                getRequestUserReference(AbstractExtensionValidator.PROPERTY_USERREFERENCE, request);

            configuration.setInteractive(request.isInteractive());
            configuration.setUser(userReference);
            configuration.setVerbose(request.isVerbose());
            configuration.setSkipMandatorytDocuments(true);
        }

        configuration.setWiki(wiki);

        // Filter entries to import if there is a plan
        if (extension != null && configuration.getXarExtensionPlan() != null) {
            Map<String, Map<XarEntry, LocalExtension>> nextXAREntries =
                configuration.getXarExtensionPlan().nextXAREntries;

            Set<String> entriesToImport = new HashSet<>();

            Map<XarEntry, LocalExtension> nextXAREntriesOnRoot = nextXAREntries.get(null);
            if (nextXAREntriesOnRoot != null) {
                for (Map.Entry<XarEntry, LocalExtension> entry : nextXAREntriesOnRoot.entrySet()) {
                    if (entry.getValue() == extension) {
                        entriesToImport.add(entry.getKey().getEntryName());
                    }
                }
            }
            Map<XarEntry, LocalExtension> nextXAREntriesOnWiki = nextXAREntries.get(wiki);
            if (nextXAREntriesOnWiki != null) {
                for (Map.Entry<XarEntry, LocalExtension> entry : nextXAREntriesOnWiki.entrySet()) {
                    if (entry.getValue() == extension) {
                        entriesToImport.add(entry.getKey().getEntryName());
                    }
                }
            }

            configuration.setEntriesToImport(entriesToImport);
        }

        return configuration;
    }
}
