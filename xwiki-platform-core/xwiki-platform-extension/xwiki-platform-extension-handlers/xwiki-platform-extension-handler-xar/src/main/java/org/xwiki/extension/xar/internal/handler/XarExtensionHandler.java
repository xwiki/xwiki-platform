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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.repository.XarLocalExtension;

@Component("xar")
public class XarExtensionHandler extends AbstractExtensionHandler
{
    @Requirement
    private Packager packager;

    @Requirement("xar")
    private LocalExtensionRepository xarRepository;

    // TODO: support question/answer with the UI to resolve conflicts
    public void install(LocalExtension localExtension, String wiki) throws InstallException
    {
        // import xar into wiki (add new version when the page already exists)
        try {
            this.packager.importXAR(localExtension.getFile(), wiki);
        } catch (Exception e) {
            throw new InstallException("Failed to import xar for extension [" + localExtension + "]", e);
        }
    }

    // TODO: support question/answer with the UI to resolve conflicts
    @Override
    public void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace)
        throws InstallException
    {
        // TODO
        // 1) find all modified pages between old and new version
        // 2) compare old version and wiki (to find pages modified by user)
        // 3) delete pages removed in new version (even if modified ?)
        // 4) merge xar
        // 4.1) merge modified pages in wiki with diff between old/new version
        // 4.2) update unmodified pages different between old and new version

        // CURRENT

        // Produce a pages diff between previous and new version
        Set<XarEntry> previousPages = new HashSet<XarEntry>();
        try {
            XarLocalExtension previousXarExtension =
                (XarLocalExtension) this.xarRepository.resolve(previousLocalExtension.getId());
            previousPages.addAll(previousXarExtension.getPages());
        } catch (ResolveException e) {
            // Not supposed to be possible
            throw new InstallException("Failed to get xar extension [" + previousLocalExtension.getId()
                + "] from xar repository", e);
        }

        List<XarEntry> newPages;
        try {
            XarLocalExtension newXarExtension =
                (XarLocalExtension) this.xarRepository.resolve(newLocalExtension.getId());
            newPages = newXarExtension.getPages();
        } catch (ResolveException e) {
            try {
                newPages = this.packager.getEntries(newLocalExtension.getFile());
            } catch (IOException e1) {
                throw new InstallException("Failed to get xar extension [" + newLocalExtension.getId() + "] pages", e);
            }
        }

        for (XarEntry entry : newPages) {
            previousPages.remove(entry);
        }

        // Install new pages
        install(newLocalExtension, namespace);

        // Remove old version pages not anymore in the new version
        try {
            this.packager.unimportPages(previousPages, namespace);
        } catch (Exception e) {
            // TODO: log warning
        }
    }

    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        // TODO: delete pages from the wiki which belong only to this extension (several extension could have some
        // common pages which is not very nice but still could happen technically)

        try {
            XarLocalExtension xarLocalExtension =
                (XarLocalExtension) this.xarRepository.resolve(localExtension.getId());
            List<XarEntry> pages = xarLocalExtension.getPages();
            this.packager.unimportPages(pages, namespace);
        } catch (Exception e) {
            // Not supposed to be possible
            throw new UninstallException("Failed to get xar extension [" + localExtension.getId()
                + "] from xar repository", e);
        }
    }
}
