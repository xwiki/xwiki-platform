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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SecureQueryExecutorManager}
 *
 * @version $Id$
 */
@ComponentTest
public class SecureQueryExecutorManagerTest
{
    @InjectMockComponents
    private SecureQueryExecutorManager executor;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    private boolean hasProgrammingRight;

    @BeforeEach
    public void before() throws Exception
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return hasProgrammingRight;
            }
        });

        this.hasProgrammingRight = true;
    }

    // Tests

    @Test
    public void executeNotSecureQueryWithoutProgrammingRight()
    {
        this.hasProgrammingRight = false;

        // Create a Query not implementing SecureQuery
        Query query = mock(Query.class);

        Throwable exception = assertThrows(QueryException.class, () -> {
            this.executor.execute(query);
        });
        assertEquals("Unsecure query require programming right. Query statement = [null]", exception.getMessage());
    }

    @Test
    public void executeNotSecureQueryWithProgrammingRight() throws QueryException
    {
        this.hasProgrammingRight = true;

        Query query = mock(Query.class);

        this.executor.execute(query);
    }

    @Test
    public void executeSecureQueryWithoutCheckCurrentAuthor() throws QueryException
    {
        DefaultQuery query = new DefaultQuery("statement", "language", this.executor);

        assertFalse(query.isCurrentAuthorChecked());

        this.executor.execute(query);

        assertTrue(query.isCurrentAuthorChecked());
    }
}
