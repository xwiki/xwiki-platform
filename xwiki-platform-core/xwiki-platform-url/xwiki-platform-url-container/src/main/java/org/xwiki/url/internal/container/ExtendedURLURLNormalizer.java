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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

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

    /** Provides access to the application context configuration. */
    @Inject
    private Environment environment;

    @Override
    public ExtendedURL normalize(ExtendedURL partialURL)
    {
        String contextPath = StringUtils.strip(getContextPath(), URL_SEGMENT_DELIMITER);

        if (contextPath == null) {
            throw new RuntimeException(String.format("Failed to normalize the URL [%s] since the "
                + "application's Servlet context couldn't be computed.", partialURL));
        }

        List<String> segments = new ArrayList<>();
        if (StringUtils.isNotEmpty(contextPath)) {
            segments.add(contextPath);
        }
        segments.addAll(partialURL.getSegments());

        return new ExtendedURL(segments, partialURL.getParameters());
    }

    private String getContextPath()
    {
        String contextPath = getContextPathFromConfiguration();

        // If the context path is not configured, try to extract it from the application context
        if (contextPath == null) {
            contextPath = getContextPathFromApplicationContext();
        }

        return contextPath;
    }

    /**
     * Look in the XWiki configuration for a hard-coded value. Currently, this is specified using the
     * {@code xwiki.webapppath} setting in {@code xwiki.cfg}.
     *
     * @return the value specified in the settings, or {@code null} if not specified
     */
    private String getContextPathFromConfiguration()
    {
        return this.configurationSource.getProperty("xwiki.webapppath");
    }

    /**
     * Look in the application context, if there is such a context.
     *
     * @return the context path taken from the application context, or {@code null} if this isn't running in a servlet
     *         environment
     */
    private String getContextPathFromApplicationContext()
    {
        if (this.environment instanceof ServletEnvironment) {
            return ((ServletEnvironment) this.environment).getServletContext().getContextPath();
        }
        return null;
    }
}
