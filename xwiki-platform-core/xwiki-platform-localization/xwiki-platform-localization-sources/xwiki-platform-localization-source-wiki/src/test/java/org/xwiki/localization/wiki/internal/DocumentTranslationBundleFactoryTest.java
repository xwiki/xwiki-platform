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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.localization.wiki.internal.TranslationDocumentModel.Scope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import org.junit.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllComponents
public class DocumentTranslationBundleFactoryTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private QueryManager mockQueryManager;

    private Query mockQuery;

    private WikiDescriptorManager mockWikiDescriptorManager;

    private LocalizationManager localization;

    public DocumentTranslationBundleFactoryTest()
    {
        this.oldcore.notifyDocumentCreatedEvent(true);
        this.oldcore.notifyDocumentUpdatedEvent(true);
        this.oldcore.notifyDocumentDeletedEvent(true);
    }

    @Before
    public void before() throws Exception
    {
        this.oldcore.getXWikiContext().setMainXWiki("xwiki");
        this.oldcore.getXWikiContext().setWikiId("xwiki");

        when(this.oldcore.getMockXWiki().getCurrentContentSyntaxId(Mockito.any(String.class),
            Mockito.any(XWikiContext.class))).thenReturn("plain/1.0");

        this.mockQuery = mock(Query.class);

        when(this.mockQueryManager.createQuery(Mockito.any(String.class),
            Mockito.any(String.class))).thenReturn(this.mockQuery);
        when(this.mockQuery.execute()).thenReturn(Collections.EMPTY_LIST);

        when(this.mockWikiDescriptorManager.getMainWikiId()).thenReturn(this.oldcore.getXWikiContext().getMainXWiki());
        when(this.mockWikiDescriptorManager.getCurrentWikiId()).thenReturn(this.oldcore.getXWikiContext().getWikiId());

        // Initialize document bundle factory
        this.oldcore.getMocker().getInstance(TranslationBundleFactory.class, DocumentTranslationBundleFactory.ID);

        this.localization = this.oldcore.getMocker().getInstance(LocalizationManager.class);

        this.oldcore.getMocker().registerMockComponent(ConfigurationSource.class);
    }

    @AfterComponent
    public void registerComponents() throws Exception
    {
        this.mockQueryManager = this.oldcore.getMocker().registerMockComponent(QueryManager.class);
        this.mockWikiDescriptorManager = this.oldcore.getMocker().registerMockComponent(WikiDescriptorManager.class);
    }

    private void addTranslation(String key, String message, DocumentReference reference, Locale locale, Scope scope)
        throws XWikiException
    {
        XWikiDocument document = this.oldcore.getMockXWiki().getDocument(reference, this.oldcore.getXWikiContext());

        if (document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE) == null) {
            BaseObject translationObject = new BaseObject();
            translationObject.setXClassReference(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(),
                "XWiki", "TranslationDocumentClass"));
            if (scope != null) {
                translationObject
                    .setStringValue(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE, scope.toString());
            }
            document.addXObject(translationObject);

            if (!locale.equals(Locale.ROOT)) {
                this.oldcore.getMockXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());
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

        StringBuilder builder = new StringBuilder(document.getContent());

        builder.append('\n');
        builder.append(key);
        builder.append('=');
        builder.append(message);

        document.setContent(builder.toString());

        this.oldcore.getMockXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());
    }

    private void assertTranslation(String key, String message, Locale locale)
    {
        Translation translation = this.localization.getTranslation(key, locale);

        if (message != null) {
            Assert.assertNotNull(translation);
            Assert.assertEquals(message, translation.getRawSource());
        } else {
            Assert.assertNull(translation);
        }
    }

    private void resetContext() throws ComponentLookupException
    {
        this.oldcore.getExecutionContext().removeProperty(DefaultTranslationBundleContext.CKEY_BUNDLES);
    }

    // tests

    @Test
    public void getTranslationScopeWiki() throws XWikiException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation", new DocumentReference(this.oldcore.getXWikiContext()
            .getWikiId(), "space", "translation"), Locale.ROOT, Scope.WIKI);

        assertTranslation("wiki.translation", null, Locale.ROOT);

        resetContext();

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }

    @Test
    public void getTranslationScopeWikiFromOtherWiki() throws XWikiException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation", new DocumentReference("otherwiki", "space",
            "translation"), Locale.ROOT, Scope.WIKI);

        assertTranslation("wiki.translation", null, Locale.ROOT);

        resetContext();

        assertTranslation("wiki.translation", null, Locale.ROOT);
    }

    @Test
    public void getTranslationScopeONDemand() throws XWikiException, TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException, ComponentLookupException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        DocumentReference translationDocument =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "translation");

        addTranslation("wiki.translation", "Wiki translation", translationDocument, Locale.ROOT, Scope.ON_DEMAND);

        resetContext();

        this.localization.use(DocumentTranslationBundleFactory.ID, translationDocument.toString());

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }
}
