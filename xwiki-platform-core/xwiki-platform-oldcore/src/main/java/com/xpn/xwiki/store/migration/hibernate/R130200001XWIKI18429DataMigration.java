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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * This migration increase the maximum size of all the columns potentially containing a document reference to the
 * maximum index supported by MySQL: 768.
 *
 * @version $Id$
 * @since 13.2RC1
 * @since 12.10.6
 */
@Component
@Named("R130200001XWIKI18429")
@Singleton
public class R130200001XWIKI18429DataMigration extends AbstractHibernateDataMigration
{
    private static final int MAXSIZE = 768;

    private static final Version MYSQL57 = new DefaultVersion("5.7");

    private static final Version MARIADB102 = new DefaultVersion("10.2");

    @Inject
    private Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Override
    public String getDescription()
    {
        return "Increase the maximum size of all the columns potentially containing a document reference"
            + " to the maximum index supported by MySQL.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(130200001);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, everything's done as part of Liquibase pre update
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // Check the version of the database server
        if (this.hibernateStore.getDatabaseProductName() == DatabaseProduct.MYSQL) {
            DatabaseMetaData databaMetadata = this.hibernateStore.getDatabaseMetaData();

            // Impossible to apply this migration on MySQL lower than 5.7
            try {
                String productName = databaMetadata.getDatabaseProductName();

                String versionString = databaMetadata.getDatabaseProductVersion();
                Version version = new DefaultVersion(versionString);

                if (productName.equalsIgnoreCase("mariadb")) {
                    if (version.compareTo(MARIADB102) < 0) {
                        this.logger.warn("The migration cannot run on MariaDB versions lower than 10.2");

                        return false;
                    }
                } else {
                    if (version.compareTo(MYSQL57) < 0) {
                        this.logger.warn("The migration cannot run on MySQL versions lower than 5.7");

                        return false;
                    }
                }
            } catch (SQLException e) {
                this.logger.warn("Failed to get database information: {}", ExceptionUtils.getRootCauseMessage(e));
            }
        } else if (this.hibernateStore.getDatabaseProductName() == DatabaseProduct.MSSQL) {
            this.logger.warn("The migration cannot run on Microsoft SQL Server");

            return false;
        }

        return true;
    }

    private void appendXmlAttribute(String property, String value, StringBuilder stringBuilder)
    {
        stringBuilder.append(' ');
        stringBuilder.append(property);
        stringBuilder.append("=\"");
        stringBuilder.append(value);
        stringBuilder.append("\" ");
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        StringBuilder builder = new StringBuilder();

        for (PersistentClass entity : this.hibernateStore.getConfigurationMetadata().getEntityBindings()) {
            // Find properties to update
            for (Iterator<Property> it = entity.getPropertyIterator(); it.hasNext();) {
                Property property = it.next();

                for (Iterator<Column> it2 = property.getColumnIterator(); it2.hasNext();) {
                    Column column = it2.next();

                    int expectedLenght = column.getLength();

                    if (expectedLenght == MAXSIZE) {
                        update(entity, column, builder);
                    }
                }
            }
        }

        if (builder.length() > 0) {
            return String.format("<changeSet author=\"xwiki\" id=\"R%s\">%s</changeSet>", getVersion().getVersion(),
                builder.toString());
        }

        return null;
    }

    private void update(PersistentClass entity, Column column, StringBuilder builder)
    {
        if (this.hibernateStore.getDatabaseProductName() == DatabaseProduct.MYSQL) {
            // Not using <modifyDataType> here because Liquibase ignores attributes likes "NOT NULL"
            builder.append("<sql>");
            JdbcEnvironment jdbcEnvironment =
                this.hibernateStore.getConfigurationMetadata().getDatabase().getJdbcEnvironment();
            String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter()
                .format(entity.getTable().getQualifiedTableName(), this.hibernateStore.getDialect());
            builder.append(this.hibernateStore.getDialect().getAlterTableString(tableName));
            builder.append(" MODIFY ");
            builder.append(column.getQuotedName(this.hibernateStore.getDialect()));
            builder.append(' ');
            builder.append(getDataType(column, this.hibernateStore.getConfigurationMetadata()));
            builder.append("</sql>");
        } else {
            builder.append("<modifyDataType");
            appendXmlAttribute("tableName", this.hibernateStore.getConfiguredTableName(entity), builder);
            appendXmlAttribute("columnName", this.hibernateStore.getConfiguredColumnName(column), builder);
            appendXmlAttribute("newDataType",
                column.getSqlType(this.hibernateStore.getDialect(), this.hibernateStore.getConfigurationMetadata()),
                builder);
            builder.append("/>");
        }
    }

    private String getDataType(Column column, Metadata configurationMetadata)
    {
        Dialect dialect = this.hibernateStore.getDialect();

        StringBuilder builder = new StringBuilder(column.getSqlType(dialect, configurationMetadata));

        if (column.isNullable()) {
            builder.append(dialect.getNullColumnString());
        } else {
            builder.append(" not null");
        }

        return builder.toString();
    }
}
