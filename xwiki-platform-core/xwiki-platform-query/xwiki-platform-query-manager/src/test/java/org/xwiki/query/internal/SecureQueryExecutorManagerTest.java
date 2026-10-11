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
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.PROGRAM;

/**
 * Tests for {@link SecureQueryExecutorManager}
 *
 * @version $Id$
 */
@ComponentTest
class SecureQueryExecutorManagerTest
{
    @InjectMockComponents
    private SecureQueryExecutorManager executor;

    @MockComponent
    private QueryExecutorManager defaultQueryExecutorManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    private boolean hasProgrammingRight;

    @BeforeEach
    void before()
    {
        when(this.authorization.hasAccess(PROGRAM))
            .then(invocation -> SecureQueryExecutorManagerTest.this.hasProgrammingRight);

        this.hasProgrammingRight = true;
    }

    // Tests

    @Test
    void executeNotSecureQueryWithoutProgrammingRight()
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
    void executeNotSecureQueryWithProgrammingRight() throws QueryException
    {
        this.hasProgrammingRight = true;

        Query query = mock(Query.class);

        this.executor.execute(query);

        verify(this.authorization).hasAccess(PROGRAM);
        verify(this.defaultQueryExecutorManager).execute(query);
    }

    @Test
    void executeSecureQueryWithoutCheckCurrentAuthor() throws QueryException
    {
        DefaultQuery query = new DefaultQuery("statement", "language", this.executor);

        assertFalse(query.isCurrentAuthorChecked());

        this.executor.execute(query);

        assertTrue(query.isCurrentAuthorChecked());
    }
}
