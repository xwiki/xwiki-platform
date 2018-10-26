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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link org.xwiki.query.internal.LanguageQueryFilter}
 *
 * @version $Id$
 * @since 5.1M2
 */
@ComponentTest
public class LanguageQueryFilterTest
{
    @InjectMockComponents
    private LanguageQueryFilter filter;

    @Test
    public void filterStatementWhenStatementMatches()
    {
        String result = this.filter.filterStatement(
            "select doc.fullName from XWikiDocument doc ...", Query.HQL);
        assertEquals("select doc.fullName, doc.language from XWikiDocument doc ...", result);
    }

    @Test
    public void filterStatementWhenStatementDoesntMatches()
    {
        String result = this.filter.filterStatement("select whatever", Query.HQL);
        assertEquals("select whatever", result);
    }

    @Test
    public void filterStatementWhenStatementIsNotHQL()
    {
        String result = this.filter.filterStatement(
            "select doc.fullName from XWikiDocument doc ...", Query.XWQL);
        assertEquals("select doc.fullName from XWikiDocument doc ...", result);
    }

    @Test
    public void filterResults()
    {
        List<String> items = Arrays.asList("one", "two");
        List<String> result = this.filter.filterResults(items);
        assertSame(items, result);
    }
}
