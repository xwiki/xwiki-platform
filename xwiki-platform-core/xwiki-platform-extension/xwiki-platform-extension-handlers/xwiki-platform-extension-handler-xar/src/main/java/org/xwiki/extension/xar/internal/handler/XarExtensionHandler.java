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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.PackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named("xar")
public class XarExtensionHandler extends AbstractExtensionHandler
{
    private static final String WIKI_NAMESPACEPREFIX = "wiki:";

    private static final String PROPERTY_USERREFERENCE = "user.reference";

    private static final String PROPERTY_CALLERREFERENCE = "caller.reference";

    private static final String PROPERTY_CHECKRIGHTS = "checkrights";

    /**
     * The full name (space.page) of the XWikiPreference page.
     */
    private static final String XWIKIPREFERENCES_FULLNAME = "XWiki.XWikiPreferences";

    /**
     * The identifier of the programming right.
     */
    private static final String RIGHTS_ADMIN = "admin";

    @Inject
    private Packager packager;

    @Inject
    @Named("xar")
    private InstalledExtensionRepository xarRepository;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    private String getWikiFromNamespace(String namespace) throws UnsupportedNamespaceException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new UnsupportedNamespaceException("Unsupported namespace [" + namespace
                    + "], only wiki:wikiid format is supported");
            }
        }

        return wiki;
    }

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void install(LocalExtension localExtension, String namespace, Request request) throws InstallException
    {
        // Only import XAR when it's a local order (otherwise it will be imported several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki;
            try {
                wiki = getWikiFromNamespace(namespace);
            } catch (UnsupportedNamespaceException e) {
                throw new InstallException("Failed to extract wiki id from namespace", e);
            }

            install(null, localExtension, wiki, request);
        }
    }

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace,
        Request request) throws InstallException
    {
        // Only import XAR when it's a local order (otherwise it will be imported several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki;
            try {
                wiki = getWikiFromNamespace(namespace);
            } catch (UnsupportedNamespaceException e) {
                throw new InstallException("Failed to extract wiki id from namespace", e);
            }

            XarInstalledExtension previousXarExtension;
            try {
                previousXarExtension =
                    (XarInstalledExtension) this.xarRepository.resolve(previousLocalExtension.getId());
            } catch (ResolveException e) {
                // Not supposed to be possible
                throw new InstallException("Failed to get xar extension [" + previousLocalExtension.getId()
                    + "] from xar repository", e);
            }

            // Install new pages
            install(previousXarExtension, newLocalExtension, wiki, request);

            // Uninstall old version pages not anymore in the new version
            Set<XarEntry> previousPages = new HashSet<XarEntry>(previousXarExtension.getPages());

            List<XarEntry> newPages;
            try {
                XarInstalledExtension newXarExtension =
                    (XarInstalledExtension) this.xarRepository.resolve(newLocalExtension.getId());
                newPages = newXarExtension.getPages();
            } catch (ResolveException e) {
                try {
                    newPages = this.packager.getEntries(new File(newLocalExtension.getFile().getAbsolutePath()));
                } catch (IOException e1) {
                    throw new InstallException("Failed to get xar extension [" + newLocalExtension.getId() + "] pages",
                        e);
                }
            }

            for (XarEntry entry : newPages) {
                previousPages.remove(entry);
            }

            try {
                this.packager.unimportPages(previousPages, createPackageConfiguration(request, wiki));
            } catch (Exception e) {
                this.logger.warn("Exception when cleaning pages removed since previous xar extension version", e);
            }
        }
    }

    private void install(XarInstalledExtension previousExtension, LocalExtension localExtension, String wiki,
        Request request) throws InstallException
    {
        // import xar into wiki (add new version when the page already exists)
        try {
            this.packager.importXAR(previousExtension != null ? new XarFile(new File(previousExtension.getFile()
                .getAbsolutePath()), previousExtension.getPages()) : null, new File(localExtension.getFile()
                .getAbsolutePath()), createPackageConfiguration(request, wiki));
        } catch (Exception e) {
            throw new InstallException("Failed to import xar for extension [" + localExtension + "]", e);
        }
    }

    @Override
    public void uninstall(LocalExtension localExtension, String namespace, Request request) throws UninstallException
    {
        // Only remove XAR when it's a local order (otherwise it will be deleted several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki;
            try {
                wiki = getWikiFromNamespace(namespace);
            } catch (UnsupportedNamespaceException e) {
                throw new UninstallException("Failed to extract wiki id from namespace", e);
            }

            // TODO: delete pages from the wiki which belong only to this extension (several extension could have some
            // common pages which will cause all sort of other issues but still could happen technically)

            // TODO: maybe remove only unmodified page ? At least ask for sure when question/answer system will be
            // implemented

            try {
                XarInstalledExtension xarLocalExtension =
                    (XarInstalledExtension) this.xarRepository.resolve(localExtension.getId());
                List<XarEntry> pages = xarLocalExtension.getPages();
                this.packager.unimportPages(pages, createPackageConfiguration(request, wiki));
            } catch (Exception e) {
                // Not supposed to be possible
                throw new UninstallException("Failed to get xar extension [" + localExtension.getId()
                    + "] from xar repository", e);
            }
        }
    }

    private PackageConfiguration createPackageConfiguration(Request request, String wiki)
    {
        DefaultPackageConfiguration configuration = new DefaultPackageConfiguration();

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        configuration.setMergeConfiguration(mergeConfiguration);

        configuration.setInteractive(request.isInteractive());
        configuration.setUser(getRequestUserReference(PROPERTY_USERREFERENCE, request));
        configuration.setWiki(wiki);
        configuration.setLogEnabled(true);

        try {
            Job currentJob = this.componentManager.<JobContext> getInstance(JobContext.class).getCurrentJob();
            if (currentJob != null) {
                configuration.setJobStatus(currentJob.getStatus());
            }
        } catch (Exception e) {
            this.logger.error("Failed to lookup JobContext, it will be impossible to do interactive install");
        }

        return configuration;
    }

    private DocumentReference getRequestUserReference(String property, Request request)
    {
        Object obj = request.getProperty(property);

        if (obj instanceof DocumentReference) {
            return (DocumentReference) obj;
        }

        return null;
    }

    private String getRequestUserString(String property, Request request)
    {
        String str = null;

        if (request.containsProperty(property)) {
            DocumentReference reference = getRequestUserReference(property, request);

            if (reference != null) {
                str = this.serializer.serialize(reference);
            } else {
                str = XWikiRightService.GUEST_USER_FULLNAME;
            }
        }

        return str;
    }

    // Check

    private boolean hasAccessLevel(String wiki, String right, String document, Request request) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        boolean hasAccess = true;

        String currentWiki = xcontext.getDatabase();
        try {
            xcontext.setDatabase(wiki != null ? wiki : xcontext.getMainXWiki());

            String caller = getRequestUserString(PROPERTY_CALLERREFERENCE, request);
            if (caller != null) {
                hasAccess = xcontext.getWiki().getRightService().hasAccessLevel(right, caller, document, xcontext);
            }

            if (hasAccess) {
                String user = getRequestUserString(PROPERTY_USERREFERENCE, request);
                if (user != null) {
                    hasAccess = xcontext.getWiki().getRightService().hasAccessLevel(right, user, document, xcontext);
                }
            }
        } finally {
            xcontext.setDatabase(currentWiki);
        }

        return hasAccess;
    }

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        String wiki;
        try {
            wiki = getWikiFromNamespace(namespace);
        } catch (UnsupportedNamespaceException e) {
            throw new InstallException("Failed to extract wiki id from namespace", e);
        }

        // TODO: check for edit right on each page of the extension ?

        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel(wiki, RIGHTS_ADMIN, XWIKIPREFERENCES_FULLNAME, request)) {
                    if (namespace == null) {
                        throw new InstallException(String.format("Admin right is required to install extension [%s]",
                            extension.getId()));
                    } else {
                        throw new InstallException(String.format(
                            "Admin right is required to install extension [%s] on namespace [%s]", extension.getId(),
                            namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new InstallException("Failed to check rights", e);
            }
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        String wiki;
        try {
            wiki = getWikiFromNamespace(namespace);
        } catch (UnsupportedNamespaceException e) {
            throw new UninstallException("Failed to extract wiki id from namespace", e);
        }

        // TODO: check for delete right on each page of the extension ?

        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel(wiki, RIGHTS_ADMIN, XWIKIPREFERENCES_FULLNAME, request)) {
                    if (namespace == null) {
                        throw new UninstallException(String.format(
                            "Admin right is required to uninstall extension [%s]", extension.getId()));
                    } else {
                        throw new UninstallException(String.format(
                            "Admin right is required to uninstall extension [%s] from namespace [%s]",
                            extension.getId(), namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new UninstallException("Failed to check rights", e);
            }
        }
    }
}
