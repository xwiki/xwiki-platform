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
import java.util.HashSet;
import java.util.Set;

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

    private void computeTrustedDomains()
    {
        this.trustedDomains = new HashSet<>(this.urlConfiguration.getTrustedDomains());

        try {
            for (WikiDescriptor wikiDescriptor : wikiDescriptorManager.getAll()) {
                this.trustedDomains.addAll(wikiDescriptor.getAliases());
            }
        } catch (WikiManagerException e) {
            logger.warn("Error while getting wiki descriptor to fill list of trusted domains: [{}]. "
                + "The subwikis won't be taken into account for the list of trusted domains.",
                ExceptionUtils.getRootCauseMessage(e));
        }
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
            if (this.trustedDomains == null) {
                computeTrustedDomains();
            }

            this.trustedDomains.add(this.getCurrentDomain());
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

        // An opaque URI is defined with a scheme but without / i.e. http:xwiki.org
        // We consider those URLs as untrusted even if they are parsed by browsers, as they are not parsed by URI
        // and we cannot properly check them.
        if (uri.isOpaque()) {
            result = false;
        } else if (uri.getAuthority() != null) {
            // If the URI has an authority it means a domain has been specified and we should check it.
            // Note that the URI might not be absolute, as it might not have a scheme
            // (e.g. //domain.org is a relative URI with an authority)
            try {
                URI uriToTransform;
                // We systematically put a http scheme if the scheme is missing, as it's how the browser would resolve
                // it.
                if (!uri.isAbsolute()) {
                    uriToTransform = new URI("http", uri.getRawAuthority(), uri.getRawPath(), uri.getRawQuery(),
                        uri.getRawFragment());
                } else {
                    uriToTransform = uri;
                }

                result = this.isDomainTrusted(uriToTransform.toURL());
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
}
