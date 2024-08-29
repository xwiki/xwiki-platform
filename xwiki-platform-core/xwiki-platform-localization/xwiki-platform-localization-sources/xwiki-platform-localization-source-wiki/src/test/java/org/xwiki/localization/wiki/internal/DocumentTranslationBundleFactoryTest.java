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
import org.mockito.Mockito;
import org.xwiki.cache.infinispan.internal.InfinispanCacheFactory;
import org.xwiki.cache.internal.DefaultCacheManager;
import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;
import org.xwiki.localization.internal.DefaultLocalizationManager;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.localization.messagetool.internal.MessageToolTranslationMessageParser;
import org.xwiki.localization.wiki.internal.TranslationDocumentModel.Scope;
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
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTranslationBundleFactory}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({
    DocumentTranslationBundleFactory.class,
    DefaultLocalizationManager.class,
    DefaultTranslationBundleContext.class,
    DefaultModelContext.class,
    PlainTextBlockRenderer.class,
    PlainTextRendererFactory.class,
    DefaultObservationManager.class,
    DefaultCacheManager.class,
    DefaultCacheManagerConfiguration.class,
    MessageToolTranslationMessageParser.class,
    PlainTextBlockParser.class,
    InfinispanCacheFactory.class })
@ReferenceComponentList
class DocumentTranslationBundleFactoryTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private WikiTranslationConfiguration translationConfiguration;

    @MockComponent
    private QueryManager mockQueryManager;

    @Mock
    private Query mockQuery;

    @MockComponent
    private WikiDescriptorManager mockWikiDescriptorManager;

    @MockComponent
    private ComponentManagerManager componentManagerManager;

    @Inject
    private LocalizationManager localization;

    /**
     * Capture logs.
     */
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    public void before() throws Exception
    {
        this.oldcore.notifyDocumentCreatedEvent(true);
        this.oldcore.notifyDocumentUpdatedEvent(true);
        this.oldcore.notifyDocumentDeletedEvent(true);

        this.oldcore.getXWikiContext().setMainXWiki("xwiki");
        this.oldcore.getXWikiContext().setWikiId("xwiki");

        doReturn("plain/1.0").when(this.oldcore.getSpyXWiki()).getCurrentContentSyntaxId(Mockito.any(String.class),
            Mockito.any(XWikiContext.class));

        when(this.mockQueryManager.createQuery(Mockito.any(String.class), Mockito.any(String.class)))
            .thenReturn(this.mockQuery);
        when(this.mockQuery.execute()).thenReturn(Collections.emptyList());

        when(this.mockWikiDescriptorManager.getMainWikiId()).thenReturn(this.oldcore.getXWikiContext().getMainXWiki());
        when(this.mockWikiDescriptorManager.getCurrentWikiId()).thenReturn(this.oldcore.getXWikiContext().getWikiId());

        // Return the "context" component manager for the current wiki and the current user but not for another wiki.
        when(this.componentManagerManager.getComponentManager("wiki:xwiki", true)).thenReturn(this.oldcore.getMocker());
        when(this.componentManagerManager.getComponentManager("wiki:otherwiki", true))
            .thenReturn(mock(ComponentManager.class));
        when(this.componentManagerManager.getComponentManager("user:null", true)).thenReturn(this.oldcore.getMocker());

        // Initialize document bundle factory
        this.oldcore.getMocker().getInstance(TranslationBundleFactory.class, DocumentTranslationBundleFactory.ID);

        // We want to be notified about new components registrations
        this.oldcore.notifyComponentDescriptorEvent();
    }

    private void addTranslation(String key, String message, DocumentReference reference, Locale locale, Scope scope)
        throws XWikiException
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(reference, this.oldcore.getXWikiContext());

        if (document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE) == null) {
            BaseObject translationObject = new BaseObject();
            translationObject.setXClassReference(
                new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "TranslationDocumentClass"));
            if (scope != null) {
                translationObject.setStringValue(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE,
                    scope.toString());
            }
            document.addXObject(translationObject);

            if (!locale.equals(Locale.ROOT)) {
                this.oldcore.getSpyXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());
            }
        }

        if (!locale.equals(Locale.ROOT)) {
            XWikiDocument tdocument = document.getTranslatedDocument(locale, this.oldcore.getXWikiContext());
            if (tdocument == document) {
                tdocument = new XWikiDocument(document.getDocumentReference(), locale);
                tdocument.setDefaultLocale(document.getDefaultLocale());
            }
            document = tdocument;
        }

        document.setSyntax(Syntax.PLAIN_1_0);

        String content = document.getContent() + '\n'
            + key
            + '='
            + message;

        document.setContent(content);

        this.oldcore.getSpyXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());
    }

    private void assertTranslation(String key, String message, Locale locale)
    {
        Translation translation = this.localization.getTranslation(key, locale);

        if (message != null) {
            assertNotNull(translation, "No translation could be found for key [" + key + "]");
            assertEquals(message, translation.getRawSource());
        } else {
            assertNull(translation);
        }
    }

    private void resetContext() throws ComponentLookupException
    {
        this.oldcore.getExecutionContext().removeProperty(DefaultTranslationBundleContext.CKEY_BUNDLES);
    }

    // tests

    @Test
    void getTranslationScopeWiki() throws XWikiException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation",
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "translation"), Locale.ROOT,
            Scope.WIKI);

        assertTranslation("wiki.translation", null, Locale.ROOT);

        // Reset the cache of translation bundles associated to the current context
        resetContext();

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }

    @Test
    void getTranslationScopeWikiFromOtherWiki() throws XWikiException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation",
            new DocumentReference("otherwiki", "space", "translation"), Locale.ROOT, Scope.WIKI);

        assertTranslation("wiki.translation", null, Locale.ROOT);

        // Reset the cache of translation bundles associated to the current context
        resetContext();

        assertTranslation("wiki.translation", null, Locale.ROOT);
    }

    @Test
    void getTranslationScopeONDemand() throws XWikiException, TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        DocumentReference translationDocument =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "translation");

        addTranslation("wiki.translation", "Wiki translation", translationDocument, Locale.ROOT, Scope.ON_DEMAND);

        // Reset the cache of translation bundles associated to the current context
        resetContext();

        this.localization.use(DocumentTranslationBundleFactory.ID, translationDocument.toString());

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }

    @Test
    void restrictUserTranslations() throws XWikiException, AccessDeniedException
    {
        DocumentReference translationDocument =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "translation");

        when(this.translationConfiguration.isRestrictUserTranslations()).thenReturn(true);

        addTranslation("user.translation", "User translation", translationDocument, Locale.ROOT, Scope.USER);

        assertTranslation("user.translation", "User translation", Locale.ROOT);

        doThrow(new AccessDeniedException(Right.SCRIPT, null, translationDocument))
            .when(this.oldcore.getMockAuthorizationManager()).checkAccess(Right.SCRIPT, null, translationDocument);

        addTranslation("user.translation2", "User translation", translationDocument, Locale.ROOT, Scope.USER);

        assertEquals("Failed to register translation bundle from document [xwiki:space.translation]",
            this.logCapture.getMessage(0));

        assertTranslation("user.translation", null, Locale.ROOT);
    }
}
