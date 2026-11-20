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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.DocumentRevisionConflictException;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code adduorg.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class,
    ModelScriptService.class,
    MergeScriptService.class
})
class AdduorgPageTest extends PageTest
{
    private static final String TEMPLATE_NAME = "adduorg.vm";

    private static final DocumentReference GROUP_CLASS_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiGroups");

    private static final DocumentReference GROUP_REFERENCE = new DocumentReference("xwiki", "XWiki", "Devs");

    private static final String ALICE = "XWiki.Alice";

    private static final String BOB = "XWiki.Bob";

    private static final String CAROL = "XWiki.Carol";

    @Mock
    private CSRFTokenScriptService csrfScriptService;

    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        this.componentManager.registerMockComponent(MergeManager.class);
        this.componentManager.registerMockComponent(MergeConflictDecisionsManager.class);
        when(this.csrfScriptService.isTokenValid(any())).thenReturn(true);

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), any(), any())).thenReturn(true);

        XWikiDocument classDocument = this.xwiki.getDocument(GROUP_CLASS_REFERENCE, this.context);
        classDocument.getXClass().addTextField("member", "Member", 30);
        this.xwiki.saveDocument(classDocument, this.context);

        XWikiDocument group = this.xwiki.getDocument(GROUP_REFERENCE, this.context).clone();
        addMember(group, "");
        addMember(group, ALICE);
        this.xwiki.saveDocument(group, this.context);

        this.oldcore.checkDocumentRevision(true);
    }

    private void addMember(XWikiDocument group, String member) throws XWikiException
    {
        BaseObject object = group.newXObject(GROUP_CLASS_REFERENCE, this.context);
        object.set("member", member, this.context);
    }

    /**
     * Saves the given change to the group, as another request would do.
     */
    private void saveConcurrently(FailableConsumer<XWikiDocument, XWikiException> change) throws XWikiException
    {
        XWikiDocument group = this.xwiki.getDocument(GROUP_REFERENCE, this.context).clone();
        change.accept(group);
        this.xwiki.saveDocument(group, this.context);
    }

    /**
     * Simulates requests changing the group before and while the template is rendered: the template is rendered on
     * the group loaded by the action before another request saved it, and another request saves it again after the
     * template loaded it.
     */
    private void changeGroupConcurrently(FailableConsumer<XWikiDocument, XWikiException> before,
        FailableConsumer<XWikiDocument, XWikiException> during) throws XWikiException
    {
        this.context.setDoc(this.xwiki.getDocument(GROUP_REFERENCE, this.context));
        saveConcurrently(before);
        AtomicBoolean loaded = new AtomicBoolean();
        doAnswer(invocation -> {
            Object document = invocation.callRealMethod();
            if (loaded.compareAndSet(false, true)) {
                saveConcurrently(during);
            }
            return document;
        }).when(this.xwiki).getDocument(eq(GROUP_REFERENCE), any(XWikiContext.class));
    }

    private List<String> getStoredMembers() throws XWikiException
    {
        return this.xwiki.getDocument(GROUP_REFERENCE, this.context).getXObjects(GROUP_CLASS_REFERENCE).stream()
            .filter(Objects::nonNull).map(object -> object.getStringValue("member")).toList();
    }

    @Test
    void addMembersAfterConcurrentSaves() throws Exception
    {
        changeGroupConcurrently(group -> addMember(group, BOB), group -> addMember(group, CAROL));
        this.request.put("name", "XWiki.Dave");
        this.request.put("name", "XWiki.Eve");

        this.templateManager.render(TEMPLATE_NAME);

        // All the changes are kept.
        assertEquals(List.of("", ALICE, BOB, CAROL, "XWiki.Dave", "XWiki.Eve"), getStoredMembers());
    }

    @Test
    void addMemberAddedConcurrently() throws Exception
    {
        changeGroupConcurrently(group -> addMember(group, BOB), group -> addMember(group, CAROL));
        this.request.put("name", CAROL);

        this.templateManager.render(TEMPLATE_NAME);

        // The member isn't added twice.
        assertEquals(List.of("", ALICE, BOB, CAROL), getStoredMembers());
    }

    @Test
    void addMemberWhenAlwaysSavedConcurrently() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(GROUP_REFERENCE, this.context));
        doThrow(new DocumentRevisionConflictException(GROUP_REFERENCE, "1.1", "2.1")).when(this.xwiki)
            .saveDocument(argThat(document -> GROUP_REFERENCE.equals(document.getDocumentReference())), anyString(),
                anyBoolean(), anyBoolean(), any(XWikiContext.class));
        XWikiServletResponseStub response = spy(this.response);
        this.context.setResponse(response);
        clearInvocations(this.xwiki);
        this.request.put("name", BOB);

        this.templateManager.render(TEMPLATE_NAME);

        // The request is applied 5 times, then the failure is reported without its details.
        verify(this.xwiki, times(5)).saveDocument(
            argThat(document -> GROUP_REFERENCE.equals(document.getDocumentReference())), anyString(),
            anyBoolean(), anyBoolean(), any(XWikiContext.class));
        verify(response).sendError(500, "Failed to save the document.");
        verify(response, never()).sendRedirect(any());
        assertEquals(List.of("", ALICE), getStoredMembers());
    }
}
