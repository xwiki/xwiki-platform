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
package org.xwiki.index.tree.internal.nestedpages.query;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ExcludedSpaceFilter}.
 * 
 * @version $Id$
 * @since 10.4
 */
@ComponentTest
public class ExcludedSpaceFilterTest
{
    @InjectMockComponents
    private ExcludedSpaceFilter excludedSpaceFilter;

    @Test
    public void filterStatement()
    {
        assertEquals("select reference from XWikiSpace space  where reference not in (:excludedSpaces) order by name",
            this.excludedSpaceFilter.filterStatement("select reference from XWikiSpace space order by name",
                Query.HQL));

        assertEquals(
            "select reference from XWikiSpace space where parent is null and reference not in (:excludedSpaces) ",
            this.excludedSpaceFilter.filterStatement("select reference from XWikiSpace space where parent is null",
                Query.HQL));

        String inputStatement = StringUtils.join("select xwikiPage.reference reference, xwikiPage.terminal terminal",
          "from (",
            "(select XWS_REFERENCE reference, 0 terminal, XWS_NAME pageName from xwikispace)",
            "union all",
            "(select XWD_FULLNAME reference, 1 terminal, XWD_NAME pageName from xwikidoc doc",
              " where XWD_TRANSLATION = 0 and XWD_NAME <> 'WebHome')",
          ") xwikiPage",
          "order by lower(xwikiPage.pageName), xwikiPage.pageName", '\n');

        assertEquals(StringUtils.join("select xwikiPage.reference reference, xwikiPage.terminal terminal",
          "from (",
            "(select XWS_REFERENCE reference, 0 terminal, XWS_NAME pageName from xwikispace ",
              "where XWS_REFERENCE not in (:excludedSpaces) )",
            "union all",
            "(select XWD_FULLNAME reference, 1 terminal, XWD_NAME pageName from xwikidoc doc",
              " where XWD_TRANSLATION = 0 and XWD_NAME <> 'WebHome')",
          ") xwikiPage",
          "order by lower(xwikiPage.pageName), xwikiPage.pageName", '\n'),
          this.excludedSpaceFilter.filterStatement(inputStatement, Query.HQL));
    }
}
