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
package org.xwiki.index.tree.internal.nestedspaces.query;

import org.junit.jupiter.api.Test;
import org.xwiki.index.tree.internal.nestedpages.query.AbstractQueryRegistrationHandlerTest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link QueryRegistrationHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class QueryRegistrationHandlerTest extends AbstractQueryRegistrationHandlerTest
{
    @InjectMockComponents
    private QueryRegistrationHandler queryRegistrationHandler;

    @Test
    void withoutConfiguration()
    {
        StringBuilder configurationString = mockConfigurationSetting("");

        this.queryRegistrationHandler.onEvent(mock(), mock(), mock());

        assertThat(configurationString.toString(),
            containsString("order by xwikiPageOrSpace.terminal, lower( xwikiPageOrSpace.title ),"));
        assertThat(configurationString.toString(),
            containsString("order by xwikiPageOrSpace.terminal, lower( xwikiPageOrSpace.pageOrSpaceName ),\n"));
    }

    @Test
    void withCollationConfiguration()
    {
        StringBuilder configurationString = mockConfigurationSetting("utf8mb4_german2_ci");

        this.queryRegistrationHandler.onEvent(mock(), mock(), mock());

        assertThat(configurationString.toString(), containsString(
            "order by xwikiPageOrSpace.terminal,  xwikiPageOrSpace.title collate utf8mb4_german2_ci,"));
        assertThat(configurationString.toString(), containsString(
            "order by xwikiPageOrSpace.terminal,  xwikiPageOrSpace.pageOrSpaceName collate utf8mb4_german2_ci,\n"));
    }
}
