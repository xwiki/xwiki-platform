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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
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
public class XarPackage implements WikiDocumentFilter, WikiSpaceFilter
{
    private static final LocalStringEntityReferenceSerializer TOSTRING_SERIALIZER =
        new LocalStringEntityReferenceSerializer();

    /**
     * @see #getPackageExtensionId()
     */
    private String packageExtensionId;

    /**
     * @see #isPackagePreserveVersion()
     */
    private boolean packagePreserveVersion = true;

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

    private final Map<LocalDocumentReference, XarEntry> entries = new LinkedHashMap<LocalDocumentReference, XarEntry>();

    public XarPackage()
    {

    }

    public XarPackage(ZipFile zipFile) throws XarException, IOException
    {
        read(zipFile);
    }

    public XarPackage(File file) throws IOException, XarException
    {
        ZipFile zipFile = new ZipFile(file);

        try {
            read(zipFile);
        } finally {
            zipFile.close();
        }
    }

    public XarPackage(InputStream xarStream) throws IOException, XarException
    {
        read(xarStream);
    }

    public XarPackage(Collection<XarEntry> entries)
    {
        for (XarEntry entry : entries) {
            this.entries.put(entry.getReference(), entry);
        }
    }

    public void read(InputStream xarStream) throws IOException, XarException
    {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(xarStream);

        try {
            for (ZipArchiveEntry entry = zis.getNextZipEntry(); entry != null; entry = zis.getNextZipEntry()) {
                if (!entry.isDirectory()) {
                    InputStream stream = zis;

                    try {
                        if (entry.getName().equals(XARModel.PATH_PACKAGE)) {
                            readDescriptor(stream);
                        } else {
                            XarEntry xarEntry = new XarEntry(XarUtils.getReference(stream), entry.getName());
                            this.entries.put(xarEntry.getReference(), xarEntry);

                        }
                    } finally {
                        stream.close();
                    }
                }
            }
        } finally {
            zis.close();
        }
    }

    public void read(ZipFile zipFile) throws IOException, XarException
    {
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();

            if (!entry.isDirectory()) {
                InputStream stream = zipFile.getInputStream(entry);

                try {
                    if (entry.getName().equals(XARModel.PATH_PACKAGE)) {
                        readDescriptor(stream);
                    } else {
                        XarEntry xarEntry = new XarEntry(XarUtils.getReference(stream), entry.getName());
                        this.entries.put(xarEntry.getReference(), xarEntry);

                    }
                } finally {
                    stream.close();
                }
            }
        }
    }

    public String getPackageExtensionId()
    {
        return this.packageExtensionId;
    }

    public void setPackageExtensionId(String packageExtensionId)
    {
        this.packageExtensionId = packageExtensionId;
    }

    public boolean isPackagePreserveVersion()
    {
        return this.packagePreserveVersion;
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.packagePreserveVersion = preserveVersion;
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
        this.entries.put(reference, new XarEntry(reference));
    }

    public Collection<XarEntry> getEntries()
    {
        return this.entries.values();
    }

    public static Collection<XarEntry> getEntries(File file) throws XarException, IOException
    {
        XarPackage xarPackage = new XarPackage(file);

        return xarPackage.getEntries();
    }

    public XarEntry getEntry(LocalDocumentReference reference)
    {
        return this.entries.get(reference);
    }

    public void readDescriptor(InputStream stream) throws XarException, IOException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new XarException("Failed to create a new Document builder", e);
        }

        Document doc;
        try {
            doc = dBuilder.parse(stream);
        } catch (SAXException e) {
            throw new XarException("Failed to parse XML document", e);
        }

        doc.getDocumentElement().normalize();

        this.packageExtensionId = getElementText(doc, "extensionId");
        this.packageVersion = getElementText(doc, "version");
        this.packageName = getElementText(doc, "name");
        this.packageDescription = getElementText(doc, "description");
        this.packageLicense = getElementText(doc, "licence");
        this.packageAuthor = getElementText(doc, "author");
        this.packageBackupPack = Boolean.valueOf(getElementText(doc, "backupPack")).booleanValue();
        this.packagePreserveVersion = Boolean.valueOf(getElementText(doc, "preserveVersion")).booleanValue();
    }

    private String getElementText(Document doc, String tagName)
    {
        NodeList nList = doc.getElementsByTagName(tagName);

        return nList.getLength() > 0 ? nList.item(0).getTextContent() : null;
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
            writer.writeElement(XARModel.ELEMENT_INFOS_ISPRESERVEVERSION, isPackagePreserveVersion() ? "1" : "0");
            writer.writeEndElement();

            writer.writeStartElement(XARModel.ELEMENT_FILES);
            for (XarEntry entry : this.entries.values()) {
                writer.writeStartElement(XARModel.ELEMENT_FILES_FILES);
                writer.writeAttribute(XARModel.ATTRIBUTE_DEFAULTACTION, String.valueOf(entry.getDefaultAction()));
                writer.writeAttribute(XARModel.ATTRIBUTE_LOCALE,
                    ObjectUtils.toString(entry.getReference().getLocale(), ""));
                writer.writeCharacters(TOSTRING_SERIALIZER.serialize(entry.getReference()));
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
