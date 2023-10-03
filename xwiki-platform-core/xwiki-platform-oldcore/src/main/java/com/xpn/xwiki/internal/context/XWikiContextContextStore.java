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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.context.internal.concurrent.AbstractContextStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.transformation.RenderingContextStore;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiRequest;

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
    public static final String PROP_SECURE_AUTHOR = "author";

    /**
     * Name of the entry containing the secure document.
     * 
     * @since 10.11.1
     * @since 11.0
     */
    public static final String PROP_SECURE_DOCUMENT = "secureDocument";

    /**
     * Name of the entry containing the locale.
     */
    public static final String PROP_LOCALE = "locale";

    /**
     * Name of the entry containing the action.
     * 
     * @since 12.10
     */
    public static final String PROP_ACTION = "action";

    /**
     * The prefix of the entries containing request related informations.
     */
    public static final String PREFIX_PROP_REQUEST = "request.";

    /**
     * The suffix of the entry containing the request base URL ({@code <protocol>://<host>[:<port>]}).
     * 
     * @since 10.11RC1
     */
    public static final String SUFFIX_PROP_REQUEST_BASE = "base";

    /**
     * The suffix of the entry containing the request URL.
     */
    public static final String SUFFIX_PROP_REQUEST_URL = "url";

    /**
     * The suffix of the entry containing the request context path (usually the first element of the URL path.
     * 
     * @since 10.11.1
     * @since 11.0
     */
    public static final String SUFFIX_PROP_REQUEST_CONTEXTPATH = "contextpath";

    /**
     * The suffix of the entry containing the request parameters.
     */
    public static final String SUFFIX_PROP_REQUEST_PARAMETERS = "parameters";

    /**
     * The suffix of the entry containing the request cookies.
     */
    public static final String SUFFIX_PROP_REQUEST_COOKIES = "cookies";

    /**
     * The suffix of the entry containing the request headers.
     * 
     * @since 14.10
     */
    public static final String SUFFIX_PROP_REQUEST_HEADERS = "headers";

    /**
     * The suffix of the entry containing the request remote address.
     * 
     * @since 14.10
     */
    public static final String SUFFIX_PROP_REQUEST_REMOTE_ADDR = "remoteAddr";

    /**
     * The suffix of the entry containing the request session.
     * 
     * @since 14.10.18
     * @since 15.5.3
     * @since 15.9RC1
     */
    public static final String SUFFIX_PROP_REQUEST_SESSION = "session";

    /**
     * The suffix of the entry containing the request wiki.
     * 
     * @since 10.11RC1
     */
    public static final String SUFFIX_PROP_REQUEST_WIKI = PROP_WIKI;

    /**
     * Name of the entry containing the request base URL.
     */
    public static final String PROP_REQUEST_BASE = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_BASE;

    /**
     * Name of the entry containing the request URL.
     */
    public static final String PROP_REQUEST_URL = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_URL;

    /**
     * Name the entry containing the request context path (usually the first element of the URL path.
     * 
     * @since 10.11.1
     * @since 11.0
     */
    public static final String PROP_REQUEST_CONTEXTPATH = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_CONTEXTPATH;

    /**
     * Name of the entry containing the request parameters.
     */
    public static final String PROP_REQUEST_PARAMETERS = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_PARAMETERS;

    /**
     * Name of the entry containing the request cookies.
     */
    public static final String PROP_REQUEST_COOKIES = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_COOKIES;

    /**
     * Name of the entry containing the request headers.
     * 
     * @since 14.10
     */
    public static final String PROP_REQUEST_HEADERS = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_HEADERS;

    /**
     * Name of the entry containing the request remote address.
     * 
     * @since 14.10
     */
    public static final String PROP_REQUEST_REMOTE_ADDR = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_REMOTE_ADDR;

    /**
     * Name of the entry containing the request session.
     * 
     * @since 14.10.18
     * @since 15.5.3
     * @since 15.9RC1
     */
    public static final String PROP_REQUEST_SESSION = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_SESSION;

    /**
     * Name of the entry containing the request wiki.
     * 
     * @since 10.11RC1
     */
    public static final String PROP_REQUEST_WIKI = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_WIKI;

    /**
     * The prefix of the entries containing context document related informations.
     */
    public static final String PREFIX_PROP_DOCUMENT = "doc.";

    /**
     * The suffix of the entry containing the context document reference.
     */
    public static final String SUFFIX_PROP_DOCUMENT_REFERENCE = "reference";

    /**
     * The suffix of the entry containing the context document revision (version).
     */
    public static final String SUFFIX_PROP_DOCUMENT_REVISION = "revision";

    /**
     * Name of the entry containing the document reference.
     */
    public static final String PROP_DOCUMENT_REFERENCE = PREFIX_PROP_DOCUMENT + SUFFIX_PROP_DOCUMENT_REFERENCE;

    /**
     * Name of the entry containing the document revision (version).
     */
    public static final String PROP_DOCUMENT_REVISION = PREFIX_PROP_DOCUMENT + SUFFIX_PROP_DOCUMENT_REVISION;

    /**
     * The XWiki context key used to store the requested document revision (version).
     */
    private static final String REV = "rev";

    /**
     * The reference of the superadmin user.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE =
        new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);

    @Inject
    private Provider<XWikiContext> writeProvider;

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readProvider;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private RequestInitializer requestInitializer;

    @Inject
    private Logger logger;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    /**
     * Default constructor.
     */
    public XWikiContextContextStore()
    {
        super(PROP_WIKI, PROP_USER, PROP_LOCALE, PROP_ACTION, PROP_REQUEST_BASE, PROP_REQUEST_URL,
            PROP_REQUEST_PARAMETERS, PROP_REQUEST_HEADERS, PROP_REQUEST_COOKIES, PROP_REQUEST_REMOTE_ADDR,
            PROP_REQUEST_SESSION, PROP_REQUEST_WIKI, PROP_DOCUMENT_REFERENCE, PROP_DOCUMENT_REVISION);
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Collection<String> entries)
    {
        XWikiContext xcontext = this.readProvider.get();

        if (xcontext != null) {
            save(contextStore, PROP_WIKI, xcontext.getWikiId(), entries);

            save(contextStore, PROP_USER, xcontext.getUserReference(), entries);
            save(contextStore, PROP_SECURE_AUTHOR, xcontext.getAuthorReference(), entries);
            save(contextStore, PROP_LOCALE, xcontext.getLocale(), entries);
            save(contextStore, PROP_ACTION, xcontext.getAction(), entries);

            saveDocument(contextStore, PREFIX_PROP_DOCUMENT, xcontext, entries);

            save(contextStore, PREFIX_PROP_REQUEST, xcontext.getRequest(), entries, xcontext);
        }
    }

    private void saveDocument(Map<String, Serializable> contextStore, String prefix, XWikiContext xcontext,
        Collection<String> entries)
    {
        XWikiDocument document = xcontext.getDoc();
        if (document != null) {
            save((key, subkey) -> {
                switch (subkey) {
                    case SUFFIX_PROP_DOCUMENT_REFERENCE:
                        contextStore.put(key, document.getDocumentReferenceWithLocale());
                        break;
                    case SUFFIX_PROP_DOCUMENT_REVISION:
                        // Save the document revision only if it matches the revision that was requested explicitly.
                        if (Objects.equals(document.getVersion(), xcontext.get(REV))) {
                            contextStore.put(key, document.getVersion());
                        }
                        break;

                    // TODO: support other properties ?

                    default:
                        break;
                }
            }, prefix, entries);
        }
    }

    private void save(Map<String, Serializable> contextStore, String prefix, XWikiRequest request,
        Collection<String> entries, XWikiContext xcontext)
    {
        if (request != null) {
            save((key, subkey) -> {
                switch (subkey) {
                    case SUFFIX_PROP_REQUEST_BASE:
                        saveRequestBase(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_URL:
                        saveRequestURL(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_PARAMETERS:
                        saveRequestParameters(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_HEADERS:
                        saveRequestHeaders(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_COOKIES:
                        saveRequestCookies(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_REMOTE_ADDR:
                        saveRequestRemoteAddr(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_SESSION:
                        saveRequestSession(contextStore, request);
                        break;

                    case SUFFIX_PROP_REQUEST_WIKI:
                        contextStore.put(key, xcontext.getOriginalWikiId());
                        break;

                    // TODO: add support for request input stream

                    default:
                        saveRequestAll(contextStore, key, request);
                }
            }, prefix, entries);
        }
    }

    private void saveRequestBase(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        URL url = HttpServletUtils.getSourceURL(request);

        try {
            contextStore.put(PROP_REQUEST_BASE, new URL(url.getProtocol(), url.getHost(), url.getPort(), ""));
        } catch (MalformedURLException e) {
            // Cannot happen
        }

        saveRequestContextPath(contextStore, request);
    }

    private void saveRequestURL(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_URL, HttpServletUtils.getSourceURL(request));

        saveRequestContextPath(contextStore, request);
    }

    private void saveRequestContextPath(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_CONTEXTPATH, request.getContextPath());
    }

    private void saveRequestParameters(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_PARAMETERS, new LinkedHashMap<>(request.getParameterMap()));
    }

    private void saveRequestHeaders(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        if (request.getHeaderNames() != null) {
            Map<String, List<String>> headers = Collections.list(request.getHeaderNames()).stream()
                .map(headerName -> Map.entry(headerName, Collections.list(request.getHeaders(headerName))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right,
                    () -> new LinkedHashMap<String, List<String>>()));
            contextStore.put(PROP_REQUEST_HEADERS, (Serializable) headers);
        }
    }

    private void saveRequestCookies(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            // Clone the cookies.
            cookies = Stream.of(cookies).map(Cookie::clone).toArray(Cookie[]::new);
        }
        contextStore.put(PROP_REQUEST_COOKIES, cookies);
    }

    private void saveRequestRemoteAddr(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_REMOTE_ADDR, request.getRemoteAddr());
    }

    private void saveRequestSession(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_SESSION, new SerializableHttpSessionWrapper(request.getSession()));
    }

    private void saveRequestAll(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        saveRequestURL(contextStore, request);
        saveRequestParameters(contextStore, request);
        saveRequestHeaders(contextStore, request);
        saveRequestCookies(contextStore, request);
        saveRequestRemoteAddr(contextStore, request);
        saveRequestSession(contextStore, request);
    }

    @Override
    public void restore(Map<String, Serializable> contextStore)
    {
        XWikiContext xcontext = this.writeProvider.get();

        // Wiki id
        String storedWikiId = null;
        if (contextStore.containsKey(PROP_WIKI)) {
            storedWikiId = (String) contextStore.get(PROP_WIKI);

            xcontext.setWikiId(storedWikiId);
        }

        // User
        DocumentReference userReference;
        if (contextStore.containsKey(PROP_USER)) {
            userReference = (DocumentReference) contextStore.get(PROP_USER);

            // If the current user is not a criteria set one which will always have all the required rights
        } else if (contextStore.containsKey(PROP_SECURE_AUTHOR)) {
            // If the author is provided use it to be as close as possible to the expected behavior
            // Except when in case of restricted context since we cannot trust the author, by definition
            boolean restricted = get(contextStore, RenderingContextStore.PROP_RESTRICTED, false);
            if (restricted) {
                userReference = null;
            } else {
                userReference = (DocumentReference) contextStore.get(PROP_SECURE_AUTHOR);
            }
        } else {
            // Fallback on superadmin when no author is provided
            userReference = SUPERADMIN_REFERENCE;
        }
        xcontext.setUserReference(userReference);

        // Locale
        if (contextStore.containsKey(PROP_LOCALE)) {
            xcontext.setLocale((Locale) contextStore.get(PROP_LOCALE));
        }

        // Action
        if (contextStore.containsKey(PROP_ACTION)) {
            xcontext.setAction((String) contextStore.get(PROP_ACTION));
        }

        // Document
        restoreDocument(storedWikiId, contextStore, xcontext);

        // Author
        restoreAuthor(contextStore, xcontext);

        // Request
        this.requestInitializer.restoreRequest(storedWikiId, contextStore, xcontext);
    }

    private void restoreAuthor(Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        if (contextStore.containsKey(PROP_SECURE_AUTHOR)) {
            DocumentReference authorReference = (DocumentReference) contextStore.get(PROP_SECURE_AUTHOR);

            restoreAuthor(contextStore, authorReference, xcontext);
        }
    }

    private void restoreAuthor(Map<String, Serializable> contextStore, DocumentReference authorReference,
        XWikiContext xcontext)
    {
        DocumentReference secureDocumentReference = (DocumentReference) contextStore.get(PROP_SECURE_DOCUMENT);

        XWikiDocument secureDocument = null;

        if (secureDocumentReference != null) {
            secureDocument = getSecureDocument(secureDocumentReference, authorReference, xcontext);
        }

        // If there is no requested secure document (which is usually a bad practice), invent one
        if (secureDocument == null) {
            secureDocument = new XWikiDocument(new DocumentReference(
                authorReference != null ? authorReference.getWikiReference().getName() : xcontext.getMainXWiki(),
                "SUSpace", "SUPage"));
            secureDocument.setAuthorReference(authorReference);
            secureDocument.setCreatorReference(authorReference);
        }

        // Set the context author
        secureDocument.setContentAuthorReference(authorReference);

        xcontext.put(XWikiDocument.CKEY_SDOC, secureDocument);
    }

    private XWikiDocument getSecureDocument(DocumentReference secureDocumentReference,
        DocumentReference authorReference, XWikiContext xcontext)
    {
        XWikiDocument secureDocument = (XWikiDocument) xcontext.get(XWikiDocument.CKEY_SDOC);

        if (secureDocument != null && secureDocument.getDocumentReference().equals(secureDocumentReference)) {
            // If the document does not have the right content author clone it to avoid messing with the
            // cache
            if (!Objects.equals(secureDocument.getContentAuthorReference(), authorReference)) {
                // Clone the document to avoid messing with the cache
                secureDocument = secureDocument.clone();
            }
        } else {
            try {
                // Get the requested secure document
                secureDocument = xcontext.getWiki().getDocument(secureDocumentReference, xcontext);

                // If the document does not have the right content author clone it to avoid messing with the
                // cache
                if (!Objects.equals(secureDocument.getContentAuthorReference(), authorReference)) {
                    secureDocument = secureDocument.clone();
                }
            } catch (XWikiException e) {
                this.logger.error("Failed to load secure document [{}]", secureDocumentReference, e);

                // Fallback on a new empty XWikiDocument instance
                secureDocument = new XWikiDocument(secureDocumentReference);
                secureDocument.setAuthorReference(authorReference);
                secureDocument.setCreatorReference(authorReference);
            }
        }

        return secureDocument;
    }

    private void restoreDocument(String storedWikiId, Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        if (contextStore.containsKey(PROP_DOCUMENT_REVISION)) {
            xcontext.put(REV, contextStore.get(PROP_DOCUMENT_REVISION));
        }

        if (contextStore.containsKey(PROP_DOCUMENT_REFERENCE)) {
            DocumentReference document = (DocumentReference) contextStore.get(PROP_DOCUMENT_REFERENCE);
            restoreDocument(document, xcontext);

            // Set the document's wiki when it's not explicitly set
            if (storedWikiId == null) {
                xcontext.setWikiReference(document.getWikiReference());
            }
        } else if (storedWikiId != null) {
            // If no document reference is provided get the wiki home page
            try {
                WikiDescriptor wikiDescriptor = this.wikis.getById(storedWikiId);

                if (wikiDescriptor != null) {
                    restoreDocument(wikiDescriptor.getMainPageReference(), xcontext);
                }
            } catch (WikiManagerException e) {
                this.logger.warn("Can't access the descriptor of the restored context wiki [{}]", storedWikiId, e);
            }
        }
    }

    private void restoreDocument(DocumentReference documentReference, XWikiContext xcontext)
    {
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(documentReference, xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to load document [{}]", documentReference, e);

            return;
        }

        String revision = (String) xcontext.get(REV);
        if (StringUtils.isNotEmpty(revision)) {
            try {
                document = this.documentRevisionProvider.getRevision(document, revision);
            } catch (XWikiException e) {
                this.logger.warn("Failed to load revision [{}] of document [{}]. Root cause is [{}].", revision,
                    documentReference, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        // TODO: customize the document with what's in the contextStore if any

        // Put a cloned document in the context so that it's not confused with the document coming from the document
        // cache. The same is done by XWiki#prepareDocuments(). This ensures for instance that the sheet specified in
        // the execution context is applied only to the context document and not to the document retrieved from the
        // cache.
        xcontext.setDoc(document.clone());
    }
}
