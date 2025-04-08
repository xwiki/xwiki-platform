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
package org.xwiki.store.hibernate.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.store.hibernate.HibernateAdapter;

/**
 * The {@link HibernateAdapter} for MariaDB.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component
@Named(MySQL8HibernateAdapter.HINT)
@Singleton
public class MySQL8HibernateAdapter extends MySQLHibernateAdapter
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "mysql/8";

    @Override
    public String getInformationShemaInnoDBTables()
    {
        // "INNODB_SYS_TABLES" was renamed to "INNODB_TABLES" in MySQL 8.0
        return "information_schema.INNODB_TABLES";
    }
}
