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
package org.xwiki.notifications.notifiers.internal.email;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.skin.InternalSkinManager;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogoAttachmentExtractor}.
 *
 * @version $Id$
 */
@ComponentTest
class LogoAttachmentExtractorTest
{
    private static final String COLOR_THEME = "FlamingoThemes.Charcoal";

    private static final DocumentReference COLOR_THEME_REFERENCE =
        new DocumentReference("wiki", "FlamingoThemes", "Charcoal");

    private static final String SKIN = "XWiki.DefaultSkin";

    private static final DocumentReference SKIN_REFERENCE = new DocumentReference("wiki", "XWiki", "DefaultSkin");

    private static final byte[] DEFAULT_LOGO_CONTENT = new byte[] { 1, 2, 3 };

    private static final String LOGO = "logo";

    private static final String SUPPORTED_MEDIA_TYPES_PROPERTY = "notifications.emails.logo.supportedMediaTypes";

    @InjectMockComponents
    private LogoAttachmentExtractor extractor;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("all")
    private ConfigurationSource configurationSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private InternalSkinManager internalSkinManager;

    @MockComponent
    private Environment environment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @XWikiTempDir
    private File temporaryDirectory;

    private XWikiContext xcontext;

    private XWikiDocument colorThemeDocument;

    private XWikiDocument skinDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        // The content of the default logo is stored in a temporary file of the environment.
        Utils.setComponentManager(this.componentManager);
        when(this.environment.getTemporaryDirectory()).thenReturn(this.temporaryDirectory);

        // The supported media types are not configured by default.
        when(this.xwikiProperties.getProperty(eq(SUPPORTED_MEDIA_TYPES_PROPERTY), eq(List.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(2));

        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        when(this.configurationSource.getProperty("colorTheme")).thenReturn(COLOR_THEME);
        when(this.documentReferenceResolver.resolve(COLOR_THEME)).thenReturn(COLOR_THEME_REFERENCE);
        this.colorThemeDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(COLOR_THEME_REFERENCE, this.xcontext)).thenReturn(this.colorThemeDocument);

        when(this.configurationSource.getProperty("skin")).thenReturn(SKIN);
        when(this.documentReferenceResolver.resolve(SKIN)).thenReturn(SKIN_REFERENCE);
        this.skinDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(SKIN_REFERENCE, this.xcontext)).thenReturn(this.skinDocument);

