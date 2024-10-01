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
package com.xpn.xwiki.store;

/**
 * Represent a Database Product name as returned by {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
 * <p>
 * Ideally we shouldn't have to take care of Database specificities since we're using Hibernate to abstract ourselves
 * from Databases. However it happens that Hibernate doesn't support setting Catalogs on some databases and instead we
 * need to use our own tricks to do that and these tricks depend on the database. Hence the need to differentiate them
 * and hence the need for this class.
 *
 * @version $Id$
 */
public final class DatabaseProduct
{
    /**
     * The Product name and the JDBC scheme to recognize an Oracle DB.
     */
    public static final DatabaseProduct ORACLE = new DatabaseProduct("Oracle", "oracle");

    /**
     * The Product name and the JDBC scheme to recognize a Derby DB.
     */
    public static final DatabaseProduct DERBY = new DatabaseProduct("Apache Derby", "derby");

    /**
     * The Product name and the JDBC scheme to recognize a HSQLDB DB.
     */
    public static final DatabaseProduct HSQLDB = new DatabaseProduct("HSQL Database Engine", "hsqldb");

    /**
     * The Product name and the JDBC scheme to recognize a DB2 DB.
     * <p>
     * Per DB2 documentation at
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9r5/topic/com.ibm.db2.luw.apdv.java.doc/doc/c0053013.html, the
     * database product name returned by the {@link java.sql.DatabaseMetaData#getDatabaseProductName()} method of DB2
     * JDBC drivers varies by the OS and environment the product is running on. Hence the DB string here uses only the
     * first 3 unique characters of the database product name. The {@link #toProduct(String)} method also hence checks
     * for {@link java.lang.String#startsWith(String)} rather than an exact match.
     */
    public static final DatabaseProduct DB2 = new DatabaseProduct("DB2/", "db2");

    /**
     * The Product name and the JDBC scheme to recognize a MySQL DB.
     */
    public static final DatabaseProduct MYSQL = new DatabaseProduct("MySQL", "mysql");

    /**
     * The Product name and the JDBC scheme to recognize a MariaDB DB.
     */
    public static final DatabaseProduct MARIADB = new DatabaseProduct("MariaDB", "mariadb");

    /**
     * The Product name and the JDBC scheme to recognize a PostgreSQL DB.
     */
    public static final DatabaseProduct POSTGRESQL = new DatabaseProduct("PostgreSQL", "postgresql");

    /**
     * The Product name and the JDBC scheme to recognize a Microsoft SQL Server DB.
     */
    public static final DatabaseProduct MSSQL = new DatabaseProduct("Microsoft SQL Server", "sqlserver");

    /**
     * The Product name and the JDBC scheme to recognize a H2 DB.
     */
    public static final DatabaseProduct H2 = new DatabaseProduct("H2", "h2");

    /**
     * Represents an unknown database for which we were not able to find the product name.
     */
    public static final DatabaseProduct UNKNOWN = new DatabaseProduct("Unknown", "unknown");

    /**
     * @see #getProductName()
     */
    private String productName;

    private String jdbcScheme;

    /**
     * Private constructor to prevent instantiations.
     *
     * @param productName the database product name as returned by
     *        {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
     * @param jdbcScheme the JDBC scheme as defined in the URL connection string for this product
     */
    private DatabaseProduct(String productName, String jdbcScheme)
    {
        this.productName = productName;
        this.jdbcScheme = jdbcScheme;
    }

    /**
     * @return the database product name. Example: "Oracle". The returned value should correspond to the value returned
     *         by {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
     */
    public String getProductName()
    {
        return this.productName;
    }

    @Override
    public boolean equals(Object object)
    {
        boolean result = false;
        if (object instanceof DatabaseProduct) {
            DatabaseProduct product = (DatabaseProduct) object;
            if (product.getProductName().equals(getProductName())) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Transform a product name represented as a string into a {@link DatabaseProduct} object.
     *
     * @param productNameOrJDBCScheme the string to parse (can either represent a product name or a URL connection DB
     *        scheme
     * @return the {@link DatabaseProduct} object
     */
    public static DatabaseProduct toProduct(String productNameOrJDBCScheme)
    {
        DatabaseProduct product;
        if (matches(productNameOrJDBCScheme, ORACLE)) {
            product = ORACLE;
        } else if (matches(productNameOrJDBCScheme, DERBY)) {
            product = DERBY;
        } else if (matches(productNameOrJDBCScheme, HSQLDB)) {
            product = HSQLDB;
        } else if (matches(productNameOrJDBCScheme, H2)) {
            product = H2;
        } else if (productNameOrJDBCScheme.startsWith(DB2.getProductName())
            || DB2.jdbcScheme.equalsIgnoreCase(productNameOrJDBCScheme))
        {
            // See documentation above on why we check starts with for DB2
            product = DB2;
        } else if (matches(productNameOrJDBCScheme, MYSQL)) {
            product = MYSQL;
        } else if (matches(productNameOrJDBCScheme, MARIADB)) {
            product = MARIADB;
        } else if (matches(productNameOrJDBCScheme, POSTGRESQL)) {
            product = POSTGRESQL;
        } else if (matches(productNameOrJDBCScheme, MSSQL)) {
            product = MSSQL;
        } else {
            product = UNKNOWN;
        }

        return product;
    }

    private static boolean matches(String productNameOrJDBCScheme, DatabaseProduct product)
    {
        return product.getProductName().equalsIgnoreCase(productNameOrJDBCScheme)
            || product.jdbcScheme.equalsIgnoreCase(productNameOrJDBCScheme);
    }

    @Override
    public int hashCode()
    {
        return getProductName().hashCode();
    }

    @Override
    public String toString()
    {
        return getProductName();
    }
}
