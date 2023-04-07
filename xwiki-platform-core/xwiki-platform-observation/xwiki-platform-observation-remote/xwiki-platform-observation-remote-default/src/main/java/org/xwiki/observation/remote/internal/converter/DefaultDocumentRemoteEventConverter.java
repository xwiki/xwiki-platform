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
package org.xwiki.observation.remote.internal.converter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.converter.DocumentRemoteEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverterException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Default implementation of {@link DocumentRemoteEventConverter}.
 *
 * @version $Id$
 * @since 15.2RC1
 */
@Component
@Singleton
public class DefaultDocumentRemoteEventConverter implements DocumentRemoteEventConverter
{
    private static final String CONTEXT_WIKI = "contextwiki";

    private static final String CONTEXT_USER = "contextuser";

    private static final String DOC_NAME = "docname";

    private static final String DOC_VERSION = "docversion";

    private static final String DOC_LANGUAGE = "doclanguage";

    private static final String ORIGDOC_VERSION = "origdocversion";

    private static final String ORIGDOC_LANGUAGE = "origdoclanguage";

    @Inject
    private Execution execution;

    /**
     * Generate stub XWikiContext.
     */
    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Inject
    private Logger logger;

    @Override
    public Serializable serializeXWikiContext(Hashtable<Object, Object> contextTable)
    {
        if (contextTable instanceof XWikiContext) {
            XWikiContext context = (XWikiContext) contextTable;
            Map<String, Serializable> remoteDataMap = new HashMap<>();

            remoteDataMap.put(CONTEXT_WIKI, context.getWikiId());
            remoteDataMap.put(CONTEXT_USER, context.getUser());

            return (Serializable) remoteDataMap;
        } else {
            throw new IllegalArgumentException("This method should only be used with an XWikiContext instance.");
        }
    }

    @Override
    public <T extends Hashtable<Object, Object>> T unserializeXWikiContext(Serializable remoteData)
        throws RemoteEventConverterException
    {
        XWikiContext xcontext = getXWikiStubContext();

        Map<String, Serializable> remoteDataMap = (Map<String, Serializable>) remoteData;

        if (xcontext != null) {
            xcontext.setWikiId((String) remoteDataMap.get(CONTEXT_WIKI));
            xcontext.setUser((String) remoteDataMap.get(CONTEXT_USER));
        } else {
            throw new RemoteEventConverterException("Can't get a proper XWikiContext."
                + " It generally mean that the wiki has never been fully initialized,"
                + " i.e. has never been accesses at least once");
        }

        return (T) xcontext;
    }

    @Override
    public Serializable serializeDocument(DocumentModelBridge documentModelBridge)
    {
        XWikiDocument document = (XWikiDocument) documentModelBridge;
        Map<String, Serializable> remoteDataMap = new HashMap<>();

        remoteDataMap.put(DOC_NAME, document.getDocumentReference());

        if (!document.isNew()) {
            remoteDataMap.put(DOC_VERSION, document.getVersion());
            remoteDataMap.put(DOC_LANGUAGE, document.getLanguage());
        }

        XWikiDocument originalDocument = document.getOriginalDocument();

        if (originalDocument != null && !originalDocument.isNew()) {
            remoteDataMap.put(ORIGDOC_VERSION, originalDocument.getVersion());
            remoteDataMap.put(ORIGDOC_LANGUAGE, originalDocument.getLanguage());
        }

        return (Serializable) remoteDataMap;
    }

    /**
     * @return a stub XWikiContext, null if none can be generated (XWiki has never been accessed yet)
     */
    private XWikiContext getXWikiStubContext()
    {
        ExecutionContext context = this.execution.getContext();
        XWikiContext xcontext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        if (xcontext == null) {
            xcontext = this.stubContextProvider.createStubContext();

            if (xcontext != null) {
                xcontext.declareInExecutionContext(context);
            }
        }

        return xcontext;
    }

