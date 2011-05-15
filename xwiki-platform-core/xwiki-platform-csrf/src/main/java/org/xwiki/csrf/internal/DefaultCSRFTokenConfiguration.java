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
package org.xwiki.csrf.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.csrf.CSRFTokenConfiguration;

/**
 * Get configuration data from the XWiki properties file. Supported options:
 * <ul>
 *   <li>"csrf.enabled", default = false</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
public class DefaultCSRFTokenConfiguration implements CSRFTokenConfiguration
{
    /** Prefix for the configuration keys for the {@link org.xwiki.csrf.CSRFToken} component. */
    private static final String PREFIX = "csrf.";

    /** Main XWiki properties configuration source. */
    @Requirement("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        String key = PREFIX + "enabled";
        return this.configuration.getProperty(key, Boolean.FALSE).booleanValue();
    }
}
