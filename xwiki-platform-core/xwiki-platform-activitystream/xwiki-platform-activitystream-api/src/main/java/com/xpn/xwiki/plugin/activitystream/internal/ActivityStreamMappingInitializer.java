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
package com.xpn.xwiki.plugin.activitystream.internal;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

/**
 * Register the activity stream mapping.
 *
 * @since 11.2RC1
 * @since 11.1.1
 * @version $Id$
 */
@Component
@Named("ActivityStreamMappingInitializer")
@Singleton
public class ActivityStreamMappingInitializer implements EventListener
{
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList(new ApplicationStartedEvent());
    }

    @Override
    public String getName()
    {
        return "Activity Stream Mapping Initializer";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        loadMappingFile("legacy-activitystream.hbm.xml");
    }

    protected void loadMappingFile(String path)
    {
        // This only adds the mappings to a queue. The mappings will be available after the session factory is created.
        this.sessionFactory.getConfiguration().addInputStream(Util.getResourceAsStream(path));
    }
}
