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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiContext;

/**
 * Same as {@link CompositeConfigurationSource} but with lazily loaded elements requiring an initialized XWikiContext.
 * 
 * @version $Id$
 * @since 9.5RC1
 */
public class CompositeWikiConfigurationSource extends CompositeConfigurationSource
{
    protected List<String> wikiHints = new ArrayList<>();

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ComponentManager componentManager;

    private volatile boolean wikiInitialized;

    @Inject
    private Logger logger;

    /**
     * @param wikiHint the ConfigurationSource hint to add
     */
    public void addWikiConfigurationSource(String wikiHint)
    {
        this.wikiHints.add(wikiHint);
    }

    private synchronized List<ConfigurationSource> initializeWikiSources()
    {
        if (this.wikiInitialized) {
            return this.sources;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        // If context is not ready don't do anything
        if (xcontext == null) {
            return Collections.emptyList();
        }

        // Inject registered configuration sources
        List<ConfigurationSource> newSources = new ArrayList<>(this.sources.size() + this.wikiHints.size());
        for (String wikiHint : this.wikiHints) {
            try {
                newSources.add(this.componentManager.getInstance(ConfigurationSource.class, wikiHint));
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup configuration source with hint [{}]. It won't be used.", wikiHint,
                    e);
            }
        }
        this.sources = newSources;

        return this.sources;

    }

    @Override
    public Iterator<ConfigurationSource> iterator()
    {
        if (!this.wikiInitialized) {
            initializeWikiSources();
        }

        return super.iterator();
    }
}
