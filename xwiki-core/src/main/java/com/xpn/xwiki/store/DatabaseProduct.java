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
 *
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
     * The product name for Oracle databases.
     */
    public static final DatabaseProduct ORACLE = new DatabaseProduct("Oracle");

    /**
     * The product name for Derby databases.
     */
    public static final DatabaseProduct DERBY = new DatabaseProduct("Apache Derby");

    /**
     * The product name for HSQLDB databases.
     */
    public static final DatabaseProduct HSQLDB = new DatabaseProduct("HSQL Database Engine");

    /**
     * The product name for DB2 databases.
     * <p>
     * Per DB2 documentation at
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9r5/topic/com.ibm.db2.luw.apdv.java.doc/doc/c0053013.html, the
     * database product name returned by the {@link java.sql.DatabaseMetaData#getDatabaseProductName()} method of DB2
     * JDBC drivers varies by the OS and environment the product is running on. Hence the DB string here uses only the
     * first 3 unique characters of the database product name. The {@link #toProduct(String)} method also hence checks
     * for {@link java.lang.String#startsWith(String)} rather than an exact match.
     * </p>
     */
    public static final DatabaseProduct DB2 = new DatabaseProduct("DB2/");

    /**
     * The product name for MySQL databases.
     */
    public static final DatabaseProduct MYSQL = new DatabaseProduct("MySQL");

    /**
     * Represents an unknown database for which we were not able to find the product name.
     */
    public static final DatabaseProduct UNKNOWN = new DatabaseProduct("Unknown");

    /**
     * @see #getProductName()
     */
    private String productName;

    /**
     * Private constructor to prevent instantiations.
     * 
     * @param productName the database product name as returned by
     *            {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
     */
    private DatabaseProduct(String productName)
    {
        this.productName = productName;
    }

    /**
     * @return the database product name. Example: "Oracle". The returned value should correspond to the value returned
     *         by {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
     */
    public String getProductName()
    {
        return this.productName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object)
    {
        boolean result = false;
        if ((object != null) && (object instanceof DatabaseProduct)) {
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
     * @param productNameAsString the string to transform
     * @return the {@link DatabaseProduct} object
     */
    public static DatabaseProduct toProduct(String productNameAsString)
    {
        DatabaseProduct product;
        if (productNameAsString.equalsIgnoreCase(ORACLE.getProductName())) {
            product = ORACLE;
        } else if (productNameAsString.equalsIgnoreCase(DERBY.getProductName())) {
            product = DERBY;
        } else if (productNameAsString.equalsIgnoreCase(HSQLDB.getProductName())) {
            product = HSQLDB;
        } else if (productNameAsString.startsWith(DB2.getProductName())) {
            // See documentation above on why we check starts with for DB2
            product = DB2;
        } else if (productNameAsString.equalsIgnoreCase(MYSQL.getProductName())) {
            product = MYSQL;
        } else {
            product = UNKNOWN;
        }

        return product;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getProductName().hashCode();
    }
}
