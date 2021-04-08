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
package org.xwiki.refactoring.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.refactoring.RefactoringConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link RefactoringConfiguration}.
 * Search for the value of the configuration keys in the following order:
 * <ul>
 *   <li>In {@code Refactoring.Code.RefactoringConfiguration} in the current wiki</li>
 *   <li>In {@code Refactoring.Code.RefactoringConfiguration} in the main wiki</li>
 *   <li>In {@code xwiki.properties} (prefixed with {@code refactoring.}</li>
 * </ul>
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
public class DefaultRefactoringConfiguration implements RefactoringConfiguration
{
    private static final String IS_RECYCLE_BIN_SKIPPING_ACTIVATED_PROPERTY = "isRecycleBinSkippingActivated";

    private static final String PREFIX = "refactoring.";

    @Inject
    @Named("refactoring")
    private ConfigurationSource currentWikiConfigurationSource;

    @Inject
    @Named("refactoringmainwiki")
    private ConfigurationSource mainWikiConfigurationSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public boolean isRecycleBinSkippingActivated()
    {
        return defaultPropertyAccess(IS_RECYCLE_BIN_SKIPPING_ACTIVATED_PROPERTY, false);
    }

    /**
     * Look for the property hierarchically.
     *
     * The value of the property is looked for in the following places, from the first to the last.
     * <ul>
     *     <li>In the {@code Refactoring.Code.RefactoringConfigurationClass} instance of the
     *     {@code Refactoring.Code.RefactoringConfiguration} object in the current wiki.</li>
     *     <li>In the {@code Refactoring.Code.RefactoringConfigurationClass} instance of the
     *     {@code Refactoring.Code.RefactoringConfiguration} object in the main wiki wiki.</li>
     *     <li>In the wiki properties (the property is prefixed with {@code refactoring.}</li>
     * </ul>
     * The search stops when a value is found.
     * If no value is found, the default value is used.
     *
     * @param property the property name
     * @param defaultValue the default value, used if no property declaration is found in the hierarchy
     * @param <T> the type of the property
     * @return the value of the property
     */
    private <T> T defaultPropertyAccess(String property, T defaultValue)
    {
        T ret = (T) this.currentWikiConfigurationSource.getProperty(property, defaultValue.getClass());
        if (ret == null && !isMainWiki()) {
            ret = (T) this.mainWikiConfigurationSource.getProperty(property, defaultValue.getClass());
        }

        if (ret == null) {
            ret = this.xwikiPropertiesSource.getProperty(PREFIX + property, defaultValue);
        }
        return ret;
    }

    private boolean isMainWiki()
    {
        return this.wikiDescriptorManager.isMainWiki(this.wikiDescriptorManager.getCurrentWikiId());
    }
}
