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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.url.internal.standard.StandardURLConfiguration;

/**
 * Prefixes the passed Extended URL with the webapp's Servlet context and the Struts "action" servlet, usually mapped as
 * {@code /bin}. For example {@code /some/path} would be normalized into {@code /xwiki/bin/some/path} if the webapp's
 * context was {@code xwiki} and the main mapping for the action servlet is {@code /bin}.
 *
 * @version $Id$
 * @since 7.4M1
 */
@Component
@Named("contextpath+actionservletpath")
@Singleton
public class ContextAndActionURLNormalizer implements URLNormalizer<ExtendedURL>, Initializable
{
    /** This will be removed from the context and servlet paths. */
    private static final String URL_SEGMENT_DELIMITER = "/";

    /** These will be removed from the configured action servlet mappings. */
    private static final String IGNORED_MAPPING_CHARACTERS = "/*";

    /** The old configuration, where the context path can be configured. */
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    /** Provides access to the current request, if any. */
    @Inject
    private Container container;

    /** Provides access to the application context configuration. */
    @Inject
    private Environment environment;

    /** The new configuration for URLs. */
    @Inject
    private StandardURLConfiguration urlConfiguration;

    /** The default mapping for the action servlet. */
    private String defaultServletMapping;

    /**
     * Valid mappings for the action servlet. If a request doesn't use one of these (for example a REST request), then
     * the default mapping will be used.
     */
    private Collection<String> validServletMappings = new HashSet<>();

    /** The mapping used for virtual wikis in path-based wiki access. */
    private String virtualWikiServletMapping;

    @Override
    public void initialize()
    {
        this.virtualWikiServletMapping = this.urlConfiguration.getWikiPathPrefix();
        this.defaultServletMapping = this.urlConfiguration.getEntityPathPrefix();
        this.validServletMappings.add(this.defaultServletMapping);
        if (this.environment instanceof ServletEnvironment) {
            for (String mapping : ((ServletEnvironment) this.environment).getServletContext()
                .getServletRegistration("action").getMappings()) {
                this.validServletMappings.add(StringUtils.strip(mapping, IGNORED_MAPPING_CHARACTERS));
            }
        }
    }

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

        List<String> servletPath = getActionServletMapping();
        for (String segment : servletPath) {
            if (StringUtils.isNotEmpty(segment)) {
                segments.add(segment);
            }
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

    /**
     * Get the path prefix used for the Struts Action Servlet, either a prefix similar to the one used in the current
     * request if it also passes through the Action servlet, or using the default path configured for it. In case the
     * current request is for a virtual wiki identified through the path, the return value also includes the wiki
     * identifier.
     *
     * @return a list of segments containing the path used for triggering the Struts Action Servlet (may be the empty
     *         string), and optionally a wiki identifier if the first segment corresponds to virtual wiki access
     */
    private List<String> getActionServletMapping()
    {
        String result = this.defaultServletMapping;
        if (this.container.getRequest() instanceof ServletRequest) {
            HttpServletRequest hsRequest = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
            result = StringUtils.strip(hsRequest.getServletPath(), IGNORED_MAPPING_CHARACTERS);

            if (StringUtils.equals(this.virtualWikiServletMapping, result)) {
                // Virtual wiki, also include the wiki identifier
                return Arrays.asList(this.virtualWikiServletMapping,
                    StringUtils.substringBetween(hsRequest.getPathInfo(), URL_SEGMENT_DELIMITER));
            }

            if (!this.validServletMappings.contains(result)) {
                // The current request doesn't pass through the Action servlet, don't reuse the path prefix
                result = this.defaultServletMapping;
            }
        }

        return Collections.singletonList(result);
    }
}
