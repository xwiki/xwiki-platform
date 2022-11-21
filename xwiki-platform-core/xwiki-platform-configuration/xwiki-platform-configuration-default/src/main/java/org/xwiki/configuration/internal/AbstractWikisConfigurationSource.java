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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Base class for document-based configuration sources that need to check first the current wiki and then the main wiki.
 * 
 * @version $Id$
 * @since 14.9RC1
 */
public abstract class AbstractWikisConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * A configuration source that reads the configuration from a specified wiki.
     * 
     * @version $Id$
     */
    private class WikiConfigurationSource implements ConfigurationSource
    {
        private final String wiki;

        /**
         * Creates a new configuration source that reads the configuration from the specified wiki.
         * 
         * @param wiki the wiki from where the configuration is read
         */
        WikiConfigurationSource(String wiki)
        {
            this.wiki = wiki;
        }

        @Override
        public boolean containsKey(String key)
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.containsKey(key));
        }

        @Override
        public List<String> getKeys()
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.getKeys());
        }

        @Override
        public <T> T getProperty(String key)
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.getProperty(key));
        }

        @Override
        public <T> T getProperty(String key, T defaultValue)
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.getProperty(key, defaultValue));
        }

        @Override
        public <T> T getProperty(String key, Class<T> valueClass)
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.getProperty(key, valueClass));
        }

        @Override
        public boolean isEmpty()
        {
            return getFromWiki(() -> AbstractWikisConfigurationSource.super.isEmpty());
        }

        private <T> T getFromWiki(Supplier<T> supplier)
        {
            XWikiContext xcontext = xcontextProvider.get();
            String currentWiki = xcontext.getWikiId();
            try {
                xcontext.setWikiId(this.wiki);
                return supplier.get();
            } finally {
                xcontext.setWikiId(currentWiki);
            }
        }
    }

    /**
     * @return the local reference of the configuration document
     */
    protected abstract LocalDocumentReference getLocalDocumentReference();

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(getLocalDocumentReference(), this.getCurrentWikiReference());
    }

    @Override
    public boolean containsKey(String key)
    {
        return getConfigurationSource().containsKey(key);
    }

    @Override
    public List<String> getKeys()
    {
        return getConfigurationSource().getKeys();
    }

    @Override
    public <T> T getProperty(String key)
    {
        return getConfigurationSource().getProperty(key);
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        return getConfigurationSource().getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        return getConfigurationSource().getProperty(key, valueClass);
    }

    @Override
    public boolean isEmpty()
    {
        return getConfigurationSource().isEmpty();
    }

    private ConfigurationSource getConfigurationSource()
    {
        CompositeConfigurationSource compositeConfigSource = new CompositeConfigurationSource();
        getWikis().stream().map(WikiConfigurationSource::new)
            .forEachOrdered(wikiConfigSource -> compositeConfigSource.addConfigurationSource(wikiConfigSource));
        return compositeConfigSource;
    }

    /**
     * @return the list of wikis where to look for configuration properties
     */
    protected List<String> getWikis()
    {
        Set<String> wikis = new LinkedHashSet<>();
        // Look for configuration properties first in the current wiki.
        wikis.add(this.wikiManager.getCurrentWikiId());
        // And then in the main wiki (of course, if it's not the same as the current wiki).
        wikis.add(this.wikiManager.getMainWikiId());
        return wikis.stream().collect(Collectors.toList());
    }
}
