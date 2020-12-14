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
package org.xwiki.eventstream.store.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;

/**
 * Register the event stream store mapping.
 *
 * @since 11.2RC1
 * @since 11.1.1
 * @version $Id$
 */
@Component
@Named("EventStreamStoreInitializer")
@Singleton
public class EventStreamStoreInitializer implements EventListener
{
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList(new ApplicationStartedEvent());
    }

    @Override
    public String getName()
    {
        return "Event Stream Store";
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
            addResource("eventstream.oracle.hbm.xml");
        } else {
            addResource("eventstream.hbm.xml");
        }
    }

    private void addResource(String resource)
    {
        try (InputStream stream = getMappingFile(resource)) {
            this.sessionFactory.getConfiguration().addInputStream(stream);
        } catch (IOException e) {
            this.logger.error("Failed to close the resource", e);
        }
    }

    private InputStream getMappingFile(String mappingFileName)
    {
        InputStream resource = Util.getResourceAsStream(mappingFileName);

        // It could happen that the resource is not found in the file system, in the Servlet Context or in the current
        // Thread Context Classloader. In this case try to get it from the CL used to load this class since the default
        // mapping file is located in the same JAR that contains this code.
        // This can happen in the case when this JAR is installed as a root extension (and thus in a ClassLoader not
        // visible from the current Thread Context Classloader at the time when the ApplicationStartedEvent event
        // is sent (the thread context CL is set to the current wiki CL later on).
        if (resource == null) {
            resource = getClass().getClassLoader().getResourceAsStream(mappingFileName);
        }
        return resource;
    }
}
