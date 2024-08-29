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
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.internal.model.reference.CurrentPageReferenceDocumentReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultLinkStore}.
 * 
 * @version $Id$
 */
@ComponentTest
@SolrComponentList
@ReferenceComponentList
@ComponentList({TestEnvironment.class, SolrSearchCoreUtils.class, SolrLinkSerializer.class,
    CurrentPageReferenceDocumentReferenceResolver.class})
class DefaultLinkStoreTest
{
    @InjectMockComponents
    private DefaultLinkStore store;

    @MockComponent
    @Named("document")
    private SolrReferenceResolver solrResolver;

    @MockComponent
    private DocumentAccessBridge bridge;

    @Inject
    private Solr solr;

    @Test
    void resolveEntities() throws Exception
    {
        SolrClient client = this.solr.getClient("search");

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Name", Locale.ROOT);
        String solrId = "wiki:Space.Name_";
        String solrReference = "document:wiki:Space.Name;";

        DocumentReference doesnotexist = new DocumentReference("does", "not", "exist", Locale.ROOT);
        DocumentReference document1DocumentLink = new DocumentReference("wiki", "space", "document1");
        DocumentReference page2DocumentLink = new DocumentReference("wiki", List.of("page1", "page2"), "WebHome");
        AttachmentReference attachmentDocumentLink = new AttachmentReference("file.ext", page2DocumentLink);

        SolrInputDocument inputDocument = new SolrInputDocument("id", solrId);

        inputDocument.setField(FieldUtils.VERSION, "1.1");

        inputDocument.setField(FieldUtils.REFERENCE, solrReference);
        inputDocument.addField(FieldUtils.LINKS, "entity:document:wiki:space.document1");
        inputDocument.addField(FieldUtils.LINKS, "entity:page:wiki:page1/page2");
        inputDocument.addField(FieldUtils.LINKS, "entity:page_attachment:wiki:page1/page2/file.ext");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:wiki:wiki");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:space:wiki:space");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:document:wiki:space.document1");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:page:wiki:page1");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:page:wiki:page1/page2");
        inputDocument.addField(FieldUtils.LINKS_EXTENDED, "entity:page_attachment:wiki:page1/page2/file.ext");

        client.add(inputDocument);
        client.commit();

        when(this.solrResolver.getId(doesnotexist)).thenReturn("doesnotexist");
        when(this.solrResolver.getId(documentReference)).thenReturn(solrId);

        // resolveLinkedEntities

        assertEquals(Set.of(), this.store.resolveLinkedEntities(doesnotexist));

        assertEquals(Set.of(attachmentDocumentLink, document1DocumentLink, page2DocumentLink),
            this.store.resolveLinkedEntities(documentReference));

        // resolveBackLinkedEntities

        assertEquals(Set.of(), this.store.resolveBackLinkedEntities(doesnotexist));
        assertEquals(Set.of(), this.store.resolveBackLinkedEntities(documentReference));
        assertEquals(Set.of(), this.store.resolveBackLinkedEntities(new PageReference("wiki", "space")));

        PageReference document1PageLink = new PageReference("wiki", "space", "document1");
        PageReference page2PageLink = new PageReference("wiki", "page1", "page2");
        PageAttachmentReference attachmentPageLink = new PageAttachmentReference("file.ext", page2PageLink);

        when(this.bridge.exists(document1DocumentLink)).thenReturn(true);

        assertEquals(Set.of(documentReference), this.store.resolveBackLinkedEntities(document1DocumentLink));
        assertEquals(Set.of(documentReference), this.store.resolveBackLinkedEntities(document1PageLink));
        assertEquals(Set.of(documentReference), this.store.resolveBackLinkedEntities(attachmentDocumentLink));
        assertEquals(Set.of(documentReference), this.store.resolveBackLinkedEntities(attachmentPageLink));

        assertEquals(Set.of(documentReference), this.store.resolveBackLinkedEntities(new WikiReference("wiki")));
        assertEquals(Set.of(documentReference),
            this.store.resolveBackLinkedEntities(new SpaceReference("wiki", "space")));
        assertEquals(Set.of(documentReference),
            this.store.resolveBackLinkedEntities(new SpaceReference("wiki", "page1")));
        assertEquals(Set.of(documentReference),
            this.store.resolveBackLinkedEntities(new PageReference("wiki", "page1")));
    }
}
