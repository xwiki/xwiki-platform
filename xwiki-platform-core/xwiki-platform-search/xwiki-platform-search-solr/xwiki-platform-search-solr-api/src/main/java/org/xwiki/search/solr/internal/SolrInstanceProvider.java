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
package org.xwiki.search.solr.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrInstance;

/**
 * Provider for {@link SolrInstance} that, based on the current configuration, returns the right component.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class SolrInstanceProvider implements Provider<SolrInstance>
{
    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Solr configuration.
     */
    @Inject
    private SolrConfiguration configuration;

    /**
     * The component manager used to lookup the configured component.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public SolrInstance get()
    {
        String type = configuration.getServerType();

        SolrInstance newInstance = null;
        try {
            newInstance = componentManager.getInstance(SolrInstance.class, type);
        } catch (ComponentLookupException e) {
            logger.error("Failed to lookup Solr instance", e);
        }

        return newInstance;
    }
}
