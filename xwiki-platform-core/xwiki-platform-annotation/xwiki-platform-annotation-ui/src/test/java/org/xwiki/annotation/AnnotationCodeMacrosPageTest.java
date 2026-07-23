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
package org.xwiki.annotation;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.annotation.internal.AnnotationConfigurationSource;
import org.xwiki.annotation.internal.DefaultAnnotationConfiguration;
import org.xwiki.annotation.internal.DefaultAnnotationService;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.internal.DefaultIOService;
import org.xwiki.annotation.io.internal.DefaultIOTargetService;
import org.xwiki.annotation.reference.internal.DefaultTypedStringEntityReferenceResolver;
import org.xwiki.annotation.rights.internal.XWikiAnnotationRightService;
import org.xwiki.annotation.script.AnnotationScriptService;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.internal.macro.velocity.filter.NoneVelocityMacroFilter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.url.internal.DefaultURLConfiguration;
import org.xwiki.url.internal.DefaultURLSecurityManager;
import org.xwiki.url.script.URLSecurityScriptService;
import org.xwiki.xml.html.script.HTMLScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.test.LogLevel.INFO;

/**
 * Test of the annotation reply button rendered by {@code AnnotationCode.Macros}.
 *
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@XWikiSyntax21ComponentList
@HTML50ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    ModelScriptService.class,
    NoneVelocityMacroFilter.class,
    // Annotations script service start
    AnnotationScriptService.class,
    DefaultAnnotationService.class,
    DefaultIOService.class,
    DefaultTypedStringEntityReferenceResolver.class,
    DefaultAnnotationConfiguration.class,
    DefaultIOTargetService.class,
    XWikiAnnotationRightService.class,
    AnnotationConfigurationSource.class,
    // Annotations script service end
    // URL Security script server start
    URLSecurityScriptService.class,
    DefaultURLSecurityManager.class,
    DefaultURLConfiguration.class
    // URL Security script server end
})
class AnnotationCodeMacrosPageTest extends PageTest
{
    private static final DocumentReference MACROS = new DocumentReference("xwiki", "AnnotationCode", "Macros");

    private static final DocumentReference ANNOTATION_CONFIG =
        new DocumentReference("xwiki", "AnnotationCode", "AnnotationConfig");

    private static final DocumentReference COMMENTS_CLASS = new DocumentReference("xwiki", "XWiki", "XWikiComments");

    private static final DocumentReference TARGET = new DocumentReference("xwiki", "Space", "Target");

    // Required by DefaultIOService, but not exercised since the tested annotations carry no uploaded files.
    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    // Required by DefaultURLSecurityManager, but only used to resolve the current domain when checking an URL with
    // an authority against the trusted domains, and the tested redirects are all relative.
    @MockComponent
    private Container container;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(INFO, URLSecurityScriptService.class.getName());

    @BeforeEach
    void setUp() throws Exception
    {
        // The annotation class referenced by the configuration, with the fields the Annotation Application requires
        // in addition to the standard comment fields (see AnnotationClassDocumentInitializer).
        XWikiDocument commentsClass = this.xwiki.getDocument(COMMENTS_CLASS, this.context);
        commentsClass.getXClass().addTextField("author", "Author", 30);
        commentsClass.getXClass().addPageField(Annotation.TARGET_FIELD, "Target", 30);
        commentsClass.getXClass().addTextField(Annotation.STATE_FIELD, "State", 30);
        // The selection field is required for the annotation to be recognized as such (rather than an ordinary
        // comment) when its target is blank, which is how annotations on the document content are now stored.
        commentsClass.getXClass().addTextAreaField(Annotation.SELECTION_FIELD, "Selection", 40, 5);
        this.xwiki.saveDocument(commentsClass, this.context);

        // The annotated document.
        XWikiDocument target = this.xwiki.getDocument(TARGET, this.context);
        target.setSyntax(XWIKI_2_0);
        this.xwiki.saveDocument(target, this.context);

        // The configuration provides the annotation class reference (XWiki.XWikiComments), so that the reply button
        // (which is only rendered for default comment annotations) is displayed.
        loadPage(ANNOTATION_CONFIG);
        loadPage(MACROS);

        // The rights service backing the toolbox goes through the real AuthorizationManager, which PageTest only
        // provides as an unstubbed mock: grant view/edit on the annotated document so the toolbox is displayed.
        when(this.oldcore.getMockAuthorizationManager().hasAccess(eq(Right.VIEW), any(), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(eq(Right.EDIT), any(), any())).thenReturn(true);

        // The annotation displayed in the toolbox, stored the same way the annotation service stores it, so that it
        // can be read back by AnnotationScriptService#getAnnotation.
        IOService ioService = this.componentManager.getInstance(IOService.class);
        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("XWiki.Author");
        ioService.addAnnotation("Space.Target", annotation);

        // The #getSanitizedURLAttributeValue macro checks the (already sanitized) URL with $services.html, backed by
        // the HTML sanitizer whose components are heavy to wire: accept any href, URL safety being covered by the
        // real URL security components registered on this test.
        HTMLScriptService htmlScriptService = mock(HTMLScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "html", htmlScriptService);
        when(htmlScriptService.isAttributeSafe(eq("a"), eq("href"), anyString())).thenReturn(true);
    }

    @Test
    void replyButtonRejectsJavascriptXRedirect() throws Exception
    {
        this.stubRequest.put("xredirect", "javascript:alert(1)//");

        Element replyButton = renderReplyButton();

        String href = replyButton.attr("href");
        assertFalse(href.contains("javascript:"),
            String.format("The xredirect payload was injected as a javascript: href: [%s]", href));
        assertEquals("The URI [javascript:alert(1)//] is considered not safe: "
            + "[The given URI [javascript:alert(1)//] is not safe on this server.]", this.logCapture.getMessage(0));
        assertEquals("/xwiki/bin/view/Space/Target#xwikicomment_0", href);
    }

    @Test
    void replyButtonFallsBackWhenXRedirectIsMissing() throws Exception
    {
        Element replyButton = renderReplyButton();

        // Without an xredirect parameter (the common case, e.g. when the toolbox is fetched by the annotations UI),
        // the reply button targets the annotated document view URL.
        assertEquals("/xwiki/bin/view/Space/Target#xwikicomment_0", replyButton.attr("href"));
    }

    @Test
    void replyButtonKeepsXRedirect() throws Exception
    {
        this.stubRequest.put("xredirect", "/xwiki/bin/view/Sandbox/WebHome");

        Element replyButton = renderReplyButton();

        assertEquals("/xwiki/bin/view/Sandbox/WebHome#xwikicomment_0", replyButton.attr("href"));
    }

    private Element renderReplyButton() throws Exception
    {
        XWikiDocument testPage = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "TestPage"),
            this.context);
        testPage.setSyntax(XWIKI_2_0);
        testPage.setContent(
            """
                {{include reference="AnnotationCode.Macros" /}}
                
                {{velocity}}
                {{html clean="false" wiki="false"}}
                #set($docRef = $services.model.createDocumentReference('xwiki', 'Space', 'Target'))
                #set($ann = $services.annotations.getAnnotation('Space.Target', '0'))
                #displayAnnotationToolboxFromReference($ann, 'view', $docRef)
                {{/html}}
                {{/velocity}}""");

        Document document = renderHTMLPage(testPage);
        Element replyButton = document.selectFirst("a.reply");
        assertNotNull(replyButton, "The annotation reply button was not rendered");
        return replyButton;
    }
}
