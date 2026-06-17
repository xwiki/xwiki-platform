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
package org.xwiki.livedata.internal.solr;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jakarta.inject.Named;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrLiveDataDocumentFormatter}.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@ComponentTest
class SolrLiveDataDocumentFormatterTest
{
    @InjectMockComponents
    private SolrLiveDataDocumentFormatter formatter;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("wiki")
    private ConfigurationSource wikiConfiguration;

    @Test
    void formatDateUsesTheWikiDateFormat()
    {
        Date date = new Date(0);
        when(this.wikiConfiguration.getProperty("dateformat")).thenReturn("yyyy/MM/dd");

        // The date is rendered with the wiki date format, not as a raw number/Date.
        assertEquals(new SimpleDateFormat("yyyy/MM/dd").format(date), this.formatter.formatDate(date));
    }

    @Test
    void formatDateReturnsNullForNonDate()
    {
        assertNull(this.formatter.formatDate(null));
        assertNull(this.formatter.formatDate("not a date"));
    }

    @Test
    void getUserProfileUrlBuildsTheProfileLink()
    {
        DocumentReference adminReference = new DocumentReference("xwiki", "XWiki", "Admin");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Admin")).thenReturn(adminReference);
        when(this.documentAccessBridge.getDocumentURL(adminReference, "view", null, null))
            .thenReturn("/xwiki/bin/view/XWiki/Admin");

        assertEquals("/xwiki/bin/view/XWiki/Admin", this.formatter.getUserProfileUrl("xwiki:XWiki.Admin"));
        assertNull(this.formatter.getUserProfileUrl(null));
    }

    @Test
    void getDocumentUrlBuildsTheViewUrl()
    {
        SolrDocument document = new SolrDocument();
        DocumentReference reference = new DocumentReference("xwiki", "Space", "Page");
        when(this.solrDocumentReferenceResolver.resolve(document)).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentURL(reference, "view", null, null))
            .thenReturn("/xwiki/bin/view/Space/Page");

        assertEquals("/xwiki/bin/view/Space/Page", this.formatter.getDocumentUrl(document));
    }

    @Test
    void buildLocationHtmlReturnsBreadcrumbOfLinks()
    {
        SolrDocument document = new SolrDocument();
        DocumentReference reference = new DocumentReference("xwiki", List.of("SpaceA", "SpaceB"), "MyPage");
        when(this.solrDocumentReferenceResolver.resolve(document)).thenReturn(reference);
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.documentAccessBridge.getDocumentURL(new DocumentReference("xwiki", "SpaceA", "WebHome"), "view",
            null, null)).thenReturn("/a");
        when(this.documentAccessBridge.getDocumentURL(
            new DocumentReference("xwiki", List.of("SpaceA", "SpaceB"), "WebHome"), "view", null, null))
            .thenReturn("/ab");
        when(this.documentAccessBridge.getDocumentURL(reference, "view", null, null)).thenReturn("/page");

        // The location is an HTML breadcrumb of links (space home pages + the terminal page), built from the reference.
        assertEquals("<a href=\"/a\">SpaceA</a> / <a href=\"/ab\">SpaceB</a> / <a href=\"/page\">MyPage</a>",
            this.formatter.buildLocationHtml(document));
    }
}
