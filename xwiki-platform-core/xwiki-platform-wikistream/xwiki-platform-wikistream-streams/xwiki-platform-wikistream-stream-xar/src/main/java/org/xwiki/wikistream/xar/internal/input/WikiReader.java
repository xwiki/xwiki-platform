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
package org.xwiki.wikistream.xar.internal.input;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARModel;

/**
 * @version $Id$
 * @since 5.2RC1
 */
public class WikiReader
{
    private SyntaxFactory syntaxFactory;

    private String extensionId;

    private String version;

    public WikiReader(SyntaxFactory syntaxFactory)
    {
        this.syntaxFactory = syntaxFactory;
    }

    public String getExtensionId()
    {
        return this.extensionId;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void read(Object filter, XARFilter proxyFilter, XARInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException
    {
        InputSource source = properties.getSource();

        InputStream stream;

        if (source instanceof InputStreamInputSource) {
            stream = ((InputStreamInputSource) source).getInputStream();
        } else {
            throw new WikiStreamException("Unsupported source type [" + source.getClass() + "]");
        }

        try {
            read(stream, filter, proxyFilter, properties);
        } finally {
            source.close();
        }
    }

    public void read(InputStream stream, Object filter, XARFilter proxyFilter, XARInputProperties properties)
        throws XMLStreamException, IOException, WikiStreamException
    {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(stream, "UTF-8", false);

        for (ZipArchiveEntry entry = zis.getNextZipEntry(); entry != null; entry = zis.getNextZipEntry()) {
            if (entry.isDirectory() || entry.getName().startsWith("META-INF")) {
                // The entry is either a directory or is something inside of the META-INF dir.
                // (we use that directory to put meta data such as LICENSE/NOTICE files.)
                continue;
            } else if (entry.getName().equals(XARModel.PATH_PACKAGE)) {
                // The entry is the manifest (package.xml). Read this differently.
                try {
                    readDescriptor(zis);
                } catch (Exception e) {
                    // TODO: LOG warning
                }
            } else {
                DocumentLocaleReader documentReader = new DocumentLocaleReader(this.syntaxFactory);

                try {
                    documentReader.read(zis, filter, proxyFilter, properties);
                } catch (Exception e) {
                    throw new WikiStreamException("Failed to read XAR XML document", e);
                }
            }
        }
    }

    public void readDescriptor(InputStream stream) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stream);

        doc.getDocumentElement().normalize();

        this.extensionId = getElementText(doc, "extensionId");
        this.version = getElementText(doc, "version");

        // this.name = getElementText(doc, "name");
        // this.description = getElementText(doc, "description");
        // this.licence = getElementText(doc, "licence");
        // this.authorName = getElementText(doc, "author");
        // this.backupPack = new Boolean(getElementText(doc, "backupPack")).booleanValue();
        // this.preserveVersion = new Boolean(getElementText(doc, "preserveVersion")).booleanValue();
    }

    private String getElementText(Document doc, String tagName)
    {
        NodeList nList = doc.getElementsByTagName(tagName);

        return nList.item(0).getTextContent();
    }
}
