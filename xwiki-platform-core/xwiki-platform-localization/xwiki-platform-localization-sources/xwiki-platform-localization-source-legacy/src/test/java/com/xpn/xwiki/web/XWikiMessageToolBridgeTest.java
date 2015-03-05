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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.environment.Environment;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

@AllComponents
public class XWikiMessageToolBridgeTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private XWikiMessageTool tool;

    private DocumentReference preferencesDocumentReference;

    private DocumentReference defaultWikiTranslationReference;

    private static final String TRANSLATION_FULLNAME = "XWiki.Translations";

    public XWikiMessageToolBridgeTest()
    {
        this.oldcore.notifyDocumentCreatedEvent(true);
        this.oldcore.notifyDocumentUpdatedEvent(true);
        this.oldcore.notifyDocumentDeletedEvent(true);
    }

    @Before
    public void before() throws Exception
    {
        // Register a mock Environment since we're in a test and we don't want spurious logs in the console.
        this.oldcore.getMocker().registerMockComponent(Environment.class);

        Locale.setDefault(Locale.ROOT);

        // checking

        this.preferencesDocumentReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "XWikiPreferences");

        XWikiDocument preferencesDocument = new XWikiDocument(this.preferencesDocumentReference);
        BaseObject preferencesObject = new BaseObject();
        preferencesObject.setXClassReference(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki",
            "XWikiPreferences"));
        preferencesDocument.addXObject(preferencesObject);
        preferencesDocument.setSyntax(Syntax.PLAIN_1_0);
        this.oldcore.getMockXWiki().saveDocument(preferencesDocument, "", this.oldcore.getXWikiContext());

        this.defaultWikiTranslationReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "Translations");

        XWikiDocument defaultWikiTranslation = new XWikiDocument(this.defaultWikiTranslationReference);
        defaultWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        this.oldcore.getMockXWiki().saveDocument(defaultWikiTranslation, "", this.oldcore.getXWikiContext());

        // MessageTool

        this.tool =
            new XWikiMessageTool(this.oldcore.getMocker().<ContextualLocalizationManager>getInstance(
                ContextualLocalizationManager.class));
    }

    private void setBundles(String bundles) throws XWikiException
    {
        XWikiDocument preferencesDocument =
            this.oldcore.getMockXWiki().getDocument(this.preferencesDocumentReference, this.oldcore.getXWikiContext());
        BaseObject preferencesObject = preferencesDocument.getXObject();

        if (!bundles.equals(preferencesObject.getStringValue("documentBundles"))) {
            preferencesObject.setStringValue("documentBundles", bundles);

            this.oldcore.getMockXWiki().saveDocument(preferencesDocument, "", this.oldcore.getXWikiContext());
        }
    }

    private void addWikiTranslation(String key, String message, Locale locale) throws XWikiException
    {
        XWikiDocument document =
            this.oldcore.getMockXWiki().getDocument(this.defaultWikiTranslationReference,
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

        this.oldcore.getMockXWiki().saveDocument(document, "", this.oldcore.getXWikiContext());

        setBundles(document.getFullName());
    }

    private void deleteWikiTranslation() throws XWikiException
    {
        this.oldcore.getMockXWiki().deleteAllDocuments(
            this.oldcore.getMockXWiki().getDocument(this.defaultWikiTranslationReference,
                this.oldcore.getXWikiContext()), this.oldcore.getXWikiContext());
    }

    // tests

    @Test
    public void getInvalidTranslation()
    {
        Assert.assertEquals("doesnotexists.translation", this.tool.get("doesnotexists.translation"));
    }

    @Test
    public void getResourceTranslation()
    {
        Assert.assertEquals("Language", this.tool.get("language"));
    }

    @Test
    public void getWikiTranslation() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.translation", "Wiki translation", Locale.ROOT);

        Assert.assertEquals("Wiki translation", this.tool.get("wiki.translation"));
    }

    @Test
    public void getTranslatedWikiTranslation() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.translation", "English translation", Locale.ENGLISH);
        addWikiTranslation("wiki.translation", "French translation", Locale.FRENCH);

        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);

        Assert.assertEquals("French translation", this.tool.get("wiki.translation"));

        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);

        Assert.assertEquals("English translation", this.tool.get("wiki.translation"));
    }

    @Test
    public void fallackOnParentLocale() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.US);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);
        addWikiTranslation("wiki.frenchtranslation", "French translation", Locale.FRENCH);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));

        this.oldcore.getXWikiContext().setLocale(Locale.FRANCE);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));
    }

    @Test
    public void updateWikiTranslationCache() throws XWikiException, ComponentLookupException
    {
        setBundles(TRANSLATION_FULLNAME);

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.anothertranslation", "Another translation", Locale.ROOT);

        Assert.assertEquals("Another translation", this.tool.get("wiki.anothertranslation"));

        deleteWikiTranslation();

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("wiki.anothertranslation", this.tool.get("wiki.anothertranslation"));
    }

    @Test
    public void updateWikiPreferencesCache() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        XWikiDocument otherWikiTranslation =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki",
                "OtherTranslations"));
        otherWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        otherWikiTranslation.setContent("wiki.othertranslation=Other translation");
        this.oldcore.getMockXWiki().saveDocument(otherWikiTranslation, "", this.oldcore.getXWikiContext());

        setBundles(" " + otherWikiTranslation.getFullName());

        Assert.assertEquals("Other translation", this.tool.get("wiki.othertranslation"));

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
    }

    @Test
    public void getTranslationWithParameters() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.translation", "{1} {0}", Locale.ROOT);

        Assert.assertEquals("Parameter translation", this.tool.get("wiki.translation", "translation", "Parameter"));
    }

    @Test
    public void getEmptyWikiTranslation() throws XWikiException, ComponentLookupException
    {
        addWikiTranslation("wiki.translation", "", Locale.ROOT);

        Assert.assertEquals("", this.tool.get("wiki.translation"));
    }

    @Test
    public void fallbackOnResource() throws XWikiException, ComponentLookupException
    {
        Assert.assertEquals("Language", this.tool.get("language"));

        XWikiDocument defaultWikiTranslation =
            this.oldcore.getMockXWiki().getDocument(this.defaultWikiTranslationReference,
                this.oldcore.getXWikiContext());
        defaultWikiTranslation.setDefaultLocale(Locale.FRENCH);

        addWikiTranslation("language", "Overwritten language", Locale.ROOT);

        // ROOT language has been overwritten
        this.oldcore.getXWikiContext().setLocale(Locale.ROOT);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));

        // The real locale of ROOT version in FRENCH so it's overwritten too
        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));

        // GERMAN hasn't been overwritten
        this.oldcore.getXWikiContext().setLocale(Locale.GERMAN);
        Assert.assertEquals("Sprache", this.tool.get("language"));

        // There is no ENGLISH translation for this key so it fallback on ROOT
        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));
    }
}
