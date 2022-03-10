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
package com.xpn.xwiki.web;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@OldcoreTest
@AllComponents
class XWikiMessageToolBridgeTest
{
    private static final String TRANSLATION_FULLNAME = "XWiki.Translations";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiMessageTool tool;

    private DocumentReference preferencesDocumentReference;

    private DocumentReference defaultWikiTranslationReference;

    @BeforeEach
    public void setUp() throws Exception
    {

        this.oldcore.notifyDocumentCreatedEvent(true);
        this.oldcore.notifyDocumentUpdatedEvent(true);
        this.oldcore.notifyDocumentDeletedEvent(true);

        Locale.setDefault(Locale.ROOT);

        // checking

        this.preferencesDocumentReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "XWikiPreferences");

        XWikiDocument preferencesDocument = new XWikiDocument(this.preferencesDocumentReference);
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setXClassReference(new LocalDocumentReference("XWiki", "XWikiPreferences"));
        preferencesDocument.addXObject(preferencesObject);
        preferencesDocument.setSyntax(Syntax.PLAIN_1_0);
        this.oldcore.getSpyXWiki().saveDocument(preferencesDocument, "", this.oldcore.getXWikiContext());

        this.defaultWikiTranslationReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "Translations");

        XWikiDocument defaultWikiTranslation = new XWikiDocument(this.defaultWikiTranslationReference);
        defaultWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        this.oldcore.getSpyXWiki().saveDocument(defaultWikiTranslation, "", this.oldcore.getXWikiContext());

        // MessageTool

        this.tool = new XWikiMessageTool(
            this.oldcore.getMocker().<ContextualLocalizationManager>getInstance(ContextualLocalizationManager.class));
    }

    private void setBundles(String bundles) throws XWikiException
    {
        XWikiDocument preferencesDocument =
            this.oldcore.getSpyXWiki().getDocument(this.preferencesDocumentReference, this.oldcore.getXWikiContext());
        BaseObject preferencesObject = preferencesDocument.getXObject();

        if (!bundles.equals(preferencesObject.getStringValue("documentBundles"))) {
            preferencesObject.setStringValue("documentBundles", bundles);

            this.oldcore.getSpyXWiki().saveDocument(preferencesDocument, "", this.oldcore.getXWikiContext());
        }
    }

    private void addWikiTranslation(String key, String message, Locale locale) throws XWikiException
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(this.defaultWikiTranslationReference,
            this.oldcore.getXWikiContext());

        if (!locale.equals(Locale.ROOT)) {
            XWikiDocument translatedDocument = document.getTranslatedDocument(locale, this.oldcore.getXWikiContext());
            if (translatedDocument == document) {
                translatedDocument = new XWikiDocument(document.getDocumentReference(), locale);
                translatedDocument.setDefaultLocale(document.getDefaultLocale());
            }
            document = translatedDocument;
        }

        document.setSyntax(Syntax.PLAIN_1_0);

        StringBuilder builder = new StringBuilder(document.getContent());

        builder.append('\n');
        builder.append(key);
        builder.append('=');
        builder.append(message);

        document.setContent(builder.toString());

        this.oldcore.getSpyXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());

        setBundles(document.getFullName());
    }

    private void deleteWikiTranslation() throws XWikiException
    {
        this.oldcore.getSpyXWiki().deleteAllDocuments(this.oldcore.getSpyXWiki().getDocument(
            this.defaultWikiTranslationReference, this.oldcore.getXWikiContext()), this.oldcore.getXWikiContext());
    }

    // tests

    @Test
    void getInvalidTranslation()
    {
        assertEquals("doesnotexists.translation", this.tool.get("doesnotexists.translation"));
    }

    @Test
    void getResourceTranslation()
    {
        assertEquals("Language", this.tool.get("language"));
    }

    @Test
    void getWikiTranslation() throws Exception
    {
        addWikiTranslation("wiki.translation", "Wiki translation", Locale.ROOT);

        assertEquals("Wiki translation", this.tool.get("wiki.translation"));
    }

    @Test
    void getTranslatedWikiTranslation() throws Exception
    {
        addWikiTranslation("wiki.translation", "English translation", Locale.ENGLISH);
        addWikiTranslation("wiki.translation", "French translation", Locale.FRENCH);

        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);

        assertEquals("French translation", this.tool.get("wiki.translation"));

        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);

        assertEquals("English translation", this.tool.get("wiki.translation"));
    }

    @Test
    void fallackOnParentLocale() throws Exception
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.US);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);
        addWikiTranslation("wiki.frenchtranslation", "French translation", Locale.FRENCH);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.FRANCE);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));
    }

    @Test
    void updateWikiTranslationCache() throws Exception
    {
        setBundles(TRANSLATION_FULLNAME);

        assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.anothertranslation", "Another translation", Locale.ROOT);

        assertEquals("Another translation", this.tool.get("wiki.anothertranslation"));

        deleteWikiTranslation();

        assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
        assertEquals("wiki.anothertranslation", this.tool.get("wiki.anothertranslation"));
    }

    @Test
    void updateWikiPreferencesCache() throws Exception
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        XWikiDocument otherWikiTranslation = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "OtherTranslations"));
        otherWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        otherWikiTranslation.setContent("wiki.othertranslation=Other translation");
        this.oldcore.getSpyXWiki().saveDocument(otherWikiTranslation, "", this.oldcore.getXWikiContext());

        setBundles(" " + otherWikiTranslation.getFullName());

        assertEquals("Other translation", this.tool.get("wiki.othertranslation"));

        assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
    }

    @Test
    void getTranslationWithParameters() throws Exception
    {
        addWikiTranslation("wiki.translation", "{1} {0}", Locale.ROOT);

        assertEquals("Parameter translation", this.tool.get("wiki.translation", "translation", "Parameter"));
    }

    @Test
    void getEmptyWikiTranslation() throws Exception
    {
        addWikiTranslation("wiki.translation", "", Locale.ROOT);

        assertEquals("", this.tool.get("wiki.translation"));
    }

    @Test
    void fallbackOnResource() throws Exception
    {
        assertEquals("Language", this.tool.get("language"));

        XWikiDocument defaultWikiTranslation = this.oldcore.getSpyXWiki()
            .getDocument(this.defaultWikiTranslationReference, this.oldcore.getXWikiContext());
        defaultWikiTranslation.setDefaultLocale(Locale.FRENCH);

        addWikiTranslation("language", "Overwritten language", Locale.ROOT);

        // ROOT language has been overwritten
        this.oldcore.getXWikiContext().setLocale(Locale.ROOT);
        assertEquals("Overwritten language", this.tool.get("language"));

        // The real locale of ROOT version in FRENCH so it's overwritten too
        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);
        assertEquals("Overwritten language", this.tool.get("language"));

        // GERMAN hasn't been overwritten
        this.oldcore.getXWikiContext().setLocale(Locale.GERMAN);
        assertEquals("Sprache", this.tool.get("language"));

        // There is no ENGLISH translation for this key so it fallback on ROOT
        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);
        assertEquals("Overwritten language", this.tool.get("language"));
    }
}
