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
package org.xwiki.localization.wiki.internal;

import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.internal.DefaultLocalizationManager;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.localization.messagetool.internal.MessageToolTranslationMessageParser;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.renderer.plain.PlainTextBlockRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ComponentDocumentTranslationBundle}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({
    DocumentTranslationBundleFactory.class,
    DefaultLocalizationManager.class,
    DefaultTranslationBundleContext.class,
    TranslationDocumentClassInitializer.class,
    DefaultModelContext.class,
    PlainTextBlockRenderer.class,
    PlainTextRendererFactory.class,
    DefaultObservationManager.class,
    MessageToolTranslationMessageParser.class,
    PlainTextBlockParser.class
})
@ReferenceComponentList
class ComponentDocumentTranslationBundleTest
{
    private static final DocumentReference TRANSLATION_ROOT_REFERENCE = new DocumentReference("xwiki", "space",
        "Translations");

    private static final DocumentReference ADMIN_USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiAdmin");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private WikiTranslationConfiguration translationConfiguration;

    @MockComponent
    private QueryManager mockQueryManager;

    @Mock
    private Query mockQuery;

    @MockComponent
    private ComponentManagerManager componentManagerManager;

    @MockComponent
    private JobProgressManager jobProgressManager;

    @Inject
    private LocalizationManager localization;

    private XWikiDocument translationFrDocument;

    private DocumentReference adminUserReference;

    /**
     * Capture logs.
     */
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private CacheManager cacheManager;

    @BeforeComponent
    void before() throws Exception
    {
        Cache<Object> cache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        this.oldcore.notifyDocumentCreatedEvent(true);

        when(this.mockQueryManager.createQuery(anyString(), anyString())).thenReturn(this.mockQuery);
        when(this.mockQuery.execute()).thenReturn(Collections.emptyList());

        when(this.componentManagerManager.getComponentManager("wiki:xwiki", true)).thenReturn(this.oldcore.getMocker());
        this.oldcore.getMocker().getInstance(TranslationBundleFactory.class, DocumentTranslationBundleFactory.ID);

        XWikiDocument translationRootDocument = this.oldcore.getSpyXWiki().getDocument(TRANSLATION_ROOT_REFERENCE,
            this.oldcore.getXWikiContext());

        BaseObject translationObject = translationRootDocument.newXObject(
            new DocumentReference("xwiki", "XWiki", "TranslationDocumentClass"),
            this.oldcore.getXWikiContext());
        translationObject.setStringValue(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE,
            TranslationDocumentModel.Scope.WIKI.toString());

        translationRootDocument.setSyntax(Syntax.PLAIN_1_0);
        translationRootDocument.setContent("xwiki.translation=root");
        translationRootDocument.setAuthorReference(ADMIN_USER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(translationRootDocument, this.oldcore.getXWikiContext());

        this.translationFrDocument = translationRootDocument.getTranslatedDocument(Locale.FRENCH,
            this.oldcore.getXWikiContext());
        if (this.translationFrDocument == translationRootDocument) {
            this.translationFrDocument =
                new XWikiDocument(this.translationFrDocument.getDocumentReference(), Locale.FRENCH);
            this.translationFrDocument.setDefaultLocale(this.translationFrDocument.getDefaultLocale());
        }
        this.translationFrDocument.setSyntax(Syntax.PLAIN_1_0);
        this.translationFrDocument.setContent("xwiki.translation=fr");
        this.oldcore.getSpyXWiki().saveDocument(this.translationFrDocument, this.oldcore.getXWikiContext());

        doThrow(new AccessDeniedException(Right.SCRIPT, null, translationRootDocument.getDocumentReference()))
            .when(this.oldcore.getMockAuthorizationManager()).checkAccess(Right.ADMIN, null,
                TRANSLATION_ROOT_REFERENCE.getWikiReference());
    }

    @Test
    void checkTranslationWithExpectedRights() throws Exception
    {
        this.translationFrDocument.setAuthorReference(ADMIN_USER_REFERENCE);
        this.translationFrDocument.setContentAuthorReference(ADMIN_USER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(this.translationFrDocument, this.oldcore.getXWikiContext());
        Translation frTranslation = this.localization.getTranslation("xwiki.translation", Locale.FRENCH);
        assertEquals("fr", frTranslation.getRawSource());
        // Authorizations are checked twice because the mocked behavior is not actual locale bundle registration.
        verify(this.oldcore.getMockAuthorizationManager(), times(4)).checkAccess(Right.ADMIN, ADMIN_USER_REFERENCE,
            TRANSLATION_ROOT_REFERENCE.getWikiReference());
    }

    @Test
    void checkTranslationWithoutExpectedRights() throws Exception
    {
        Translation frTranslation = this.localization.getTranslation("xwiki.translation", Locale.FRENCH);
        assertEquals(
            "Failed to load and register the translation for locale [fr] from document [xwiki:space.Translations]. "
                + "Falling back to default locale.",
            this.logCapture.getMessage(0));
        assertEquals("root", frTranslation.getRawSource());
        verify(this.oldcore.getMockAuthorizationManager()).checkAccess(Right.ADMIN, null,
            TRANSLATION_ROOT_REFERENCE.getWikiReference());
    }
}
