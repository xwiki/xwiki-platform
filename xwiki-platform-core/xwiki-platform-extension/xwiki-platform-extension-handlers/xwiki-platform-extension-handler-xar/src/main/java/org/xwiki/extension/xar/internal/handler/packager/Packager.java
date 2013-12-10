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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.output.XWikiDocumentOutputWikiStream;
import org.xwiki.wikistream.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.wikistream.internal.input.BeanInputWikiStream;
import org.xwiki.wikistream.internal.input.DefaultInputStreamInputSource;
import org.xwiki.wikistream.xar.input.XARInputProperties;
import org.xwiki.wikistream.xar.internal.XARModel;
import org.xwiki.wikistream.xar.internal.XARWikiStreamUtils;
import org.xwiki.wikistream.xar.internal.XarEntry;
import org.xwiki.wikistream.xar.internal.XarException;
import org.xwiki.wikistream.xar.internal.XarFile;
import org.xwiki.wikistream.xar.internal.XarPackage;

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
@Component(roles = Packager.class)
@Singleton
public class Packager
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
    @Named(XARWikiStreamUtils.ROLEHINT)
    private InputWikiStreamFactory xarWikiStreamFactory;

    public void importXAR(String comment, File xarFile, PackageConfiguration configuration) throws IOException,
        XWikiException, ComponentLookupException, WikiStreamException
    {
        if (configuration.getWiki() == null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            for (String subwiki : wikis) {
                importXARToWiki(comment, xarFile, new WikiReference(subwiki), configuration);
            }
        } else {
            importXARToWiki(comment, xarFile, new WikiReference(configuration.getWiki()), configuration);
        }
    }

    private XarMergeResult importXARToWiki(String comment, File xarFile, WikiReference wikiReference,
        PackageConfiguration configuration) throws IOException, ComponentLookupException, XWikiException,
        WikiStreamException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        try {
            return importXARToWiki(comment, fis, wikiReference, configuration);
        } finally {
            fis.close();
        }
    }

    private XarMergeResult importXARToWiki(String comment, InputStream xarInputStream, WikiReference wikiReference,
        PackageConfiguration configuration) throws IOException, ComponentLookupException, XWikiException,
        WikiStreamException
    {
        XarMergeResult mergeResult = new XarMergeResult();

        ZipArchiveInputStream zis = new ZipArchiveInputStream(xarInputStream);

        XWikiContext xcontext = this.xcontextProvider.get();

        String currentWiki = xcontext.getDatabase();
        try {
            xcontext.setDatabase(wikiReference.getName());

            this.observation.notify(new XARImportingEvent(), null, xcontext);

            for (ArchiveEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (!entry.isDirectory()) {
                    // Only import what should be imported
                    if (!entry.getName().equals(XARModel.PATH_PACKAGE)
                        && (configuration.getEntriesToImport() == null || configuration.getEntriesToImport().contains(
                            entry.getName()))) {
                        XarEntryMergeResult entityMergeResult =
                            importDocumentToWiki(comment, wikiReference, zis, configuration);
                        if (entityMergeResult != null) {
                            mergeResult.addMergeResult(entityMergeResult);
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

    private XarEntryMergeResult importDocumentToWiki(String comment, WikiReference wikiReference,
        InputStream inputStream, PackageConfiguration configuration) throws XWikiException, WikiStreamException,
        ComponentLookupException, IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument nextDocument;
        try {
            nextDocument = getXWikiDocument(inputStream, wikiReference);
        } catch (Exception e) {
            this.logger.error("Failed to parse document", e);

            return null;
        }

        DocumentReference reference = nextDocument.getDocumentReferenceWithLocale();
        LocalDocumentReference localReference = new LocalDocumentReference(reference);
        XWikiDocument currentDocument = xcontext.getWiki().getDocument(reference, xcontext);
        currentDocument.loadAttachments(xcontext);
        XWikiDocument previousDocument;
        XarFile previousXarFile = configuration.getPreviousPages().get(localReference);
        if (previousXarFile != null) {
            previousDocument = getXWikiDocument(wikiReference, localReference, previousXarFile);
        } else {
            previousDocument = null;
        }

        try {
            XarEntryMergeResult entityMergeResult =
                this.importer.saveDocument(comment, previousDocument, currentDocument, nextDocument, configuration);

            if (configuration.isLogEnabled()) {
                this.logger.info("Successfully imported document [{}] in language [{}]",
                    nextDocument.getDocumentReference(), nextDocument.getRealLocale());
            }

            return entityMergeResult;
        } catch (Exception e) {
            if (configuration.isLogEnabled()) {
                this.logger.info("Failed to import document [{}] in language [{}]",
                    nextDocument.getDocumentReference(), nextDocument.getRealLocale());
            }
        }

        return null;
    }

    public void unimportXAR(File xarFile, PackageConfiguration configuration) throws IOException, XWikiException,
        XarException
    {
        Collection<XarEntry> entries = XarPackage.getEntries(xarFile);

        if (configuration.getWiki() == null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            for (String subwiki : wikis) {
                unimportPagesFromWiki(entries, subwiki, configuration);
            }
        } else {
            unimportPagesFromWiki(entries, configuration.getWiki(), configuration);
        }
    }

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

    private void unimportPagesFromWiki(Collection<XarEntry> entries, String wiki, PackageConfiguration configuration)
    {
        WikiReference wikiReference = new WikiReference(wiki);

        XWikiContext xcontext = this.xcontextProvider.get();
        for (XarEntry xarEntry : entries) {
            // Only delete what should be deleted.
            if (configuration.getEntriesToImport() == null
                || configuration.getEntriesToImport().contains(xarEntry.getName())) {
                DocumentReference documentReference =
                    new DocumentReference(this.resolver.resolve(xarEntry.getReference(), wikiReference), xarEntry
                        .getReference().getLocale());

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

    public XWikiDocument getXWikiDocument(WikiReference wikiReference, LocalDocumentReference documentReference,
        XarFile xarFile) throws WikiStreamException, ComponentLookupException, IOException
    {
        XarEntry realEntry = xarFile.getEntry(documentReference);
        if (realEntry != null) {
            InputStream stream = xarFile.getInputStream(realEntry.getReference());

            try {
                return getXWikiDocument(stream, wikiReference);
            } finally {
                stream.close();
            }
        }

        return null;
    }

    public XWikiDocument getXWikiDocument(InputStream stream, WikiReference wikiReference) throws WikiStreamException,
        ComponentLookupException, IOException
    {
        XWikiDocumentOutputWikiStream documentFilter =
            this.componentManager.getInstance(XWikiDocumentOutputWikiStream.class);

        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();
        documentProperties.setDefaultReference(wikiReference);

        XARInputProperties xarProperties = new XARInputProperties();

        xarProperties.setForceDocument(true);
        xarProperties.setSource(new DefaultInputStreamInputSource(stream));

        BeanInputWikiStream<XARInputProperties> xarWikiStream =
            ((BeanInputWikiStreamFactory<XARInputProperties>) this.xarWikiStreamFactory)
                .createInputWikiStream(xarProperties);

        xarWikiStream.read(documentFilter);

        return documentFilter.getDocument();
    }
}
