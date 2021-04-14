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
@Unstable
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
}
