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

package com.xpn.xwiki.store.migration.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-8948: Each property type should be stored in its own database table.
 * 
 * @version $Id$
 */
@Component
@Named("R50000XWIKI8948")
@Singleton
public class R50000XWIKI8948DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/browse/XWIKI-8948";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(50000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException
            {
                session.doWork(new R50000Work());
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Hibernate {@link Work} class doing the actual work of this migrator.
     * 
     * @version $Id$
     */
    private static class R50000Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            connection.createStatement().execute(
                "INSERT INTO xwikistringlists(XWL_ID, XWL_NAME, XWL_VALUE)"
                    + "  SELECT prop.XWL_ID, prop.XWL_NAME, prop.XWL_VALUE"
                    + "    FROM xwikilargestrings prop, xwikiproperties baseProp"
                    + "    WHERE prop.XWL_ID = baseProp.XWP_ID AND prop.XWL_NAME = baseProp.XWP_NAME"
                    + "      AND baseProp.XWP_CLASSTYPE = '" + StringListProperty.class.getName() + "'");
            try {
                connection.createStatement().execute(
                    "DELETE FROM xwikilargestrings WHERE(XWL_ID, XWL_NAME) IN "
                        + "  (SELECT XWP_ID, XWP_NAME FROM xwikiproperties WHERE XWP_CLASSTYPE = '"
                        + StringListProperty.class.getName() + "')");
            } catch (Exception ex) {
                // Derby doesn't support "WHERE (A, B) IN (SELECT)" queries, use the less performant "WHERE EXISTS"
                connection.createStatement().execute(
                    "DELETE FROM xwikilargestrings xls WHERE EXISTS (SELECT * FROM xwikiproperties xwp WHERE"
                        + "  xwp.XWP_ID = xls.XWL_ID AND xwp.XWP_NAME = xls.XWL_NAME AND XWP_CLASSTYPE = '"
                        + StringListProperty.class.getName() + "')");
            }
        }
    }
}
