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
package org.xwiki.wiki.user.internal.membermigration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMemberRightsMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class DefaultMemberRightsMigratorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMemberRightsMigrator> mocker =
            new MockitoComponentMockingRule(DefaultMemberRightsMigrator.class);

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private QueryManager queryManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));

        mocker.registerMockComponent(QueryFilter.class, "unique");

        xcontext = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);

        when(xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void upgradeRights() throws Exception
    {
        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);

        // XWikiRights
        String docFullname1 = "Space.Doc1";
        DocumentReference docRef1 = new DocumentReference("subwiki", "Space", "Doc1");
        XWikiDocument doc1 = mock(XWikiDocument.class);
        when(documentReferenceResolver.resolve(eq(docFullname1), any(WikiReference.class))).thenReturn(docRef1);
        when(xwiki.getDocument(docRef1, xcontext)).thenReturn(doc1);

        BaseObject obj1 = mock(BaseObject.class);
        List<BaseObject> objList = new ArrayList<BaseObject>();
        objList.add(null);
        objList.add(obj1);
        when(doc1.getXObjects(eq(new DocumentReference("subwiki", "XWiki", "XWikiRights")))).thenReturn(objList);
        when(obj1.getLargeStringValue("groups")).thenReturn("Test,,XWiki.XWikiAllGroup,Test2");

        // XWikiGlobalRights
        String docFullname2 = "Space.Doc2";
        DocumentReference docRef2 = new DocumentReference("subwiki", "Space", "Doc2");
        XWikiDocument doc2 = mock(XWikiDocument.class);
        when(documentReferenceResolver.resolve(eq(docFullname2), any(WikiReference.class))).thenReturn(docRef2);
        when(xwiki.getDocument(docRef2, xcontext)).thenReturn(doc2);

        BaseObject obj2 = mock(BaseObject.class);
        List<BaseObject> objList2 = new ArrayList<BaseObject>();
        objList2.add(obj2);
        when(doc2.getXObjects(eq(new DocumentReference("subwiki", "XWiki", "XWikiGlobalRights")))).thenReturn(objList2);
        when(obj2.getLargeStringValue("groups")).thenReturn("XWiki.XWikiAllGroup");

        // Query
        List<String> results = new ArrayList<String>();
        results.add(docFullname1);
        results.add(docFullname2);
        when(query.<String>execute()).thenReturn(results);

        // Test
        mocker.getComponentUnderTest().upgradeRights("subwiki");

        // Verify XWikiRights
        verify(obj1).setLargeStringValue(eq("groups"), eq("Test,,XWiki.XWikiMemberGroup,Test2"));
        verify(xwiki).saveDocument(doc1, "Set rights for XWikiMemberGroup", xcontext);

        // Verify XWikiGlobalRights
        verify(obj2).setLargeStringValue(eq("groups"), eq("XWiki.XWikiMemberGroup"));
        verify(xwiki).saveDocument(doc2, "Set rights for XWikiMemberGroup", xcontext);
    }

    @Test
    public void upgradeRightsQueryException() throws Exception
    {
        Exception exception = new QueryException("error in createQuery", null, null);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenThrow(exception);

        // Test
        boolean exceptionCaught = false;
        try {
            mocker.getComponentUnderTest().upgradeRights("subwiki");
        } catch (DataMigrationException e) {
            exceptionCaught = true;
            assertEquals("Failed to create a query to get all document containing rights set for XWiki.XWikiAllGroup " +
                    "in wiki [subwiki].", e.getMessage());
        }

        assertTrue(exceptionCaught);
    }

    @Test
    public void upgradeRightsXWikiException() throws Exception
    {
        Exception exception = new XWikiException();

        // Query
        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);

        // XWikiRights
        String docFullname1 = "Space.Doc1";
        DocumentReference docRef1 = new DocumentReference("subwiki", "Space", "Doc1");
        when(documentReferenceResolver.resolve(eq(docFullname1), any(WikiReference.class))).thenReturn(docRef1);
        when(xwiki.getDocument(docRef1, xcontext)).thenThrow(exception);

        // Query
        List<String> results = new ArrayList<String>();
        results.add(docFullname1);
        when(query.<String>execute()).thenReturn(results);

        // Test
        boolean exceptionCaught = false;
        try {
            mocker.getComponentUnderTest().upgradeRights("subwiki");
        } catch (DataMigrationException e) {
            exceptionCaught = true;
            assertEquals("Failed to get or save documents in the wiki [subwiki].", e.getMessage());
        }

        assertTrue(exceptionCaught);
    }

}
