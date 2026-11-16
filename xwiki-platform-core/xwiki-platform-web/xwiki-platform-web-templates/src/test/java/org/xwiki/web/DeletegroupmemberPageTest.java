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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code deletegroupmember.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class,
    ModelScriptService.class,
    MergeScriptService.class
})
class DeletegroupmemberPageTest extends PageTest
{
    private static final String TEMPLATE_NAME = "deletegroupmember.vm";

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
    void deleteMemberAfterConcurrentSaves() throws Exception
    {
        changeGroupConcurrently(group -> addMember(group, BOB), group -> addMember(group, CAROL));
        this.request.put("fullname", ALICE);
        this.request.put("ajax", "1");

        assertEquals("ok", this.templateManager.render(TEMPLATE_NAME).trim());

        // All the changes are kept.
        assertEquals(List.of("", BOB, CAROL), getStoredMembers());
    }

    @Test
    void deleteLastMember() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(GROUP_REFERENCE, this.context));
        saveConcurrently(group -> group.removeXObjects(GROUP_CLASS_REFERENCE));
        saveConcurrently(group -> addMember(group, ALICE));
        this.request.put("fullname", ALICE);
        this.request.put("ajax", "1");

        assertEquals("added empty group\n  ok", this.templateManager.render(TEMPLATE_NAME).trim());

        // An empty member object is added so that the group still exists.
        assertEquals(List.of(""), getStoredMembers());
    }
}
