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
package com.xpn.xwiki.internal.context;

import java.io.Serializable;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.context.internal.concurrent.AbstractContextStore;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Save and restore well known {@link XWikiContext} entries.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Singleton
@Named("XWikiContext")
public class XWikiContextContextStore extends AbstractContextStore
{
    /**
     * Name of the entry containing the wiki.
     */
    public static final String PROP_WIKI = "wiki";

    /**
     * Name of the entry containing the user.
     */
    public static final String PROP_USER = "user";

    /**
     * Name of the entry containing the author.
     */
    public static final String PROP_AUTHOR = "author";

    /**
     * Name of the entry containing the locale.
     */
    public static final String PROP_LOCALE = "locale";

    /**
     * The prefix of the entries containing request related informations.
     */
    public static final String PREFIX_PROP_REQUEST = "request.";

    /**
     * The suffix of the entry containing the request URL.
     */
    public static final String SUFFIX_PROP_REQUEST_URL = "url";

    /**
     * The suffix of the entry containing the request parameters.
     */
    public static final String SUFFIX_PROP_REQUEST_PARAMETERS = "parameters";

    /**
     * Name of the entry containing the document reference.
     */
    public static final String PROP_REQUEST_URL = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_URL;

    /**
     * Name of the entry containing the document reference.
     */
    public static final String PROP_REQUEST_PARAMETERS = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_PARAMETERS;

    /**
     * The prefix of the entries containing context document related informations.
     */
    public static final String PREFIX_PROP_DOCUMENT = "doc.";

    /**
     * The suffix of the entry containing the context document reference.
     */
    public static final String SUFFIX_PROP_DOCUMENT_REFERENCE = "reference";

    /**
     * Name of the entry containing the document reference.
     */
    public static final String PROP_DOCUMENT_REFERENCE = PREFIX_PROP_DOCUMENT + SUFFIX_PROP_DOCUMENT_REFERENCE;

    protected interface SubContextStore
    {
        void save(String key, String subkey);
    }

    @Inject
    private Provider<XWikiContext> writeProvider;

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readProvider;

    @Inject
    private Container container;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public XWikiContextContextStore()
    {
        super(PROP_WIKI, PROP_USER, PROP_AUTHOR, PROP_LOCALE, PROP_REQUEST_URL, PROP_REQUEST_PARAMETERS,
            PROP_DOCUMENT_REFERENCE);
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Set<String> entries)
    {
        XWikiContext xcontext = this.readProvider.get();

        if (xcontext != null) {
            save(contextStore, PROP_WIKI, xcontext.getWikiId(), entries);
            save(contextStore, PROP_USER, xcontext.getUserReference(), entries);
            save(contextStore, PROP_AUTHOR, xcontext.getAuthorReference(), entries);
            save(contextStore, PROP_LOCALE, xcontext.getLocale(), entries);

            save(contextStore, PREFIX_PROP_DOCUMENT, xcontext.getDoc(), entries);

            save(contextStore, PREFIX_PROP_REQUEST, xcontext.getRequest(), entries);
        }
    }

    private void save(Map<String, Serializable> contextStore, String prefix, XWikiDocument document,
        Set<String> entries)
    {
        save((key, subkey) -> {
            switch (subkey) {
                case SUFFIX_PROP_DOCUMENT_REFERENCE:
                    contextStore.put(key, document.getDocumentReferenceWithLocale());
                    break;

                // TODO: support other properties ?

                default:
                    break;
            }
        }, prefix, entries);
    }

    private void save(Map<String, Serializable> contextStore, String prefix, XWikiRequest request, Set<String> entries)
    {
        save((key, subkey) -> {
            switch (subkey) {
                case SUFFIX_PROP_REQUEST_URL:
                    saveRequestURL(contextStore, key, request);
                    break;

                case SUFFIX_PROP_REQUEST_PARAMETERS:
                    saveRequestParameters(contextStore, key, request);
                    break;

                // TODO: add support for request input stream

                default:
                    saveRequestAll(contextStore, key, request);
            }
        }, prefix, entries);
    }

    private void saveRequestURL(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        contextStore.put(key, HttpServletUtils.getSourceURL(request));
    }

    private void saveRequestParameters(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        contextStore.put(key, new LinkedHashMap<>(request.getParameterMap()));
    }

    private void saveRequestAll(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        saveRequestURL(contextStore, key, request);
        saveRequestParameters(contextStore, key, request);
    }

    @Override
    public void restore(Map<String, Serializable> contextStore)
    {
        XWikiContext xcontext = this.writeProvider.get();

        // Wiki id
        if (contextStore.containsKey(PROP_WIKI)) {
            xcontext.setWikiId((String) contextStore.get(PROP_WIKI));
        }

        // User
        if (contextStore.containsKey(PROP_USER)) {
            xcontext.setUserReference((DocumentReference) contextStore.get(PROP_USER));
        }

        // Locale
        if (contextStore.containsKey(PROP_LOCALE)) {
            xcontext.setLocale((Locale) contextStore.get(PROP_LOCALE));
        }

        // Document
        restoreDocument(contextStore, xcontext);

        // Request
        restoreRequest(contextStore, xcontext);

        // Author
        restoreAuthor(contextStore, xcontext);
    }

    private void restoreAuthor(Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        if (contextStore.containsKey(PROP_AUTHOR)) {
            DocumentReference authorReference = (DocumentReference) contextStore.get(PROP_AUTHOR);

            if (!Objects.equals(xcontext.getAuthorReference(), authorReference)) {
                XWikiDocument secureDocument = (XWikiDocument) xcontext.get(XWikiDocument.CKEY_SDOC);

                if (secureDocument != null) {
                    secureDocument = secureDocument.clone();
                } else {
                    secureDocument = new XWikiDocument(new DocumentReference(
                        authorReference != null ? authorReference.getWikiReference().getName() : "xwiki", "SUSpace",
                        "SUPage"));
                    secureDocument.setAuthorReference(authorReference);
                    secureDocument.setCreatorReference(authorReference);
                }

                // Set the context author
                secureDocument.setContentAuthorReference(authorReference);

                xcontext.put(XWikiDocument.CKEY_SDOC, secureDocument);
            }
        }
    }

    private void restoreDocument(Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        if (contextStore.containsKey(PROP_DOCUMENT_REFERENCE)) {
            DocumentReference documentReference = (DocumentReference) contextStore.get(PROP_DOCUMENT_REFERENCE);

            XWikiDocument document;
            try {
                document = xcontext.getWiki().getDocument(documentReference, xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to load document [{}]", documentReference, e);

                return;
            }

            // TODO: customize the document with what's in the contextStore if any

            xcontext.setDoc(document);
        }
    }

    private void restoreRequest(Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        URL url = (URL) contextStore.get(PROP_REQUEST_URL);
        Map<String, String[]> parameters = (Map<String, String[]>) contextStore.get(PROP_REQUEST_PARAMETERS);

        XWikiRequest request = xcontext.getRequest();

        if (url == null && request != null) {
            url = HttpServletUtils.getSourceURL(request);
        }

        if (parameters == null && request != null) {
            parameters = request.getParameterMap();
        }

        if (url != null) {
            request = new XWikiServletRequestStub(url, parameters);
            xcontext.setRequest(request);
            this.container.setRequest(new ServletRequest(request));
        }
    }
}
