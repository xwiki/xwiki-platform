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
package org.xwiki.xar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.xar.internal.XarUtils;
import org.xwiki.xar.internal.model.XarModel;

/**
 * Manipulate package.xml XAR package file.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public class XarPackage
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
            this.entries.put(entry, entry);
        }
    }

    public void read(InputStream xarStream) throws IOException, XarException
    {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(xarStream);

        try {
            for (ZipArchiveEntry entry = zis.getNextZipEntry(); entry != null; entry = zis.getNextZipEntry()) {
                if (!entry.isDirectory()) {
                    readEntry(zis, entry);
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
                    readEntry(stream, entry);
                } finally {
                    stream.close();
                }
            }
        }
    }

    private void readEntry(InputStream stream, ZipArchiveEntry entry) throws XarException, IOException
    {
        if (entry.getName().equals(XarModel.PATH_PACKAGE)) {
            readDescriptor(stream);
        } else {
            XarEntry xarEntry = new XarEntry(XarUtils.getReference(stream), entry.getName());
            this.entries.put(xarEntry, xarEntry);

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

    public void write(ZipArchiveOutputStream zipStream, String encoding) throws XarException, IOException
    {
        ZipArchiveEntry zipentry = new ZipArchiveEntry(XarModel.PATH_PACKAGE);
        zipStream.putArchiveEntry(zipentry);

        try {
            write((OutputStream) zipStream, encoding);
        } finally {
            zipStream.closeArchiveEntry();
        }
    }

    public void write(OutputStream stream, String encoding) throws XarException, IOException
    {
        XMLStreamWriter writer;
        try {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stream, encoding);
        } catch (Exception e) {
            throw new XarException("Failed to create an instance of XML stream writer", e);
        }

        writer = new IndentingXMLStreamWriter(writer);

        try {
            writer.writeStartDocument();
            writer.writeStartElement(XarModel.ELEMENT_PACKAGE);

            writer.writeStartElement(XarModel.ELEMENT_INFOS);
            writeElement(writer, XarModel.ELEMENT_INFOS_NAME, getPackageName());
            writeElement(writer, XarModel.ELEMENT_INFOS_DESCRIPTION, getPackageDescription());
            writeElement(writer, XarModel.ELEMENT_INFOS_LICENSE, getPackageLicense());
            writeElement(writer, XarModel.ELEMENT_INFOS_AUTHOR, getPackageAuthor());
            writeElement(writer, XarModel.ELEMENT_INFOS_VERSION, getPackageVersion());
            writeElement(writer, XarModel.ELEMENT_INFOS_ISBACKUPPACK, isPackageBackupPack() ? "1" : "0");
            writeElement(writer, XarModel.ELEMENT_INFOS_ISPRESERVEVERSION, isPackagePreserveVersion() ? "1" : "0");
            writer.writeEndElement();

            writer.writeStartElement(XarModel.ELEMENT_FILES);
            for (XarEntry entry : this.entries.values()) {
                writer.writeStartElement(XarModel.ELEMENT_FILES_FILES);
                writer.writeAttribute(XarModel.ATTRIBUTE_DEFAULTACTION, String.valueOf(entry.getDefaultAction()));
                writer.writeAttribute(XarModel.ATTRIBUTE_LOCALE, ObjectUtils.toString(entry.getLocale(), ""));
                writer.writeCharacters(TOSTRING_SERIALIZER.serialize(entry));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();

            writer.flush();
        } catch (Exception e) {
            throw new XarException("Failed to write XML", e);
        } finally {
            try {
                writer.close();
            } catch (XMLStreamException e) {
                throw new XarException("Failed to close XML writer", e);
            }
        }
    }

    private void writeElement(XMLStreamWriter streamWriter, String localName, String value) throws XMLStreamException
    {
        if (value != null) {
            if (value.isEmpty()) {
                streamWriter.writeEmptyElement(localName);
            } else {
                streamWriter.writeStartElement(localName);
                streamWriter.writeCharacters(value);
                streamWriter.writeEndElement();
            }
        } else {
            streamWriter.writeEmptyElement(localName);
        }
    }

}
