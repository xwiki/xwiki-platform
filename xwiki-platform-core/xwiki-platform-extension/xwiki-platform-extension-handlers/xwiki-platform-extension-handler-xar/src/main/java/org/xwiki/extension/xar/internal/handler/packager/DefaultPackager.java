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
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.slf4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentImporterHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.RootHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.UnknownRootElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.internal.input.DefaultFileInputSource;
import org.xwiki.wikistream.xar.input.XARInputProperties;
import org.xwiki.wikistream.xar.internal.XARPackage;
import org.xwiki.wikistream.xar.internal.XARPackage.Entry;
import org.xwiki.wikistream.xar.internal.XARUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;

/**
 * Default implementation of {@link Packager}.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultPackager implements Packager, Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<EntityReference> resolver;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentMergeImporter importer;

    @Inject
    private MandatoryDocumentInitializerManager initializerManager;

    @Inject
    @Named(XARUtils.ROLEHINT)
    private InputWikiStreamFactory xarReader;

    private SAXParserFactory parserFactory;

    @Override
    public void initialize() throws InitializationException
    {
        this.parserFactory = SAXParserFactory.newInstance();
    }

    @Override
    public void importXAR(File xarFile, PackageConfiguration configuration) throws IOException, XWikiException,
        ComponentLookupException
    {
        if (configuration.getWiki() == null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            for (String subwiki : wikis) {
                importXARToWiki(xarFile, subwiki, configuration);
            }
        } else {
            importXARToWiki(xarFile, configuration.getWiki(), configuration);
        }
    }

    private XarMergeResult importXARToWiki(File xarFile, String wiki, PackageConfiguration configuration)
        throws IOException, ComponentLookupException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        try {
            return importXARToWiki(fis, wiki, configuration);
        } finally {
            fis.close();
        }
    }

    private XarMergeResult importXARToWiki(InputStream xarInputStream, String wiki, PackageConfiguration configuration)
        throws IOException, ComponentLookupException
    {
        XarMergeResult mergeResult = new XarMergeResult();

        ZipArchiveInputStream zis = new ZipArchiveInputStream(xarInputStream);

        XWikiContext xcontext = this.xcontextProvider.get();

        String currentWiki = xcontext.getDatabase();
        try {
            xcontext.setDatabase(wiki);

            this.observation.notify(new XARImportingEvent(), null, xcontext);

            for (ArchiveEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (!entry.isDirectory()) {
                    // Only import what should be imported
                    if (configuration.getEntriesToImport() == null
                        || configuration.getEntriesToImport().contains(entry.getName())) {
                        DocumentImporterHandler documentHandler =
                            new DocumentImporterHandler(this, this.componentManager, wiki, this.importer);

                        try {
                            documentHandler.setConfiguration(configuration);

                            parseDocument(zis, documentHandler);

                            if (documentHandler.getMergeResult() != null) {
                                mergeResult.addMergeResult(documentHandler.getMergeResult());
                            }

                            if (configuration.isLogEnabled()) {
                                this.logger.info("Successfully imported document [{}] in language [{}]",
                                    documentHandler.getDocument().getDocumentReference(), documentHandler.getDocument()
                                        .getRealLocale());
                            }
                        } catch (NotADocumentException e) {
                            // Impossible to know that before parsing
                            this.logger.debug("Entry [" + entry + "] is not a document", e);
                        } catch (Exception e) {
                            this.logger.error("Failed to parse document [" + entry.getName() + "]", e);

                            if (configuration.isLogEnabled()) {
                                this.logger.info("Failed to import document [{}] in language [{}]", documentHandler
                                    .getDocument().getDocumentReference(), documentHandler.getDocument()
                                    .getRealLocale());
                            }
                        }
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
    public void unimportXAR(File xarFile, PackageConfiguration configuration) throws IOException, XWikiException
    {
        if (configuration.getWiki() == null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            for (String subwiki : wikis) {
                unimportXARFromWiki(xarFile, subwiki, configuration);
            }
        } else {
            unimportXARFromWiki(xarFile, configuration.getWiki(), configuration);
        }
    }

    private void unimportXARFromWiki(File xarFile, String wiki, PackageConfiguration configuration) throws IOException
    {
        unimportPagesFromWiki(getEntries(xarFile), wiki, configuration);
    }

    @Override
    public void unimportPages(Collection<XarEntry> pages, PackageConfiguration configuration) throws XWikiException
    {
        if (configuration.getWiki() == null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            for (String subwiki : wikis) {
                unimportPagesFromWiki(pages, subwiki, configuration);
            }
        } else {
            unimportPagesFromWiki(pages, configuration.getWiki(), configuration);
        }
    }

    private void unimportPagesFromWiki(Collection<XarEntry> pages, String wiki, PackageConfiguration configuration)
    {
        WikiReference wikiReference = new WikiReference(wiki);

        XWikiContext xcontext = this.xcontextProvider.get();
        for (XarEntry xarEntry : pages) {
            // Only delete what should be deleted.
            if (configuration.getEntriesToImport() == null
                || configuration.getEntriesToImport().contains(xarEntry.getEntryName())) {
                DocumentReference documentReference =
                    new DocumentReference(this.resolver.resolve(xarEntry.getDocumentReference(), wikiReference),
                        xarEntry.getDocumentReference().getLocale());

                if (!configuration.isSkipMandatorytDocuments() || !isMandatoryDocument(documentReference)) {
                    try {
                        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

                        if (!document.isNew()) {
                            xcontext.getWiki().deleteDocument(document, xcontext);

                            this.logger.info("Successfully deleted document [{}] in language [{}]",
                                document.getDocumentReference(), document.getRealLocale());
                        }
                    } catch (XWikiException e) {
                        this.logger.error("Failed to delete document [{}]", documentReference, e);
                    }
                }
            }
        }
    }

    private boolean isMandatoryDocument(DocumentReference documentReference)
    {
        return this.initializerManager.getMandatoryDocumentInitializer(documentReference) != null;
    }

    @Override
    public List<Entry> getEntries(File xarFile) throws IOException
    {
        XARInputProperties properties = new XARInputProperties();
        properties.setSource(new DefaultFileInputSource(xarFile));
        properties.setReferencesOnly(true);

        BeanInputWikiStreamFactory<XARInputProperties> inputWikiStreamFactory =
            (BeanInputWikiStreamFactory<XARInputProperties>) this.xarReader;

        XARPackage xarPackage = new XARPackage();
        inputWikiStreamFactory.createInputWikiStream(properties).read(xarPackage);

        return xarPackage.getEntries();
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

    @Override
    public XWikiDocument getXWikiDocument(LocalDocumentReference documentReference, XarFile xarFile)
        throws NotADocumentException, ParserConfigurationException, SAXException, IOException
    {
        DocumentHandler documentHandler =
            new DocumentHandler(this.componentManager, documentReference.getWikiReference().getName());

        XarEntry realEntry = xarFile.getEntry(documentReference);
        if (realEntry != null) {
            parseDocument(xarFile.getInputStream(realEntry), documentHandler);

            return documentHandler.getDocument();
        }

        return null;
    }
}
