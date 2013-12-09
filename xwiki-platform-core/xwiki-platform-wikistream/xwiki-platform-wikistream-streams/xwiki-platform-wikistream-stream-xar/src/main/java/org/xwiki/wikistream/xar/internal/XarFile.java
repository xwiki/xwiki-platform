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
package org.xwiki.wikistream.xar.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * @version $Id$
 * @since 5.4M1
 */
public class XarFile implements Closeable
{
    private ZipFile zipFile;

    private XarPackage xarPackage;

    public XarFile(File file) throws XarException, IOException
    {
        this(file, null);
    }

    public XarFile(File file, Collection<XarEntry> pages) throws XarException, IOException
    {
        try {
            this.zipFile = new ZipFile(file);
        } catch (IOException e) {
            throw new XarException("Failed to pase zip file", e);
        }

        this.xarPackage = pages != null ? new XarPackage(pages) : new XarPackage(this.zipFile);
    }

    public void close() throws IOException
    {
        this.zipFile.close();
    }

    public InputStream getInputStream(LocalDocumentReference reference) throws IOException
    {
        XarEntry entry = this.xarPackage.getEntry(reference);
        if (entry == null) {
            throw new IOException("Failed to find entry for referenc [" + reference + "]");
        }

        return this.zipFile.getInputStream(this.zipFile.getEntry(entry.getName()));
    }

    public Collection<XarEntry> getEntries()
    {
        return this.xarPackage.getEntries();
    }

    public XarEntry getEntry(LocalDocumentReference reference)
    {
        return this.xarPackage.getEntry(reference);
    }

    @Override
    public String toString()
    {
        return this.zipFile.toString();
    }
}
