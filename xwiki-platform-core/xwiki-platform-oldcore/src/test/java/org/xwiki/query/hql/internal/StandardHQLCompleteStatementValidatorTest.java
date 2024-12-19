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
package org.xwiki.query.hql.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link StandardHQLCompleteStatementValidator}.
 * 
 * @version $Id$
 */
@ComponentTest
public class StandardHQLCompleteStatementValidatorTest
{
    @InjectMockComponents
    private StandardHQLCompleteStatementValidator validator;

    private void assertSafe(String statement)
    {
        assertTrue(this.validator.isSafe(statement).get());
    }

    private void assertNotSafe(String statement)
    {
        assertFalse(this.validator.isSafe(statement).get());
    }

    @Test
    void safe()
    {
        assertSafe("select name from XWikiDocument");
        assertSafe("select doc.name, space.name from XWikiDocument doc, XWikiSpace space");
        assertSafe("select doc.name, space.name from XWikiDocument doc, XWikiSpace space, OtherTable as ot");
        assertSafe("select count(name) from XWikiDocument");
        assertSafe("select count(doc.name) from XWikiDocument doc");
        assertSafe("select doc.fullName from XWikiDocument as doc, com.xpn.xwiki.objects.StringProperty as str");

        assertSafe("select count(*) from XWikiSpace");
        assertSafe("select count(space.*) from XWikiSpace space");

        assertSafe("select attachment.filename from XWikiAttachment attachment");
        assertSafe("select count(*) from XWikiAttachment");

        assertSafe("select name from XWikiDocument where lower(name)='name'");

        assertSafe("select doc.fullName from XWikiDocument doc where doc.name = :name");
    }

    @Test
    void notSafe()
    {
        assertNotSafe("select name from OtherTable");
        assertNotSafe("select doc.* from XWikiDocument doc, XWikiSpace space");
        assertNotSafe("select * from XWikiDocument doc");
        assertNotSafe("select * from XWikiAttachment");
        assertNotSafe("select attachment.mimeType from XWikiAttachment attachment");
        assertNotSafe("select doc.name, ot.field from XWikiDocument doc, XWikiSpace space, OtherTable as ot");
        assertNotSafe("select count(*) from OtherTable");
        assertNotSafe("select count(other.*) from OtherTable other");
        assertNotSafe("select name from XWikiDocument where notsafe(name)='name'");
        assertNotSafe("select doc.fullName from XWikiDocument doc union all select name from OtherTable");
        assertNotSafe(
            "select doc.fullName from XWikiDocument doc where 1<>'1\\'' union select name from OtherTable #'");
        assertNotSafe(
            "select doc.fullName from XWikiDocument doc where $$='$$=concat( chr( 61 ),(chr( 39 )) ) ;select 1 -- comment'");
        assertNotSafe(
            "select doc.fullName from XWikiDocument doc where NVL(TO_CHAR(DBMS_XMLGEN.getxml('select 1 where 1337>1')),'1')!='1'");
    }
}
