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

import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingInstalledExtension;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarPackage;

/**
 * @version $Id$
 * @since 4.0M1
 */
public class XarInstalledExtension extends WrappingInstalledExtension<InstalledExtension>
{
    private XarInstalledExtensionRepository repository;

    private XarPackage xarPackage;

    public XarInstalledExtension(InstalledExtension installedExtension, XarInstalledExtensionRepository repository)
        throws IOException, XarException
    {
        super(installedExtension);

        this.repository = repository;
        this.xarPackage = new XarPackage(new File(getFile().getAbsolutePath()));
    }

    /**
     * @since 5.4M1
     */
    public XarPackage getXarPackage()
    {
        return this.xarPackage;
    }

    // ExtensionRepository

    @Override
    public ExtensionRepository getRepository()
    {
        return this.repository;
    }
}
