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
package org.xwiki.attachment;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultContentParser;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.icon.internal.DefaultIconManagerComponentList;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager;
import org.xwiki.rendering.internal.macro.wikibridge.WikiMacroEventListener;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacroFactory;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacroRenderer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.store.script.TemporaryAttachmentsScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.IconSetup;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@code XWiki.AttachmentSelector}.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.2
 * @since 13.10.7
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultIconManagerComponentList
@ComponentList({
    // Start -  Required in addition of RenderingScriptServiceComponentList
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // Start -  Required in addition of RenderingScriptServiceComponentList
    ControlCharactersFilter.class,
    ModelScriptService.class,
    TestNoScriptMacro.class,
    // Start WikiMacroEventListener
    WikiMacroEventListener.class,
    DefaultWikiMacroFactory.class,
    DefaultWikiMacroManager.class,
    DefaultContentParser.class,
    org.xwiki.rendering.internal.parser.DefaultContentParser.class,
    DefaultWikiMacroRenderer.class,
    // End WikiMacroEventListener
    TemporaryAttachmentsScriptService.class,
    IconManagerScriptService.class,
    DocumentReferenceConverter.class
})
class AttachmentSelectorPageTest extends PageTest
{
    /**
     * Mocked because we don't want to deal with actual sessions for this test.
     */
    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.temporaryAttachmentSessionsManager.getUploadedAttachments(any(DocumentReference.class)))
            .thenReturn(List.of());

        // Initializes then environment for the icon extension.
        IconSetup.setUp(this, "/icons.properties");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{{noscript}}println(\"Hello from noscript!\"){{/noscript}}.png",
        "]] {{noscript}}println(\"Hello from noscript!\"){{/noscript}}.png"
    })
    void renderDisplayImageFalse(String fileName) throws Exception
    {
        commonFixup(fileName);

        this.request.put("docname", "xwiki:Space.Test");
        this.request.put("classname", "xwiki:Space.Test");
        this.request.put("property", "avatar");
        this.request.put("object", "0");
        this.request.put("filter", "png");
        this.request.put("displayImage", "false");
        this.request.put("xpage", "plain");

        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "AttachmentSelector"));

        assertNotNull(document);
        Element galleryAttachmentTitle = document.select(".current .gallery_attachmenttitle").get(0);
        assertEquals(fileName, galleryAttachmentTitle.attr("title"));
        assertEquals(fileName, galleryAttachmentTitle.text());
        assertEquals(fileName, document.select(".gallery_attachmentframe .filename").text());
        assertEquals(String.format("attach:xwiki:Space.Test@%s", fileName),
            document.select(".gallery_attachmentframe a").attr("href"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{{noscript}}println(\"Hello from noscript!\"){{/noscript}}.png",
        "]] {{noscript}}println(\"Hello from noscript!\"){{/noscript}}.png"
    })
    void renderDisplayImageTrue(String fileName) throws Exception
    {
        commonFixup(fileName);

        this.request.put("docname", "xwiki:Space.Test");
        this.request.put("classname", "xwiki:Space.Test");
        this.request.put("property", "avatar");
        this.request.put("object", "0");
        this.request.put("filter", "png");
        this.request.put("displayImage", "true");
        this.request.put("xpage", "plain");

        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "AttachmentSelector"));
        assertNotNull(document);
        Element galleryAttachmentTitle = document.select(".current .gallery_attachmenttitle").get(0);
        assertEquals(fileName, galleryAttachmentTitle.attr("title"));
        assertEquals(fileName, galleryAttachmentTitle.text());
        assertEquals(String.format("attach:xwiki:Space.Test@%s", fileName),
            document.select(".gallery_attachmentframe a").attr("href"));

        Elements img = document.select(".gallery_attachmentframe img");
        assertEquals(String.format("xwiki:Space.Test@%s", fileName), img.attr("src"));
        assertEquals(String.format("xwiki:Space.Test@%s", fileName), img.attr("alt"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroWidth(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "width=\"%s\" "
            + "displayImage=\"true\"/}}", widthValue));

        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("width"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroHeight(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "height=\"%s\" "
            + "displayImage=\"true\"/}}", widthValue));
        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("height"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroAlternateText(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "alternateText=\"%s\" "
            + "displayImage=\"true\"/}}", widthValue));
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("alt"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroWidthWithLink(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "width=\"%s\" "
            + "link=\"true\" "
            + "displayImage=\"true\"/}}", widthValue));

        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("width"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroHeightWithLink(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "height=\"%s\" "
            + "link=\"true\" "
            + "displayImage=\"true\"/}}", widthValue));
        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("height"));
    }

    @ParameterizedTest
    @MethodSource("attachmentSelectorMacroSource")
    void attachmentSelectorMacroAlternateTextWithLink(String widthValue, String expectedWidth) throws Exception
    {
        attachmentSelectorMacroFixup();

        XWikiDocument xwikiDocument = commonFixup("test.png");

        xwikiDocument.setContent(String.format("{{attachmentSelector "
            + "classname=\"Space.Test\" "
            + "property=\"avatar\" "
            + "savemode=\"direct\" "
            + "alternateText=\"%s\" "
            + "link=\"true\" "
            + "displayImage=\"true\"/}}", widthValue));
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        Document document = renderHTMLPage(xwikiDocument);
        assertEquals(expectedWidth, document.select(".displayed img").attr("alt"));
    }

    @Test
    void withTemporaryAttachment() throws Exception
    {
        String fileName = "test.png";

        XWikiDocument xWikiDocument = commonFixup(fileName);

        XWikiAttachment xWikiAttachment = mock(XWikiAttachment.class);
        when(xWikiAttachment.getFilename()).thenReturn(fileName);
        AttachmentReference attachmentReference =
            new AttachmentReference(fileName, xWikiDocument.getDocumentReference());
        when(xWikiAttachment.getReference()).thenReturn(attachmentReference);
        when(this.temporaryAttachmentSessionsManager.getUploadedAttachments(xWikiDocument.getDocumentReference()))
            .thenReturn(List.of(xWikiAttachment));
        when(this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachmentReference))
            .thenReturn(Optional.of(xWikiAttachment));

        this.request.put("docname", "xwiki:Space.Test");
        this.request.put("classname", "xwiki:Space.Test");
        this.request.put("property", "avatar");
        this.request.put("object", "0");
        this.request.put("filter", "png");
        this.request.put("displayImage", "true");
        this.request.put("xpage", "plain");

        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "AttachmentSelector"));
        Element element = document.selectFirst(".gallery_attachmentbox.current");
        assertTrue(element.classNames().contains("temporary_attachment"),
            String.format("temporary_attachment class not found in %s", element.classNames()));
        assertNotNull(element.selectFirst(".fake-attach"), "An icon with class fake-attach is expected to "
            + "be found");
    }

    private void attachmentSelectorMacroFixup() throws Exception
    {
        DocumentReference attachmentSelectorDocumentReference =
            new DocumentReference("xwiki", "XWiki", "AttachmentSelector");
        XWikiDocument xWikiDocument = loadPage(attachmentSelectorDocumentReference);

        // TODO: The code below is more generic than this test and should be moved to be reusable. 
        // Make the wiki component manager point to the default component manager.
        this.componentManager.registerComponent(ComponentManager.class, "wiki",
            this.componentManager.getInstance(ComponentManager.class));
        when(this.componentManager.<AuthorizationManager>getInstance(AuthorizationManager.class)
            .hasAccess(Right.ADMIN, new DocumentReference("xwiki", "XWiki", "Admin"),
                xWikiDocument.getDocumentReference().getWikiReference())).thenReturn(true);
        // Simulate the event not thrown by page test for now.
        this.componentManager.<EventListener>getInstance(EventListener.class, "wikimacrolistener")
            .onEvent(new DocumentCreatedEvent(), xWikiDocument, null);
    }

    private XWikiDocument commonFixup(String fileName) throws XWikiException, IOException
    {
        // Initialize a document containing an XClass definition and an XObject of this XClass.
        // The interesting property is the avatar string field, which references the name of an attachment contained in
        // the document itself too.
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Test");
        XWikiDocument xwikiDocument = this.xwiki.getDocument(documentReference, this.context);
        BaseClass xClass = xwikiDocument.getXClass();
        xClass.addTextField("avatar", "Avatar", 10);
        BaseObject baseObject = xwikiDocument.newXObject(xwikiDocument.getDocumentReference(), this.context);
        baseObject.setStringValue("avatar", fileName);
        // Simulates the file rename action.
        xwikiDocument.setAttachment("tmp.png", IOUtils.toInputStream("", Charset.defaultCharset()), this.context);
        xwikiDocument.getAttachment("tmp.png").setFilename(fileName);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);

        this.xwiki.saveDocument(xwikiDocument, this.context);

        this.context.setDoc(xwikiDocument);

        return xwikiDocument;
    }

    public static Stream<Arguments> attachmentSelectorMacroSource()
    {
        return Stream.of(
            arguments("{{noscript /~}~}", "{{noscript /}}"),
            arguments("]] {{noscript /~}~}", "]] {{noscript /}}"),
            arguments("]] {{noscript /~}~} [[", "]] {{noscript /}} [[")
        );
    }
}
