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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfigurationLoadedEvent;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;

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
        return Collections.singletonList(new HibernateConfigurationLoadedEvent());
    }

    @Override
    public String getName()
    {
        return "Activity Stream Mapping Initializer";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // This only adds the mappings to a queue. The mappings will be available after the session factory is created.
        // We need to use a different mapping for Oracle
        String driverClass = StringUtils.defaultString(
            this.sessionFactory.getConfiguration().getProperty("hibernate.connection.driver_class"),
            this.sessionFactory.getConfiguration().getProperty("connection.driver_class"));
        if (StringUtils.containsIgnoreCase(driverClass, "oracle")) {
            this.sessionFactory.getConfiguration()
                .addInputStream(Util.getResourceAsStream("legacy-activitystream.oracle.hbm.xml"));
        } else {
            this.sessionFactory.getConfiguration()
                .addInputStream(Util.getResourceAsStream("legacy-activitystream.hbm.xml"));
        }
    }
}
