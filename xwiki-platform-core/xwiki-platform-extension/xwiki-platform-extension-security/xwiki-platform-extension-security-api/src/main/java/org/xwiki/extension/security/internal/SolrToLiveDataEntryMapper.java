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
package org.xwiki.extension.security.internal;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.common.SolrDocument;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.web.ViewAction;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.ADVICE;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.CVE_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.EXTENSION_ID;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.FIX_VERSION;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.MAX_CVSS;
import static org.xwiki.extension.security.internal.ExtensionSecurityLiveDataConfigurationProvider.NAME;

/**
 * Converts a {@link SolrDocument} to a {@link Map} of Live Data entries.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = SolrToLiveDataEntryMapper.class)
@Singleton
public class SolrToLiveDataEntryMapper
{
    @Inject
    private SolrUtils solrUtils;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    private ExtensionIndexStore extensionIndexStore;

    /**
     * @param doc the document to convert to Live Data entries.
     * @return Converts a {@link SolrDocument} to a {@link Map} of Live Data entries.
     */
    public Map<String, Object> mapDocToEntries(SolrDocument doc)
    {
        List<Object> cveIds =
            new ArrayList<>(doc.getFieldValues(ExtensionIndexSolrCoreInitializer.SECURITY_CVE_ID));
        List<Object> cveLinks =
            new ArrayList<>(doc.getFieldValues(ExtensionIndexSolrCoreInitializer.SECURITY_CVE_LINK));
        List<Object> cveCvss =
            new ArrayList<>(doc.getFieldValues(ExtensionIndexSolrCoreInitializer.SECURITY_CVE_CVSS));
        Stream<String> objectStream = IntStream.range(0, cveIds.size())
            .mapToObj(value -> String.format("<a href='%s'>%s</a>&nbsp;(%s)",
                StringEscapeUtils.escapeXml(String.valueOf(cveLinks.get(value))),
                StringEscapeUtils.escapeXml(String.valueOf(cveIds.get(value))),
                StringEscapeUtils.escapeXml(String.valueOf(cveCvss.get(value)))));

        return Map.of(
            NAME, buildExtensionName(doc),
            EXTENSION_ID, doc.get(AbstractSolrCoreInitializer.SOLR_FIELD_ID),
            MAX_CVSS, doc.get(ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS),
            CVE_ID, objectStream.collect(Collectors.joining("<br/>")),
            FIX_VERSION, this.solrUtils.get(ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION, doc),
            ADVICE,
            this.l10n.getTranslationPlain(this.solrUtils.get(ExtensionIndexSolrCoreInitializer.SECURITY_ADVICE, doc))
        );
    }

    private String buildExtensionName(SolrDocument solrDoc)
    {
        String extensionName;
        // If the extension does not have a name and version indexed, we fall back to an extensionId parsing.
        // Note: this is not supposed to happen in practice.
        ExtensionId extensionId = this.extensionIndexStore.getExtensionId(solrDoc);
        List<BasicNameValuePair> parameters = buildParameter(extensionId.getId(), extensionId.getVersion().getValue());
        if (solrDoc.get(FieldUtils.NAME) != null) {
            extensionName = this.solrUtils.get(FieldUtils.NAME, solrDoc);
        } else {
            String[] versionId = this.solrUtils.getId(solrDoc).split("/");
            extensionName = versionId[0];
        }
        String url = this.documentAccessBridge.getDocumentURL(new LocalDocumentReference("XWiki", "Extensions"),
            ViewAction.VIEW_ACTION, URLEncodedUtils.format(parameters, Charset.defaultCharset()), null);

        return String.format("<a href='%s'>%s</a>", escapeXml(url), escapeXml(String.valueOf(extensionName)));
    }

    private static List<BasicNameValuePair> buildParameter(String extensionId, String extensionVersion)
    {
        return List.of(
            new BasicNameValuePair("section", "XWiki.Extensions"),
            new BasicNameValuePair("extensionId", extensionId),
            new BasicNameValuePair("extensionVersion", extensionVersion)
        );
    }
}
