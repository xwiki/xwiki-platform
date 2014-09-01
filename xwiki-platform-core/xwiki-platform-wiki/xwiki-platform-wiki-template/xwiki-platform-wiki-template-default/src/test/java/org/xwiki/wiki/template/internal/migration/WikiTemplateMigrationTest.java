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
package org.xwiki.wiki.template.internal.migration;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link WikiTemplateMigration}.
 *
 * @since 5.4RC1
 * @version $Id$
 */
public class WikiTemplateMigrationTest
{
    @Rule
    public MockitoComponentMockingRule<WikiTemplateMigration> mocker =
            new MockitoComponentMockingRule(WikiTemplateMigration.class, HibernateDataMigration.class,
                    "R54000WikiTemplateMigration");

    private WikiDescriptorManager wikiDescriptorManager;

    private QueryManager queryManager;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private Execution execution;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private Query query;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        queryManager = mocker.getInstance(QueryManager.class);
        //documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class, String.class));
        execution = mock(Execution.class);
        mocker.registerComponent(Execution.class, execution);
        xcontext = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);
    }

    @Test
    public void upgrade() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");

        String document1 = "XWiki.XWikiServerTemplate";
        String document2 = "XWiki.XWikiServerNotATemplateAnymore";
        String document3 = "XWiki.XWikiServerNotATemplate";
        String document4 = "XWiki.XWikiServerTemplateNow";

        DocumentReference documentReference1 = new DocumentReference("mainWiki", "XWiki", "XWikiServerTemplate");
        DocumentReference documentReference2 = new DocumentReference("mainWiki", "XWiki", "XWikiServerNotATemplateAnymore");
        DocumentReference documentReference3 = new DocumentReference("mainWiki", "XWiki", "XWikiServerNotATemplate");
        DocumentReference documentReference4 = new DocumentReference("mainWiki", "XWiki", "XWikiServerTemplateNow");

        XWikiDocument doc1 = mock(XWikiDocument.class);
        XWikiDocument doc2 = mock(XWikiDocument.class);
        XWikiDocument doc3 = mock(XWikiDocument.class);
        XWikiDocument doc4 = mock(XWikiDocument.class);

        // Return the document
        List<String> documents = new ArrayList<String>();
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);
        documents.add(document4);
        when(query.<String>execute()).thenReturn(documents);

        // Document Reference resolver
        when(documentReferenceResolver.resolve(document1)).thenReturn(documentReference1);
        when(documentReferenceResolver.resolve(document2)).thenReturn(documentReference2);
        when(documentReferenceResolver.resolve(document3)).thenReturn(documentReference3);
        when(documentReferenceResolver.resolve(document4)).thenReturn(documentReference4);

        // Document getter
        when(xwiki.getDocument(documentReference1, xcontext)).thenReturn(doc1);
        when(xwiki.getDocument(documentReference2, xcontext)).thenReturn(doc2);
        when(xwiki.getDocument(documentReference3, xcontext)).thenReturn(doc3);
        when(xwiki.getDocument(documentReference4, xcontext)).thenReturn(doc4);

        // WikiManager.WikiTemplateClass reference
        DocumentReference templateClassReference = new DocumentReference("mainWiki", "WikiManager", "WikiTemplateClass");

        // XWiki.XWikiServerClass reference
        DocumentReference descriptorClassReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerClass");

        BaseObject descriptorObj1 = mock(BaseObject.class);
        BaseObject descriptorObj2 = mock(BaseObject.class);
        BaseObject descriptorObj3 = mock(BaseObject.class);
        BaseObject descriptorObj4 = mock(BaseObject.class);
        when(doc1.getXObject(eq(descriptorClassReference))).thenReturn(descriptorObj1);
        when(doc2.getXObject(eq(descriptorClassReference))).thenReturn(descriptorObj2);
        when(doc3.getXObject(eq(descriptorClassReference))).thenReturn(descriptorObj3);
        when(doc4.getXObject(eq(descriptorClassReference))).thenReturn(descriptorObj4);

        when(descriptorObj1.getIntValue("iswikitemplate", 0)).thenReturn(1);
        when(descriptorObj2.getIntValue("iswikitemplate", 0)).thenReturn(1);
        when(descriptorObj3.getIntValue("iswikitemplate", 0)).thenReturn(0);
        when(descriptorObj4.getIntValue("iswikitemplate", 0)).thenReturn(0);

        BaseObject wikitemplateObj1 = mock(BaseObject.class);
        BaseObject wikitemplateObj2 = mock(BaseObject.class);
        BaseObject wikitemplateObj3 = mock(BaseObject.class);
        BaseObject wikitemplateObj4 = mock(BaseObject.class);
        when(doc1.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class))).thenReturn(wikitemplateObj1);
        when(doc2.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class))).thenReturn(wikitemplateObj2);
        when(doc3.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class))).thenReturn(wikitemplateObj3);
        when(doc4.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class))).thenReturn(wikitemplateObj4);

        when(wikitemplateObj1.getIntValue("iswikitemplate", 1)).thenReturn(1);
        when(wikitemplateObj2.getIntValue("iswikitemplate", 1)).thenReturn(0);
        when(wikitemplateObj3.getIntValue("iswikitemplate", 0)).thenReturn(0);
        when(wikitemplateObj4.getIntValue("iswikitemplate", 0)).thenReturn(1);

        // Run
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify

        // doc1 is a template
        verify(wikitemplateObj1).setIntValue("iswikitemplate", 1);
        // doc2 is a not a template anymore
        verify(wikitemplateObj2).setIntValue("iswikitemplate", 0);
        // doc3 has never been a template
        verify(wikitemplateObj3).setIntValue("iswikitemplate", 0);
        // doc4 has became a template
        verify(wikitemplateObj4).setIntValue("iswikitemplate", 1);

        // the old properties has been removed
        verify(descriptorObj1).removeField("iswikitemplate");
        verify(descriptorObj2).removeField("iswikitemplate");
        verify(descriptorObj3).removeField("iswikitemplate");
        verify(descriptorObj4).removeField("iswikitemplate");

        // superadmin is the author
        DocumentReference superadmin = new DocumentReference("mainWiki", "XWiki", "superadmin");
        verify(doc1).setAuthorReference(eq(superadmin));
        verify(doc2).setAuthorReference(eq(superadmin));
        verify(doc3).setAuthorReference(eq(superadmin));
        verify(doc4).setAuthorReference(eq(superadmin));

        // all the documents has been saved
        verify(xwiki).saveDocument(doc1, "[UPGRADE] Upgrade the template section.", xcontext);
        verify(xwiki).saveDocument(doc2, "[UPGRADE] Upgrade the template section.", xcontext);
        verify(xwiki).saveDocument(doc3, "[UPGRADE] Upgrade the template section.", xcontext);
        verify(xwiki).saveDocument(doc4, "[UPGRADE] Upgrade the template section.", xcontext);
    }

    @Test
    public void shouldExecuteTrue() throws Exception
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        XWikiDBVersion version = new XWikiDBVersion(52000);
        assertTrue(mocker.getComponentUnderTest().shouldExecute(version));
    }

    @Test
    public void shouldExecuteFalse() throws Exception
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        XWikiDBVersion version = new XWikiDBVersion(52000);
        assertFalse(mocker.getComponentUnderTest().shouldExecute(version));
    }
}
