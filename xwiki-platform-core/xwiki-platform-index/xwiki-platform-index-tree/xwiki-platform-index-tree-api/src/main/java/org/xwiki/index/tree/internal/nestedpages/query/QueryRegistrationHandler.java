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
package org.xwiki.index.tree.internal.nestedpages.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;

/**
 * Registers the named query mapping.
 * 
 * @version $Id$
 * @since 8.3RC1
 * @since 7.4.5
 */
@Component
@Named("queryRegistrationHandler/nestedPages")
@Singleton
public class QueryRegistrationHandler implements EventListener
{
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new ApplicationStartedEvent());
    }

    @Override
    public String getName()
    {
        return "queryRegistrationHandler/nestedPages";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        loadMappingFile("org/xwiki/index/tree/internal/nestedpages/query/queries.hbm.xml");
    }

    protected void loadMappingFile(String path)
    {
        try (InputStream stream = Util.getResourceAsStream(path)) {
            // This only adds the mappings to a queue. The mappings will be available after the session factory is
            // created.
            this.sessionFactory.getConfiguration().addInputStream(stream);
        } catch (IOException e) {
            this.logger.error("Failed to close the resoure stream", e);
        }
    }
}