    private XWikiDocument getDocument(XWikiDocument document, String version, XWikiContext xcontext)
        throws RemoteEventConverterException
    {
        // Force bypassing the cache to make extra sure we get the last version of the document.
        XWikiDocument targetDocument = null;
        try {
            targetDocument = xcontext.getWiki().getDocument(document, xcontext);
        } catch (XWikiException e) {
            throw new RemoteEventConverterException(String.format("Error when loading document [%s]",
                document.getDocumentReferenceWithLocale()), e);
        }

        if (!targetDocument.getVersion().equals(version)) {
            // It's not the last version of the document, ask versioning store.
            try {
                targetDocument = xcontext.getWiki().getVersioningStore().loadXWikiDoc(document, version, xcontext);
            } catch (XWikiException e) {
                throw new RemoteEventConverterException(String.format("Error when loading document [%s] version [%s]",
                    document.getDocumentReferenceWithLocale(), version), e);
            }
        }

        return targetDocument;
    }

    @Override
    public DocumentModelBridge unserializeDocument(Serializable remoteData) throws RemoteEventConverterException
    {
        Map<String, Serializable> remoteDataMap = (Map<String, Serializable>) remoteData;

        DocumentReference docReference = (DocumentReference) remoteDataMap.get(DOC_NAME);
        Locale locale = LocaleUtils.toLocale((String) remoteDataMap.get(DOC_LANGUAGE));
        Locale origLocale = LocaleUtils.toLocale((String) remoteDataMap.get(ORIGDOC_LANGUAGE));

        XWikiContext xcontext = getXWikiStubContext();

        XWikiDocument document = new XWikiDocument(docReference, locale);
        XWikiDocument origDoc = new XWikiDocument(docReference, origLocale);

        // Force invalidating the cache to be sure it return (and keep) the right document
        if (xcontext.getWiki().getStore() instanceof XWikiCacheStore) {
            ((XWikiCacheStore) xcontext.getWiki().getStore()).invalidate(document);
            ((XWikiCacheStore) xcontext.getWiki().getStore()).invalidate(origDoc);
        }

        String version = (String) remoteDataMap.get(DOC_VERSION);
        if (version != null) {
            document = getDocument(document, version, xcontext);
        }

        String origVersion = (String) remoteDataMap.get(ORIGDOC_VERSION);
        if (origVersion != null) {
            origDoc = getDocument(origDoc, origVersion, xcontext);
        }

        document.setOriginalDocument(origDoc);

        return document;
    }

    @Override
    public <T extends Hashtable<Object, Object>> DocumentModelBridge unserializeDeletedDocument(Serializable remoteData,
        T context)
        throws RemoteEventConverterException
    {
        Map<String, Serializable> remoteDataMap = (Map<String, Serializable>) remoteData;
        if (context instanceof XWikiContext) {
            XWikiContext xcontext = (XWikiContext) context;
            DocumentReference docReference = (DocumentReference) remoteDataMap.get(DOC_NAME);
            Locale locale = LocaleUtils.toLocale((String) remoteDataMap.get(DOC_LANGUAGE));

            XWikiDocument doc = new XWikiDocument(docReference, locale);

            XWikiDocument origDoc = new XWikiDocument(docReference, locale);

            // We have to get deleted document from the trash (hoping it is in the trash...)
            XWiki xwiki = xcontext.getWiki();
            XWikiRecycleBinStoreInterface store = xwiki.getRecycleBinStore();
            XWikiDeletedDocument[] deletedDocuments = new XWikiDeletedDocument[0];
            try {
                deletedDocuments = store.getAllDeletedDocuments(origDoc, xcontext, true);
            } catch (XWikiException e) {
                throw new RemoteEventConverterException("Error when getting deleted documents", e);
            }
            if (deletedDocuments != null && deletedDocuments.length > 0) {
                long index = deletedDocuments[0].getId();
                try {
                    origDoc = store.restoreFromRecycleBin(index, xcontext, true);
                } catch (Exception e) {
                    // The deleted document can be found in the database but there is an issue with the content
                    // Better a partial notification than no notification at all (what most listeners care about is the
                    // reference of the deleted document)
                    this.logger.error("Failed to restore deleted document [{}]", docReference, e);
                }
            }

            doc.setOriginalDocument(origDoc);

            // Force invalidating the cache to be sure it return (and keep) the right document
            if (xcontext.getWiki().getStore() instanceof XWikiCacheStore) {
                ((XWikiCacheStore) xcontext.getWiki().getStore()).invalidate(doc);
            }

            return doc;
        } else {
            throw new IllegalArgumentException("The context argument should be an instance of XWikiContext");
        }
    }
}
