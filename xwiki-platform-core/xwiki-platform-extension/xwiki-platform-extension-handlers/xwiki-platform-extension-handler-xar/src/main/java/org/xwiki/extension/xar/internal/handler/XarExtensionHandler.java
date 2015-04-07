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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;

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

    protected static final String PROPERTY_USERREFERENCE = "user.reference";

    protected static final String PROPERTY_CALLERREFERENCE = "caller.reference";

    protected static final String PROPERTY_CHECKRIGHTS = "checkrights";

    private static final TranslationMarker LOG_EXTENSIONPLAN_BEGIN = new TranslationMarker(
        "extension.xar.log.extensionplan.begin");

    private static final TranslationMarker LOG_EXTENSIONPLAN_END = new TranslationMarker(
        "extension.xar.log.extensionplan.end");

    @Inject
    private Packager packager;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarRepository;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private LocalExtensionRepository localReposirory;

    @Inject
    private AuthorizationManager authorization;

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

        if (context != null) {
            ExtensionPlan plan = (ExtensionPlan) context.getProperty(AbstractExtensionJob.CONTEXTKEY_PLAN);

            if (plan != null) {
                if (context.getProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN) == null) {
                    if (request.isVerbose()) {
                        this.logger.info(LOG_EXTENSIONPLAN_BEGIN, "Preparing XAR extension plan");
                    }

                    context.setProperty(XarExtensionPlan.CONTEXTKEY_XARINSTALLPLAN, new XarExtensionPlan(plan,
                        this.xarRepository, this.localReposirory));

                    if (request.isVerbose()) {
                        this.logger.info(LOG_EXTENSIONPLAN_END, "XAR extension plan ready");
                    }
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

    private void installInternal(LocalExtension newLocalExtension, String wiki, Request request)
        throws InstallException
    {
        try {
            initializePagesIndex(request);
        } catch (Exception e) {
            throw new InstallException("Failed to initialize extension plan index", e);
        }

        // import xar into wiki (add new version when the page already exists)
        PackageConfiguration configuration =
            createPackageConfiguration(newLocalExtension, request, wiki, getXARExtensionPlan());
        try {
            this.packager.importXAR("Install extension [" + newLocalExtension + "]", new File(newLocalExtension
                .getFile().getAbsolutePath()), configuration);
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
        } catch (Exception e) {
            throw new UninstallException("Failed to initialize extension plan index", e);
        }

        // Only remove XAR when it's a local order (otherwise it will be deleted several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            Job currentJob;
            try {
                currentJob = this.componentManager.<JobContext>getInstance(JobContext.class).getCurrentJob();
            } catch (ComponentLookupException e1) {
                currentJob = null;
            }

            if (currentJob == null) {
                String wiki;
                try {
                    wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
                } catch (UnsupportedNamespaceException e) {
                    throw new UninstallException("Failed to extract wiki id from namespace", e);
                }

                // TODO: delete pages from the wiki which belong only to this extension (several extension could have
                // some common pages which will cause all sort of other issues but still could happen technically)

                // TODO: maybe remove only unmodified page ? At least ask for sure when question/answer system will be
                // implemented

                PackageConfiguration configuration =
                    createPackageConfiguration(null, request, wiki, getXARExtensionPlan());
                try {
                    XarInstalledExtension xarLocalExtension =
                        (XarInstalledExtension) this.xarRepository.resolve(installedExtension.getId());
                    Collection<XarEntry> pages = xarLocalExtension.getXarPackage().getEntries();
                    this.packager.unimportPages(pages, configuration);
                } catch (Exception e) {
                    // Not supposed to be possible
                    throw new UninstallException("Failed to get xar extension [" + installedExtension.getId()
                        + "] from xar repository", e);
                }
            } else {
                // The actual delete of pages is done in XarExtensionJobFinishedListener
            }
        }
    }

    private PackageConfiguration createPackageConfiguration(LocalExtension extension, Request request, String wiki,
        XarExtensionPlan xarExtensionPlan)
    {
        PackageConfiguration configuration = new PackageConfiguration();

        configuration.setInteractive(request.isInteractive());
        configuration.setUser(getRequestUserReference(PROPERTY_USERREFERENCE, request));
        configuration.setWiki(wiki);
        configuration.setVerbose(request.isVerbose());
        configuration.setSkipMandatorytDocuments(true);
        configuration.setXarExtensionPlan(xarExtensionPlan);

        try {
            Job currentJob = this.componentManager.<JobContext>getInstance(JobContext.class).getCurrentJob();
            if (currentJob != null) {
                configuration.setJobStatus(currentJob.getStatus());
            }
        } catch (Exception e) {
            this.logger.error("Failed to lookup JobContext, it will be impossible to do interactive install");
        }

        // Filter entries to import if there is a plan
        if (extension != null && xarExtensionPlan != null) {
            Map<String, Map<XarEntry, LocalExtension>> nextXAREntries = xarExtensionPlan.nextXAREntries;

            Set<String> entriesToImport = new HashSet<String>();

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

    // Check

    private boolean hasAccessLevel(String wiki, Right right, Request request)
    {
        boolean hasAccess = true;

        WikiReference wikiReference = wiki != null ? new WikiReference(wiki) : null;

        if (request.getProperty(PROPERTY_CALLERREFERENCE) != null) {
            hasAccess =
                this.authorization.hasAccess(right, getRequestUserReference(PROPERTY_CALLERREFERENCE, request),
                    wikiReference);
        }

        if (hasAccess) {
            DocumentReference user = getRequestUserReference(PROPERTY_USERREFERENCE, request);
            if (user != null) {
                hasAccess = this.authorization.hasAccess(right, user, wikiReference);
            }
        }

        return hasAccess;
    }

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        String wiki;
        try {
            wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
        } catch (UnsupportedNamespaceException e) {
            throw new InstallException("Failed to extract wiki id from namespace", e);
        }

        // TODO: check for edit right on each page of the extension ?

        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            if (!hasAccessLevel(wiki, Right.ADMIN, request)) {
                if (namespace == null) {
                    throw new InstallException(String.format("Admin right is required to install extension [%s]",
                        extension.getId()));
                } else {
                    throw new InstallException(String.format(
                        "Admin right is required to install extension [%s] on namespace [%s]", extension.getId(),
                        namespace));
                }
            }
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        String wiki;
        try {
            wiki = XarHandlerUtils.getWikiFromNamespace(namespace);
        } catch (UnsupportedNamespaceException e) {
            throw new UninstallException("Failed to extract wiki id from namespace", e);
        }

        // TODO: check for delete right on each page of the extension ?

        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            if (!hasAccessLevel(wiki, Right.ADMIN, request)) {
                if (namespace == null) {
                    throw new UninstallException(String.format("Admin right is required to uninstall extension [%s]",
                        extension.getId()));
                } else {
                    throw new UninstallException(String.format(
                        "Admin right is required to uninstall extension [%s] from namespace [%s]", extension.getId(),
                        namespace));
                }
            }
        }
    }
}
