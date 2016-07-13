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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Replaces {@link DefaultStandardURLConfiguration} with fallbacks to configuration properties defined in the
 * {@code xwiki.cfg} configuration file.
 * 
 * @version $Id$
 * @since 5.1M1
 */
@Component
@Singleton
public class LegacyStandardURLConfiguration extends DefaultStandardURLConfiguration
{
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Override
    public boolean isPathBasedMultiWiki()
    {
        return super.isPathBasedMultiWiki("1".equals(this.xwikicfg.getProperty("xwiki.virtual.usepath", "1")));
    }

    @Override
    public String getWikiPathPrefix()
    {
        return super.getWikiPathPrefix(this.xwikicfg.getProperty("xwiki.virtual.usepath.servletpath", "wiki"));
    }

    @Override
    public String getEntityPathPrefix()
    {
        String prefix = super.getWikiPathPrefix(this.xwikicfg.getProperty("xwiki.defaultservletpath", "bin"));

        // Remove potential trailing "/" since the documentation in xwiki.cfg says it should contain a trailing "/" but
        // getEntityPathPrefix should return the prefix without "/"...
        prefix = StringUtils.removeEnd(prefix, "/");

        return prefix;
    }

    @Override
    public boolean isViewActionHidden()
    {
        return super.isViewActionHidden("0".equals(this.xwikicfg.getProperty("xwiki.showviewaction", "1")));
    }

    @Override
    public WikiNotFoundBehavior getWikiNotFoundBehavior()
    {
        WikiNotFoundBehavior legacyBehavior = WikiNotFoundBehavior.DISPLAY_ERROR;

        String legacyValue = this.xwikicfg.getProperty("xwiki.virtual.failOnWikiDoesNotExist", "0");

        if (!"1".equals(legacyValue)) {
            legacyBehavior = WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI;
        }

        return super.getWikiNotFoundBehavior(legacyBehavior);
    }
}
