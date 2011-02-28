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
package org.xwiki.xml.html;

import java.io.Reader;

import org.w3c.dom.Document;
import org.xwiki.component.annotation.ComponentRole;

/**
 * Transforms any HTML content into valid XHTML that can be fed to the XHTML Parser for example.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@ComponentRole
public interface HTMLCleaner
{
    /**
     * Transforms any HTML content into valid XHTML that can be fed to the XHTML Parser for example.
     * A default configuration is applied for cleaning the original HTML (see {@link #getDefaultConfiguration()}).
     * 
     * @param originalHtmlContent the original content (HTML) to clean
     * @return the cleaned HTML as a w3c DOM (this allows further transformations if needed)
     */
    Document clean(Reader originalHtmlContent);

    /**
     * Transforms any HTML content into valid XHTML. A specific cleaning configuration can be passed to control
     * the cleaning process.
     * 
     * @param originalHtmlContent the original HTML content to be cleaned.
     * @param configuration the configuration to use for cleaning the HTML content
     * @return the cleaned HTML as a w3c DOM
     * @since 1.8.1
     */
    Document clean(Reader originalHtmlContent, HTMLCleanerConfiguration configuration);

    /**
     * Allows getting the default configuration that will be used thus allowing the user to configure it like 
     * adding some more filters before or after or even remove some filters to completely control what filters will
     * be executed. This is to be used for very specific use cases. In the majority of cases you should instead use
     * the clean API that doesn't require passing a configuration.
     * 
     * @return the default configuration that will be used to clean the original HTML
     * @since 1.8.1
     */
    HTMLCleanerConfiguration getDefaultConfiguration();
}
