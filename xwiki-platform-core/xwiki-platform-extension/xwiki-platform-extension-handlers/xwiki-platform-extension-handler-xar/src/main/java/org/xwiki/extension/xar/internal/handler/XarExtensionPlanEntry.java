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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarFile;
import org.xwiki.xar.XarPackage;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarExtensionPlanEntry implements Closeable
{
    /**
     * The extension.
     */
    public final XarInstalledExtension extension;

    /**
     * The extension file opened as XAR file.
     */
    public final XarFile xarFile;

    /**
     * @param extension the extension
     * @throws XarException when failing to parse extension file
     * @throws IOException when failing to parse extension file
     */
    public XarExtensionPlanEntry(XarInstalledExtension extension) throws XarException, IOException
    {
        this.extension = extension;
        this.xarFile = new XarFile(new File(extension.getFile().getAbsolutePath()));
    }

    /**
     * @param extension the extension
     * @param xarPackage the xar package
     * @throws XarException when failing to parse extension file
     * @throws IOException when failing to parse extension file
     */
    public XarExtensionPlanEntry(XarInstalledExtension extension, XarPackage xarPackage) throws XarException,
        IOException
    {
        this.extension = extension;
        this.xarFile = new XarFile(new File(extension.getFile().getAbsolutePath()), xarPackage);
    }

    @Override
    public void close() throws IOException
    {
        this.xarFile.close();
    }
}
