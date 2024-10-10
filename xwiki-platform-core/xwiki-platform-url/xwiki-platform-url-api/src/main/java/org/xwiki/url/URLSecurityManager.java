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
package org.xwiki.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Dedicated component to perform security checks on URLs.
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.7
 */
@Role
public interface URLSecurityManager
{
    /**
     * Constant to be used in {@link org.xwiki.context.ExecutionContext} with the value {@code "true"} to bypass a
     * check of {@link #isDomainTrusted(URL)}.
     */
    String BYPASS_DOMAIN_SECURITY_CHECK_CONTEXT_PROPERTY = "bypassDomainSecurityCheck";

    /**
     * Check if the given {@link URL} can be trusted based on the trusted domains of the wiki.
     * This method check on both the list of trusted domains given by the configuration
     * (see {@link URLConfiguration#getTrustedDomains()}) and the list of aliases used by the wiki descriptors.
     * Note that this method always returns {@code true} if {@link URLConfiguration#isTrustedDomainsEnabled()} returns
     * {@code true}. Also the method will return {@code true} whenever the {@link org.xwiki.context.ExecutionContext}
     * contains a property named {@link #BYPASS_DOMAIN_SECURITY_CHECK_CONTEXT_PROPERTY} with the value {@code "true"}.
     *
     * @param urlToCheck the URL for which we want to know if the domain is trusted or not.
     * @return {@code true} if the URL domain can be trusted or if the check is skipped, {@code false} otherwise
     */
    boolean isDomainTrusted(URL urlToCheck);

    /**
     * Check if the given URI can be trusted.
     * A URI can be trusted if:
     * <ul>
     *     <li>it's not opaque (see {@link URI} documentation for definition of opaque URI. TL;DR: a URI without
     *     {@code //} is opaque): note that following this, any URI such as {@code mailto:acme@foo.org} won't be
     *     trusted</li>
     *     <li>it refers to a specific domain and this domain is trusted (see {@link #isDomainTrusted(URL)})</li>
     *     <li>it's completely relative: it doesn't refer to an external domain</li>
     * </ul>
     *
     * @param uri the URI to check if it can be trusted or not
     * @return {@code true} only if the URI can be trusted per the criteria given in the above documentation
     * @since 14.10.4
     * @since 15.0
     */
    @Unstable
    default boolean isURITrusted(URI uri)
    {
        return false;
    }

    /**
     * Parse the given string to create a URI that is safe to use.
     * This method throws a {@link SecurityException} if the parsed URI is not safe to use according to
     * {@link #isURITrusted(URI)}. It might also throw a {@link URISyntaxException} if the parameter cannot be properly
     * parsed.
     * Note that this method might try to "repair" URI that are not parsed correctly by {@link URI#URI(String)}
     * (e.g. serialized uri containing spaces).
     *
     * @param serializedURI a string representing a URI that needs to be parsed.
     * @return a URI safe to use
     * @throws URISyntaxException if the given parameter cannot be properly parsed
     * @throws SecurityException if the parsed URI is not safe according to {@link #isURITrusted(URI)}
     * @since 14.10.4
     * @since 15.0
     */
    @Unstable
    default URI parseToSafeURI(String serializedURI) throws URISyntaxException, SecurityException
    {
        throw new SecurityException("Cannot guarantee safeness of " + serializedURI);
    }

    /**
     * Parse the given string to create a URI that is safe to use.
     * This method throws a {@link SecurityException} if the parsed URI is not safe to use according to
     * {@link #isURITrusted(URI)}. It might also throw a {@link URISyntaxException} if the parameter cannot be properly
     * parsed.
     * Note that this method might try to "repair" URI that are not parsed correctly by {@link URI#URI(String)}
     * (e.g. serialized uri containing spaces).
     *
     * @param serializedURI a string representing a URI that needs to be parsed.
     * @param requestHost the host the current request, this host will be added to the safe domains, when omitted this
     * host is extracted from the context, this parameter exists for cases where the context is not available
     * @return a URI safe to use
     * @throws URISyntaxException if the given parameter cannot be properly parsed
     * @throws SecurityException if the parsed URI is not safe according to {@link #isURITrusted(URI)}
     * @since 16.8.0
     * @since 16.4.4
     * @since 15.10.13
     */
    @Unstable
    default URI parseToSafeURI(String serializedURI, String requestHost) throws URISyntaxException, SecurityException
    {
        throw new SecurityException("Cannot guarantee that " + serializedURI + " is safe.");
    }
}
