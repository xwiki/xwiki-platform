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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentImporterHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.RootHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.UnknownRootElement;
import org.xwiki.extension.xar.internal.handler.packager.xml.XarPageLimitedHandler;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class DefaultPackager extends AbstractLogEnabled implements Packager, Initializable
{
    @Requirement
    private ComponentManager componentManager;

    @Requirement
    private Execution execution;

    @Requirement("explicit/reference")
    private DocumentReferenceResolver<EntityReference> resolver;

    private SAXParserFactory parserFactory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.parserFactory = SAXParserFactory.newInstance();
    }

    public void importXAR(File xarFile, String wiki) throws IOException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        ZipInputStream zis = new ZipInputStream(fis);

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (!entry.isDirectory()) {
                    try {
                        DocumentImporterHandler documentHandler =
                            new DocumentImporterHandler(this.componentManager, wiki);

                        parseDocument(zis, documentHandler);
                    } catch (NotADocumentException e) {
                        // Impossible to know that before parsing
                    } catch (Exception e) {
                        getLogger().error("Failed to parse document [" + entry.getName() + "]", e);
                    }
                }
            }
        } finally {
            zis.close();
        }
    }

    public void unimportXAR(File xarFile, String wiki) throws IOException
    {
        unimportPages(getEntries(xarFile), wiki);
    }

    public void unimportPages(Collection<XarEntry> pages, String wiki)
    {
        WikiReference wikiReference = new WikiReference(wiki);

        XWikiContext xcontext = getXWikiContext();
        for (XarEntry xarEntry : pages) {
            DocumentReference documentReference = this.resolver.resolve(xarEntry.getDocumentReference(), wikiReference);
            try {
                XWikiDocument document = getXWikiContext().getWiki().getDocument(documentReference, xcontext);

                if (!document.isNew()) {
                    String language = xarEntry.getLanguage();
                    if (language != null) {
                        document = document.getTranslatedDocument(language, xcontext);
                        getXWikiContext().getWiki().deleteDocument(document, xcontext);
                    } else {
                        getXWikiContext().getWiki().deleteAllDocuments(document, xcontext);
                    }
                }
            } catch (XWikiException e) {
                getLogger().error("Failed to delete document [" + documentReference + "]", e);
            }
        }
    }

    public List<XarEntry> getEntries(File xarFile) throws IOException
    {
        List<XarEntry> documents = null;

        FileInputStream fis = new FileInputStream(xarFile);
        ZipInputStream zis = new ZipInputStream(fis);

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (!entry.isDirectory()) {
                    try {
                        XarPageLimitedHandler documentHandler = new XarPageLimitedHandler(this.componentManager);

                        parseDocument(zis, documentHandler);

                        if (documents == null) {
                            documents = new ArrayList<XarEntry>();
                        }

                        XarEntry xarEntry = documentHandler.getXarEntry();
                        xarEntry.setPath(entry.getName());

                        documents.add(xarEntry);
                    } catch (NotADocumentException e) {
                        // Impossible to know that before parsing
                    } catch (Exception e) {
                        getLogger().error("Failed to parse document [" + entry.getName() + "]", e);
                    }
                }
            }
        } finally {
            zis.close();
        }

        return documents != null ? documents : Collections.<XarEntry> emptyList();
    }

    private void parseDocument(InputStream in, ContentHandler documentHandler) throws ParserConfigurationException,
        SAXException, IOException, NotADocumentException
    {
        SAXParser saxParser = this.parserFactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();

        RootHandler handler = new RootHandler(this.componentManager);
        handler.setHandler("xwikidoc", documentHandler);
        xmlReader.setContentHandler(handler);

        try {
            xmlReader.parse(new InputSource(new CloseShieldInputStream(in)));
        } catch (UnknownRootElement e) {
            throw new NotADocumentException("Failed to parse stream", e);
        }
    }

    private ExecutionContext getExecutionContext()
    {
        return this.execution.getContext();
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) getExecutionContext().getProperty("xwikicontext");
    }
}
