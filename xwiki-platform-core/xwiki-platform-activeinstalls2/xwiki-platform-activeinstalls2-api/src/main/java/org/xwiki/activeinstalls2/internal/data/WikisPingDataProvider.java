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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the number of wikis in the XWiki instance.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Component
@Named("wikis")
@Singleton
public class WikisPingDataProvider extends AbstractPingDataProvider
{
    private static final String PROPERTY_TOTAL = "total";

    private static final String PROPERTY_WIKIS = "wikis";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_TOTAL, Property.of(b1 -> b1.long_(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_WIKIS, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        Collection<String> wikiIds;
        try {
            wikiIds = this.wikiDescriptorManager.getAllIds();
        } catch (WikiManagerException e) {
            logWarning("Failed to get the list of wikis", e);
            return;
        }

        WikisPing wikisPing = new WikisPing();
        wikisPing.setTotal(wikiIds.size());
        ping.setWikis(wikisPing);
    }
}
