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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.SecureQuery;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link ScriptQuery}.
 * 
 * @version $Id$
 */
@ComponentTest
class ScriptQueryTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Test
    void execute() throws QueryException
    {
        Query query = mock(Query.class);
        ScriptQuery scriptQuery = new ScriptQuery(query, this.componentManager);

        scriptQuery.execute();

        verify(query).execute();
    }

    @Test
    void executeWithSwitchedAuthor() throws Exception
    {
        SecureQuery query = mock(SecureQuery.class);
        ScriptQuery scriptQuery = new ScriptQuery(query, this.componentManager);

        DocumentReference author = new DocumentReference("wiki", "XWiki", "author");
        DocumentReference document = new DocumentReference("wiki", "space", "document");

        scriptQuery.setQueryAuthor(author, document);

        scriptQuery.execute();

        verify(this.authorExecutor).call(any(), eq(author), eq(document));
    }
}
