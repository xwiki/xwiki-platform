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
 * Composite Configuration Source that looks in the following sources in that order:
 * <ul>
 * <li>current user preferences wiki page</li>
 * <li>current space preferences wiki page and all its parent spaces preferences pages</li>
 * <li>current wiki preferences wiki page</li>
 * <li>xwiki properties file (xwiki.properties)</li>
 * </ul>
 * Should be used when a configuration can be overridden by the user in his/her profile.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Named("all")
@Singleton
public class AllConfigurationSource extends CompositeConfigurationSource implements Initializable
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Inject
    @Named("documents")
    private ConfigurationSource documentsPreferencesSource;

    @Inject
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @Override
    public void initialize() throws InitializationException
    {
        // First source is searched first when a property value is requested.
        addConfigurationSource(this.userPreferencesSource);
        addConfigurationSource(this.documentsPreferencesSource);
        addConfigurationSource(this.xwikiPropertiesSource);
    }
}
