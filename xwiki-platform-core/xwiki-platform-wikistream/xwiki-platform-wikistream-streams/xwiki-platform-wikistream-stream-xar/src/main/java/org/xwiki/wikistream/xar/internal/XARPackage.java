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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.model.filter.WikiDocumentFilter;
import org.xwiki.wikistream.model.filter.WikiSpaceFilter;
import org.xwiki.wikistream.xml.internal.output.WikiStreamXMLStreamWriter;

/**
 * Manipulate package.xml XAR package file.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
public class XARPackage implements WikiDocumentFilter, WikiSpaceFilter
{
    private static final LocalStringEntityReferenceSerializer TOSTRING_SERIALIZER =
        new LocalStringEntityReferenceSerializer();

    /**
     * An entry in a XAR.
     * 
     * @version $Id$
     */
    public static class Entry
    {
        /**
         * The reference of the entry.
         */
        public LocalDocumentReference reference;

        /**
         * The default action to set in package.xml.
         */
        public int defaultAction = XARModel.ACTION_OVERWRITE;

        /**
         * @param reference the reference of the entry
         */
        public Entry(LocalDocumentReference reference)
        {
            this.reference = reference;
        }
    }

    /**
     * @see #isPreserveVersion()
     */
    private boolean preserveVersion = true;

    /**
     * @see #getPackageName()
     */
    private String packageName;

    /**
     * @see #getPackageDescription()
     */
    private String packageDescription;

    /**
     * @see #getPackageLicense()
     */
    private String packageLicense;

    /**
     * @see #getPackageAuthor()
     */
    private String packageAuthor;

    /**
     * @see #getPackageVersion()
     */
    private String packageVersion;

    /**
     * @see #isPackageBackupPack()
     */
    private boolean packageBackupPack;

    private final List<Entry> entries = new LinkedList<Entry>();

    public boolean isPreserveVersion()
    {
        return this.preserveVersion;
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.preserveVersion = preserveVersion;
    }

    public String getPackageName()
    {
        return this.packageName;
    }

    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    public String getPackageDescription()
    {
        return this.packageDescription;
    }

    public void setPackageDescription(String packageDescription)
    {
        this.packageDescription = packageDescription;
    }

    public String getPackageLicense()
    {
        return this.packageLicense;
    }

    public void setPackageLicense(String packageLicense)
    {
        this.packageLicense = packageLicense;
    }

    public String getPackageAuthor()
    {
        return this.packageAuthor;
    }

    public void setPackageAuthor(String packageAuthor)
    {
        this.packageAuthor = packageAuthor;
    }

    public String getPackageVersion()
    {
        return this.packageVersion;
    }

    public void setPackageVersion(String packageVersion)
    {
        this.packageVersion = packageVersion;
    }

    public boolean isPackageBackupPack()
    {
        return this.packageBackupPack;
    }

    public void setPackageBackupPack(boolean packageBackupPack)
    {
        this.packageBackupPack = packageBackupPack;
    }

    public void addEntry(LocalDocumentReference reference)
    {
        this.entries.add(new Entry(reference));
    }

    public void write(ZipArchiveOutputStream zipStream, String encoding) throws WikiStreamException, IOException
    {
        ZipArchiveEntry zipentry = new ZipArchiveEntry(XARModel.PATH_PACKAGE);
        zipStream.putArchiveEntry(zipentry);

        try {
            write((OutputStream) zipStream, encoding);
        } finally {
            zipStream.closeArchiveEntry();
        }
    }

    public void write(OutputStream stream, String encoding) throws WikiStreamException, IOException
    {
        WikiStreamXMLStreamWriter writer = new WikiStreamXMLStreamWriter(stream, encoding, true, true);

        try {
            writer.writeStartDocument();
            writer.writeStartElement(XARModel.ELEMENT_PACKAGE);

            writer.writeStartElement(XARModel.ELEMENT_INFOS);
            writer.writeElement(XARModel.ELEMENT_INFOS_NAME, getPackageName());
            writer.writeElement(XARModel.ELEMENT_INFOS_DESCRIPTION, getPackageDescription());
            writer.writeElement(XARModel.ELEMENT_INFOS_LICENSE, getPackageLicense());
            writer.writeElement(XARModel.ELEMENT_INFOS_AUTHOR, getPackageAuthor());
            writer.writeElement(XARModel.ELEMENT_INFOS_VERSION, getPackageVersion());
            writer.writeElement(XARModel.ELEMENT_INFOS_ISBACKUPPACK, isPackageBackupPack() ? "1" : "0");
            writer.writeElement(XARModel.ELEMENT_INFOS_ISPRESERVEVERSION, isPreserveVersion() ? "1" : "0");
            writer.writeEndElement();

            writer.writeStartElement(XARModel.ELEMENT_FILES);
            for (Entry entry : this.entries) {
                writer.writeStartElement(XARModel.ELEMENT_FILES_FILES);
                writer.writeAttribute(XARModel.ATTRIBUTE_DEFAULTACTION, String.valueOf(entry.defaultAction));
                writer.writeAttribute(XARModel.ATTRIBUTE_LOCALE, ObjectUtils.toString(entry.reference.getLocale(), ""));
                writer.writeCharacters(TOSTRING_SERIALIZER.serialize(entry.reference));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();

            writer.flush();
        } finally {
            writer.close();
        }
    }

    // WikiSpaceFilter

    private String currentSpace;

    private String currentDocument;

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentSpace = name;
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentSpace = null;
    }

    // WikiDocumentFilter

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentDocument = name;
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentDocument = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        addEntry(new LocalDocumentReference(this.currentSpace, this.currentDocument, locale));
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        // Nothing to do
    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters) throws WikiStreamException
    {
        // Don't care
    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters) throws WikiStreamException
    {
        // Don't care
    }
}
