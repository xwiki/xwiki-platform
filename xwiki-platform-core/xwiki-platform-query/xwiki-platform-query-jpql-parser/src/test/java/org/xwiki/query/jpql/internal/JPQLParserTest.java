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
package org.xwiki.query.jpql.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.query.internal.jpql.node.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link JPQLParser}.
 *
 * @version $Id$
 */
class JPQLParserTest
{
    private JPQLParser parser = new JPQLParser();

    @Test
    void parseVariousJPQL() throws Exception
    {
        // quotes
        parser.parse("select a from A as a where a.f='str'");
        parser.parse("select a from A as a where a.f=\"str\"");

        // order by
        parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by g.number");
        parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by g.number desc");
        parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by g.number asc");
        parser.parse("select doc from Document doc, doc.object(XWiki.XWikiGroups) as g order by 1 asc");

        // group by
        parser.parse("select doc.XWD_FULLNAME from Document doc group by doc.XWD_FULLNAME");
        parser.parse("select doc.XWD_FULLNAME from Document doc group by 1");

        // member of
        parser.parse("select a from A as a where :param member of a.prop");

        // input parameters
        parser.parse("select a from A as a where a.p = ?1 or :name = a.p or ?2 member of a.p or a.p like :qwe");
    }

    @Test
    void parseXWQLExtensions() throws Exception
    {
        // object() in from clause
        parser.parse("select doc from Document as doc, doc.object('XWiki.Test') as test where test.some=1");
        parser.parse("select doc from Document as doc, doc.object(XWiki.Test) as test where test.some=1");

        // object() in where clause
        parser.parse("select doc from Document as doc where doc.object('XWiki.Test').prop=1");
        parser.parse("select doc from Document as doc where doc.object(XWiki.Test).prop=1");
    }

    @Test
    void parseMethodsInLike() throws Exception
    {
        Start result = this.parser.parse("SELECT doc.fullName FROM Document doc, doc.object(XWiki.XWikiUsers) obj "
            + "where obj.first_name like LOWER('%DMIN%')");
        assertEquals("SELECT doc.fullName FROM Document doc , doc.object ( XWiki.XWikiUsers ) obj where "
            + "obj.first_name like LOWER ( '%DMIN%' )  ", result.toString());
        result = this.parser.parse("SELECT doc.fullName FROM Document doc, doc.object(XWiki.XWikiUsers) obj "
            + "where obj.first_name like LOWER(CONCAT('%', 'DMIN%'))");
        assertEquals("SELECT doc.fullName FROM Document doc , doc.object ( XWiki.XWikiUsers ) obj where "
            + "obj.first_name like LOWER ( CONCAT ( '%' , 'DMIN%' ) )  ", result.toString());
    }
}
