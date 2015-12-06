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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

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

    /** Provides access to the current request, if any. */
    @Inject
    private Container container;

    /** Provides access to the application context configuration. */
    @Inject
    private Environment environment;

    /** The default mapping for the action servlet. */
    private String defaultServletMapping = "bin";

    /**
     * Valid mappings for the action servlet. If a request doesn't use one of these (for example a REST request), then
     * the default mapping will be used.
     */
    private Collection<String> validServletMappings = new LinkedHashSet<>();

    @Override
    public void initialize()
    {
        if (this.environment instanceof ServletEnvironment) {
            for (String mapping : ((ServletEnvironment) this.environment).getServletContext()
                .getServletRegistration("action").getMappings()) {
                this.validServletMappings.add(StringUtils.strip(mapping, IGNORED_MAPPING_CHARACTERS));
            }
        }
        if (!this.validServletMappings.isEmpty()) {
            this.defaultServletMapping = this.validServletMappings.iterator().next();
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

        String servletPath = getActionServletMapping();
        if (StringUtils.isNotEmpty(servletPath)) {
            segments.add(servletPath);
        }

        segments.addAll(partialURL.getSegments());

        return new ExtendedURL(segments, partialURL.getParameters());
    }

    protected String getContextPath()
    {
        if (this.environment instanceof ServletEnvironment) {
            return ((ServletEnvironment) this.environment).getServletContext().getContextPath();
        }
        return null;
    }

    /**
     * Get the path prefix used for the Struts Action Servlet, either a prefix similar to the one used in the current
     * request if it also passes through the Action servlet, or using the default path configured for it.
     *
     * @return a path used for triggering the Struts Action Servlet (may be the empty string)
     */
    protected String getActionServletMapping()
    {
        String result = this.defaultServletMapping;
        if (this.container.getRequest() instanceof ServletRequest) {
            HttpServletRequest hsRequest = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
            result = StringUtils.strip(hsRequest.getServletPath(), IGNORED_MAPPING_CHARACTERS);

            if (!this.validServletMappings.contains(result)) {
                // The current request doesn't pass through the Action servlet, don't reuse the path prefix
                result = this.defaultServletMapping;
            }
        }

        return result;
    }
}
