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
package org.xwiki.url.internal.standard;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.url.internal.standard.URLFormatConfiguration;

@Component
public class StandardURLFormatConfiguration implements URLFormatConfiguration
{
    /**
     * Prefix for configuration keys for the Core module.
     */
    private static final String PREFIX = "url.default.";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Requirement("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     * @see org.xwiki.url.internal.standard.URLFormatConfiguration#isPathBasedMultiWikiFormat()
     */
    public boolean isPathBasedMultiWikiFormat()
    {
        // Note: the id corresponds to the component hint for the WikiReferenceResolver component role.
        return this.configuration.getProperty(PREFIX + "pathBasedMultiWiki", Boolean.FALSE);
    }
}
