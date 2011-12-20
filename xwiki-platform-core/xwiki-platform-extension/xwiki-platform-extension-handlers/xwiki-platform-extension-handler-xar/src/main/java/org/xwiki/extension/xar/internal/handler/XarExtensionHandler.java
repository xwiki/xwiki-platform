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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.extension.xar.internal.repository.XarLocalExtension;

import com.xpn.xwiki.doc.merge.MergeConfiguration;

@Component
@Singleton
@Named("xar")
public class XarExtensionHandler extends AbstractExtensionHandler
{
    private static final String WIKI_NAMESPACEPREFIX = "wiki:";

    @Inject
    private Packager packager;

    @Inject
    @Named("xar")
    private LocalExtensionRepository xarRepository;

    @Inject
    private Logger logger;

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void install(LocalExtension localExtension, String namespace) throws InstallException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new InstallException("Unsupported namespace [" + namespace + "], only " + WIKI_NAMESPACEPREFIX
                    + "wikiid format is supported");
            }
        }

        install(null, localExtension, wiki);
    }

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace)
        throws InstallException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new InstallException("Unsupported namespace [" + namespace + "], only " + WIKI_NAMESPACEPREFIX
                    + "wikiid format is supported");
            }
        }

        // TODO
        // 1) find all modified pages between old and new version
        // 2) compare old version and wiki (to find pages modified by user)
        // 3) delete pages removed in new version (even if modified ?)
        // 4) merge xar
        // 4.1) merge modified pages in wiki with diff between old/new version
        // 4.2) update unmodified pages different between old and new version

        // CURRENT

        XarLocalExtension previousXarExtension;
        try {
            previousXarExtension = (XarLocalExtension) this.xarRepository.resolve(previousLocalExtension.getId());
        } catch (ResolveException e) {
            // Not supposed to be possible
            throw new InstallException("Failed to get xar extension [" + previousLocalExtension.getId()
                + "] from xar repository", e);
        }

        // Install new pages
        install(previousXarExtension, newLocalExtension, wiki);

        // Uninstall old version pages not anymore in the new version
        Set<XarEntry> previousPages = new HashSet<XarEntry>(previousXarExtension.getPages());

        List<XarEntry> newPages;
        try {
            XarLocalExtension newXarExtension =
                (XarLocalExtension) this.xarRepository.resolve(newLocalExtension.getId());
            newPages = newXarExtension.getPages();
        } catch (ResolveException e) {
            try {
                newPages = this.packager.getEntries(new File(newLocalExtension.getFile().getAbsolutePath()));
            } catch (IOException e1) {
                throw new InstallException("Failed to get xar extension [" + newLocalExtension.getId() + "] pages", e);
            }
        }

        for (XarEntry entry : newPages) {
            previousPages.remove(entry);
        }

        try {
            this.packager.unimportPages(previousPages, wiki);
        } catch (Exception e) {
            this.logger.warn("Exception when cleaning pages removed since previous xar extension version", e);
        }
    }

    private void install(XarLocalExtension previousExtension, LocalExtension localExtension, String wiki)
        throws InstallException
    {
        // TODO: should be configurable
        MergeConfiguration mergeConfiguration = new MergeConfiguration();

        // import xar into wiki (add new version when the page already exists)
        try {
            this.packager.importXAR(previousExtension != null ? new XarFile(new File(previousExtension.getFile()
                .getAbsolutePath()), previousExtension.getPages()) : null, new File(localExtension.getFile()
                .getAbsolutePath()), wiki, mergeConfiguration);
        } catch (Exception e) {
            throw new InstallException("Failed to import xar for extension [" + localExtension + "]", e);
        }
    }

    @Override
    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        String wiki = namespace;

        if (wiki != null) {
            if (wiki.startsWith(WIKI_NAMESPACEPREFIX)) {
                wiki = wiki.substring(WIKI_NAMESPACEPREFIX.length());
            } else {
                throw new UninstallException("Unsupported namespace [" + namespace + "], only " + WIKI_NAMESPACEPREFIX
                    + "wikiid format is supported");
            }
        }

        // TODO: delete pages from the wiki which belong only to this extension (several extension could have some
        // common pages which will cause all sort of other issues but still could happen technically)

        // TODO: maybe remove only unmodified page ? At least ask for sure when question/answer system will be
        // implemented

        try {
            XarLocalExtension xarLocalExtension =
                (XarLocalExtension) this.xarRepository.resolve(localExtension.getId());
            List<XarEntry> pages = xarLocalExtension.getPages();
            this.packager.unimportPages(pages, wiki);
        } catch (Exception e) {
            // Not supposed to be possible
            throw new UninstallException("Failed to get xar extension [" + localExtension.getId()
                + "] from xar repository", e);
        }
    }
}
