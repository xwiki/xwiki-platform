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
package org.xwiki.wikistream.xar.internal.output;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.output.FileOutputTarget;
import org.xwiki.wikistream.output.OutputStreamOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.xar.output.XAROutputProperties;
import org.xwiki.xar.XarPackage;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARWikiWriter implements Closeable
{
    private final String name;

    private final XAROutputProperties xarProperties;

    private final ZipArchiveOutputStream zipStream;

    private XarPackage xarPackage = new XarPackage();

    public XARWikiWriter(String name, XAROutputProperties xarProperties) throws WikiStreamException
    {
        this.name = name;
        this.xarProperties = xarProperties;

        OutputTarget target = this.xarProperties.getTarget();

        try {
            if (target instanceof FileOutputTarget && ((FileOutputTarget) target).getFile().isDirectory()) {
                this.zipStream =
                    new ZipArchiveOutputStream(new File(((FileOutputTarget) target).getFile(), name + ".xar"));
            } else if (target instanceof OutputStreamOutputTarget) {
                this.zipStream =
                    new ZipArchiveOutputStream(new CloseShieldOutputStream(
                        ((OutputStreamOutputTarget) target).getOutputStream()));
            } else {
                throw new WikiStreamException(String.format("Unsupported output target [%s]. Only [%s] is supported",
                    target, OutputStreamOutputTarget.class));
            }
        } catch (IOException e) {
            throw new WikiStreamException("Failed to create zip output stream", e);
        }

        this.zipStream.setEncoding("UTF8");

        // By including the unicode extra fields, it is possible to extract XAR-files containing documents with
        // non-ascii characters in the document name using InfoZIP, and the filenames will be correctly
        // converted to the character set of the local file system.
        this.zipStream.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
    }

    public String getName()
    {
        return this.name;
    }

    public OutputStream newEntry(LocalDocumentReference reference) throws WikiStreamException
    {
        StringBuilder path = new StringBuilder();

        // Add space name
        path.append(reference.getParent().getName()).append('/');

        // Add document name
        path.append(reference.getName());

        // Add language
        if (reference.getLocale() != null && !reference.getLocale().equals(Locale.ROOT)) {
            path.append('.');
            path.append(reference.getLocale());
        }

        // Add extension
        path.append(".xml");

        ZipArchiveEntry zipentry = new ZipArchiveEntry(path.toString());
        try {
            this.zipStream.putArchiveEntry(zipentry);
        } catch (IOException e) {
            throw new WikiStreamException("Failed to add a new zip entry for [" + path + "]", e);
        }

        this.xarPackage.addEntry(reference);

        return this.zipStream;
    }

    public void closeEntry() throws WikiStreamException
    {
        try {
            this.zipStream.closeArchiveEntry();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close zip archive entry", e);
        }
    }

    private void writePackage() throws WikiStreamException
    {
        try {
            this.xarPackage.write(this.zipStream, xarProperties.getEncoding());
        } catch (Exception e) {
            throw new WikiStreamException("Failed to write package.xml entry", e);
        }
    }

    public void close() throws IOException
    {
        // Add package.xml descriptor
        try {
            writePackage();
        } catch (WikiStreamException e) {
            throw new IOException("Failed to write package", e);
        }

        // Close zip stream
        this.zipStream.close();
    }
}
