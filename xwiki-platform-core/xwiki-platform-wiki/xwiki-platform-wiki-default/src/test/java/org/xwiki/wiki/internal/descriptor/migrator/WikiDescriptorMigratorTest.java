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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class WikiDescriptorMigratorTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private WikiDescriptorMigrator wikiDescriptorMigrator;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private Logger logger;

    private XWikiContext context;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.context = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.context);
        this.xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void hibernateMigrate() throws Exception
    {
        List<String> documentList = new ArrayList<>();
        documentList.add("XWiki.XWikiServerSubwiki1");

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(query.<String>execute()).thenReturn(documentList);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(this.documentReferenceResolver.resolve(documentList.getFirst())).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.context)).thenReturn(document);

        List<BaseObject> objects = new ArrayList<>();
        objects.add(null);

        BaseObject object = mock(BaseObject.class);
        objects.add(object);

        when(document.getXObjects(XWikiServerClassDocumentInitializer.SERVER_CLASS)).thenReturn(objects);
        when(object.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME)).thenReturn("");

        // Test
        this.wikiDescriptorMigrator.hibernateMigrate();

        // Verify
        verify(object).setStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME, "Subwiki1");
        verify(this.xwiki).saveDocument(document, "[UPGRADE] Set a default pretty name.", this.context);
    }

    @Test
    void hibernateMigrateWhenQueryException() throws Exception
    {
        Exception exception = new QueryException("error in queryManager.createQuery()", null, null);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenThrow(exception);

        // Test
        this.wikiDescriptorMigrator.hibernateMigrate();

        // Verify
        verify(this.logger).error("Failed to perform a query on the main wiki.", exception);
    }

    @Test
    void hibernateMigrateWhenXWikiException() throws Exception
    {
        List<String> documentList = new ArrayList<>();
        documentList.add("XWiki.XWikiServerSubwiki1");

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(query.<String>execute()).thenReturn(documentList);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(this.documentReferenceResolver.resolve(documentList.getFirst())).thenReturn(documentReference);

        Exception exception = new XWikiException(0, 0, "error in xwiki.getDocument()");
        when(this.xwiki.getDocument(documentReference, this.context)).thenThrow(exception);

        // Test
        this.wikiDescriptorMigrator.hibernateMigrate();

        // Verify
        verify(this.logger).warn(
            "Failed to get or save the wiki descriptor document [{}]. You"
                + " will not see the corresponding wiki in the Wiki Index unless you give it a Pretty Name manually. {}",
            documentList.getFirst(), ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void shouldExecuteTrue()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        // Test
        assertTrue(this.wikiDescriptorMigrator.shouldExecute(null));
    }

    @Test
    void shouldExecuteFalse()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        // Test
        assertFalse(this.wikiDescriptorMigrator.shouldExecute(null));
    }
}
