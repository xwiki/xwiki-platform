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
package org.xwiki.store.filesystem.internal;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.container.servlet.ServletSession;

/**
 * Identifies the storage scope of the temporary attachments of the current browser.
 * <p>
 * The scope identifier is a random value kept in an HTTP only cookie, so that every cluster member that receives a
 * request of the same browser finds the same temporary attachments, without session replication. The identifier is
 * also copied into the HTTP session (as a plain {@link String}) so that a browser that lost the cookie keeps its scope
 * as long as its session lives. The cookie lives as long as an upload ({@code store.temporaryAttachments.maxAge}) and
 * is refreshed by each upload; it is expired as soon as the browser has no pending upload any more, so that the
 * downloads of a browser without pending upload do not look into the store. The identifier alone gives access to no
 * upload of another user: the storage key also depends on the current user (see {@link TemporaryAttachmentStore}).
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = TemporaryAttachmentScopeManager.class)
@Singleton
public class TemporaryAttachmentScopeManager
{
    /**
     * The name of the cookie, of the session attribute and of the request attribute holding the scope identifier.
     */
    public static final String NAME = "xwikiTemporaryAttachments";

    /**
     * 128 random bits, encoded in Base64url without padding (22 characters).
     */
    private static final int SCOPE_ID_BYTES = 16;

    private static final Pattern SCOPE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{22}");

    private static final String SET_COOKIE = "Set-Cookie";

    /**
     * The request attribute remembering that the cookie was already sent in the current request.
     */
    private static final String COOKIE_SENT = NAME + ".cookieSent";

    private final SecureRandom random = new SecureRandom();

    @Inject
    private Provider<Container> container;

    @Inject
    private TemporaryAttachmentsConfiguration configuration;

    @Inject
    private Logger logger;

    /**
     * Indicates whether the current thread can have a scope at all: there is no scope outside of a request or a session
     * (for example in a background job).
     *
     * @return {@code true} if a scope can be read or created in the current context
     */
    public boolean isAvailable()
    {
        return getHttpRequest() != null || getHttpSession() != null;
    }

    /**
     * Gets the scope identifier of the current browser without creating one. The request (an identifier created
     * earlier in the same request), then the cookie, then the HTTP session are looked at.
     *
     * @return the scope identifier, or {@link Optional#empty()} when the browser has none (it never uploaded a
     *     temporary attachment, or not in this browser session)
     */
    public Optional<String> getScopeId()
    {
        HttpServletRequest request = getHttpRequest();
        if (request != null) {
            Object attribute = request.getAttribute(NAME);
            if (attribute instanceof String scopeId) {
                return Optional.of(scopeId);
            } else if (Boolean.FALSE.equals(attribute)) {
                // The scope was reset earlier in this request.
                return Optional.empty();
            }
            String cookieValue = getCookieValue(request);
            if (cookieValue != null) {
                return Optional.of(cookieValue);
            }
        }

        HttpSession session = getHttpSession();
        if (session != null && session.getAttribute(NAME) instanceof String scopeId && isValid(scopeId)) {
            return Optional.of(scopeId);
        }

        return Optional.empty();
    }

    /**
     * Gets the scope identifier of the current browser, creating it when needed. The identifier is sent to the browser
     * in a cookie (once per request, which also refreshes its lifetime) and copied into the HTTP session, if any, when
     * the session does not have it yet (for example after a switch of cluster member).
     *
     * @return the scope identifier, or {@link Optional#empty()} when there is neither a request nor a session
     */
    public Optional<String> getOrCreateScopeId()
    {
        HttpServletRequest request = getHttpRequest();
        HttpSession session = getHttpSession();
        if (request == null && session == null) {
            return Optional.empty();
        }

        String scopeId = getScopeId().orElseGet(this::generateScopeId);

        if (request != null) {
            request.setAttribute(NAME, scopeId);
            if (request.getAttribute(COOKIE_SENT) == null) {
                addCookie(request, scopeId, false);
                request.setAttribute(COOKIE_SENT, Boolean.TRUE);
            }
        }
        if (session != null && !scopeId.equals(session.getAttribute(NAME))) {
            session.setAttribute(NAME, scopeId);
        }

        return Optional.of(scopeId);
    }

    /**
     * Forgets the scope of the current browser: the cookie is expired, the session and request attributes are removed.
     * The next temporary upload of the browser creates a new scope.
     */
    public void resetScope()
    {
        HttpServletRequest request = getHttpRequest();
        if (request != null) {
            boolean sent = request.getAttribute(COOKIE_SENT) != null;
            // Remember for the rest of the request that the cookie it carries is not valid any more.
            request.setAttribute(NAME, Boolean.FALSE);
            request.removeAttribute(COOKIE_SENT);
            if (sent || getCookieValue(request) != null) {
                addCookie(request, "", true);
            }
        }

        HttpSession session = getHttpSession();
        if (session != null) {
            try {
                session.removeAttribute(NAME);
            } catch (IllegalStateException e) {
                // The session was already invalidated: nothing to remove.
            }
        }
    }

    private String generateScopeId()
    {
        byte[] bytes = new byte[SCOPE_ID_BYTES];
        this.random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isValid(String scopeId)
    {
        return SCOPE_ID_PATTERN.matcher(scopeId).matches();
    }

    private static String getCookieValue(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NAME.equals(cookie.getName()) && cookie.getValue() != null && isValid(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void addCookie(HttpServletRequest request, String value, boolean expire)
    {
        HttpServletResponse response = getHttpResponse();
        if (response == null || response.isCommitted()) {
            this.logger.debug("Cannot send the temporary attachments cookie: no response or response committed");
            return;
        }

        // The Servlet API in use has no way to set the SameSite attribute on a Cookie object, so the header is written
        // directly.
        StringBuilder header = new StringBuilder(NAME).append('=').append(value);
        header.append("; Path=").append(StringUtils.defaultIfEmpty(request.getContextPath(), "/"));
        if (expire) {
            header.append("; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
        } else {
            header.append("; Max-Age=").append(this.configuration.getMaxAge().toSeconds());
        }
        header.append("; HttpOnly; SameSite=Lax");
        if (request.isSecure()) {
            header.append("; Secure");
        }
        response.addHeader(SET_COOKIE, header.toString());
    }

    private HttpServletRequest getHttpRequest()
    {
        Request request = this.container.get().getRequest();

        return request instanceof ServletRequest servletRequest ? servletRequest.getRequest() : null;
    }

    private HttpServletResponse getHttpResponse()
    {
        Response response = this.container.get().getResponse();

        return response instanceof ServletResponse servletResponse ? servletResponse.getResponse() : null;
    }

    private HttpSession getHttpSession()
    {
        Session session = this.container.get().getSession();

        return session instanceof ServletSession servletSession ? servletSession.getSession() : null;
    }
}
