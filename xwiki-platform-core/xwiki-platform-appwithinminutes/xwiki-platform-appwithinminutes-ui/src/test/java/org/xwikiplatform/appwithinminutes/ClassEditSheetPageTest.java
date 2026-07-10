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
package org.xwikiplatform.appwithinminutes;

import java.util.List;
import java.util.Objects;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.groovy.internal.DefaultGroovyConfiguration;
import org.xwiki.groovy.internal.GroovyScriptEngineFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.internal.macro.groovy.GroovyMacro;
import org.xwiki.rendering.internal.macro.script.PermissionCheckerListener;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacroPermissionPolicy;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page Test of {@code AppWithinMinutes.ClassEditSheet}.
 *
 * @version $Id$
 * @since 14.4.8
 * @since 14.10.4
 * @since 15.0
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    // Start GroovyMacro
    GroovyMacro.class,
    GroovyScriptEngineFactory.class,
    DefaultGroovyConfiguration.class,
    // End GroovyMacro
    PermissionCheckerListener.class,
    VelocityMacroPermissionPolicy.class,
    // Required by #displayNewField to resolve the template document reference ($services.model.resolveDocument).
    ModelScriptService.class,
})
class ClassEditSheetPageTest extends PageTest
{
    private QueryManagerScriptService queryManagerScriptService;

