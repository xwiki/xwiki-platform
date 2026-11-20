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
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code saverights.vm} template.
 *
 * @version $Id$
 */
@ComponentList({
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class,
    ModelScriptService.class,
    MergeScriptService.class
})
class SaverightsPageTest extends PageTest
{
    private static final String TEMPLATE_NAME = "saverights.vm";

    private static final String RIGHTS_CLASS = "XWiki.XWikiGlobalRights";

    private static final DocumentReference PREFERENCES_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiPreferences");

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
        StoreConfiguration storeConfiguration = this.componentManager.registerMockComponent(StoreConfiguration.class);
        when(storeConfiguration.isRevisionCheckEnabled()).thenReturn(true);
        when(this.csrfScriptService.isTokenValid(any())).thenReturn(true);

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), any(), any())).thenReturn(true);

        XWikiDocument classDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights"), this.context);
        BaseClass rightsClass = classDocument.getXClass();
        rightsClass.addTextField("users", "Users", 30);
        rightsClass.addTextField("groups", "Groups", 30);
        rightsClass.addTextField("levels", "Levels", 30);
        rightsClass.addBooleanField("allow", "Allow", "allow");
        this.xwiki.saveDocument(classDocument, this.context);

        XWikiDocument preferences = this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context).clone();
        addRule(preferences, "XWiki.Alice", "view");
        this.xwiki.saveDocument(preferences, this.context);

        this.oldcore.checkDocumentRevision(true);

        this.request.put("uorg", "users");
        this.request.put("clsname", RIGHTS_CLASS);
    }

    private void addRule(XWikiDocument preferences, String user, String levels) throws XWikiException
    {
        BaseObject rule = preferences.newXObject(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights"),
            this.context);
        rule.set("users", user, this.context);
        rule.set("levels", levels, this.context);
        rule.set("allow", 1, this.context);
    }

    /**
     * Saves the given change to the preferences, as another request would do.
     */
    private void saveConcurrently(FailableConsumer<XWikiDocument, XWikiException> change) throws XWikiException
    {
        XWikiDocument preferences = this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context).clone();
        change.accept(preferences);
        this.xwiki.saveDocument(preferences, this.context);
    }

    private List<String> getStoredRules() throws XWikiException
    {
        return this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context)
            .getXObjects(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights")).stream()
            .filter(Objects::nonNull)
            .map(rule -> rule.getStringValue("users") + ':' + rule.getStringValue("levels"))
            .toList();
    }

    @Test
    void saveRightAfterConcurrentSaves() throws Exception
    {
        // The template is rendered on the preferences loaded by the action, before another click in the rights
        // matrix saved them.
        this.context.setDoc(this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context));
        saveConcurrently(preferences -> addRule(preferences, "XWiki.Bob", "view"));
        // Another click saves them again while the template updates them.
        AtomicBoolean loaded = new AtomicBoolean();
        doAnswer(invocation -> {
            Object document = invocation.callRealMethod();
            if (loaded.compareAndSet(false, true)) {
                saveConcurrently(preferences -> addRule(preferences, "XWiki.Carol", "view"));
            }
            return document;
        }).when(this.xwiki).getDocument(eq(PREFERENCES_REFERENCE), any(XWikiContext.class));

        this.request.put("fullname", "XWiki.Alice");
        this.request.put("right", "edit");
        this.request.put("action", "allow");

        assertEquals("SUCCESS", this.templateManager.render(TEMPLATE_NAME).trim());

        // All the changes are kept.
        assertEquals(List.of("XWiki.Alice:view,edit", "XWiki.Bob:view", "XWiki.Carol:view"), getStoredRules());
    }

    @Test
    void saveRightFailing() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context));
        doThrow(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Not allowed")).when(this.xwiki)
            .saveDocument(argThat(document -> PREFERENCES_REFERENCE.equals(document.getDocumentReference())),
                anyString(), anyBoolean(), anyBoolean(), any(XWikiContext.class));

        this.request.put("fullname", "XWiki.Bob");
        this.request.put("right", "edit");
        this.request.put("action", "deny");

        assertFalse(this.templateManager.render(TEMPLATE_NAME).contains("SUCCESS"));
        assertEquals(List.of("XWiki.Alice:view"), getStoredRules());
    }

    @Test
    void saveRightWhenAlwaysSavedConcurrently() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context));
        doThrow(new DocumentRevisionConflictException(PREFERENCES_REFERENCE, "1.1", "2.1")).when(this.xwiki)
            .saveDocument(argThat(document -> PREFERENCES_REFERENCE.equals(document.getDocumentReference())),
                anyString(), anyBoolean(), anyBoolean(), any(XWikiContext.class));
        XWikiServletResponseStub response = spy(this.response);
        this.context.setResponse(response);
        clearInvocations(this.xwiki);

        this.request.put("fullname", "XWiki.Bob");
        this.request.put("right", "edit");
        this.request.put("action", "allow");

        assertFalse(this.templateManager.render(TEMPLATE_NAME).contains("SUCCESS"));

        // The request is applied 5 times, then the failure is reported without its details.
        verify(this.xwiki, times(5)).saveDocument(
            argThat(document -> PREFERENCES_REFERENCE.equals(document.getDocumentReference())), anyString(),
            anyBoolean(), anyBoolean(), any(XWikiContext.class));
        verify(response).sendError(500, "Failed to save the document.");
        assertEquals(List.of("XWiki.Alice:view"), getStoredRules());
    }

    @Test
    void saveRightWhenReloadFails() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(PREFERENCES_REFERENCE, this.context));
        saveConcurrently(preferences -> addRule(preferences, "XWiki.Bob", "view"));
        // Loading the preferences again after the conflicting save fails, which gives an empty new document to the script.
        AtomicBoolean failLoading = new AtomicBoolean();
        doAnswer(invocation -> {
            if (failLoading.get()) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Failed to load");
            }
            return invocation.callRealMethod();
        }).when(this.xwiki).getDocument(eq(PREFERENCES_REFERENCE), any(XWikiContext.class));
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } finally {
                failLoading.set(true);
            }
        }).when(this.xwiki).saveDocument(argThat(document -> PREFERENCES_REFERENCE.equals(document.getDocumentReference())),
            anyString(), anyBoolean(), anyBoolean(), any(XWikiContext.class));
        XWikiServletResponseStub response = spy(this.response);
        this.context.setResponse(response);

        this.request.put("fullname", "XWiki.Carol");
        this.request.put("right", "edit");
        this.request.put("action", "allow");
        clearInvocations(this.xwiki);

        String output = this.templateManager.render(TEMPLATE_NAME);
        failLoading.set(false);
        assertEquals("Failed to access document [xwiki:XWiki.XWikiPreferences]. Root reason: [XWikiException: Error number 3202 in 3:"
            + " Failed to load]", this.logCapture.getMessage(0));

        assertFalse(output.contains("SUCCESS"));
        verify(response).sendError(500, "Failed to save the document.");
        // The empty document isn't saved over the stored preferences: only the first attempt saves.
        verify(this.xwiki).saveDocument(
            argThat(document -> PREFERENCES_REFERENCE.equals(document.getDocumentReference())), anyString(),
            anyBoolean(), anyBoolean(), any(XWikiContext.class));
        assertEquals(List.of("XWiki.Alice:view", "XWiki.Bob:view"), getStoredRules());
    }
}
