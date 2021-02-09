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
package org.xwiki.tag.internal.livedata;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;

import static org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;

/**
 * Provides the tag live data configuration.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component
@Named(TaggedDocumentLiveDataConfigurationResolver.HINT)
@Singleton
public class TaggedDocumentLiveDataConfigurationResolver implements LiveDataConfigurationResolver<LiveDataConfiguration>
{
    /**
     * This component's hint.
     */
    public static final String HINT = "taggedDocument";

    @Inject
    @Named(HINT)
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration input) throws LiveDataException
    {
        LiveDataConfiguration liveDataConfiguration = this.defaultConfigProvider.get();
        liveDataConfiguration.setQuery(input.getQuery());

        LiveDataPropertyDescriptor liveDataPropertyDescriptor = new LiveDataPropertyDescriptor();
        liveDataPropertyDescriptor.setId("page");
        DisplayerDescriptor linkDisplayer = new DisplayerDescriptor("link");
        linkDisplayer.setParameter("propertyHref", "page_link");
        liveDataPropertyDescriptor.setDisplayer(linkDisplayer);
        liveDataPropertyDescriptor.setSortable(false);
        liveDataPropertyDescriptor.setEditable(false);
        liveDataPropertyDescriptor.setVisible(true);
        liveDataPropertyDescriptor.setFilterable(false);
        liveDataPropertyDescriptor.setName("Page");
        liveDataPropertyDescriptor.setType("document");

        LiveDataMeta meta = new LiveDataMeta();
        liveDataConfiguration.setMeta(meta);
        meta.setPropertyDescriptors(Arrays.asList(
            liveDataPropertyDescriptor
        ));

        return liveDataConfiguration;
    }
}