    private CSRFTokenScriptService csrfTokenScriptService;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        this.queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        this.csrfTokenScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "csrf", CSRFTokenScriptService.class,
                false);
    }

    @Test
    void displayFieldPalette() throws Exception
    {
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "VelocityMacros"));
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "ClassEditSheet"));

        when(this.queryManagerScriptService.xwql("from doc.object(AppWithinMinutes.FormFieldCategoryClass) as category "
            + "order by category.priority")).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of("xwiki:XWiki.Category"));
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any())).thenReturn(true);

        XWikiDocument xWikiDocumentCategory =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "Category"), this.context);
        xWikiDocumentCategory.setTitle("<strong>TITLE</strong>");
        this.xwiki.saveDocument(xWikiDocumentCategory, this.context);

        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);

        xwikiDocument.setContent("{{include reference=\"AppWithinMinutes.ClassEditSheet\" /}}\n"
            + "\n"
            + "{{velocity}}\n"
            + "#displayFieldPalette()\n"
            + "{{/velocity}}\n");
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        Document document = renderHTMLPage(xwikiDocument);

        assertEquals("<strong>TITLE</strong>", document.selectFirst(".category").text());
    }

    @Test
    void previewFieldUsesCurrentUserRights() throws Exception
    {
        XWikiDocument editedDocument = setUpPreviewRequest();

        // The only difference with a working preview is that the preview is requested by U1, a user that has neither
        // script nor programming right, instead of the privileged author of the edited class.
        setUpLowPrivilegedCurrentUser();

        Document document = renderHTMLPage(editedDocument);

        // The custom display is evaluated with the rights of the current user (U1) and not with those of the class
        // document author. Since U1 has no script right, the custom display script is not executed at all (so the
        // programming right escalation does not happen) and a rendering error is displayed instead.
        assertFalse(document.text().contains("POC_PR=true"));
        assertEquals("Failed to execute the [velocity] macro. Cause: [The execution of the [velocity] script macro is "
                + "not allowed in [xwiki:Space.Page]. Check the rights of its last author or the parameters if it's "
                + "rendered from another script.]. Click on this message for details.",
            document.selectFirst(".xwikirenderingerror").text());
    }

    @Test
    void previewFieldFailsWithoutValidCSRFToken() throws Exception
    {
        XWikiDocument editedDocument = setUpPreviewRequest();

        // The only difference with a working preview is that the request does not carry a valid CSRF token.
        when(this.csrfTokenScriptService.isTokenValid(any())).thenReturn(false);
        // A fresh token is offered to the client so that it can recover from an expired token.
        when(this.csrfTokenScriptService.getToken()).thenReturn("freshToken");

        XWikiServletResponseStub responseSpy = spy(this.response);
        this.context.setResponse(responseSpy);

        Document document = renderHTMLPage(editedDocument);

        // The CSRF check rejects the forged request with a 403 before the custom display is rendered: the only output
        // is the JSON carrying a fresh token, so the custom display (which would have leaked POC_PR) was not executed.
        verify(responseSpy).setStatus(403);
        verify(responseSpy).setContentType("application/json");
        // The fresh token is returned in the response body (same JSON shape as XWikiAction#csrfTokenCheck) so that the
        // client can refresh it (after an explicit, user-triggered confirmation) and retry the preview.
        assertEquals("{\"errorType\":\"CSRF\",\"newToken\":\"freshToken\"}", document.text());
    }

    @Test
    void displayNewFieldUsesCurrentUserRights() throws Exception
    {
        XWikiDocument editedDocument = setUpDisplayNewFieldRequest();

        // The only difference with a working add-field request is that it is made by U1, a user that has neither
        // script nor programming right, instead of the privileged author of the edited class.
        setUpLowPrivilegedCurrentUser();

        Document document = renderHTMLPage(editedDocument);

        // The custom display is evaluated with the rights of the current user (U1) and not with those of the edited
        // class document author. Since U1 has no script right, the custom display script is not executed at all (so
        // the programming right escalation does not happen) and a rendering error is displayed instead.
        assertFalse(document.text().contains("POC_PR=true"));
        assertEquals("Failed to execute the [velocity] macro. Cause: [The execution of the [velocity] script macro is "
                + "not allowed in [xwiki:Space.Page]. Check the rights of its last author or the parameters if it's "
                + "rendered from another script.]. Click on this message for details.",
            document.selectFirst(".xwikirenderingerror").text());
    }

    /**
     * Sets up the working baseline shared by the preview and add-field requests: a class being edited that is authored
     * by a privileged user and whose ClassEditSheet is included (so its edit macros run exactly like when the edit
     * action is called with the sheet), with the required script services stubbed and a valid CSRF token, the request
     * being made by that same privileged user and its submitted form bound to the context. Each test starts from this
     * shared, working baseline and changes only the single dimension it exercises (the rights of the current user, the
     * validity of the CSRF token, the kind of request, ...) so that any failure can only come from that dimension and
     * not from a difference in the setup.
     *
     * @return the document being edited, set as the current document in the context
     */
    private XWikiDocument setUpEditedClassRequest() throws Exception
    {
        DocumentReference adminDocumentReference = new DocumentReference("xwiki", "XWiki", "Admin");
        when(this.xwiki.getRightService().hasProgrammingRights(any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any())).thenReturn(true);
        when(this.csrfTokenScriptService.isTokenValid(any())).thenReturn(true);

        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "VelocityMacros"));
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "ClassEditSheet"));

        // The class being edited, authored by a privileged user.
        XWikiDocument editedDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        editedDocument.setContent("{{include reference=\"AppWithinMinutes.ClassEditSheet\" /}}");
        editedDocument.setSyntax(Syntax.XWIKI_2_1);
        editedDocument.setAuthorReference(adminDocumentReference);
        editedDocument.setContentAuthorReference(adminDocumentReference);

        // The request is made by the privileged author of the edited class, with the submitted form bound to it. The
        // form reads the request parameters lazily, so each test can still add its own parameters after this point.
        this.context.setUserReference(adminDocumentReference);
        this.context.setDoc(editedDocument);
        EditForm editForm = new EditForm();
        editForm.setRequest(this.request);
        this.context.setForm(editForm);

        return editedDocument;
    }

    /**
     * Extends {@link #setUpEditedClassRequest()} into a working preview request of a String field whose custom display
     * tries to escalate to programming right.
     *
     * @return the document being edited, set as the current document in the context
     */
    private XWikiDocument setUpPreviewRequest() throws Exception
    {
        XWikiDocument editedDocument = setUpEditedClassRequest();
        loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "String"));

        // Simulate the POST submitted to the edit action in preview mode, requesting the preview of a String field
        // whose custom display tries to escalate to programming right.
        this.stubRequest.put("preview", "true");
        this.stubRequest.put("template-testField", "AppWithinMinutes.String");
        this.stubRequest.put("field-testField_customDisplay",
            "{{velocity}}#set($pr = $services.security.authorization.hasAccess('programming'))POC_PR=$pr{{/velocity}}");

        return editedDocument;
    }

    /**
     * Extends {@link #setUpEditedClassRequest()} into a working add-field request based on the AppWithinMinutes.String
     * field template whose (first) class property is given a custom display that tries to escalate to programming
     * right. #displayNewField clones that property into the edited class, so the escalation must be prevented by the
     * current user's rights and not by the field template author's rights.
     *
     * @return the document being edited, set as the current document in the context
     */
    private XWikiDocument setUpDisplayNewFieldRequest() throws Exception
    {
        XWikiDocument editedDocument = setUpEditedClassRequest();

        // The form field template cloned by #displayNewField. Its first class property is given a custom display that
        // tries to escalate to programming right.
        XWikiDocument fieldTemplate =
            loadPage(new DocumentReference("xwiki", "AppWithinMinutes", "String"));
        PropertyClass shortText = (PropertyClass) fieldTemplate.getXClass().get("shortText");
        shortText.setCustomDisplay(
            "{{velocity}}#set($pr = $services.security.authorization.hasAccess('programming'))POC_PR=$pr{{/velocity}}");
        this.xwiki.saveDocument(fieldTemplate, this.context);

        // The template document from which #displayNewField creates the object used to render the field being added.
        XWikiDocument templateDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageTemplate"), this.context);
        templateDocument.newXObject(editedDocument.getDocumentReference(), this.context);
        this.xwiki.saveDocument(templateDocument, this.context);

        // Simulate the AJAX request that adds a new form field based on the AppWithinMinutes.String template.
        this.stubRequest.put("field", "AppWithinMinutes.String");

        return editedDocument;
    }

    /**
     * Makes the current user U1, a user that has neither script nor programming right, so that any privileged code
     * fails. As {@code DefaultContextualAuthorizationManager} does, script and programming rights are checked against
     * the content author of the secure document (which the {@code AuthorExecutor} sets to the author the code is
     * executed with).
     *
     * @return the reference of the low-privileged current user
     */
    private DocumentReference setUpLowPrivilegedCurrentUser()
    {
        DocumentReference userDocumentReference = new DocumentReference("xwiki", "XWiki", "U1");
        this.context.setUserReference(userDocumentReference);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any())).thenAnswer(invocation -> {
            Right right = invocation.getArgument(0);
            if (right == Right.PROGRAM || right == Right.SCRIPT) {
                XWikiDocument secureDocument = (XWikiDocument) this.context.get(XWikiDocument.CKEY_SDOC);
                DocumentReference effectiveAuthor =
                    secureDocument == null ? null : secureDocument.getContentAuthorReference();
                // Only U1 has no PR and Script rights.
                return !Objects.equals(userDocumentReference, effectiveAuthor);
            }
            return true;
        });
        return userDocumentReference;
    }
}
