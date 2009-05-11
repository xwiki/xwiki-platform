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
package org.xwiki.configuration.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceCollection;
import org.xwiki.configuration.internal.commons.CommonsConfigurationSource;
import org.xwiki.container.Container;

/**
 * Default list of {@link org.xwiki.configuration.ConfigurationSource}s that contain the default global XWiki
 * configuration file (xwiki.properties).
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component
public class DefaultConfigurationSourceCollection implements ConfigurationSourceCollection, Initializable
{
    private static final String XWIKI_PROPERTIES_FILE = "/WEB-INF/xwiki.properties";

    /**
     * Injected by the Component Manager.
     */
    @Requirement
    private Container container;

    private List<ConfigurationSource> sources;

    public void initialize() throws InitializationException
    {
        this.sources = new ArrayList<ConfigurationSource>();

        // Register the Commons Properties Configuration, looking for a xwiki.properties file
        // in the XWiki path somewhere.
        URL xwikiPropertiesUrl = null;
        try {
            xwikiPropertiesUrl = this.container.getApplicationContext().getResource(XWIKI_PROPERTIES_FILE);
        } catch (MalformedURLException e) {
            throw new InitializationException("Failed to locate property file [" + XWIKI_PROPERTIES_FILE + "]", e);
        }
        try {
            this.sources.add(new CommonsConfigurationSource(new PropertiesConfiguration(xwikiPropertiesUrl)));
        } catch (ConfigurationException e) {
            throw new InitializationException("Failed to load property file [" + XWIKI_PROPERTIES_FILE + "]", e);
        }
    }

    public List<ConfigurationSource> getConfigurationSources()
    {
        return Collections.unmodifiableList(this.sources);
    }
}
