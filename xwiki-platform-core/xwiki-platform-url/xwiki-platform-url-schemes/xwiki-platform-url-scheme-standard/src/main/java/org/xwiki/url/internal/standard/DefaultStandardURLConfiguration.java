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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Default implementation reading data from the {@code xwiki.properties} file.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultStandardURLConfiguration implements StandardURLConfiguration
{
    /**
     * Prefix for configuration keys for the Resource module.
     */
    private static final String PREFIX = "url.standard.";

    /**
     * Defines from where to read the Resource configuration data.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public boolean isPathBasedMultiWiki()
    {
        return isPathBasedMultiWiki(Boolean.TRUE);
    }

    /**
     * Makes it easy for this class to be extended.
     *
     * @param defaultValue the default value to use if the key is not found in the configuration
     * @return see {@link #isPathBasedMultiWiki()}
     */
    protected boolean isPathBasedMultiWiki(Boolean defaultValue)
    {
        return this.configuration.getProperty(PREFIX + "multiwiki.isPathBased", defaultValue);
    }

    @Override
    public String getWikiPathPrefix()
    {
        return getWikiPathPrefix("wiki");
    }

    @Override
    public WikiNotFoundBehavior getWikiNotFoundBehavior()
    {
        return getWikiNotFoundBehavior(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
    }

    @Override
    public String getEntityPathPrefix()
    {
        return getEntityPathPrefix("bin");
    }

    @Override
    public boolean isViewActionHidden()
    {
        return isViewActionHidden(false);
    }

    /**
     * Makes it easy for this class to be extended.
     *
     * @param defaultValue the default value to use if the key is not found in the configuration
     * @return see {@link #getWikiPathPrefix()}
     */
    protected String getWikiPathPrefix(String defaultValue)
    {
        return this.configuration.getProperty(PREFIX + "multiwiki.wikiPathPrefix", defaultValue);
    }

    /**
     * Makes it easy for this class to be extended.
     *
     * @param defaultValue the default value to use if the key is not found in the configuration
     * @return see {@link #isViewActionHidden()} ()}
     */
    protected boolean isViewActionHidden(boolean defaultValue)
    {
        return this.configuration.getProperty(PREFIX + "hideViewAction", defaultValue);
    }

    /**
     * Makes it easy for this class to be extended.
     *
     * @param defaultValue the default value to use if the key is not found in the configuration
     * @return see {@link #getEntityPathPrefix()} ()}
     */
    protected String getEntityPathPrefix(String defaultValue)
    {
        return this.configuration.getProperty(PREFIX + "entityPathPrefix", defaultValue);
    }

    /**
     * Makes it easy for this class to be extended.
     *
     * @param defaultValue the default value to use if the key is not found in the configuration
     * @return see {@link #getWikiNotFoundBehavior()}
     */
    protected WikiNotFoundBehavior getWikiNotFoundBehavior(WikiNotFoundBehavior defaultValue)
    {
        return this.configuration.getProperty(PREFIX + "multiwiki.notFoundBehavior", defaultValue);
    }
}
