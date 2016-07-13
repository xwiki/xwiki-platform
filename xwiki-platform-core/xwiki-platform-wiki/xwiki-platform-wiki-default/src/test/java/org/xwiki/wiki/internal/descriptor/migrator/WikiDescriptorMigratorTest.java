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
package org.xwiki.wiki.internal.descriptor.migrator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiDescriptorMigratorTest
{
    @Rule
    public MockitoComponentMockingRule<WikiDescriptorMigrator> mocker =
            new MockitoComponentMockingRule(WikiDescriptorMigrator.class, HibernateDataMigration.class,
                    "R54300WikiDescriptorMigration");

    private QueryManager queryManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private XWikiContext context;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        queryManager = mocker.getInstance(QueryManager.class);
        documentReferenceResolver= mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);

        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
    }

    @Test
    public void hibernateMigrate() throws Exception
    {
        List<String> documentList = new ArrayList<>();
        documentList.add("XWiki.XWikiServerSubwiki1");

        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        when(query.<String>execute()).thenReturn(documentList);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(documentReferenceResolver.resolve(documentList.get(0))).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(documentReference, context)).thenReturn(document);

        List<BaseObject> objects = new ArrayList<>();
        objects.add(null);

        BaseObject object = mock(BaseObject.class);
        objects.add(object);

        when(document.getXObjects(XWikiServerClassDocumentInitializer.SERVER_CLASS)).thenReturn(objects);
        when(object.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME)).thenReturn("");

        // Test
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify
        verify(object).setStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME, "Subwiki1");
        verify(xwiki).saveDocument(document, "[UPGRADE] Set a default pretty name.", context);

    }

    @Test
    public void hibernateMigrateWhenQueryException() throws Exception
    {
        List<String> documentList = new ArrayList<>();
        documentList.add("XWiki.XWikiServerSubwiki1");

        Exception exception = new QueryException("error in queryManager.createQuery()", null, null);
        when(queryManager.createQuery(anyString(), eq(Query.HQL))).thenThrow(exception);

        // Test
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify
        verify(mocker.getMockedLogger()).error("Failed to perform a query on the main wiki.", exception);
    }

    @Test
    public void hibernateMigrateWhenXWikiException() throws Exception
    {
        List<String> documentList = new ArrayList<>();
        documentList.add("XWiki.XWikiServerSubwiki1");

        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        when(query.<String>execute()).thenReturn(documentList);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(documentReferenceResolver.resolve(documentList.get(0))).thenReturn(documentReference);

        Exception exception = new XWikiException(0, 0, "error in xwiki.getDocument()");
        when(xwiki.getDocument(documentReference, context)).thenThrow(exception);

        // Test
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify
        verify(mocker.getMockedLogger()).warn("Failed to get or save the wiki descriptor document [{}]. You" +
                " will not see the corresponding wiki in the Wiki Index unless you give it a Pretty Name manually. {}",
                documentList.get(0), ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    public void shouldExecuteTrue() throws Exception
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        // Test
        assertTrue(mocker.getComponentUnderTest().shouldExecute(null));
    }

    @Test
    public void shouldExecuteFalse() throws Exception
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        // Test
        assertFalse(mocker.getComponentUnderTest().shouldExecute(null));
    }
}