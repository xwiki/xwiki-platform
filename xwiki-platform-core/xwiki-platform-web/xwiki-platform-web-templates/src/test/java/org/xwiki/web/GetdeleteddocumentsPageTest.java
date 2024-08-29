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
package org.xwiki.web;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code getdeleteddocuments.vm} template. Assert that the returned results are well-formed.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@ComponentList({
    StoreConfiguration.class
})
@UserReferenceComponentList
class GetdeleteddocumentsPageTest extends PageTest
{
    private static final String GETDELETEDDOCUMENTS = "getdeleteddocuments.vm";

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    @Mock
    private XWikiRecycleBinStoreInterface recycleBinStore;

    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.xwiki.setRecycleBinStore(this.recycleBinStore);
    }

    @Test
    void getDeletedDocumentsObfuscatedResultsAreFiltered() throws Exception
    {
        defaultQueryMocks();

        when(this.query.execute()).thenReturn(asList("1", "2"), singletonList(2));

        when(this.recycleBinStore.getDeletedDocument(1L, this.context, true))
            .thenReturn(new XWikiDeletedDocument("fullName1", null, null, null, null, null));
        XWikiDeletedDocumentContent xWikiDeletedDocumentContent = mock(XWikiDeletedDocumentContent.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "fullName2");
        when(xWikiDeletedDocumentContent.getXWikiDocument(null)).thenReturn(
            new XWikiDocument(documentReference));
        XWikiDeletedDocument deletedDocument2 =
            new XWikiDeletedDocument("fullName2", null, null, null, null, xWikiDeletedDocumentContent);
        when(this.recycleBinStore.getDeletedDocument(2L, this.context, true))
            .thenReturn(deletedDocument2);

        when(this.recycleBinStore.hasAccess(Right.EDIT, GuestUserReference.INSTANCE, deletedDocument2))
            .thenReturn(true);

        Map<String, Object> results = getJsonResultMap();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(1, rows.size());
        assertTrue((Boolean) rows.get(0).get("doc_viewable"));
        assertEquals("fullName2", rows.get(0).get("doc_name"));
        verify(this.queryService).hql("SELECT ddoc.id FROM XWikiDeletedDocument as ddoc WHERE 1=1 ");
        verify(this.queryService).hql("SELECT COUNT(ddoc.id) FROM XWikiDeletedDocument as ddoc WHERE 1=1");
    }

    @Test
    void getDeletedDocumentsOfuscatedResultsAreNotFiltered() throws Exception
    {
        this.request.put("limit", "1");

        defaultQueryMocks();

        when(this.query.execute()).thenReturn(asList("1"), singletonList(2));

        when(this.recycleBinStore.getDeletedDocument(1L, this.context, true))
            .thenReturn(new XWikiDeletedDocument("fullName1", null, null, null, null, null));

        when(this.xwiki.getRightService().hasAccessLevel("admin", "XWiki.XWikiGuest", "fullName2", this.context))
            .thenReturn(true);

        Map<String, Object> results = getJsonResultMap();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(1, rows.size());
        assertFalse((Boolean) rows.get(0).get("doc_viewable"));
        verify(this.queryService).hql("SELECT ddoc.id FROM XWikiDeletedDocument as ddoc WHERE 1=1 ");
        verify(this.queryService).hql("SELECT COUNT(ddoc.id) FROM XWikiDeletedDocument as ddoc WHERE 1=1");
    }

    private void defaultQueryMocks() throws QueryException
    {
        when(this.queryService.hql(any())).thenReturn(this.query, this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(anyList())).thenReturn(this.query, this.query);
        when(this.query.setWiki(any())).thenReturn(this.query, this.query);
    }

    private Map<String, Object> getJsonResultMap() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        this.templateManager.render(GETDELETEDDOCUMENTS);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        return (Map<String, Object>) argument.getValue();
    }
}
