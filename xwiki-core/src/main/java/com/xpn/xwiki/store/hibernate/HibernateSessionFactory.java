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

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.xwiki.component.annotation.ComponentRole;

/**
 * Class used by Hibernate stores for obtain sessions. Simple holder for real sessionFactory and configuration for now.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@ComponentRole
public interface HibernateSessionFactory
{
    /**
     * @return Hibernate Configuration object
     */
    Configuration getConfiguration();

    /**
     * @return Real Hibernate session factory
     */
    SessionFactory getSessionFactory();

    // NOTE: this method will be removed in 3rd step of XWIKI-2332
    void setSessionFactory(SessionFactory sessionFactory);
}
