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

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.extension.InstalledExtension.FIELD_INSTALLED_NAMESPACES;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_ADVICE;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_FIX_VERSION;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.SECURITY_MAX_CVSS;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * Test of {@link SolrToLiveDataEntryMapper}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrToLiveDataEntryMapperTest
{
    @InjectMockComponents
    private SolrToLiveDataEntryMapper mapper;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @MockComponent
    private ExtensionIndexStore extensionIndexStore;

    @Mock
    private SolrDocument doc;

    @Test
    void mapDocToEntries()
    {
        when(this.extensionIndexStore.getExtensionId(this.doc)).thenReturn(new ExtensionId("org.test:ext", "7.5"));
        when(this.doc.get(FieldUtils.NAME)).thenReturn("Ext Name");
        when(this.solrUtils.get(FieldUtils.NAME, this.doc)).thenReturn("Ext Name");
        when(this.solrUtils.get(SOLR_FIELD_ID, this.doc)).thenReturn("org.test:ext/7.5");
        when(this.solrUtils.get(SECURITY_MAX_CVSS, this.doc)).thenReturn(5.0);
        when(this.solrUtils.get(SECURITY_FIX_VERSION, this.doc)).thenReturn("8.4");
        when(this.solrUtils.get(SECURITY_ADVICE, this.doc)).thenReturn("translation.key");
        when(this.solrUtils.getList(FIELD_INSTALLED_NAMESPACES, this.doc)).thenReturn(List.of(
            "wiki:xwiki",
            "{root}",
            "wiki:s1"));
        when(this.l10n.getTranslationPlain("translation.key")).thenReturn("Translation Value");
        assertEquals(Map.of(
            "cveID", "",
            "fixVersion", "8.4",
            "maxCVSS", 5.0,
            "advice", "Translation Value",
            "name",
            "<a href='null' title='Ext Name'>Ext Name</a><br/><span class='xHint' title='org.test:ext/7.5'>org.test:ext/7.5</span>",
            "wikis", "xwiki, s1"
        ), this.mapper.mapDocToEntries(this.doc));
    }
}
