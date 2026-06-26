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
package org.xwiki.whatsnew.script;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.whatsnew.NewsConfiguration;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceFactory;

/**
 * Script service to access the What's New API in a secure way from wiki pages by using scripting.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Component
@Singleton
@Named("whatsnew")
public class NewsScriptService implements ScriptService
{
    @Inject
    private NewsSourceFactory configuredNewsSourceFactory;

    @Inject
    private NewsConfiguration configuration;

    /**
     * @return the composite new source defined in XWiki's configuration (i.e. a new source wrapping all configured
     *         news sources)
     * @throws NewsException
     * @since 15.2RC1
     */
    public NewsSource getConfiguredNewsSource() throws NewsException
    {
        return this.configuredNewsSourceFactory.create(Collections.emptyMap());
    }

    /**
     * @return the configuration for the what's new feature (we need to be careful that it doesn't contain anything
     *         sensitive, which should be fine for that feature)
     * @since 15.2RC1
     */
    public NewsConfiguration getConfiguration()
    {
        return this.configuration;
    }
}
