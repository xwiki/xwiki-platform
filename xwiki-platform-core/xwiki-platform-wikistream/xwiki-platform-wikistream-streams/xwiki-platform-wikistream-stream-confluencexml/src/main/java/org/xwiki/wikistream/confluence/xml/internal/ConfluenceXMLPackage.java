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
package org.xwiki.wikistream.confluence.xml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.xml.stax.StAXUtils;

public class ConfluenceXMLPackage
{
    private File directory;

    private File entities;

    private File descriptor;

    private File tree;

    public ConfluenceXMLPackage(InputSource source) throws IOException, WikiStreamException, XMLStreamException,
        FactoryConfigurationError, NumberFormatException, ConfigurationException
    {
        InputStream stream;

        if (source instanceof InputStreamInputSource) {
            stream = ((InputStreamInputSource) source).getInputStream();
        } else {
            throw new WikiStreamException(String.format("Unsupported input source of type [%s]", source.getClass()
                .getName()));
        }

        try {
            // Get temporary folder
            this.directory = File.createTempFile("confluencexml", "");
            this.directory.delete();
            this.directory.mkdir();
            this.directory.deleteOnExit();

            // Extract the zip
            ZipArchiveInputStream zais = new ZipArchiveInputStream(stream);
            for (ZipArchiveEntry zipEntry = zais.getNextZipEntry(); zipEntry != null; zipEntry = zais.getNextZipEntry()) {
                if (!zipEntry.isDirectory()) {
                    String path = zipEntry.getName();
                    File file = new File(this.directory, path);

                    if (path.equals("entities.xml")) {
                        this.entities = file;
                    } else if (path.equals("exportDescriptor.properties")) {
                        this.descriptor = file;
                    }

                    FileUtils.copyInputStreamToFile(new CloseShieldInputStream(zais), file);
                }
            }
        } finally {
            source.close();
        }

        // Initialize

        createTree();
    }

