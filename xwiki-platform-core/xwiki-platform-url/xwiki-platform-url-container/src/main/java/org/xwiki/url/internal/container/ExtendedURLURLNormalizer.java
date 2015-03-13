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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.url.URLNormalizer;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Prefixes the passed Extended URL with the webapp's Servlet context. For example {@code /some/path} would
 * be normalized into {@code /xwiki/some/path} if the webapp's context was {@code xwiki}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class ExtendedURLURLNormalizer implements URLNormalizer<ExtendedURL>
{
    private static final String URL_SEGMENT_DELIMITER = "/";

    @Inject
    private Execution execution;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @Override
    public ExtendedURL normalize(ExtendedURL partialURL)
    {
        String contextPath = this.configurationSource.getProperty("xwiki.webapppath");

        // If not defined by the user, extract it from the current Request
        if (contextPath == null) {
            XWikiContext context = getXWikiContext();

            // Check if there's a Request and if not, extract the webapp context from the URL in the execution context
            XWikiRequest request = context.getRequest();
            if (request != null) {
                contextPath = request.getContextPath();
            } else {
                URL currentURL = context.getURL();
                if (currentURL != null) {
                    String path = currentURL.getPath();
                    contextPath = StringUtils.substringBetween(path, URL_SEGMENT_DELIMITER);
                } else {
                    throw new RuntimeException(String.format("Failed to normalize the URL [%s] since the "
                        + "application's Servlet context couldn't be computed.", partialURL));
                }
            }
        } else {
            // Remove any leading or trailing slashes that would have been put by error by the user
            contextPath = StringUtils.strip(contextPath, URL_SEGMENT_DELIMITER);
        }

        List<String> segments = new ArrayList<>();
        segments.add(contextPath);
        segments.addAll(partialURL.getSegments());

        return new ExtendedURL(segments);
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
