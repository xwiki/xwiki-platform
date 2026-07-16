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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class StandardHQLCompleteStatementValidatorTest
{
    @InjectMockComponents
    private StandardHQLCompleteStatementValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {
        "select name from XWikiDocument",
        "select doc.name, space.name from XWikiDocument doc, XWikiSpace space",
        "select doc.name, space.name from XWikiDocument doc, XWikiSpace space, OtherTable as ot",
        "select count(name) from XWikiDocument",
        "select count(doc.name) from XWikiDocument doc",
        "select doc.fullName from XWikiDocument as doc, com.xpn.xwiki.objects.StringProperty as str",
        "select count(*) from XWikiSpace",
        "select COUNT(*) from XWikiSpace",
        "select count(space.*) from XWikiSpace space",
        "select attachment.filename from XWikiAttachment attachment",
        "select count(*) from XWikiAttachment",
        "select name from XWikiDocument where lower(name)='name'",
        "select name from XWikiDocument where LOWER(name)='name'",
        "select doc.fullName from XWikiDocument doc where doc.name = :name",
        // Test different plain values.
        "select 1 from OtherTable",
        "select 1.0 from OtherTable",
        "select 'test' from OtherTable",
        "select null from OtherTable",
        "select x'1A' from OtherTable"
    })
    void safe(String statement)
    {
        assertTrue(this.validator.isSafe(statement).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "select name from OtherTable",
        "select doc.* from XWikiDocument doc, XWikiSpace space",
        "select * from XWikiDocument doc",
        "select * from XWikiAttachment",
        "select attachment.mimeType from XWikiAttachment attachment",
        "select doc.name, ot.field from XWikiDocument doc, XWikiSpace space, OtherTable as ot",
        "select count(*) from OtherTable",
        "select count(other.*) from OtherTable other",
        "select name from XWikiDocument where notsafe(name)='name'",
        "select doc.fullName from XWikiDocument doc union all select name from OtherTable",
        "select doc.fullName from XWikiDocument doc where 1<>'1\\'' union select name from OtherTable #'",
        "select doc.fullName from XWikiDocument doc where $$='$$=concat( chr( 61 ),(chr( 39 )) ) ;select 1 -- comment'",
        "select doc.fullName from XWikiDocument doc where NVL(TO_CHAR(DBMS_XMLGEN.getxml('select 1 where "
            + "1337>1')),'1')!='1'",
        "select name from OtherTable",
        "from OtherTable"
    })
    void notSafe(String statement)
    {
        assertFalse(this.validator.isSafe(statement).orElseThrow());
    }
}
