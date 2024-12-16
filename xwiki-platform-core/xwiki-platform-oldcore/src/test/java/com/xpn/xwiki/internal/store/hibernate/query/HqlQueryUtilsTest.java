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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link HqlQueryUtils}.
 * 
 * @version $Id$
 */
public class HqlQueryUtilsTest
{
    @Test
    public void isSafe()
    {
        // allowed

        assertTrue(HqlQueryUtils.isSafe("select name from XWikiDocument"));
        assertTrue(HqlQueryUtils.isSafe("select doc.name, space.name from XWikiDocument doc, XWikiSpace space"));
        assertTrue(HqlQueryUtils
            .isSafe("select doc.name, space.name from XWikiDocument doc, XWikiSpace space, OtherTable as ot"));
        assertTrue(HqlQueryUtils.isSafe("select count(name) from XWikiDocument"));
        assertTrue(HqlQueryUtils.isSafe("select count(doc.name) from XWikiDocument doc"));
        assertTrue(HqlQueryUtils
            .isSafe("select doc.fullName from XWikiDocument as doc, com.xpn.xwiki.objects.StringProperty as str"));

        assertTrue(HqlQueryUtils.isSafe("select count(*) from XWikiSpace"));
        assertTrue(HqlQueryUtils.isSafe("select count(space.*) from XWikiSpace space"));

        assertTrue(HqlQueryUtils.isSafe("select attachment.filename from XWikiAttachment attachment"));
        assertTrue(HqlQueryUtils.isSafe("select count(*) from XWikiAttachment"));

        // not allowed

        assertFalse(HqlQueryUtils.isSafe("select name from OtherTable"));
        assertFalse(HqlQueryUtils.isSafe("select doc.* from XWikiDocument doc, XWikiSpace space"));
        assertFalse(HqlQueryUtils.isSafe("select * from XWikiDocument doc"));
        assertFalse(HqlQueryUtils.isSafe("select * from XWikiAttachment"));
        assertFalse(HqlQueryUtils.isSafe("select attachment.mimeType from XWikiAttachment attachment"));
        assertFalse(HqlQueryUtils
            .isSafe("select doc.name, ot.field from XWikiDocument doc, XWikiSpace space, OtherTable as ot"));
        assertFalse(HqlQueryUtils.isSafe("select count(*) from OtherTable"));
        assertFalse(HqlQueryUtils.isSafe("select count(other.*) from OtherTable other"));
        assertFalse(
            HqlQueryUtils.isSafe("select doc.fullName from XWikiDocument doc union all select name from OtherTable"));
        assertFalse(HqlQueryUtils
            .isSafe("select doc.fullName from XWikiDocument doc where 1<>'1\\'' union select name from OtherTable #'"));
        assertFalse(HqlQueryUtils.isSafe(
            "select doc.fullName from XWikiDocument doc where $$='$$=concat( chr( 61 ),(chr( 39 )) ) ;select 1 -- comment'"));
        assertFalse(HqlQueryUtils.isSafe(
            "select doc.fullName from XWikiDocument doc where NVL(TO_CHAR(DBMS_XMLGEN.getxml('select 1 where 1337>1')),'1')!='1'"));
    }

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
