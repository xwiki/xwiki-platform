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
package org.xwiki.display.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default {@link DisplayConfiguration} implementation.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Singleton
public class DefaultDisplayConfiguration implements DisplayConfiguration
{
    /**
     * Prefix for configuration keys for the display module.
     */
    private static final String PREFIX = "display.";

    /**
     * The configuration source.
     */
    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public String getDocumentDisplayerHint()
    {
        // TODO: Find a better/cleaner way to inject the "sheet" document displayer by default. The display module
        // shouldn't be aware of the sheet module.
        return configurationSource.getProperty(PREFIX + "documentDisplayerHint", "sheet");
    }

    @Override
    public int getTitleHeadingDepth()
    {
        return configurationSource.getProperty(PREFIX + "titleHeadingDepth", 2);
    }
}
