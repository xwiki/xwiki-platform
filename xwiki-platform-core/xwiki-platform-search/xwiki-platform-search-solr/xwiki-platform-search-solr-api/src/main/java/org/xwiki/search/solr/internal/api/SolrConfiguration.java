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
package org.xwiki.search.solr.internal.api;

import java.io.InputStream;

import org.xwiki.component.annotation.Role;

/**
 * Provides configuration for Solr.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Role
public interface SolrConfiguration
{
    /**
     * @return the type of Solr server used. Supported values: "embedded" or "remote".
     */
    String getServerType();

    /**
     * @param instanceType the instance type value for which configuration is requested.
     * @param propertyName the name of the configuration property that is requested.
     * @param defaultValue the default value to return if the requested value is not set.
     * @return the instance specific configuration value.
     * @param <T> the type of the default parameter.
     */
    <T> T getInstanceConfiguration(String instanceType, String propertyName, T defaultValue);

    /**
     * Retrieves the configuration files required by the Solr instance's home directory in order to initialize.
     * 
     * @return a map of (fileName, fileURL) to be used to initialize the Solr instance.
     */
    InputStream getHomeDirectoryConfiguration();

    // Indexer

    /**
     * @return the size of the batch when indexing enqueued entities
     * @since 5.1M2
     */
    int getIndexerBatchSize();

    /**
     * @return the size of the batch when indexing enqueued entities
     * @since 5.1M2
     */
    int getIndexerBatchMaxLengh();

    /**
     * @return the maximum size of the indexer queue
     * @since 5.1M2
     */
    int getIndexerQueueCapacity();

    /**
     * @return true if a full synchronization job between the database and SOLR index should be run when XWiki starts
     * @since 6.1M2
     */
    boolean synchronizeAtStartup();
}
