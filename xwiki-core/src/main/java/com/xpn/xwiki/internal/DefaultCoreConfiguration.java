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

import com.xpn.xwiki.CoreConfiguration;

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
    @Requirement("all")
    private ConfigurationSource configuration;

    /**
     * Main XWiki Properties configuration source, see {@link #getDefaultDocumentSyntax()}. 
     */
    @Requirement("xwikiproperties")
    private ConfigurationSource xwikiPropertiesConfiguration;

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    public String getDefaultDocumentSyntax()
    {
        // If the found value is an empty string then default to the configuration value in the main configuration
        // source.
        // TODO: In the future we would need the notion of initialized/not-initialized property values in the wiki.
        // When this is implemented modify the code below.
        String key = PREFIX + "defaultDocumentSyntax";
        String value = this.configuration.getProperty(key, String.class); 
        
        if (StringUtils.isEmpty(value)) {
            value = this.xwikiPropertiesConfiguration.getProperty(key, DEFAULT_DEFAULT_DOCUMENT_SYNTAX);
        }
            
        return value; 
    }
}
