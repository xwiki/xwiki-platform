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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;
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
public class DefaultCoreConfiguration implements Initializable, CoreConfiguration
{
    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    private String defaultDocumentSyntax = "xwiki/2.0";

    /**
     * Injected by the Component Manager.
     */
    @Requirement
    private ConfigurationManager configurationManager;

    /**
     * Injected by the Component Manager.
     */
    @Requirement
    private ConfigurationSourceCollection sourceCollection;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     * <p>
     * FIXME: need to refactor the configuration loading to be able to get wiki preference for a ConfigurationSource
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.configurationManager
            .initializeConfiguration(this, this.sourceCollection.getConfigurationSources(), "core");
    }

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
            defaultDocumentSyntax = this.defaultDocumentSyntax;
        }

        return defaultDocumentSyntax;
    }

    public void setDefaultDocumentSyntax(String defaultDocumentSyntax)
    {
        this.defaultDocumentSyntax = defaultDocumentSyntax;
    }
}
