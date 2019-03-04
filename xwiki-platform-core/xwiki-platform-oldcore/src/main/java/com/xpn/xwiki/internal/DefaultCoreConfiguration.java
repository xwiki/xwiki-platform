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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.CoreConfiguration;

/**
 * Configuration for the Core module.
 *
 * @version $Id$
 * @since 1.8RC2
 */
@Component
@Singleton
public class DefaultCoreConfiguration implements CoreConfiguration
{
    /**
     * Prefix for configuration keys for the Core module.
     */
    private static final String PREFIX = "core.";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    @Named("all")
    private ConfigurationSource configuration;

    /**
     * Main XWiki Properties configuration source, see {@link #getDefaultDocumentSyntax()}.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesConfiguration;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     * @since 2.3M1
     * @deprecated starting with 11.0, use
     *     {@link org.xwiki.rendering.configuration.ExtendedRenderingConfiguration#getDefaultContentSyntax()}
     */
    @Override
    @Deprecated
    public Syntax getDefaultDocumentSyntax()
    {
        // If the found value is an empty string then default to the configuration value in the main configuration
        // source.
        // TODO: In the future we would need the notion of initialized/not-initialized property values in the wiki.
        // When this is implemented modify the code below.
        String key = PREFIX + "defaultDocumentSyntax";
        String syntaxId = this.configuration.getProperty(key, String.class);

        if (StringUtils.isEmpty(syntaxId)) {
            syntaxId = this.xwikiPropertiesConfiguration.getProperty(key, Syntax.XWIKI_2_1.toIdString());
        }

        // Try to parse the syntax and if it fails defaults to the XWiki Syntax 2.1
        Syntax syntax;
        try {
            syntax = Syntax.valueOf(syntaxId);
        } catch (ParseException e) {
            this.logger.warn("Invalid default document Syntax [{}], defaulting to [{}] instead", syntaxId,
                Syntax.XWIKI_2_1.toIdString(),  e);
            syntax = Syntax.XWIKI_2_1;
        }

        return syntax;
    }
}
