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
package com.xpn.xwiki.internal.store.hibernate.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link HqlQueryUtils}.
 * 
 * @version $Id$
 */
public class HqlQueryUtilsTest
{
    @Test
    public void replaceLegacyQueryParameters()
    {
        assertEquals("select column from table where table.column = ?1",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column = ?"));
        assertEquals("select column from table where table.column =?1",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column =?"));
        assertEquals("select column from table where table.column =?1 ",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column =? "));
        assertEquals("select column from table where table.column in (?1)",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column in (?)"));
        assertEquals("select column from table where table.column in (?1,?2)",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column in (?,?)"));
        assertEquals("select column from table where table.column in (?1,?2 )",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column in (?,? )"));
        assertEquals("select column from table where table.column in ( ?1 , ?2 )",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column in ( ? , ? )"));
        assertEquals("select column from table where table.column >?1",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column >?"));
        assertEquals("select column from table where table.column <?1",
            HqlQueryUtils.replaceLegacyQueryParameters("select column from table where table.column <?"));
    }
}
