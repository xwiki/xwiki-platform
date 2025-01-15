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

import java.io.Writer;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExplicitlyAllowedValuesDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
public class ExplicitlyAllowedValuesDBListQueryBuilderTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("math", "Some", "Page");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("wiki", "Users", "alice");

    private static final String SQL = "select ...";

    @InjectMockComponents
    private ExplicitlyAllowedValuesDBListQueryBuilder builder;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    @Named("secure")
    private QueryManager secureQueryManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private VelocityManager velocityManager;

    @Mock
    private VelocityEngine velocityEngine;

    private DBListClass dbListClass = new DBListClass();

    @BeforeEach
    public void configure() throws Exception
    {
        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        when(ownerDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(ownerDocument.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);

        BaseClass xclass = new BaseClass();
        xclass.setDocumentReference(ownerDocument.getDocumentReference());

        this.dbListClass.setOwnerDocument(ownerDocument);
        this.dbListClass.setObject(xclass);
        this.dbListClass.setName("category");
        this.dbListClass.setSql(SQL);

        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);
    }

    @Test
    public void buildWithScriptRight() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(true);

        String evaluatedStatement = "test";
        when(this.velocityEngine.evaluate(any(), any(), any(), eq(SQL))).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<Writer>getArgument(1).write(evaluatedStatement);

                return true;
            }
        });
        when(this.authorExecutor.call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE))).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.<Callable<String>>getArgument(0).call();
            }
        });

        Query query = mock(Query.class);
        when(this.secureQueryManager.createQuery(evaluatedStatement, Query.HQL)).thenReturn(query);

        assertSame(query, this.builder.build(this.dbListClass));
    }

    @Test
    public void buildWithoutScriptRight() throws Exception
    {
        Query query = mock(Query.class);
        when(this.secureQueryManager.createQuery(this.dbListClass.getSql(), Query.HQL)).thenReturn(query);

        assertSame(query, this.builder.build(this.dbListClass));

        verify(query).setWiki("math");
    }
}
