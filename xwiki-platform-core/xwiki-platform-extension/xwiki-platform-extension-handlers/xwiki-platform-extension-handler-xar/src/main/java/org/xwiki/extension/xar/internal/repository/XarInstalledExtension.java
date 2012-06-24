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
package org.xwiki.extension.xar.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingInstalledExtension;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;

/**
 * @version $Id$
 * @since 4.0M1
 */
public class XarInstalledExtension extends WrappingInstalledExtension<InstalledExtension>
{
    private XarInstalledExtensionRepository repository;

    private List<XarEntry> pages;

    public XarInstalledExtension(InstalledExtension installedExtension, XarInstalledExtensionRepository repository,
        Packager packager) throws IOException
    {
        super(installedExtension);

        enumeratePages(packager);
    }

    public List<XarEntry> getPages()
    {
        return this.pages;
    }

    private void enumeratePages(Packager packager) throws IOException
    {
        this.pages = packager.getEntries(new File(getFile().getAbsolutePath()));
    }

    // ExtensionRepository

    @Override
    public ExtensionRepository getRepository()
    {
        return this.repository;
    }
}
