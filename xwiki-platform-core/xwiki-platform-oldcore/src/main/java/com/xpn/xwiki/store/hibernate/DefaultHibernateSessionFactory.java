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
package com.xpn.xwiki.store.hibernate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

/**
 * Default implementation for {@link HibernateSessionFactory}.
 *
 * @version $Id$
 * @since 2.0M1
 * @deprecated since 11.5RC1
 */
@Component
@Singleton
@Deprecated
public class DefaultHibernateSessionFactory implements HibernateSessionFactory
{
    @Inject
    private HibernateStore hibernate;

    @Override
    public Configuration getConfiguration()
    {
        return this.hibernate.getConfiguration();
    }

    @Override
    public SessionFactory getSessionFactory()
    {
        return this.hibernate.getSessionFactory();
    }

    @Override
    public void setSessionFactory(SessionFactory sessionFactory)
    {
        // Do nothing
    }
}
