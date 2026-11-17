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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.csrf.CSRFTokenConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authentication.AuthenticationConfiguration;

import com.google.common.base.Suppliers;

/**
 * Concrete implementation of the {@link CSRFToken} component.
 * <p>
 * The token of a user is not stored: it is derived from the authentication validation key (see
 * {@link AuthenticationConfiguration#getValidationKey()}), the serialized reference of the user and the current time
 * period, as the first 16 bytes of an HMAC-SHA256, BASE64 encoded. All the nodes of a cluster must already share that
 * key for the authentication cookies to be accepted by every node, so they all compute the same token for the same
 * user without any state to replicate. As a consequence the token does not change on restart or logout: it rotates
 * when the time period changes (see {@link CSRFTokenConfiguration#getTokenLifetime()}, the token of the
 * previous period is still accepted) and when the validation key changes. The validation key must be a random value
 * set explicitly on every node of a cluster; the example value of the configuration documentation must not be used.
 * The key is read once, the first time a token is needed, and changing it requires a restart.
 * </p>
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Singleton
public class DefaultCSRFToken implements CSRFToken
{
    /**
     * Length of the token in bytes, before encoding.
     */
    private static final int TOKEN_LENGTH = 16;

    /**
     * Algorithm used to derive the tokens.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Prefix of the HMAC message, so that the derived tokens cannot collide with other uses of the same key.
     */
    private static final String MESSAGE_PREFIX = "csrf:";

    /**
     * Subject used in the HMAC message when there is no current user.
     */
    private static final String GUEST_SUBJECT = "guest";

    /**
     * Value of the time period when the tokens never expire.
     */
    private static final long NO_PERIOD = -1;

    private static final long SECONDS_PER_HOUR = 3600;

    /**
     * Resubmission template name.
     */
    private static final String RESUBMIT_TEMPLATE = "resubmit";

    private static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";

    /**
     * Used to find out the current username and the current document.
     */
    @Inject
    private DocumentAccessBridge docBridge;

    /**
     * Needed to access the current request.
     */
    @Inject
    private Container container;

    /**
     * CSRFToken component configuration.
     */
    @Inject
    private CSRFTokenConfiguration configuration;

    /**
     * Provides the secret the tokens are derived from.
     */
    @Inject
    private AuthenticationConfiguration authenticationConfiguration;

    /**
     * Used to identify the current user in the HMAC message.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Clock used to compute the current time period (replaced in tests).
     */
    private Clock clock = Clock.systemUTC();

    /**
     * The secret used to derive the tokens, read from the configuration the first time it is needed (a failure is not
     * memoized, so a key configured later is picked up).
     */
    private final Supplier<byte[]> secret = Suppliers.memoize(this::readSecret);

    @Override
    public String getToken()
    {
        return computeToken(getSubject(), getCurrentPeriod());
    }

    @Override
    public void clearToken()
    {
        // The token is derived from the validation key and the current user, there is nothing to forget.
        this.logger.debug("Ignoring request to clear the CSRF token for [{}]: tokens are derived from the "
            + "authentication validation key and cannot be cleared", getTokenKey());
    }

    @Override
    // The received token is logged to help investigating failed checks (as before); it is an opaque short string
    @SuppressWarnings("javasecurity:S5145")
    public boolean isTokenValid(String token)
    {
        if (!this.configuration.isEnabled()) {
            return true;
        }
        if (token != null && !token.isEmpty()) {
            String subject = getSubject();
            long period = getCurrentPeriod();
            // Accept the token of the current period and the one of the previous period, so that a form rendered
            // just before the period changed can still be submitted.
            if (matches(token, computeToken(subject, period))
                || (period > 0 && matches(token, computeToken(subject, period - 1)))) {
                return true;
            }
        }
        this.logger.warn("Secret CSRF token verification failed for user [{}] (token: [{}])", getTokenKey(), token);

        return false;
    }

    private boolean matches(String token, String expected)
    {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the subject identifying the current user in the HMAC message
     */
    private String getSubject()
    {
        DocumentReference user = getTokenKey();
        return user == null ? GUEST_SUBJECT : this.serializer.serialize(user);
    }

    /**
     * @return the current time period (the number of token lifetimes elapsed since the epoch), or {@link #NO_PERIOD}
     *         when the tokens never expire
     */
    private long getCurrentPeriod()
    {
        long lifetime = this.configuration.getTokenLifetime() * SECONDS_PER_HOUR;
        if (lifetime <= 0) {
            return NO_PERIOD;
        }
        return this.clock.instant().getEpochSecond() / lifetime;
    }

    /**
     * @return the secret the tokens are derived from
     * @throws IllegalStateException when no validation key is configured
     */
    private byte[] getSecret()
    {
        return this.secret.get();
    }

    private byte[] readSecret()
    {
        String key = this.authenticationConfiguration.getValidationKey();
        if (StringUtils.isBlank(key)) {
            throw new IllegalStateException("No authentication validation key configured, cannot derive CSRF tokens");
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private String computeToken(String subject, long period)
    {
        String message = MESSAGE_PREFIX + subject;
        if (period != NO_PERIOD) {
            message += ':' + Long.toString(period);
        }

        byte[] mac;
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(getSecret(), HMAC_ALGORITHM));
            mac = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 is mandatory for every Java implementation, this should never happen
            throw new IllegalStateException("Failed to derive the CSRF token", e);
        }

        // Base64 encoded token can contain __ or -- which breaks the layout (see XWIKI-5996). Replacing them
        // with x reduces randomness a bit, but it seems that other special characters are either used in XWiki
        // syntax or not URL-safe
        return Base64.encodeBase64URLSafeString(Arrays.copyOf(mac, TOKEN_LENGTH)).replaceAll("[_=+-]", "x");
    }

    @Override
    public String getResubmissionURL()
    {
        // back URL is the URL of the document that was about to be modified, so in most
        // cases we can redirect back to the correct document (if the user clicks "no")
        String backUrl = getDocumentURL(this.docBridge.getCurrentDocumentReference(), null);
        String query = "xpage=" + RESUBMIT_TEMPLATE;

        if (this.isResubmitAllowedForCurrentRequest()) {
            Pair<String, String> requestURIInfo = buildRequestURI();
            query += "&xback=" + urlEncode(backUrl);
            query += "&resubmit=" + urlEncode(requestURIInfo.getValue());
            // note we don't use srid so that it doesn't trigger the SavedRequestRestorerFilter
            query += "&sridKey=" + requestURIInfo.getKey();
        }

        // redirect to the resubmission template
        return backUrl + "?" + query;
    }

    @Override
    public boolean isResubmitAllowedForCurrentRequest()
    {
        boolean result = false;
        HttpServletRequest httpServletRequest = getHttpServletRequest();
        if (httpServletRequest != null) {
            result = Strings.CI.equals("POST", httpServletRequest.getMethod())
                && !Strings.CI.equals("cross-site", httpServletRequest.getHeader(SEC_FETCH_SITE_HEADER));
        }
        return result;
    }

    @Override
    public boolean isResubmitAllowedForRequestId(String srid)
    {
        boolean result = false;
        HttpServletRequest httpServletRequest = getHttpServletRequest();
        if (!StringUtils.isBlank(srid) && httpServletRequest != null) {
            Map<String, SavedRequestManager.SavedRequest> savedRequests =
                (Map<String, SavedRequestManager.SavedRequest>) httpServletRequest.getSession()
                    .getAttribute(SavedRequestManager.getSavedRequestKey());
            if (savedRequests != null) {
                return savedRequests.containsKey(srid);
            }
        }
        return result;
    }

    private HttpServletRequest getHttpServletRequest()
    {
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest servletRequest)
        {
            return servletRequest.getRequest();
        }
        return null;
    }

    private Pair<String, String> buildRequestURI()
    {
        String resubmitUrl = getRequest().getRequestURI();
        // request URL is the one that performs the modification
        String srid = SavedRequestManager.saveRequest(getRequest());
        resubmitUrl += '?' + SavedRequestManager.getSavedRequestIdentifier() + "=" + srid;
        return Pair.of(srid, resubmitUrl);
    }

    @Override
    public String getRequestURI()
    {
        if (isResubmitAllowedForCurrentRequest()) {
            return buildRequestURI().getValue();
        } else {
            return null;
        }
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
        if (request instanceof ServletRequest servletRequest) {
            return servletRequest.getRequest();
        }

        throw new RuntimeException("Not supported request type");
    }

    /**
     * Get the reference of the current user, {@code null} for guests.
     * 
     * @return the reference of the current user
     */
    private DocumentReference getTokenKey()
    {
        return this.docBridge.getCurrentUserReference();
    }
}