        Skin skin = mock(Skin.class);
        when(this.internalSkinManager.getCurrentSkin(true)).thenReturn(skin);
        Resource<InputStreamInputSource> defaultLogo = mock(Resource.class);
        doReturn(defaultLogo).when(skin).getResource("logo.png");
        when(defaultLogo.getInputSource()).thenAnswer(
            invocation -> new DefaultInputStreamInputSource(new ByteArrayInputStream(DEFAULT_LOGO_CONTENT)));
    }

    @AfterEach
    void tearDown()
    {
        Utils.setComponentManager(null);
    }

    @Test
    void getLogoWithSupportedColorThemeLogo() throws Exception
    {
        XWikiAttachment attachment = mockLogo(this.colorThemeDocument, LOGO, "logo.jpg", "image/jpeg");

        Attachment logo = this.extractor.getLogo();

        assertEquals(LOGO, logo.getFilename());
        assertEquals("image/jpeg", logo.getMimeType());
        verify(attachment).loadAttachmentContent(this.xcontext);
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/png", "image/gif", "image/bmp", "image/x-icon", "image/vnd.microsoft.icon",
        "image/webp" })
    void getLogoWithColorThemeLogoSupportedByDefault(String mediaType) throws Exception
    {
        mockLogo(this.colorThemeDocument, LOGO, "logo.img", mediaType);

        Attachment logo = this.extractor.getLogo();

        assertEquals(LOGO, logo.getFilename());
        assertEquals(mediaType, logo.getMimeType());
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/svg+xml", "image/tiff", "image/avif", "image/heif", "image/jxl" })
    void getLogoWithColorThemeLogoNotSupportedByDefault(String mediaType) throws Exception
    {
        mockLogo(this.colorThemeDocument, LOGO, "logo.img", mediaType);
        mockLogo(this.skinDocument, LOGO, "logo.gif", "image/gif");

        Attachment logo = this.extractor.getLogo();

        assertEquals(LOGO, logo.getFilename());
        assertEquals("image/gif", logo.getMimeType());
    }

    @Test
    void getLogoWithUnsupportedColorThemeLogoName() throws Exception
    {
        // The logo is skipped because of its media type, whatever its file name.
        mockLogo(this.colorThemeDocument, LOGO, "Logo.SVG", "image/svg+xml");
        mockLogo(this.skinDocument, LOGO, "logo.png", "image/png");

        Attachment logo = this.extractor.getLogo();

        assertEquals("image/png", logo.getMimeType());
    }

    @Test
    void getLogoWithUnsupportedColorThemeAndSkinLogos() throws Exception
    {
        mockLogo(this.colorThemeDocument, "logoImage", "logo.svg", "image/svg+xml");
        mockLogo(this.skinDocument, LOGO, "logo.tiff", "image/tiff");

        Attachment logo = this.extractor.getLogo();

        assertEquals(LOGO, logo.getFilename());
        assertEquals("image/png", logo.getMimeType());
        assertArrayEquals(DEFAULT_LOGO_CONTENT, logo.getContentAsBytes());
    }

    @Test
    void getLogoWithConfiguredSupportedMediaTypes() throws Exception
    {
        // The configured list replaces the default one, and its values are trimmed and compared ignoring the case.
        when(this.xwikiProperties.getProperty(eq(SUPPORTED_MEDIA_TYPES_PROPERTY), eq(List.class), any()))
            .thenReturn(List.of(" Image/SVG+xml ", ""));
        mockLogo(this.colorThemeDocument, LOGO, "logo.webp", "image/webp");
        mockLogo(this.skinDocument, LOGO, "logo.svg", "image/svg+xml");

        Attachment logo = this.extractor.getLogo();

        assertEquals(LOGO, logo.getFilename());
        assertEquals("image/svg+xml", logo.getMimeType());
    }

    @Test
    void getLogoWithEmptySupportedMediaTypes() throws Exception
    {
        // An empty list means that the default logo is always used.
        when(this.xwikiProperties.getProperty(eq(SUPPORTED_MEDIA_TYPES_PROPERTY), eq(List.class), any()))
            .thenReturn(List.of());
        mockLogo(this.colorThemeDocument, LOGO, "logo.png", "image/png");
        mockLogo(this.skinDocument, LOGO, "logo.png", "image/png");

        Attachment logo = this.extractor.getLogo();

        assertEquals("image/png", logo.getMimeType());
        assertArrayEquals(DEFAULT_LOGO_CONTENT, logo.getContentAsBytes());
    }

    @Test
    void getLogoWithUpperCaseMediaType() throws Exception
    {
        mockLogo(this.colorThemeDocument, LOGO, "logo.png", "IMAGE/PNG");

        Attachment logo = this.extractor.getLogo();

        assertEquals("IMAGE/PNG", logo.getMimeType());
    }

    @Test
    void getLogoWithMissingLogoAttachments() throws Exception
    {
        when(this.colorThemeDocument.getStringValue(LOGO)).thenReturn("logo.png");
        when(this.skinDocument.getStringValue(LOGO)).thenReturn("logo.png");

        Attachment logo = this.extractor.getLogo();

        assertEquals("image/png", logo.getMimeType());
        assertArrayEquals(DEFAULT_LOGO_CONTENT, logo.getContentAsBytes());
    }

    private XWikiAttachment mockLogo(XWikiDocument document, String property, String fileName, String mediaType)
    {
        when(document.getStringValue(property)).thenReturn(fileName);
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(document.getAttachment(fileName)).thenReturn(attachment);
        when(attachment.getFilename()).thenReturn(fileName);
        when(attachment.getMimeType(this.xcontext)).thenReturn(mediaType);
        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(attachment.getAttachment_content()).thenReturn(attachmentContent);

        return attachment;
    }
}
