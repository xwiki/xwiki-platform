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
package org.xwiki.url.internal.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.ModelContext;
import org.xwiki.url.ExtendedURL;

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
public class ContextAndActionURLNormalizer extends org.xwiki.url.internal.container.ContextAndActionURLNormalizer
    implements Initializable
{
    /** This will be removed from the context and servlet paths. */
    private static final String URL_SEGMENT_DELIMITER = "/";

    /** The old configuration, where the context path can be configured. */
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    /** The new configuration for URLs. */
    @Inject
    private StandardURLConfiguration urlConfiguration;

    @Inject
    private ModelContext context;

    /** The mapping used for virtual wikis in path-based wiki access. */
    private String virtualWikiServletMapping;

    @Override
    public void initialize()
    {
        super.initialize();
        this.virtualWikiServletMapping = this.urlConfiguration.getWikiPathPrefix();
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

        List<String> servletPath = getActionAndWikiServletMapping();
        for (String segment : servletPath) {
            if (StringUtils.isNotEmpty(segment)) {
                segments.add(segment);
            }
        }

        segments.addAll(partialURL.getSegments());

        return new ExtendedURL(segments, partialURL.getParameters());
    }

    @Override
    protected String getContextPath()
    {
        // Look in the XWiki configuration for a hard-coded value. Currently, this is specified using the
        // {@code xwiki.webapppath} setting in {@code xwiki.cfg}.
        String contextPath = this.configurationSource.getProperty("xwiki.webapppath");

        // If the context path is not configured, try to extract it from the application context
        if (contextPath == null) {
            contextPath = super.getContextPath();
        }

        return contextPath;
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
    protected List<String> getActionAndWikiServletMapping()
    {
        String result = super.getActionServletMapping();
        if (Strings.CS.equals(this.virtualWikiServletMapping, result)) {
            // Virtual wiki, also include the wiki identifier
            return Arrays.asList(this.virtualWikiServletMapping,
                this.context.getCurrentEntityReference().getRoot().getName());
        }

        return Collections.singletonList(result);
    }
}
