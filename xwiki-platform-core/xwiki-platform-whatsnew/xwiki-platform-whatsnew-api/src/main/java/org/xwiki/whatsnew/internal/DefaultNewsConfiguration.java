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
package org.xwiki.whatsnew.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.whatsnew.NewsConfiguration;

/**
 * Implementation for configuration data for the What's New extension, looking first in the current space for a
 * {@code XWiki.XWikiPreferences} xobject in the space {@code WebPreferences} document, then in the wiki's
 *  {@code XWiki.XWikiPreferences} document, and then in the {@code xwiki.properties} file.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Component
@Singleton
public class DefaultNewsConfiguration implements NewsConfiguration
{
    /**
     * Prefix for configuration keys for the What's New module.
     */
    private static final String PREFIX = "whatsnew.";

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public List<String> getNewsSourceHints()
    {
        return null;
    }

    @Override
    public long getNewsRefreshRate()
    {
        return 0;
    }

    @Override
    public int getNewsDisplayCount()
    {
        return 0;
    }
}
