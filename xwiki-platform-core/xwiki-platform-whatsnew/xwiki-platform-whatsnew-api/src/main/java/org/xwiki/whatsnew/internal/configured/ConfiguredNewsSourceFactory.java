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
package org.xwiki.whatsnew.internal.configured;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.whatsnew.NewsConfiguration;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceDescriptor;
import org.xwiki.whatsnew.NewsSourceFactory;

/**
 * Create a News Source based on defined sources in {@code xwiki.properties}.
 * See {@link org.xwiki.whatsnew.internal.DefaultNewsConfiguration} for the format definition.
 *
 * @version $Id$
 */
@Component
@Singleton
public class ConfiguredNewsSourceFactory implements NewsSourceFactory
{
    @Inject
    private NewsConfiguration configuration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Cached news source so that calling several times {@link #create(Map)} will be performant and return the same
     * News source.
     */
    private NewsSource source;

    @Override
    public NewsSource create(Map<String, String> parameters) throws NewsException
    {
        NewsSource result;
        if (this.source != null) {
            result = this.source;
        } else {
            // Create the News Source from configuration and cache it.
            result = create();
            this.source = result;
        }
        return result;
    }

    private NewsSource create() throws NewsException
    {
        List<NewsSource> sources = new ArrayList<>();
        for (NewsSourceDescriptor descriptor : this.configuration.getNewsSourceDescriptors()) {
            NewsSourceFactory factory = getFactory(descriptor.getSourceTypeHint());
            if (factory != null) {
                sources.add(factory.create(descriptor.getParameters()));
            }
        }
        return new CompositeNewsSource(sources);
    }

    private NewsSourceFactory getFactory(String hint) throws NewsException
    {
        try {
            return this.componentManager.getInstance(NewsSourceFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new NewsException(String.format("Failed to locate News Source Factory for [%s].", hint), e);
        }
    }
}
