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
package org.xwiki.configuration.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Composite Restricted Configuration Source that looks in the following sources in that order:
 * <ul>
 * <li>xwiki properties file (xwiki.properties)</li>
 * </ul>
 * If you need to use a Configuration source that can find its configuration in the wiki you should consider using
 * either {@link DefaultConfigurationSource} or {@link AllConfigurationSource}.
 *
 * @version $Id$
 * @since 4.1M2
 */
@Component
@Named("restricted")
@Singleton
public class RestrictedConfigurationSource extends CompositeConfigurationSource implements Initializable
{
    /**
     * @see XWikiPropertiesConfigurationSource
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public void initialize() throws InitializationException
    {
        // First source is looked first when a property value is requested.
        addConfigurationSource(this.xwikiPropertiesSource);
    }
}
