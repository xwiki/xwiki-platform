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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.output.FileOutputTarget;
import org.xwiki.wikistream.output.OutputStreamOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.xar.internal.XARModel;
import org.xwiki.wikistream.xml.internal.output.WikiStreamXMLStreamWriter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARWikiWriter
{
    private static final LocalStringEntityReferenceSerializer TOSTRING_SERIALIZER =
        new LocalStringEntityReferenceSerializer();

    private static class Entry
    {
        public LocalDocumentReference reference;

        public Locale locale = Locale.ROOT;

        public int defaultAction = XARModel.ACTION_OVERWRITE;

        public Entry(LocalDocumentReference reference, Locale locale)
        {
            this.reference = reference;
            this.locale = locale;
        }
    }

    private final String name;

    private final Map<String, Object> wikiProperties;

    private final XAROutputProperties xarProperties;

    private final ZipArchiveOutputStream zipStream;

    private final List<Entry> entries = new LinkedList<Entry>();

    public XARWikiWriter(String name, Map<String, Object> wikiParameters, XAROutputProperties xarProperties)
        throws WikiStreamException
    {
        this.name = name;
        this.wikiProperties = wikiParameters;
        this.xarProperties = xarProperties;

        OutputTarget target = this.xarProperties.getTarget();

        try {
            if (target instanceof FileOutputTarget && ((FileOutputTarget) target).getFile().isDirectory()) {
                this.zipStream =
                    new ZipArchiveOutputStream(new File(((FileOutputTarget) target).getFile(), name + ".xar"));
            } else if (target instanceof OutputStreamOutputTarget) {
                this.zipStream = new ZipArchiveOutputStream(((OutputStreamOutputTarget) target).getOutputStream());
            } else {
                throw new WikiStreamException(String.format("Unsupported output target [%s]. Only [%s] is suppoted",
                    target, OutputStreamOutputTarget.class));
            }
        } catch (IOException e) {
            throw new WikiStreamException("Files to create zip output stream", e);
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

    public Map<String, Object> getMetadata()
    {
        return this.wikiProperties;
    }

    public OutputStream newEntry(LocalDocumentReference reference, Locale locale) throws WikiStreamException
    {
        StringBuilder path = new StringBuilder();

        // Add space name
        path.append(reference.getParent().getName()).append('/');

        // Add document name
        path.append(reference.getName());

        // Add language
        if (locale != null && !locale.equals(Locale.ROOT)) {
            path.append('.');
            path.append(locale);
        }

        // Add extension
        path.append(".xml");

        ZipArchiveEntry zipentry = new ZipArchiveEntry(path.toString());
        try {
            this.zipStream.putArchiveEntry(zipentry);
        } catch (IOException e) {
            throw new WikiStreamException("Failed to add a new zip entry for [" + path + "]", e);
        }

        this.entries.add(new Entry(reference, locale));

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

    private void writePackage() throws WikiStreamException, IOException
    {
        ZipArchiveEntry zipentry = new ZipArchiveEntry(XARModel.PATH_PACKAGE);
        this.zipStream.putArchiveEntry(zipentry);

        WikiStreamXMLStreamWriter writer = new WikiStreamXMLStreamWriter(this.zipStream, this.xarProperties);

        try {
            writer.writeStartDocument();
            writer.writeStartElement(XARModel.ELEMENT_PACKAGE);

            writer.writeStartElement(XARModel.ELEMENT_INFOS);
            writer.writeElement(XARModel.ELEMENT_INFOS_NAME, this.xarProperties.getName());
            writer.writeElement(XARModel.ELEMENT_INFOS_DESCRIPTION, this.xarProperties.getDescription());
            writer.writeElement(XARModel.ELEMENT_INFOS_LICENSE, this.xarProperties.getLicense());
            writer.writeElement(XARModel.ELEMENT_INFOS_AUTHOR, this.xarProperties.getAuthor());
            writer.writeElement(XARModel.ELEMENT_INFOS_VERSION, this.xarProperties.getVersion());
            writer.writeElement(XARModel.ELEMENT_INFOS_ISBACKUPPACK, this.xarProperties.isBackupPack() ? "1" : "0");
            writer.writeElement(XARModel.ELEMENT_INFOS_ISPRESERVEVERSION, this.xarProperties.isPreserveVersion() ? "1"
                : "0");
            writer.writeEndElement();

            writer.writeStartElement(XARModel.ELEMENT_FILES);
            for (Entry entry : this.entries) {
                writer.writeStartElement(XARModel.ELEMENT_FILES_FILES);
                writer.writeAttribute(XARModel.ATTRIBUTE_DEFAULTACTION, String.valueOf(entry.defaultAction));
                writer.writeAttribute(XARModel.ATTRIBUTE_LOCALE, ObjectUtils.toString(entry.locale, ""));
                writer.writeCharacters(TOSTRING_SERIALIZER.serialize(entry.reference));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();

            writer.flush();
        } finally {
            writer.close();
            this.zipStream.closeArchiveEntry();
        }
    }

    public void close() throws WikiStreamException
    {
        // Add package.xml descriptor
        try {
            writePackage();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to write package.xml entry", e);
        }

        // Close zip stream
        try {
            this.zipStream.close();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close zip output stream", e);
        }
    }
}
