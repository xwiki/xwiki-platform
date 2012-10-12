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
package org.xwiki.search.solr.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

/**
 * Embedded Solr instance running in the same JVM.
 * 
 * @version $Id$
 */
@Component
@Named("embedded")
@InstantiationStrategy(ComponentInstantiationStrategy.SINGLETON)
public class EmbeddedSolrInstance extends AbstractSolrInstance
{
    /**
     * SOLR HOME KEY.
     */
    public static final String SOLR_HOME_KEY = "solr.solr.home";

    /**
     * Default directory name for Solr's configuration and index files.
     */
    public static final String DEFAULT_SOLR_DIRECTORY_NAME = "solr";

    /**
     * Properties.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * Environment used to get the xwiki permanent directory.
     */
    @Inject
    private Environment environment;

    @Override
    public void initialize() throws InitializationException
    {
        String solrHome = determineHomeDirectory();
        try {
            // Start embedded Solr server.
            logger.info("Starting embedded Solr server...");
            System.setProperty(SOLR_HOME_KEY, solrHome);
            logger.info("Using Solr home directory: {}", solrHome);

            // Initialize the SOLR backend using an embedded server.
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            CoreContainer initializedContainer = initializer.initialize();

            container = initializedContainer;
            server = new EmbeddedSolrServer(container, "");

            logger.info("Started embedded Solr server.");
        } catch (Exception e) {
            throw new InitializationException(String.format(
                "Failed to initialize the solr embedded server with home directory set to '%s'", solrHome), e);
        }
    }

    /**
     * @return the home directory determined from the {@value #SOLR_HOME_KEY} system property, configuration or default
     *         location (in that order).
     */
    private String determineHomeDirectory()
    {
        if (StringUtils.isNotBlank(System.getProperty(SOLR_HOME_KEY))) {
            return System.getProperty(SOLR_HOME_KEY);
        }

        String defaultValue = getDefaultHomeDirectory();

        return configuration.getProperty("search.solr.home", defaultValue);
    }

    /**
     * @return the default home directory located inside the environment's permanent directory.
     */
    String getDefaultHomeDirectory()
    {
        String result = new File(environment.getPermanentDirectory(), DEFAULT_SOLR_DIRECTORY_NAME).getPath();

        return result;
    }
}
