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
package org.xwiki.wiki.internal.descriptor.properties;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.properties.WikiPropertyGroup;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;

/**
 * Default implementation for WikiPropertyGroupManager.
 *
 * @since 5.3M2
 * @version $Id :$
 */
@Component
@Singleton
public class DefaultWikiPropertyGroupManager implements WikiPropertyGroupManager
{
    @Inject
    private Map<String, WikiPropertyGroupProvider> propertyGroupProviders;

    @Inject
    private Logger logger;

    @Override
    public void loadForDescriptor(WikiDescriptor descriptor) throws WikiPropertyGroupException
    {
        String wikiId = descriptor.getId();
        for (String propertyGroupName : propertyGroupProviders.keySet()) {
            WikiPropertyGroupProvider provider = propertyGroupProviders.get(propertyGroupName);
            try {
                descriptor.addPropertyGroup(provider.get(wikiId));
            } catch (WikiPropertyGroupException e) {
                logger.warn(String.format("Unable to load property groups [%s].", propertyGroupName), e);
            }
        }
    }

    @Override
    public void saveForDescriptor(WikiDescriptor descriptor) throws WikiPropertyGroupException
    {
        String wikiId = descriptor.getId();
        for (String propertyGroupName : propertyGroupProviders.keySet()) {
            WikiPropertyGroup group = descriptor.getPropertyGroup(propertyGroupName);
            if (group != null) {
                WikiPropertyGroupProvider provider = propertyGroupProviders.get(propertyGroupName);
                provider.save(group, wikiId);
            }
        }
    }
}
