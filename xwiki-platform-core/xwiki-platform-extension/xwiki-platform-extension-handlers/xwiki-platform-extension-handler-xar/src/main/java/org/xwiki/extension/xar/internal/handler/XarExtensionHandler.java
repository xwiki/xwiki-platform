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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.InstallException;
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

import com.xpn.xwiki.doc.merge.MergeConfiguration;

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

    @Inject
    private Packager packager;

    @Inject
    @Named("xar")
    private InstalledExtensionRepository xarRepository;

    @Inject
    private ComponentManager componentManager;

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void install(LocalExtension localExtension, String namespace, Request request) throws InstallException
    {
        // Only import XAR when it's a local order (otherwise it will be imported several times and the wiki will
        // probably not be in an expected state)
        if (!request.isRemote()) {
            String wiki = namespace;

            if (wiki != null) {
                if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                    wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
                } else {
                    throw new InstallException("Unsupported namespace [" + namespace + "], only "
                        + WIKI_NAMESPACEPREFIX + "wikiid format is supported");
                }
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
            String wiki = namespace;

            if (wiki != null) {
                if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                    wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
                } else {
                    throw new InstallException("Unsupported namespace [" + namespace + "], only "
                        + WIKI_NAMESPACEPREFIX + "wikiid format is supported");
                }
            }

            XarInstalledExtension previousXarExtension;
            try {
                previousXarExtension = (XarInstalledExtension) this.xarRepository.resolve(previousLocalExtension.getId());
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
            String wiki = namespace;

            if (wiki != null) {
                if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                    wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
                } else {
                    throw new UninstallException("Unsupported namespace [" + namespace + "], only "
                        + WIKI_NAMESPACEPREFIX + "wikiid format is supported");
                }
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
        configuration.setUser((DocumentReference) request.getProperty(PROPERTY_USERREFERENCE));
        configuration.setWiki(wiki);

        try {
            Job currentJob = this.componentManager.<JobContext>getInstance(JobContext.class).getCurrentJob();
            if (currentJob != null) {
                configuration.setJobStatus(currentJob.getStatus());
            }
        } catch (Exception e) {
            this.logger.error("Failed to lookup JobContext, it will be impossible to do interactive install");
        }

        return configuration;
    }
}
