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
package org.xwiki.url.internal.container;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import com.xpn.xwiki.XWikiContext;

/**
 * Prefixes the passed Extended URL with the webapp's Servlet context. For example {@code /some/path} would be
 * normalized into {@code /xwiki/some/path} if the webapp's context was {@code xwiki}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("contextpath")
@Singleton
public class ExtendedURLURLNormalizer implements URLNormalizer<ExtendedURL>
{
    private static final String URL_SEGMENT_DELIMITER = "/";

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @Inject
    private Container container;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public ExtendedURL normalize(ExtendedURL partialURL)
    {
        String contextPath = getConfiguredContextPath();

        // If the context path is not configured, extract it from the current request.
        if (contextPath == null) {
            contextPath = getRequestContextPath();

            // If there's no request (e.g. the code is executed by a daemon thread), extract the context path from the
            // URL specified by the current execution context.
            // TODO: Instead of trying to extract from the URL, save the context path at webapp init (using a
            // ServlettContextListener for example).
            if (contextPath == null) {
                contextPath = getExecutionContextPath();

                if (contextPath == null) {
                    throw new RuntimeException(String.format("Failed to normalize the URL [%s] since the "
                        + "application's Servlet context couldn't be computed.", partialURL));
                }
            }
        }

        // Remove any leading or trailing slashes.
        contextPath = StringUtils.strip(contextPath, URL_SEGMENT_DELIMITER);

        List<String> segments = new ArrayList<>();
        segments.add(contextPath);
        segments.addAll(partialURL.getSegments());

        return new ExtendedURL(segments, partialURL.getParameters());
    }

    private String getConfiguredContextPath()
    {
        return this.configurationSource.getProperty("xwiki.webapppath");
    }

    private String getRequestContextPath()
    {
        Request request = this.container.getRequest();
        if (request instanceof ServletRequest) {
            return ((ServletRequest) request).getHttpServletRequest().getContextPath();
        }
        return null;
    }

    private String getExecutionContextPath()
    {
        URL currentURL = this.xcontextProvider.get().getURL();
        if (currentURL != null) {
            // Extract the context path by getting the first path segment.
            return StringUtils.substringBefore(StringUtils.stripStart(currentURL.getPath(), URL_SEGMENT_DELIMITER),
                URL_SEGMENT_DELIMITER);
        }
        return null;
    }
}
