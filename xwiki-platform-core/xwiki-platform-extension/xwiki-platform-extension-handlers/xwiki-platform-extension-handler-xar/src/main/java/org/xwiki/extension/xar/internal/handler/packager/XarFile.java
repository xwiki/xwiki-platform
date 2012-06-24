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
package org.xwiki.extension.xar.internal.handler.packager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 * @since 4.0M1
 */
public class XarFile
{
    private ZipFile zipFile;

    private Map<XarEntry, XarEntry> entries = new HashMap<XarEntry, XarEntry>();

    public XarFile(File file, Collection<XarEntry> entries) throws IOException
    {
        this.zipFile = new ZipFile(file);

        for (XarEntry xarEntry : entries) {
            this.entries.put(xarEntry, xarEntry);
        }
    }

    public void close() throws IOException
    {
        this.zipFile.close();
    }

    public InputStream getInputStream(XarEntry entry) throws IOException
    {
        XarEntry realEntry = this.entries.get(entry);
        if (realEntry == null) {
            throw new IOException("Failed to find entry [" + entry + "]");
        }

        return this.zipFile.getInputStream(this.zipFile.getEntry(realEntry.getEntryName()));
    }

    public Collection<XarEntry> getEntries()
    {
        return this.entries.values();
    }

    public XarEntry getEntry(EntityReference reference, String language)
    {
        return this.entries.get(new XarEntry(reference, language));
    }
}
