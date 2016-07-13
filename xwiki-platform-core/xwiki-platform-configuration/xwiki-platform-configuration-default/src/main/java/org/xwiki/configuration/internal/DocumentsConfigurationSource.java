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
 * Composite Configuration Source that looks in the following Document sources (configuration in wiki pages) in that
 * order:
 * <ul>
 * <li>space preferences wiki page</li>
 * <li>wiki preferences wiki page</li>
 * </ul>
 * Should be used when you need to look for configuration properties only in Documents (wiki pages).
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("documents")
@Singleton
public class DocumentsConfigurationSource extends CompositeConfigurationSource implements Initializable
{
    @Inject
    @Named("wiki")
    private ConfigurationSource wikiPreferencesSource;

    @Inject
    @Named("spaces")
    private ConfigurationSource spacesPreferencesSource;

    @Override
    public void initialize() throws InitializationException
    {
        // First source is looked up first when a property value is requested.
        addConfigurationSource(this.spacesPreferencesSource);
        addConfigurationSource(this.wikiPreferencesSource);
    }
}