    private void createTree() throws XMLStreamException, FactoryConfigurationError, NumberFormatException, IOException,
        ConfigurationException, WikiStreamException
    {
        this.tree = new File(this.directory, "tree");
        this.tree.mkdir();

        InputStream stream = new FileInputStream(getEntities());

        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(stream);

        xmlReader.nextTag();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("object")) {
                readObject(xmlReader);
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }
    }

    private void readObject(XMLStreamReader xmlReader) throws XMLStreamException, NumberFormatException, IOException,
        ConfigurationException, WikiStreamException
    {
        String type = xmlReader.getAttributeValue(null, "class");

        if (type != null) {
            if (type.equals("Page")) {
                readPageObject(xmlReader);
            } else if (type.equals("Space")) {
                readSpaceObject(xmlReader);
            } else if (type.equals("BodyContent")) {
                readBodyContentObject(xmlReader);
            } else if (type.equals("SpaceDescription")) {
                // TODO: any idea how to convert that ?
                StAXUtils.skipElement(xmlReader);
            } else if (type.equals("SpacePermission")) {
                readSpacePermissionObject(xmlReader);
            } else if (type.equals("Attachment")) {
                readAttachmentObject(xmlReader);
            } else if (type.equals("ReferralLink")) {
                // TODO: any idea how to convert that ?
                StAXUtils.skipElement(xmlReader);
            } else if (type.equals("Label")) {
                // TODO: any idea how to convert that ?
                StAXUtils.skipElement(xmlReader);
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }
    }

    private void readAttachmentObject(XMLStreamReader xmlReader) throws XMLStreamException
    {
        // TODO
        StAXUtils.skipElement(xmlReader);
    }

    private void readSpaceObject(XMLStreamReader xmlReader) throws XMLStreamException
    {
        // TODO
        StAXUtils.skipElement(xmlReader);
    }

    private void readSpacePermissionObject(XMLStreamReader xmlReader) throws XMLStreamException
    {
        // TODO
        StAXUtils.skipElement(xmlReader);
    }

    private void readBodyContentObject(XMLStreamReader xmlReader) throws XMLStreamException
    {
        // TODO
        StAXUtils.skipElement(xmlReader);
    }

    private void readPageObject(XMLStreamReader xmlReader) throws IOException, NumberFormatException,
        XMLStreamException, ConfigurationException, WikiStreamException
    {
        int spaceId = -1;

        int pageId = -1;

        PropertiesConfiguration properties = new PropertiesConfiguration();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                pageId = Integer.valueOf(xmlReader.getElementText());
            } else if (elementName.equals("property")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else if (elementName.equals("collection")) {
                String propertyName = xmlReader.getAttributeValue(null, "name");

                properties.setProperty(propertyName, readProperty(xmlReader));
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        savePageProperties(properties, spaceId, pageId);
    }

    private Object readProperty(XMLStreamReader xmlReader) throws XMLStreamException, WikiStreamException
    {
        String propertyClass = xmlReader.getAttributeValue(null, "class");

        if (propertyClass == null) {
            return xmlReader.getElementText();
        } else if (propertyClass.equals("java.util.List") || propertyClass.equals("java.util.Collection")) {
            return readListProperty(xmlReader);
        } else if (propertyClass.equals("Page") || propertyClass.equals("Space") || propertyClass.equals("BodyContent")
            || propertyClass.equals("Attachment") || propertyClass.equals("SpaceDescription")
            || propertyClass.equals("Labelling") || propertyClass.equals("SpacePermission")) {
            return readIdProperty(xmlReader);
        } else {
            StAXUtils.skipElement(xmlReader);
        }

        return null;
    }

    private Object readIdProperty(XMLStreamReader xmlReader) throws WikiStreamException, XMLStreamException
    {
        xmlReader.nextTag();

        if (!xmlReader.getLocalName().equals("id")) {
            throw new WikiStreamException(String.format("Was expecting id element but found [%s]",
                xmlReader.getLocalName()));
        }

        Integer value = Integer.valueOf(xmlReader.getElementText());

        xmlReader.nextTag();

        return value;
    }

    private Object readListProperty(XMLStreamReader xmlReader) throws XMLStreamException, WikiStreamException
    {
        List<Object> list = new ArrayList<Object>();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            list.add(readProperty(xmlReader));
        }

        return list;
    }

    private int getPropertyId(XMLStreamReader xmlReader) throws NumberFormatException, XMLStreamException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals("id")) {
                return Integer.valueOf(xmlReader.getElementText());
            } else {
                StAXUtils.skipElement(xmlReader);
            }
        }

        return -1;
    }

    private File getSpaceFolder(int spaceId)
    {
        return new File(this.tree, String.valueOf(spaceId));
    }

    private File getPageFolder(int spaceId, int pageId)
    {
        File spaceFolder = getSpaceFolder(spaceId);

        return new File(spaceFolder, String.valueOf(pageId));
    }

    private File getPagePropertiesFile(int spaceId, int pageId)
    {
        File folder = getPageFolder(spaceId, pageId);

        return new File(folder, "properties.properties");
    }

    private PropertiesConfiguration getPageProperties(int spaceId, int pageId) throws ConfigurationException
    {
        File file = getPagePropertiesFile(spaceId, pageId);

        return new PropertiesConfiguration(file);
    }

    private void savePageProperties(PropertiesConfiguration properties, int spaceId, int pageId) throws IOException,
        ConfigurationException
    {
        PropertiesConfiguration fileProperties = getPageProperties(spaceId, pageId);

        fileProperties.copy(properties);

        fileProperties.save();
    }

    public File getEntities()
    {
        return this.entities;
    }

    public File getDescriptor()
    {
        return this.descriptor;
    }

    public File getAttachment(Integer id0, Integer id1)
    {
        return null;
    }

    public void close() throws IOException
    {
        if (this.tree != null) {
            FileUtils.deleteDirectory(this.tree);
        }
    }
}
