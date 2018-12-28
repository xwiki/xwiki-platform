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

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;

/**
 * Remove a non-null constraint from a colomn.
 *
 * @version $Id$
 * @since 9.11RC1
 */
public abstract class AbstractDropNotNullDataMigration extends AbstractHibernateDataMigration
{
    private Class<?> table;

    private String property;

    /**
     * @param table the {@link Class} corresponding to the table from which to remove the non-null constraint
     * @param property the name of the hibernate property from which to remove the non-null constraint
     */
    public AbstractDropNotNullDataMigration(Class<?> table, String property)
    {
        this.table = table;
        this.property = property;
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        XWikiHibernateBaseStore store = getStore();
        Dialect dialect = store.getDialect();
        Configuration configuration = store.getConfiguration();
        Mapping mapping = configuration.buildMapping();
        PersistentClass pClass = configuration.getClassMapping(this.table.getName());
        Column column = ((Column) pClass.getProperty(this.property).getColumnIterator().next());
        String columnType = column.getSqlType(dialect, mapping);

        StringBuilder builder = new StringBuilder();

        builder.append("<changeSet author=\"xwiki\" id=\"R").append(this.getVersion().getVersion()).append("\">\n");
        builder.append("    <dropNotNullConstraint\n");
        builder.append("            columnDataType=\"").append(columnType).append('"').append('\n');
        builder.append("            columnName=\"").append(column.getName()).append('"').append('\n');
        builder.append("            tableName=\"").append(pClass.getTable().getName()).append("\"/>\n");
        builder.append("</changeSet>");

        return builder.toString();
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here
    }
}
