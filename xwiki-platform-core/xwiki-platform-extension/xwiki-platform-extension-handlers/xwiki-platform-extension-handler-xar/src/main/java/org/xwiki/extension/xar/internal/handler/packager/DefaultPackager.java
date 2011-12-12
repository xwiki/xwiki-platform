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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.slf4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
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
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;

/**
 * Default implementation of {@link Packager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultPackager implements Packager, Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Execution execution;

    @Inject
    @Named("explicit/reference")
    private DocumentReferenceResolver<EntityReference> resolver;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observation;

    private SAXParserFactory parserFactory;

    public void initialize() throws InitializationException
    {
        this.parserFactory = SAXParserFactory.newInstance();
    }

    @Override
    public void importXAR(XarFile previousXarFile, File xarFile, String wiki, MergeConfiguration mergeConfiguration)
        throws IOException, XWikiException
    {
        if (wiki == null) {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().isVirtualMode()) {
                List<String> wikis = getXWikiContext().getWiki().getVirtualWikisDatabaseNames(context);

                if (!wikis.contains(context.getMainXWiki())) {
                    importXARToWiki(previousXarFile, xarFile, context.getMainXWiki(), mergeConfiguration);
                }

                for (String subwiki : wikis) {
                    importXARToWiki(previousXarFile, xarFile, subwiki, mergeConfiguration);
                }
            } else {
                importXARToWiki(previousXarFile, xarFile, context.getMainXWiki(), mergeConfiguration);
            }
        } else {
            importXARToWiki(previousXarFile, xarFile, wiki, mergeConfiguration);
        }
    }

    public XarMergeResult importXARToWiki(XarFile previousXarFile, File xarFile, String wiki,
        MergeConfiguration mergeConfiguration) throws IOException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        try {
            return importXARToWiki(previousXarFile, fis, wiki, mergeConfiguration);
        } finally {
            fis.close();
        }
    }

    public XarMergeResult importXARToWiki(XarFile previousXarFile, InputStream xarInputStream, String wiki,
        MergeConfiguration mergeConfiguration) throws IOException
    {
        XarMergeResult mergeResult = new XarMergeResult();

        ZipInputStream zis = new ZipInputStream(xarInputStream);

        XWikiContext xcontext = getXWikiContext();

        String currentWiki = xcontext.getDatabase();
        try {
            xcontext.setDatabase(wiki);

            this.observation.notify(new XARImportingEvent(), null, xcontext);

            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (!entry.isDirectory()) {
                    try {
                        DocumentImporterHandler documentHandler =
                            new DocumentImporterHandler(this, this.componentManager, wiki);
                        documentHandler.setPreviousXarFile(previousXarFile);
                        documentHandler.setMergeConfiguration(mergeConfiguration);

                        parseDocument(zis, documentHandler);

                        if (documentHandler.getMergeResult() != null) {
                            mergeResult.addMergeResult(documentHandler.getMergeResult());
                        }
                    } catch (NotADocumentException e) {
                        // Impossible to know that before parsing
                        this.logger.debug("Entry [" + entry + "] is not a document", e);
                    } catch (Exception e) {
                        this.logger.error("Failed to parse document [" + entry.getName() + "]", e);
                    }
                }
            }
        } finally {
            this.observation.notify(new XARImportedEvent(), null, xcontext);

            xcontext.setDatabase(currentWiki);
        }

        return mergeResult;
    }

    @Override
    public void unimportXAR(File xarFile, String wiki) throws IOException, XWikiException
    {
        if (wiki == null) {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().isVirtualMode()) {
                List<String> wikis = getXWikiContext().getWiki().getVirtualWikisDatabaseNames(context);

                if (!wikis.contains(context.getMainXWiki())) {
                    unimportXARFromWiki(xarFile, context.getMainXWiki());
                }

                for (String subwiki : wikis) {
                    unimportXARFromWiki(xarFile, subwiki);
                }
            } else {
                unimportXARFromWiki(xarFile, context.getMainXWiki());
            }
        } else {
            unimportXARFromWiki(xarFile, wiki);
        }
    }

    public void unimportXARFromWiki(File xarFile, String wiki) throws IOException
    {
        unimportPagesFromWiki(getEntries(xarFile), wiki);
    }

    @Override
    public void unimportPages(Collection<XarEntry> pages, String wiki) throws XWikiException
    {
        if (wiki == null) {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().isVirtualMode()) {
                List<String> wikis = getXWikiContext().getWiki().getVirtualWikisDatabaseNames(context);

                if (!wikis.contains(context.getMainXWiki())) {
                    unimportPagesFromWiki(pages, context.getMainXWiki());
                }

                for (String subwiki : wikis) {
                    unimportPagesFromWiki(pages, subwiki);
                }
            } else {
                unimportPagesFromWiki(pages, context.getMainXWiki());
            }
        } else {
            unimportPagesFromWiki(pages, wiki);
        }
    }

    public void unimportPagesFromWiki(Collection<XarEntry> pages, String wiki)
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
                this.logger.error("Failed to delete document [" + documentReference + "]", e);
            }
        }
    }

    @Override
    public List<XarEntry> getEntries(File xarFile) throws IOException
    {
        List<XarEntry> documents = null;

        FileInputStream fis = new FileInputStream(xarFile);
        ZipInputStream zis = new ZipInputStream(fis);

        try {
            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                if (!zipEntry.isDirectory()) {
                    try {
                        XarPageLimitedHandler documentHandler = new XarPageLimitedHandler(this.componentManager);

                        parseDocument(zis, documentHandler);

                        if (documents == null) {
                            documents = new ArrayList<XarEntry>();
                        }

                        XarEntry xarEntry = documentHandler.getXarEntry();
                        xarEntry.setZipEntry(zipEntry);

                        documents.add(xarEntry);
                    } catch (NotADocumentException e) {
                        // Impossible to know that before parsing
                    } catch (Exception e) {
                        this.logger.error("Failed to parse document [" + zipEntry.getName() + "]", e);
                    }
                }
            }
        } finally {
            zis.close();
        }

        return documents != null ? documents : Collections.<XarEntry> emptyList();
    }

    public void parseDocument(InputStream in, ContentHandler documentHandler) throws ParserConfigurationException,
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
