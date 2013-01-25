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

import java.net.URL;
import java.util.List;
import java.util.Map;

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
     * Each language has its particularities and needs to be handled specially in order to index and retrieve the best
     * results.
     * 
     * @return the list of supported language codes for which optimized indexing can be performed (using
     *         language-code-suffixed fields that are analyzed and queries with the analysis chain specific to the
     *         language).
     * @see #getOptimizedLanguages() for the selected languages.
     */
    List<String> getOptimizableLanguages();

    /**
     * Each language has its particularities and needs to be handled specially in order to index and retrieve the best
     * results.
     * 
     * @return the list of language codes for which to perform optimized indexing (using language-code-suffixed fields
     *         that are analyzed and queries with the analysis chain specific to the language).
     * @see #getOptimizableLanguages() for the full list of supported languages.
     */
    List<String> getOptimizedLanguages();

    /**
     * @return the list of multilingual fields for which to apply optimized indexing.
     */
    List<String> getMultilingualFields();

    /**
     * Retrieves the the configuration files required by the Solr instance's home directory in order to initialize.
     * 
     * @return a map of (fileName, fileURL) to be used to initialize the Solr instance.
     */
    Map<String, URL> getHomeDirectoryConfiguration();
}
