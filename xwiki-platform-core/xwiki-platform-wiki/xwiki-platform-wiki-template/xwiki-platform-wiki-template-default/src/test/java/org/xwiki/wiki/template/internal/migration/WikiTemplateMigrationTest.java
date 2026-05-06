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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link WikiTemplateMigration}.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@ComponentTest
class WikiTemplateMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private WikiTemplateMigration wikiTemplateMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private Execution execution;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");

        this.query = mock(Query.class);
        when(this.queryManager.createQuery(any(), eq(Query.XWQL))).thenReturn(this.query);
    }

    @Test
    void upgrade() throws Exception
    {
        // Mocks
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");

        String document1 = "XWiki.XWikiServerTemplate";
        String document2 = "XWiki.XWikiServerNotATemplateAnymore";
        String document3 = "XWiki.XWikiServerNotATemplate";
        String document4 = "XWiki.XWikiServerTemplateNow";

        DocumentReference documentReference1 = new DocumentReference("mainWiki", "XWiki", "XWikiServerTemplate");
        DocumentReference documentReference2 =
            new DocumentReference("mainWiki", "XWiki", "XWikiServerNotATemplateAnymore");
        DocumentReference documentReference3 = new DocumentReference("mainWiki", "XWiki", "XWikiServerNotATemplate");
        DocumentReference documentReference4 = new DocumentReference("mainWiki", "XWiki", "XWikiServerTemplateNow");

        XWikiDocument doc1 = mock(XWikiDocument.class);
        XWikiDocument doc2 = mock(XWikiDocument.class);
        XWikiDocument doc3 = mock(XWikiDocument.class);
        XWikiDocument doc4 = mock(XWikiDocument.class);

        // Return the document
        List<String> documents = new ArrayList<>();
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);
        documents.add(document4);
        when(this.query.<String>execute()).thenReturn(documents);

        // Document Reference resolver
        when(this.documentReferenceResolver.resolve(document1)).thenReturn(documentReference1);
        when(this.documentReferenceResolver.resolve(document2)).thenReturn(documentReference2);
        when(this.documentReferenceResolver.resolve(document3)).thenReturn(documentReference3);
        when(this.documentReferenceResolver.resolve(document4)).thenReturn(documentReference4);

        // Document getter
        when(this.xwiki.getDocument(documentReference1, this.xcontext)).thenReturn(doc1);
        when(this.xwiki.getDocument(documentReference2, this.xcontext)).thenReturn(doc2);
        when(this.xwiki.getDocument(documentReference3, this.xcontext)).thenReturn(doc3);
        when(this.xwiki.getDocument(documentReference4, this.xcontext)).thenReturn(doc4);

        // WikiManager.WikiTemplateClass reference
        DocumentReference templateClassReference =
            new DocumentReference("mainWiki", "WikiManager", "WikiTemplateClass");

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
        when(doc1.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class)))
            .thenReturn(wikitemplateObj1);
        when(doc2.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class)))
            .thenReturn(wikitemplateObj2);
        when(doc3.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class)))
            .thenReturn(wikitemplateObj3);
        when(doc4.getXObject(eq(templateClassReference), eq(true), any(XWikiContext.class)))
            .thenReturn(wikitemplateObj4);

        when(wikitemplateObj1.getIntValue("iswikitemplate", 1)).thenReturn(1);
        when(wikitemplateObj2.getIntValue("iswikitemplate", 1)).thenReturn(0);
        when(wikitemplateObj3.getIntValue("iswikitemplate", 0)).thenReturn(0);
        when(wikitemplateObj4.getIntValue("iswikitemplate", 0)).thenReturn(1);

        // Run
        this.wikiTemplateMigration.hibernateMigrate();

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

        // all the documents have been saved
        verify(this.xwiki).saveDocument(doc1, "[UPGRADE] Upgrade the template section.", this.xcontext);
        verify(this.xwiki).saveDocument(doc2, "[UPGRADE] Upgrade the template section.", this.xcontext);
        verify(this.xwiki).saveDocument(doc3, "[UPGRADE] Upgrade the template section.", this.xcontext);
        verify(this.xwiki).saveDocument(doc4, "[UPGRADE] Upgrade the template section.", this.xcontext);
    }

    @Test
    void shouldExecuteTrue()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        XWikiDBVersion version = new XWikiDBVersion(52000);
        assertTrue(this.wikiTemplateMigration.shouldExecute(version));
    }

    @Test
    void shouldExecuteFalse()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        XWikiDBVersion version = new XWikiDBVersion(52000);
        assertFalse(this.wikiTemplateMigration.shouldExecute(version));
    }
}
