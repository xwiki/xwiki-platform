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
package org.xwiki.query.internal;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TextQueryFilter}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class TextQueryFilterTest
{
    @InjectMockComponents
    private TextQueryFilter filter;

    @Test
    void filterStatement()
    {
        String result = this.filter
            .filterStatement(" \nseLEct  disTinCT   user.alias  as alias,\n user.name, user.age as unfilterable_age "
                + "\n\r\tfROm Users user\r\nwHere  user.age >= 18\n", Query.HQL);
        assertEquals("seLEct  disTinCT   user.alias  as alias,\n user.name, user.age as unfilterable_age "
            + "\n\r\tfROm Users user\r\n"
            + "wHere  (lower(str(user.alias)) like lower(:text) or lower(str(user.name)) like lower(:text))"
            + " and (user.age >= 18)", result);
    }

    @Test
    void filterStatementWithNoFilterableColumns()
    {
        String statement = "select age as unfilterable_age from Users";
        assertEquals(statement, this.filter.filterStatement(statement, Query.HQL));
    }

    @Test
    void filterStatementWithClob()
    {
        String statement = "select prop.textValue as stringValue, 1 as unfilterable0 "
            + "from BaseObject as obj, StringListProperty as prop "
            + "where obj.className = :className "
            + "  and obj.name <> :templateName "
            + "  and prop.id.id = obj.id "
            + "  and prop.id.name = :propertyName "
            + "  and not exists ("
            + "    select 1 from StringListProperty as prop2 "
            + "    where prop2.id.name = :propertyName "
            + "      and prop2.id.id < prop.id.id "
            + "      and FUNCTION('DBMS_LOB.COMPARE', prop2.textValue, prop.textValue) = 0"
            + "  ) "
            + "order by obj.id";
        String result = this.filter.filterStatement(statement, Query.HQL);
        assertEquals("select prop.textValue as stringValue, 1 as unfilterable0 "
            + "from BaseObject as obj, StringListProperty as prop "
            + "where (lower(prop.textValue) like lower(:text)) "
            + "and (obj.className = :className "
            + "  and obj.name <> :templateName "
            + "  and prop.id.id = obj.id "
            + "  and prop.id.name = :propertyName "
            + "  and not exists ("
            + "    select 1 from StringListProperty as prop2 "
            + "    where prop2.id.name = :propertyName "
            + "      and prop2.id.id < prop.id.id "
            + "      and FUNCTION('DBMS_LOB.COMPARE', prop2.textValue, prop.textValue) = 0"
            + "  )) "
            + "order by obj.id", result);
    }

    @Test
    void filterResults()
    {
        assertEquals(Arrays.asList("one", "two"), this.filter.filterResults(Arrays.asList("one", "two")),
            "Results should not be filtered.");
    }
}
