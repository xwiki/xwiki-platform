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
package org.xwiki.url.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link URLSecurityManager}.
 * This implementation keeps a HashSet in memory containing the trusted domains defined in the configuration and
 * for all subwikis. Use {@link #invalidateCache()} to compute back this hashset.
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.7
 */
@Component
@Singleton
public class DefaultURLSecurityManager implements URLSecurityManager
{
    private static final char DOT = '.';
    private static final char PERCENT = '%';

    /**
     * Dedicated string used to escape {@code %} character.
     * @see #parseToSafeURI(String)
     */
    private static final String PERCENT_ESCAPE = "__XWIKI_URL_SECURITY_PERCENT__";

    // Regular expression taken from https://www.rfc-editor.org/rfc/rfc3986#appendix-B.
    private static final Pattern URI_PATTERN =
        Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static final String ERROR_TRANSFORMING_URI_LOG =
        "Error while transforming redirect to [{}] to proper URI: [{}]";

    private static final String FULL_STACK_TRACE = "Full stack trace:";

    @Inject
    private URLConfiguration urlConfiguration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private Set<String> trustedDomains;

    private synchronized void computeTrustedDomains()
    {
        // Check if another thread already computed the list of trusted domains.
        if (this.trustedDomains != null) {
            return;
        }

        Set<String> result = ConcurrentHashMap.newKeySet();
        result.addAll(this.urlConfiguration.getTrustedDomains());

        try {
            for (WikiDescriptor wikiDescriptor : wikiDescriptorManager.getAll()) {
                result.addAll(wikiDescriptor.getAliases());
            }
        } catch (WikiManagerException e) {
            logger.warn("Error while getting wiki descriptor to fill list of trusted domains: [{}]. "
                + "The subwikis won't be taken into account for the list of trusted domains.",
                ExceptionUtils.getRootCauseMessage(e));
        }

        // Set the list of trusted domains only at the end to avoid exposing an incomplete list of trusted domains to
        // other threads.
        this.trustedDomains = result;
    }

    private String getCurrentDomain()
    {
        XWikiContext context = this.contextProvider.get();
        if (context.getRequest() != null && context.getRequest().getHttpServletRequest() != null) {
            String request = context.getRequest().getHttpServletRequest().getRequestURL().toString();
            try {
                URL requestURL = new URL(request);
                return requestURL.getHost();
            } catch (MalformedURLException e) {
                // this should never happen
                throw new RuntimeException(
                    String.format("URL used to access the server is not a proper URL: [%s]", request));
            }
        }
        return "";
    }

    @Override
    public boolean isDomainTrusted(URL urlToCheck)
    {
        if (this.urlConfiguration.isTrustedDomainsEnabled()) {
            maybeInitializeWithDomain(this.getCurrentDomain());
            String host = urlToCheck.getHost();

            do {
                if (trustedDomains.contains(host)) {
                    return true;
                } else if (StringUtils.contains(host, DOT)) {
                    host = host.substring(host.indexOf(DOT) + 1);
                } else {
                    host = "";
                }
            } while (!"".equals(host));

            Object bypassCheckProperty = execution.getContext()
                .getProperty(URLSecurityManager.BYPASS_DOMAIN_SECURITY_CHECK_CONTEXT_PROPERTY);
            boolean bypassCheck = bypassCheckProperty != null && Boolean.parseBoolean(bypassCheckProperty.toString());

            if (bypassCheck) {
                logger.info("Domain of URL [{}] does not belong to the list of trusted domains but it's considered as "
                    + "trusted since the check has been bypassed.", urlToCheck);
            }

            return bypassCheck;
        } else {
            return true;
        }
    }

    /**
     * Initialize the trusted domains with the given domain as additional trusted domains if trusted domains are
     * enabled.
     *
     * @param domain the domain to add to the trusted domains
     */
    private void maybeInitializeWithDomain(String domain)
    {
        if (this.urlConfiguration.isTrustedDomainsEnabled()) {
            if (this.trustedDomains == null) {
                computeTrustedDomains();
            }

            this.trustedDomains.add(domain);
        }
    }

    /**
     * Invalidate the set of trusted domains: this should mainly be used when a subwiki is added/edited/deleted.
     */
    public void invalidateCache()
    {
        this.trustedDomains = null;
    }

    @Override
    public boolean isURITrusted(URI uri)
    {
        boolean result = true;

        // An opaque URI is defined with a scheme but without //
        // e.g. mailto:someone@acme.org or http:xwiki.org
        // We consider those URLs as untrusted even if they are parsed by browsers, as they are not parsed by URI
        // and we cannot properly check them.
        // Also distrust absolute URIs without authority. See
        // https://claroty.com/team82/research/exploiting-url-parsing-confusion
        if (uri.isOpaque() || (uri.getAuthority() == null && uri.isAbsolute())) {
            result = false;
        } else if (uri.getAuthority() != null) {
            // If the URI has an authority it means a domain has been specified and we should check it.
            // Note that the URI might not be absolute, as it might not have a scheme
            // (e.g. //domain.org is a relative URI with an authority)
            try {
                // We systematically put a https scheme if the scheme is missing, as it's how the browser would resolve
                // it. Note that the scheme used here is only for building a proper URL for then checking domain:
                // it's never actually used to perform any request.
                if (!uri.isAbsolute()) {
                    URI uriWithScheme = new URI("https",
                        uri.getRawAuthority(),
                        uri.getRawPath(),
                        uri.getRawQuery(),
                        uri.getRawFragment());
                    result = this.isDomainTrusted(uriWithScheme.toURL());
                } else if (this.urlConfiguration.getTrustedSchemes().contains(uri.getScheme().toLowerCase())) {
                    result = this.isDomainTrusted(uri.toURL());
                } else {
                    result = false;
                }
            } catch (MalformedURLException e) {
                logger.error("Error while transforming URI [{}] to URL: [{}]", uri,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full error stack trace of the URL resolution: ", e);
                result = false;
            } catch (URISyntaxException e) {
                logger.error("Error while transforming URI [{}] to absolute URI with http scheme: [{}]", uri,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full error stack trace of the URI resolution: ", e);
            }
        }
        return result;
    }

    @Override
    public URI parseToSafeURI(String serializedURI) throws URISyntaxException, SecurityException
    {
        URI uri;
        try {
            uri = new URI(serializedURI);
        } catch (URISyntaxException e) {
            // We don't try to repair URI if they use our internal marker to avoid mistakes.
            if (serializedURI.contains(PERCENT_ESCAPE)) {
                throw new IllegalArgumentException(
                    String.format("The given uri [%s] contains the string [%s] which is used internally "
                        + "for performing escaping operations when trying to 'repair' a URI which cannot be parsed. "
                        + "Check the original error for repairing the URI or try to use a different marker.",
                        serializedURI,
                        PERCENT_ESCAPE), e);
            }
            // Attempt repairing the invalid URI similar to org.eclipse.jetty.client.HttpRedirector#sanitize by
            // extracting the different parts and then passing them to the multi-argument constructor that quotes
            // illegal characters.
            Matcher matcher = URI_PATTERN.matcher(serializedURI);
            if (matcher.matches()) {
                String scheme = matcher.group(2);
                String authority = replaceUnquotedPercent(matcher.group(4));
                String path = replaceUnquotedPercent(matcher.group(5));
                String query = replaceUnquotedPercent(matcher.group(7));
                String fragment = replaceUnquotedPercent(matcher.group(9));
                // This constructor automatically encodes all % characters, that's why we replaced them all first by
                // a chain if they belonged to a percent-encoded byte: if they don't, they will be encoded and that's
                // correct. By doing so we avoid any double-encoding.
                uri = new URI(scheme, authority, path, query, fragment);
                // the URI should be parsed again after properly replacing the escape chain by the % character since
                // it won't be encoded anymore with the single argument constructor.
                uri = new URI(uri.toString().replace(PERCENT_ESCAPE, "%"));
            } else {
                throw e;
            }
        }
        if (this.isURITrusted(uri)) {
            return uri;
        } else {
            throw new SecurityException(String.format("The given URI [%s] is not safe on this server.",
                uri));
        }
    }

    @Override
    public URI parseToSafeURI(String serializedURI, String requestHost) throws URISyntaxException, SecurityException
    {
        maybeInitializeWithDomain(requestHost);

        return parseToSafeURI(serializedURI);
    }

    /**
     * The goal of this method is to parse the given String and to replace all {@code %} character by an internal
     * replacement chain if and only if this {@code %} character belongs to a percent encoded byte.
     *
     * @param originalString the string to parse
     * @return a string containing a replacement chain for all {@code %} characters not belonging to a percent
     *         encoded byte
     */
    private String replaceUnquotedPercent(String originalString)
    {
        if (!StringUtils.isBlank(originalString) && originalString.indexOf(PERCENT) > -1) {
            StringBuilder result = new StringBuilder();
            char[] charArray = originalString.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                char currentChar = charArray[i];
                if ((currentChar == PERCENT) && (i < (charArray.length - 2))
                    && isQuotedChar(charArray[i + 1]) && isQuotedChar(charArray[i + 2])) {
                    result.append(PERCENT_ESCAPE);
                } else {
                    result.append(currentChar);
                }
            }
            return result.toString();
        } else {
            return originalString;
        }
    }

    /**
     * Check if the given char belongs to the range of character that forms a percent encoded byte.
     * @param nextChar the char to check if it belongs to the range
     * @return {@code true} if it belongs to the range
     */
    private boolean isQuotedChar(char nextChar)
    {
        // Definition of % encoded bytes with ranges is given in https://url.spec.whatwg.org/#percent-encoded-bytes
        // the next two bytes are in the ranges 0x30 (0) to 0x39 (9), 0x41 (A) to 0x46 (F),
        // and 0x61 (a) to 0x66 (f), all inclusive

        boolean result;
        // Not assigning the result with a boolean expression to comply with checkstyle which is not happy if the
        // boolean expression is too complex...
        if (nextChar >= 0x30 && nextChar <= 0x39) {
            result = true;
        } else if (nextChar >= 0x41 && nextChar <= 0x46) {
            result = true;
        } else if (nextChar >= 0x61 && nextChar <= 0x66) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }
}
