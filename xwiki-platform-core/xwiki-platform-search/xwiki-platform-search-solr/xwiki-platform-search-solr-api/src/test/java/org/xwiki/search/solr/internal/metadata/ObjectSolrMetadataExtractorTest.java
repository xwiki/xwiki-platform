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
package org.xwiki.search.solr.internal.metadata;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.reference.DocumentSolrReferenceResolver;
import org.xwiki.search.solr.internal.reference.ObjectSolrReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link ObjectSolrMetadataExtractor}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList({SolrSearchCoreUtils.class, SolrLinkSerializer.class, ObjectSolrReferenceResolver.class,
    DocumentSolrReferenceResolver.class})
@ReferenceComponentList
class ObjectSolrMetadataExtractorTest
{
    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("wiki", List.of("Path", "To", "Page"), "WebHome");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private ObjectSolrMetadataExtractor metadataExtractor;

    private XWikiDocument document;

    private BaseObject xobject;

    @BeforeEach
    void setUp() throws Exception
    {
        // XWikiDocument
        this.document = new XWikiDocument(DOCUMENT_REFERENCE);

        // Setup xclass
        BaseClass xclass = this.document.getXClass();
        xclass.addTextField("property", "Propertt", 42);

        // Setup xobject
        this.xobject = this.document.newXObject(new DocumentReference("wiki", List.of("Path", "To"), "Class"),
            this.oldcore.getXWikiContext());

        this.xobject.setStringValue("property", "value");

        this.oldcore.getSpyXWiki().saveDocument(this.document, this.oldcore.getXWikiContext());
    }

    @Test
    void getObjectDocument() throws Exception
    {
        EntityReference objectReference = this.xobject.getReference();
        DocumentReference documentReference = (DocumentReference) objectReference.getParent();

        ///////////////////
        // Call

        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(objectReference);

        ///////////////////
        // Assert

        assertEquals("wiki:Path.To.Page.WebHome^Path.To.Class[0]", solrDocument.getFieldValue(FieldUtils.ID));

        assertEquals("OBJECT", solrDocument.getFieldValue(FieldUtils.TYPE));

        assertEquals(documentReference.getWikiReference().getName(), solrDocument.getFieldValue(FieldUtils.WIKI));
        assertEquals(List.of("Path", "To", "Page"), solrDocument.getFieldValues(FieldUtils.SPACES));
        assertEquals(documentReference.getName(), solrDocument.getFieldValue(FieldUtils.NAME));
        assertEquals(List.of("Path", "Path.To", "Path.To.Page"), solrDocument.getFieldValues(FieldUtils.SPACE_PREFIX));
        assertEquals(List.of("0/Path.", "1/Path.To.", "2/Path.To.Page."),
            solrDocument.getFieldValues(FieldUtils.SPACE_FACET));
    }
}
