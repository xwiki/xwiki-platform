/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 15 avr. 2004
 * Time: 01:49:53
 */

package org.hibernate.tool.hbm2ddl;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MyDatabaseMetadata extends DatabaseMetadata {
	private final Map tables = new HashMap();
	private final Set sequences = new HashSet();

	private java.sql.DatabaseMetaData meta;

	public MyDatabaseMetadata(Connection connection, Dialect dialect) throws SQLException {
		super(connection, dialect);
   		meta = connection.getMetaData();
		initSequences(connection, dialect);
	}

	public TableMetadata getTableMetadata(String name) throws HibernateException {
		TableMetadata table = null;

		if (name!=null) {

			table = (TableMetadata) tables.get( name.toUpperCase() );
			if (table==null) {
				String[] types = {"TABLE"};
				ResultSet rs = null;

				try {
					try {

                        // Ludovic Dubost changed name.toUpperCase() to name
                        // This seems to fail with mysql 4.0.15
						rs = meta.getTables(null, "%", name, types);

						while ( rs.next() ) {
							if ( name.equalsIgnoreCase( rs.getString("TABLE_NAME") ) ) {
								table = new TableMetadata(rs, meta);
								tables.put( name.toUpperCase(), table );
								break;
							}
						}
					}
					finally {
						if (rs!=null) rs.close();
					}
				}
				catch(SQLException e) {
					throw new HibernateException(e);
				}
			}
		}

		return table;
	}

    	private void initSequences(Connection connection, Dialect dialect) throws SQLException {
		String sql = dialect.getQuerySequencesString();

		if (sql==null) return;

		Statement statement = null;
		ResultSet rs = null;
		try {
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			while ( rs.next() ) sequences.add( rs.getString(1).toUpperCase() );
		}
		finally {
			if (rs!=null) rs.close();
			if (statement!=null) statement.close();
		}
	}

	public boolean isSequence(Object key) {
		return key instanceof String && sequences.contains( ( (String) key ).toUpperCase() );
	}

	public boolean isTable(Object key) throws HibernateException {
		return key instanceof String && ( getTableMetadata( (String) key ) != null );
	}
}
