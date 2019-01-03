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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.context.internal.concurrent.AbstractContextStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

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
     * @since 11.0RC1
     */
    public static final String PROP_SECURE_DOCUMENT = "secureDocument";

    /**
     * Name of the entry containing the locale.
     */
    public static final String PROP_LOCALE = "locale";

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
     * @since 11.0RC1
     */
    public static final String SUFFIX_PROP_REQUEST_CONTEXTPATH = "contextpath";

    /**
     * The suffix of the entry containing the request parameters.
     */
    public static final String SUFFIX_PROP_REQUEST_PARAMETERS = "parameters";

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
     * @since 11.0RC1
     */
    public static final String PROP_REQUEST_CONTEXTPATH = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_CONTEXTPATH;

    /**
     * Name of the entry containing the request parameters.
     */
    public static final String PROP_REQUEST_PARAMETERS = PREFIX_PROP_REQUEST + SUFFIX_PROP_REQUEST_PARAMETERS;

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
     * Name of the entry containing the document reference.
     */
    public static final String PROP_DOCUMENT_REFERENCE = PREFIX_PROP_DOCUMENT + SUFFIX_PROP_DOCUMENT_REFERENCE;

    /**
     * The reference of the superadmin user.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE =
        new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);

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
    private WikiDescriptorManager wikis;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public XWikiContextContextStore()
    {
        super(PROP_WIKI, PROP_USER, PROP_LOCALE, PROP_REQUEST_BASE, PROP_REQUEST_URL, PROP_REQUEST_PARAMETERS,
            PROP_REQUEST_WIKI, PROP_DOCUMENT_REFERENCE);
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Collection<String> entries)
    {
        XWikiContext xcontext = this.readProvider.get();

        if (xcontext != null) {
            save(contextStore, PROP_WIKI, xcontext.getWikiId(), entries);

            // No need to save the request wiki if it's the same as the current wiki
            if (!StringUtils.equals((String) contextStore.get(PROP_WIKI), xcontext.getOriginalWikiId())) {
                save(contextStore, PROP_REQUEST_WIKI, xcontext.getOriginalWikiId(), entries);
            }

            save(contextStore, PROP_USER, xcontext.getUserReference(), entries);
            save(contextStore, PROP_SECURE_AUTHOR, xcontext.getAuthorReference(), entries);
            save(contextStore, PROP_LOCALE, xcontext.getLocale(), entries);

            save(contextStore, PREFIX_PROP_DOCUMENT, xcontext.getDoc(), entries);

            save(contextStore, PREFIX_PROP_REQUEST, xcontext.getRequest(), entries);
        }
    }

    private void save(Map<String, Serializable> contextStore, String prefix, XWikiDocument document,
        Collection<String> entries)
    {
        if (document != null) {
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
    }

    private void save(Map<String, Serializable> contextStore, String prefix, XWikiRequest request,
        Collection<String> entries)
    {
        if (request != null) {
            save((key, subkey) -> {
                switch (subkey) {
                    case SUFFIX_PROP_REQUEST_BASE:
                        saveRequestBase(contextStore, key, request);
                        break;

                    case SUFFIX_PROP_REQUEST_URL:
                        saveRequestURL(contextStore, key, request);
                        break;

                    case SUFFIX_PROP_REQUEST_PARAMETERS:
                        saveRequestParameters(contextStore, key, request);
                        break;

                    case SUFFIX_PROP_REQUEST_WIKI:
                        // Handled in a different place
                        break;

                    // TODO: add support for request input stream

                    default:
                        saveRequestAll(contextStore, key, request);
                }
            }, prefix, entries);
        }
    }

    private void saveRequestBase(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        URL url = HttpServletUtils.getSourceURL(request);

        try {
            contextStore.put(key, new URL(url.getProtocol(), url.getHost(), url.getPort(), ""));
        } catch (MalformedURLException e) {
            // Cannot happen
        }

        saveRequestContextPath(contextStore, request);
    }

    private void saveRequestURL(Map<String, Serializable> contextStore, String key, XWikiRequest request)
    {
        contextStore.put(key, HttpServletUtils.getSourceURL(request));

        saveRequestContextPath(contextStore, request);
    }

    private void saveRequestContextPath(Map<String, Serializable> contextStore, XWikiRequest request)
    {
        contextStore.put(PROP_REQUEST_CONTEXTPATH, request.getContextPath());
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
        String storedWikiId = null;
        if (contextStore.containsKey(PROP_WIKI)) {
            storedWikiId = (String) contextStore.get(PROP_WIKI);

            xcontext.setWikiId(storedWikiId);
        }

        // User
        if (contextStore.containsKey(PROP_USER)) {
            xcontext.setUserReference((DocumentReference) contextStore.get(PROP_USER));

            // If the current user is not a criteria set one which will always have all the required rights
        } else if (contextStore.containsKey(PROP_SECURE_AUTHOR)) {
            // If the author is provided use it to be as close as possible to the expected behavior
            xcontext.setUserReference((DocumentReference) contextStore.get(PROP_SECURE_AUTHOR));
        } else {
            // Fallback on superadmin when no author is provided
            xcontext.setUserReference(SUPERADMIN_REFERENCE);
        }

        // Locale
        if (contextStore.containsKey(PROP_LOCALE)) {
            xcontext.setLocale((Locale) contextStore.get(PROP_LOCALE));
        }

        // Document
        restoreDocument(storedWikiId, contextStore, xcontext);

        // Author
        restoreAuthor(contextStore, xcontext);

        // Request
        restoreRequest(storedWikiId, contextStore, xcontext);
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
        if (contextStore.containsKey(PROP_DOCUMENT_REFERENCE)) {
            restoreDocument((DocumentReference) contextStore.get(PROP_DOCUMENT_REFERENCE), xcontext);
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

        // TODO: customize the document with what's in the contextStore if any

        xcontext.setDoc(document);
    }

    private void restoreRequest(String storedWikiId, Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        // Find and set the wiki corresponding to the request
        String requestWiki = (String) contextStore.get(PROP_REQUEST_WIKI);
        if (requestWiki == null) {
            requestWiki = storedWikiId;
        } else {
            xcontext.setOriginalWikiId(requestWiki);
        }

        // Find the URL to put in the context request
        URL url = (URL) contextStore.get(PROP_REQUEST_URL);
        if (url == null) {
            url = (URL) contextStore.get(PROP_REQUEST_BASE);
        }
        Map<String, String[]> parameters = (Map<String, String[]>) contextStore.get(PROP_REQUEST_PARAMETERS);

        // Try to deduce missing URL from the request wiki (if provided)
        if (url == null && requestWiki != null) {
            try {
                url = xcontext.getWiki().getServerURL(requestWiki, xcontext);
            } catch (MalformedURLException e) {
                this.logger.warn("Failed to get the URL for stored context wiki [{}]", requestWiki);
            }
        }

        boolean daemon;

        String contextPath = null;

        // Fallback on the first request URL
        if (url == null) {
            XWikiRequest request = xcontext.getRequest();

            if (request != null) {
                url = HttpServletUtils.getSourceURL(request);

                contextPath = request.getContextPath();

                if (parameters == null) {
                    parameters = request.getParameterMap();
                }
            }

            // We don't want to take into account the context request URL when generating URLs
            daemon = true;
        } else {
            // Find the request context path
            contextPath = (String) contextStore.get(PROP_REQUEST_CONTEXTPATH);

            // We want to take into account the context request URL when generating URLs
            daemon = false;
        }

        // Set the context request
        if (url != null) {
            restoreRequest(url, contextPath, parameters, daemon, xcontext);
        }
    }

    private void restoreRequest(URL url, String contextPath, Map<String, String[]> parameters, boolean daemon,
        XWikiContext xcontext)
    {
        XWikiServletRequestStub stubRequest = new XWikiServletRequestStub(url, contextPath, parameters);
        xcontext.setRequest(stubRequest);
        // Indicate that the URL should be taken into account when generating a URL
        stubRequest.setDaemon(daemon);
        this.container.setRequest(new ServletRequest(stubRequest));

        // Update to create the URL factory
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        if (urlFactory == null) {
            urlFactory = new XWikiServletURLFactory();
            xcontext.setURLFactory(urlFactory);
        }
        urlFactory.init(xcontext);
    }
}
