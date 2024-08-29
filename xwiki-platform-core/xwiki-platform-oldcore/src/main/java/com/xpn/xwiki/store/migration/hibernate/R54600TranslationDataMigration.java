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
import java.sql.Statement;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Fix any existing mistake with document translation field. It should be synchronized with the language.
 * <p>
 * This kind of mistake should not be possible anymore with the fix for XWIKI-11110.
 *
 * @version $Id$
 * @since 5.4.6
 */
@Component
@Named("R54600Translation")
@Singleton
public class R54600TranslationDataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Fix any existing mistake with document translation field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54600);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new R54600Work());
                return Boolean.TRUE;
            }
        });
    }

    private static final class R54600Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            try (Statement statement = connection.createStatement()) {
                statement.execute("UPDATE xwikidoc set XWD_TRANSLATION = 1"
                    + " where XWD_TRANSLATION = 0 and (XWD_LANGUAGE is not null and XWD_LANGUAGE <> '')");
                statement.execute("UPDATE xwikidoc set XWD_TRANSLATION = 0"
                    + " where XWD_TRANSLATION = 1 and (XWD_LANGUAGE is null or XWD_LANGUAGE = '')");
            }
        }
    }
}
