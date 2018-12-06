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
package com.xpn.xwiki.internal.objects.classes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExplicitlyAllowedValuesDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class ExplicitlyAllowedValuesDBListQueryBuilderTest
{
    @Rule
    public MockitoComponentMockingRule<QueryBuilder<DBListClass>> mocker =
        new MockitoComponentMockingRule<QueryBuilder<DBListClass>>(ExplicitlyAllowedValuesDBListQueryBuilder.class);

    private AuthorizationManager authorizationManager;

    private QueryManager secureQueryManager;

    private DBListClass dbListClass = new DBListClass();

    @Before
    public void configure() throws Exception
    {
        this.authorizationManager = this.mocker.getInstance(AuthorizationManager.class);
        this.secureQueryManager = this.mocker.getInstance(QueryManager.class, "secure");

        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        when(ownerDocument.getDocumentReference()).thenReturn(new DocumentReference("math", "Some", "Page"));
        when(ownerDocument.getAuthorReference()).thenReturn(new DocumentReference("wiki", "Users", "alice"));

        BaseClass xclass = new BaseClass();
        xclass.setDocumentReference(ownerDocument.getDocumentReference());

        this.dbListClass.setOwnerDocument(ownerDocument);
        this.dbListClass.setObject(xclass);
        this.dbListClass.setName("category");
        this.dbListClass.setSql("select ...");
    }

    @Test
    public void buildWithScriptRight() throws Exception
    {
        DocumentReference authorReference = this.dbListClass.getOwnerDocument().getAuthorReference();
        when(this.authorizationManager.hasAccess(Right.SCRIPT, authorReference, dbListClass.getReference()))
            .thenReturn(true);

        AuthorExecutor authorExector = this.mocker.getInstance(AuthorExecutor.class);
        String evaluatedStatement = "test";
        when(authorExector.call(any(), eq(authorReference), eq(this.dbListClass.getDocumentReference())))
            .thenReturn(evaluatedStatement);

        Query query = mock(Query.class);
        when(this.secureQueryManager.createQuery(evaluatedStatement, Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(this.dbListClass));
    }

    @Test
    public void buildWithoutScriptRight() throws Exception
    {
        Query query = mock(Query.class);
        when(this.secureQueryManager.createQuery(this.dbListClass.getSql(), Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(this.dbListClass));

        verify(query).setWiki("math");
    }
}
