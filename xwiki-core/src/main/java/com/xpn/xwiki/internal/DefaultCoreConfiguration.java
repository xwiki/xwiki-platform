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
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.CoreConfiguration;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;

/**
 * Configuration for the Core module.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@Component
public class DefaultCoreConfiguration extends AbstractLogEnabled implements CoreConfiguration
{
    /**
     * Prefix for configuration keys for the Core module.
     */
    private static final String PREFIX = "core.";

    /**
     * Defines from where to read the rendering configuration data. 
     */
    @Requirement("all")
    private ConfigurationSource configuration;

    /**
     * Used to parse the syntax specified as a String in the configuration.
     * @since 2.3M1
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * Main XWiki Properties configuration source, see {@link #getDefaultDocumentSyntax()}. 
     */
    @Requirement("xwikiproperties")
    private ConfigurationSource xwikiPropertiesConfiguration;

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     * @since 2.3M1
     */
    public Syntax getDefaultDocumentSyntax()
    {
        // If the found value is an empty string then default to the configuration value in the main configuration
        // source.
        // TODO: In the future we would need the notion of initialized/not-initialized property values in the wiki.
        // When this is implemented modify the code below.
        String key = PREFIX + "defaultDocumentSyntax";
        String syntaxId = this.configuration.getProperty(key, String.class);
        
        if (StringUtils.isEmpty(syntaxId)) {
            syntaxId = this.xwikiPropertiesConfiguration.getProperty(key, Syntax.XWIKI_2_0.toIdString());
        }

        // Try to parse the syntax and if it fails defaults to the XWiki Syntax 2.0
        Syntax syntax;
        try {
            syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxId);
        } catch (ParseException e) {
            getLogger().warn("Invalid default document Syntax [" + syntaxId + "], defaulting to ["
                + Syntax.XWIKI_2_0.toIdString() + "] instead", e);
            syntax = Syntax.XWIKI_2_0;
        }

        return syntax; 
    }
}
