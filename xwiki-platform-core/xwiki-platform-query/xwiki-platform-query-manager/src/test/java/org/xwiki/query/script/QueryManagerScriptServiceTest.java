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

package org.xwiki.query.script;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QueryManagerScriptService}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@ComponentTest
class QueryManagerScriptServiceTest
{
    @InjectMockComponents
    private QueryManagerScriptService queryManagerScriptService;

    @MockComponent
    @Named("secure")
    private QueryManager secureQueryManager;

    @MockComponent
    private ComponentManager componentManager;

    @Test
    void xwql() throws QueryException
    {
        String statement = "some statement";
        SecureQuery secureQuery = mock(SecureQuery.class);
        when(this.secureQueryManager.createQuery(statement, Query.XWQL)).thenReturn(secureQuery);
        ScriptQuery scriptQuery = new ScriptQuery(secureQuery, this.componentManager);
        assertEquals(scriptQuery, this.queryManagerScriptService.xwql(statement));
        verify(secureQuery).checkCurrentUser(false);
        verify(secureQuery).checkCurrentAuthor(true);
    }

    @Test
    void hql() throws QueryException
    {
        String statement = "some statement";
        SecureQuery secureQuery = mock(SecureQuery.class);
        when(this.secureQueryManager.createQuery(statement, Query.HQL)).thenReturn(secureQuery);
        ScriptQuery scriptQuery = new ScriptQuery(secureQuery, this.componentManager);
        assertEquals(scriptQuery, this.queryManagerScriptService.hql(statement));
        verify(secureQuery).checkCurrentUser(false);
        verify(secureQuery).checkCurrentAuthor(true);
    }

    @Test
    void createQuery() throws QueryException
    {
        String statement = "some statement";
        String language = "some language";
        SecureQuery secureQuery = mock(SecureQuery.class);
        when(this.secureQueryManager.createQuery(statement, language)).thenReturn(secureQuery);
        ScriptQuery scriptQuery = new ScriptQuery(secureQuery, this.componentManager);
        assertEquals(scriptQuery, this.queryManagerScriptService.createQuery(statement, language));
        verify(secureQuery).checkCurrentUser(true);
        verify(secureQuery).checkCurrentAuthor(true);
    }

    @Test
    void parameter()
    {
        assertEquals(new DefaultQueryParameter(null), this.queryManagerScriptService.parameter());
    }
}