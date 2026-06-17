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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.xml.XMLUtils;

/**
 * Builds the display values of the Solr live data columns that need more than the raw Solr field value: the location
 * breadcrumb, the formatted dates and the user profile URLs. Extracted from {@link SolrLiveDataEntryStore} to keep that
 * class focused on querying Solr.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@Component(roles = SolrLiveDataDocumentFormatter.class)
@Singleton
public class SolrLiveDataDocumentFormatter
{
    private static final String VIEW_ACTION = "view";

    /**
     * The date format used to render the date columns when the wiki does not define a {@code dateformat} preference
     * (mirrors the historical XWiki default).
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to resolve the reference of the matching document from its Solr document.
     */
    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    /**
     * Used to resolve the serialized user reference stored in the {@code author}/{@code creator} Solr fields into a
     * {@link DocumentReference} so that the profile URL can be built.
     */
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Used to know the default document name (e.g. {@code WebHome}) when building the location breadcrumb.
     */
    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Used to read the wiki {@code dateformat} preference for rendering the date columns.
     */
    @Inject
    @Named("wiki")
    private ConfigurationSource wikiConfiguration;

    /**
     * @param document the Solr document of the matching page
     * @return the view URL of the matching document
     */
    public String getDocumentUrl(SolrDocument document)
    {
        return this.documentAccessBridge.getDocumentURL(this.solrDocumentReferenceResolver.resolve(document),
            VIEW_ACTION, null, null);
    }

    /**
     * @param value a value read from a Solr date field (expected to be a {@link Date})
     * @return the value formatted with the wiki {@code dateformat} preference (or {@link #DEFAULT_DATE_FORMAT}), or
     *         {@code null} when the value is not a date
     */
    public String formatDate(Object value)
    {
        if (!(value instanceof Date)) {
            return null;
        }
        String format =
            StringUtils.defaultIfBlank(this.wikiConfiguration.getProperty("dateformat"), DEFAULT_DATE_FORMAT);
        return new SimpleDateFormat(format).format((Date) value);
    }

    /**
     * @param serializedUserReference the serialized user reference stored in an {@code author}/{@code creator} field
     * @return the view URL of the user profile page, or {@code null} when the reference is {@code null}
     */
    public String getUserProfileUrl(Object serializedUserReference)
    {
        if (serializedUserReference == null) {
            return null;
        }
        DocumentReference userReference =
            this.documentReferenceResolver.resolve(serializedUserReference.toString());
        return this.documentAccessBridge.getDocumentURL(userReference, VIEW_ACTION, null, null);
    }

    /**
     * Builds an HTML breadcrumb of links for the location of the passed document, using only its reference (no document
     * is loaded): one link per space (to the space home page) plus a link to the page itself when it is not the space
     * home page. The segment labels are the space/page names (not the titles, which would require loading the
     * documents).
     *
     * @param document the Solr document whose location must be rendered
     * @return the location as an HTML breadcrumb of links
     */
    public String buildLocationHtml(SolrDocument document)
    {
        DocumentReference reference = this.solrDocumentReferenceResolver.resolve(document);
        String defaultDocumentName =
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        List<String> segments = new ArrayList<>();
        for (var space : reference.getSpaceReferences()) {
            DocumentReference spaceHome = new DocumentReference(defaultDocumentName, space);
            segments.add(toLink(this.documentAccessBridge.getDocumentURL(spaceHome, VIEW_ACTION, null, null),
                space.getName()));
        }
        // Append the page itself only when it is a terminal page (its home page is already the last space segment).
        if (!defaultDocumentName.equals(reference.getName())) {
            segments.add(toLink(this.documentAccessBridge.getDocumentURL(reference, VIEW_ACTION, null, null),
                reference.getName()));
        }
        return StringUtils.join(segments, " / ");
    }

    private String toLink(String url, String label)
    {
        return "<a href=\"" + XMLUtils.escape(url) + "\">" + XMLUtils.escape(label) + "</a>";
    }
}
