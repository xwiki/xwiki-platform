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
package org.xwiki.invitation;

import java.util.stream.IntStream;

import javax.script.ScriptContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.UserReferenceComponentList;

import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Test of {@code Invitation.InvitationCommon}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 * @since 14.4.8
 * @since 13.10.11
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@UserReferenceComponentList
@ComponentList({
    InfoMessageMacro.class,
    ErrorMessageMacro.class,
    TestNoScriptMacro.class
})
class InvitationCommonPageTest extends PageTest
{
    public static final DocumentReference INVITATION_COMMON_REFERENCE =
        new DocumentReference("xwiki", "Invitation", "InvitationCommon");

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        AuthorizationManager authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);
        // Allow everything by default.
        when(authorizationManager.hasAccess(any(), any(), any())).thenReturn(true);
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
    }

    @Test
    void testEq0() throws Exception
    {
        this.context.setDoc(this.xwiki.getDocument(
            new DocumentReference("xwiki", "]]  {{noscript/}}", "InvitationCommon"), this.context));

        Document document = Jsoup.parse(loadPage(INVITATION_COMMON_REFERENCE).getRenderedContent(this.context));
        assertEquals("xe.invitation.internalDocument []] {{noscript/}}.WebHome]",
            document.selectFirst(".infomessage").text());
    }

    @Test
    void testEq1ConfigClassIsNew() throws Exception
    {
        String spaceName = "<script>console.log</script>]]{{noscript/}}";

        XWikiDocument doc =
            this.xwiki.getDocument(new DocumentReference("xwiki", spaceName, "InvitationCommon"), this.context);
        this.xwiki.saveDocument(doc, this.context);
        this.context.setDoc(doc);

        this.request.put("test", "1");

        Document document = Jsoup.parse(loadPage(INVITATION_COMMON_REFERENCE).getRenderedContent(this.context));

        assertEquals("testLoadInvitationConfig", document.selectFirst(".infomessage").text());
        Element errorMessage = document.selectFirst(".errormessage");
        assertEquals("Class document <script>console\\.log</script>]]{{noscript/}}.WebHome not found. "
            + "can't run test.", errorMessage.text());
        Element errorLink = errorMessage.selectFirst("a");
        assertEquals("<script>console\\.log</script>]]{{noscript/}}.WebHome", errorLink.text());
        assertEquals("<script>console\\.log</script>]]{{noscript/}}.WebHome", errorLink.attr("href"));
    }

    @Test
    void testEq1ConfigClassExistsInvalidFromAddress() throws Exception
    {
        // Create an InvitationCommon file.
        DocumentReference documentReference =
            new DocumentReference("xwiki", "<script>console.log</script>", "InvitationCommon");
        XWikiDocument doc = this.xwiki.getDocument(documentReference, this.context);
        this.xwiki.saveDocument(doc, this.context);
        this.context.setDoc(doc);

        // Create a WebHome file in the same space as InvitationCommon, with an XClass containing the expected fields.
        DocumentReference configDocumentReference =
            new DocumentReference("xwiki", "<script>console.log</script>", "WebHome");
        XWikiDocument configDoc = this.xwiki.getDocument(configDocumentReference, this.context);
        configDoc.getXClass().addTextField("from_address", "From Address", 30);
        this.xwiki.saveDocument(configDoc, this.context);

        // Initialize the document in the same space as InvitationCommon, containing an XObject with a WebHome XObject.
        XWikiDocument hopefullyNonExistantSpaceDoc = this.xwiki.getDocument(
            new DocumentReference("xwiki", "<script>console.log</script>", "HopefullyNonexistantSpace"), this.context);
        BaseObject invitationConfigXObject =
            hopefullyNonExistantSpaceDoc.newXObject(configDocumentReference, this.context);
        invitationConfigXObject.set("from_address", "<script>console.log('from_address')</script>", this.context);
        this.xwiki.saveDocument(hopefullyNonExistantSpaceDoc, this.context);

        this.request.put("test", "1");

        XWikiDocument invitationCommonDoc = loadPage(INVITATION_COMMON_REFERENCE);
        Document document = Jsoup.parse(invitationCommonDoc.getRenderedContent(this.context));

        assertEquals("testLoadInvitationConfig", document.selectFirst(".infomessage").text());
        Element firstErrorMessage = document.selectFirst(".errormessage");
        assertEquals("Config map too small", firstErrorMessage.text());
        Element secondErrorMessage = document.select(".errormessage").get(1);
        assertEquals("form_address incorrect, expecting \"no-reply@localhost.localdomain\" "
            + "got \"<script>console.log('from_address')</script>\"", secondErrorMessage.text());
    }

    @Test
    void testEq1ConfigClassExistsConfigMapTooSmall() throws Exception
    {
        // Create an InvitationCommon file.
        XWikiDocument doc = this.xwiki
            .getDocument(new DocumentReference("xwiki", "<script>console.log</script>", "InvitationCommon"),
                this.context);
        this.xwiki.saveDocument(doc, this.context);
        this.context.setDoc(doc);

        // Create a WebHome file in the same space as InvitationCommon, with an XClass containing the expected fields.
        DocumentReference configDocumentReference =
            new DocumentReference("xwiki", "<script>console.log</script>", "WebHome");
        XWikiDocument configDoc = this.xwiki.getDocument(configDocumentReference, this.context);
        configDoc.getXClass().addTextField("from_address", "From Address", 30);

        this.xwiki.saveDocument(configDoc, this.context);

        // Initialize the document in the same space as InvitationCommon, containing an XObject with a WebHome XObject.
        XWikiDocument hopefullyNonExistantSpaceDoc = this.xwiki.getDocument(
            new DocumentReference("xwiki", "<script>console.log</script>", "HopefullyNonexistantSpace"), this.context);
        BaseObject invitationConfigXObject =
            hopefullyNonExistantSpaceDoc.newXObject(configDocumentReference, this.context);
        invitationConfigXObject.set("from_address", "no-reply@localhost.localdomain", this.context);
        this.xwiki.saveDocument(hopefullyNonExistantSpaceDoc, this.context);

        this.request.put("test", "1");

        XWikiDocument invitationCommonDoc = loadPage(INVITATION_COMMON_REFERENCE);
        Document document = Jsoup.parse(invitationCommonDoc.getRenderedContent(this.context));

        assertEquals("testLoadInvitationConfig", document.selectFirst(".infomessage").text());
        Element firstErrorMessage = document.selectFirst(".errormessage");
        assertEquals("Config map too small", firstErrorMessage.text());
        Element secondErrorMessage = document.select(".errormessage").get(1);
        assertEquals("Config document not created", secondErrorMessage.text());
    }

    @Test
    void testEq1ConfigClassExistsNewInvitationConfig() throws Exception
    {
        // Create an InvitationCommon file.
        XWikiDocument doc = this.xwiki.getDocument(
            new DocumentReference("xwiki", "<script>console.log</script>", "InvitationCommon"), this.context);
        this.xwiki.saveDocument(doc, this.context);
        this.context.setDoc(doc);

        // Create a WebHome file in the same space as InvitationCommon, with an XClass containing the expected fields.
        DocumentReference configDocumentReference =
            new DocumentReference("xwiki", "<script>console.log</script>", "WebHome");
        XWikiDocument configDoc = this.xwiki.getDocument(configDocumentReference, this.context);
        configDoc.getXClass().addTextField("from_address", "From Address", 30);
        // Initialize 8 fields, because that's the only check performed in the document currently.
        IntStream.range(0, 8)
            .forEach(value -> configDoc.getXClass().addTextField("field" + value, "Field " + value, 30));
        this.xwiki.saveDocument(configDoc, this.context);

        // Initialize the document in the same space as InvitationCommon, containing an XObject with a WebHome XObject.
        XWikiDocument hopefullyNonExistantSpaceDoc = this.xwiki.getDocument(
            new DocumentReference("xwiki", "<script>console.log</script>", "HopefullyNonexistantSpace"), this.context);
        BaseObject invitationConfigXObject =
            hopefullyNonExistantSpaceDoc.newXObject(configDocumentReference, this.context);
        invitationConfigXObject.set("from_address", "no-reply@localhost.localdomain", this.context);
        IntStream.range(0, 8)
            .forEach(value -> invitationConfigXObject.set("field" + value, "value " + value, this.context));
        this.xwiki.saveDocument(hopefullyNonExistantSpaceDoc, this.context);

        this.request.put("test", "1");

        XWikiDocument invitationCommonDoc = loadPage(INVITATION_COMMON_REFERENCE);
        Document document = Jsoup.parse(invitationCommonDoc.getRenderedContent(this.context));

        assertEquals("testLoadInvitationConfig", document.selectFirst(".infomessage").text());
        Element firstErrorMessage = document.selectFirst(".errormessage");
        assertEquals("Config document not created", firstErrorMessage.text());
    }

    @Test
    void displayMessageVelocityMacro() throws Exception
    {
        loadPage(INVITATION_COMMON_REFERENCE);
        DocumentReference invitationMailClassDocumentReference =
            new DocumentReference("xwiki", "Invitation", "InvitationMailClass");
        loadPage(invitationMailClassDocumentReference);

        XWikiDocument page = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        BaseObject invitationMailXObject = page.newXObject(invitationMailClassDocumentReference, this.context);
        invitationMailXObject.set("messageBody", "<strong>message body</strong>", this.context);
        page.setSyntax(XWIKI_2_1);
        page.setContent("{{include reference=\"Invitation.InvitationCommon\"/}}\n"
            + "\n"
            + "{{velocity}}\n"
            + "$mail.class\n"
            + "#displayMessage($mail)\n"
            + "{{/velocity}}");

        this.scriptContext.setAttribute("mail", new Object(invitationMailXObject, this.context), GLOBAL_SCOPE);

        Document document = renderHTMLPage(page);
        assertEquals("<strong>message body</strong>", document.selectFirst("#preview-messagebody-field").html());
    }

    /**
     * Check if the value of subjectLine correctly escape its parameters.
     */
    @Test
    void subjectLineTemplate() throws Exception
    {
        com.xpn.xwiki.api.Document invitationCommonDocument =
            new com.xpn.xwiki.api.Document(loadPage(INVITATION_COMMON_REFERENCE), this.context);
        String value = String.valueOf(
            invitationCommonDocument.getObject("xwiki:Invitation.WebHome").getProperty("subjectLineTemplate")
                .getValue());
        this.oldcore.getScriptContext().setAttribute("subjectLine", "{{noscript/}}", GLOBAL_SCOPE);
        com.xpn.xwiki.api.Document testDocument = new com.xpn.xwiki.api.Document(
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Test"), this.context), this.context);
        String renderedContent = testDocument.getRenderedContent(value, XWIKI_2_0.toIdString(), PLAIN_1_0.toIdString());
        assertEquals("xe.invitation.emailContent.subjectLine [XWikiGuest, null, {{noscript/}}]", renderedContent);
    }
}
