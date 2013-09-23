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
package com.xpn.xwiki.internal.store;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.SessionFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.engine.SessionFactoryImplementor;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Properly close all open connections in the Database Connection Pool.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("hibernateShutdown")
@Singleton
public class HibernateShutdownEventListener implements EventListener
{
    /**
     * Used to access the connection pool instance used.
     */
    @Inject
    private HibernateSessionFactory sessionFactoryFactory;

    @Override
    public String getName()
    {
        return "hibernateShutdown";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new ApplicationStoppedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // TODO: See http://jira.xwiki.org/jira/browse/XWIKI-471. Note that this code currently duplicates
        // XWikiHibernateBaseStore.shutdownHibernate() which is not public and getting a Store implementation from
        // this component is very difficult since there's no XWikiContext and the store used is defined in xwiki.cfg
        SessionFactory sessionFactory = this.sessionFactoryFactory.getSessionFactory();
        if (sessionFactory != null) {
            // Close all connections in the Connection Pool.
            // Note that we need to do the cast because this is how Hibernate suggests to get the Connection Provider.
            // See http://bit.ly/QAJXlr
            ConnectionProvider provider = ((SessionFactoryImplementor) sessionFactory).getConnectionProvider();
            // If the user has specified a Data Source we shouldn't close it. Fortunately the way Hibernate works is
            // the following: if the user has configured Hibernate to use a Data Source then Hibernate will use
            // the DatasourceConnectionProvider class which has a close() method that doesn't do anything...
            if (provider != null) {
                provider.close();
            }
        }
    }
}
