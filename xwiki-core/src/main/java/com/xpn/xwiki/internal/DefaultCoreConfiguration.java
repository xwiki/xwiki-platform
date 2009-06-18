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
package com.xpn.xwiki.internal;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;

/**
 * Configuration for the Core module.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@Component
public class DefaultCoreConfiguration implements CoreConfiguration
{
    /**
     * Prefix for configuration keys for the Core module.
     */
    private static final String PREFIX = "core.";

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    private static final String DEFAULT_DEFAULT_DOCUMENT_SYNTAX = "xwiki/2.0";

    /**
     * Defines from where to read the rendering configuration data. 
     */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     * <p>
     * FIXME: need to refactor the configuration loading to be able to get wiki preference for a ConfigurationSource
     */
    @Requirement
    private Execution execution;

    /**
     * FIXME: need to refactor the configuration loading to be able to get wiki preference for a ConfigurationSource
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * FIXME: need to refactor the configuration loading to be able to get wiki preference for a ConfigurationSource
     */
    private String getWikiConfigurationAsString(String configName)
    {
        return getContext().getWiki().getXWikiPreference(configName, null, getContext());
    }

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    public String getDefaultDocumentSyntax()
    {
        // FIXME: need to refactor the configuration loading to be able to get wiki preference for a ConfigurationSource
        String defaultDocumentSyntax = getWikiConfigurationAsString("core.defaultDocumentSyntax");

        if (StringUtils.isEmpty(defaultDocumentSyntax)) {
            defaultDocumentSyntax = this.configuration.getProperty(PREFIX + "defaultDocumentSyntax", 
                DEFAULT_DEFAULT_DOCUMENT_SYNTAX);
        }

        return defaultDocumentSyntax;
    }
}
