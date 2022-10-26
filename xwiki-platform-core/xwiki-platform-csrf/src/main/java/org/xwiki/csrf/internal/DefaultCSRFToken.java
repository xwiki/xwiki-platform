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
package org.xwiki.csrf.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.csrf.CSRFTokenConfiguration;
import org.xwiki.model.reference.DocumentReference;

/**
 * Concrete implementation of the {@link CSRFToken} component.
 * <p>
 * This implementation uses a <code>user =&gt; token</code> map to store the tokens. The tokens are random BASE64
 * encoded bit-strings.
 * </p>
 * <p>
 * TODO Expire tokens every couple of hours (configurable). Expiration can be implemented using two maps, oldTokens and
 * currentTokens, old tokens are replaced by current tokens every 1/2 period, check is performed on both and new tokens
 * are added to the current tokens.
 * </p>
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Singleton
public class DefaultCSRFToken implements CSRFToken, Initializable
{
    /** Length of the random string in bytes. */
    private static final int TOKEN_LENGTH = 16;

    /** Resubmission template name. */
    private static final String RESUBMIT_TEMPLATE = "resubmit";

    /** Token storage (one token per user). */
    private final ConcurrentMap<DocumentReference, String> tokens = new ConcurrentHashMap<DocumentReference, String>();

    /** Token for guest user. */
    private String guestToken;

    /** Random number generator. */
    private SecureRandom random;

    /** Used to find out the current user name and the current document. */
    @Inject
    private DocumentAccessBridge docBridge;

    /** Needed to access the current request. */
    @Inject
    private Container container;

    /** CSRFToken component configuration. */
    @Inject
    private CSRFTokenConfiguration configuration;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * {@inheritDoc}
     * <p>
     * Initializes the storage and random number generator.
     * </p>
     */
    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            // use the default implementation then
            this.random = new SecureRandom();
            this.logger.warn("CSRFToken: Using default implementation of SecureRandom");
        }
        byte[] seed = this.random.generateSeed(TOKEN_LENGTH);
        this.random.setSeed(seed);
        this.logger.debug("CSRFToken: Anti-CSRF secret token component has been initialized");
    }

    /**
     * Set the source of random numbers manually.
     *
     * @param random a source of random numbers to use.
     */
    protected void setRandom(final SecureRandom random)
    {
        this.random = random;
    }

    @Override
    public String getToken()
    {
        DocumentReference key = getTokenKey();
        // Handle the case where the current user is Guest
        if (key == null) {
            if (guestToken == null) {
                guestToken = newToken();
            }
            return guestToken;
        }

        // Return the current token if it exists, or generate a new one and return it.
        return this.tokens.computeIfAbsent(key, documentReference -> newToken());
    }

    private String newToken()
    {
        byte[] bytes = new byte[TOKEN_LENGTH];
        this.random.nextBytes(bytes);
        // Base64 encoded token can contain __ or -- which breaks the layout (see XWIKI-5996). Replacing them
        // with x reduces randomness a bit, but it seems that other special characters are either used in XWiki
        // syntax or not URL-safe
        return Base64.encodeBase64URLSafeString(bytes).replaceAll("[_=+-]", "x");
    }

    @Override
    public void clearToken()
    {
        this.logger.debug("Forgetting CSRF token for [{}]", getTokenKey());
        this.tokens.remove(getTokenKey());
    }

    @Override
    public boolean isTokenValid(String token)
    {
        if (!this.configuration.isEnabled()) {
            return true;
        }
        String storedToken = getToken();
        if (token == null || token.equals("") || !storedToken.equals(token)) {
            this.logger.warn("CSRFToken: Secret token verification failed, token: \"" + token
                + "\", stored token: \"" + storedToken + "\"");
            return false;
        }
        return true;
    }

    @Override
    public String getResubmissionURL()
    {
        String query = "resubmit=" + urlEncode(getRequestURI());

        // back URL is the URL of the document that was about to be modified, so in most
        // cases we can redirect back to the correct document (if the user clicks "no")
        String backUrl = getDocumentURL(this.docBridge.getCurrentDocumentReference(), null);
        query += "&xback=" + urlEncode(backUrl);

        // redirect to the resubmission template
        query += "&xpage=" + RESUBMIT_TEMPLATE;
        return backUrl + "?" + query;
    }

    @Override
    public String getRequestURI()
    {
        // request URL is the one that performs the modification
        String srid = SavedRequestManager.saveRequest(getRequest());
        String resubmitUrl = getRequest().getRequestURI();
        resubmitUrl += '?' + SavedRequestManager.getSavedRequestIdentifier() + "=" + srid;
        return resubmitUrl;
    }

    /**
     * Satisfy checkstyle ("view" used twice).
     * 
     * @param reference reference of the current document
     * @param query query part of the URL
     * @return URL of the given document
     */
    private String getDocumentURL(DocumentReference reference, String query)
    {
        return this.docBridge.getDocumentURL(reference, "view", query, null);
    }

    /**
     * URL-encode given string.
     * 
     * @param str the string to encode
     * @return URL-encoded string
     */
    private String urlEncode(String str)
    {
        // TODO find out which encoding is used for response
        String encoding = "UTF-8";
        try {
            return URLEncoder.encode(str, encoding);
        } catch (UnsupportedEncodingException exception) {
            // Shouldn't happen, UTF-8 is always available
            return "";
        }
    }

    /**
     * Get the underlying HTTP request. Throws a runtime error if it is not a servlet request.
     * 
     * @return HTTP servlet request
     */
    private HttpServletRequest getRequest()
    {
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest) {
            return ((ServletRequest) request).getHttpServletRequest();
        }
        throw new RuntimeException("Not supported request type");
    }

    /**
     * Get the token map key for the current user.
     * 
     * @return key for the token map
     */
    private DocumentReference getTokenKey()
    {
        return this.docBridge.getCurrentUserReference();
    }
}
