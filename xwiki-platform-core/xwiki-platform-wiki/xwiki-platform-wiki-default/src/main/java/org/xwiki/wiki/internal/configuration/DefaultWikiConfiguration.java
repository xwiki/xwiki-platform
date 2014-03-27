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
package org.xwiki.wiki.internal.configuration;

import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.resource.ResourceCreationException;
import org.xwiki.url.internal.ExtendedURL;
import org.xwiki.wiki.configuration.WikiConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link WikiConfiguration}.
 *
 * @version $Id$
 * @since 5.4.4
 */
@Component
public class DefaultWikiConfiguration implements WikiConfiguration
{
    /**
     * Prefix for configuration keys for the wiki module.
     */
    private static final String PREFIX = "wiki.";

    /**
     * Defines from where to read the wiki configuration data.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public String getAliasSuffix()
    {
        String suffix = configuration.getProperty(PREFIX + "alias.suffix");
        if (suffix == null) {
            // Try to deduce it from the request URL.
            suffix = deduceAliasSuffixFromRequest();
            // If it has failed, then we return an empty suffix
            if (suffix == null) {
                return "";
            }
        }
        return suffix;
    }

    private String deduceAliasSuffixFromRequest()
    {
        String suffix = null;
        String host = getHost();
        // We cannot deduce a suffix with a request to "locahost" or to an IP.
        if (host != null && !"localhost".equals(host) && !host.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")) {
            suffix = StringUtils.substringAfter(host, ".");
        }
        return suffix;
    }

    private String getHost()
    {
        String host = null;
        final String error = "Failed to get the host name of the request.";
        try {
            XWikiContext xcontext = xcontextProvider.get();
            ExtendedURL url = new ExtendedURL(new java.net.URL(xcontext.getRequest().getRequestURL().toString()),
                    "xwiki");
            host = url.getURI().getHost();
        } catch (ResourceCreationException e) {
            logger.error(error, e);
        } catch (MalformedURLException e) {
            logger.error(error, e);
        }
        return host;
    }
}
