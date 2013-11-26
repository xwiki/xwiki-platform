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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

public class XWikiMessageToolBridgeTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private ServletContext mockServletContext;

    private Map<DocumentReference, Map<Locale, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<Locale, XWikiDocument>>();

    private XWikiMessageTool tool;

    private XWikiDocument preferencesDocument;

    private BaseObject preferencesObject;

    private XWikiDocument defaultWikiTranslation;

    private ObservationManager observation;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");

        this.mockStore = getMockery().mock(XWikiStoreInterface.class);

        ServletEnvironment environment = (ServletEnvironment) getComponentManager().getInstance(Environment.class);
        this.mockServletContext = environment.getServletContext();

        // checking

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getDocument(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("getDocument")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        Map<Locale, XWikiDocument> documentLanguages = documents.get(invocation.getParameter(0));

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<Locale, XWikiDocument>();
                            documents.put((DocumentReference) invocation.getParameter(0), documentLanguages);
                        }

                        XWikiDocument document = documentLanguages.get(Locale.ROOT);

                        if (document == null) {
                            document = new XWikiDocument((DocumentReference) invocation.getParameter(0));
                        }

                        return document;
                    }
                });

                allowing(mockStore).loadXWikiDoc(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(new CustomAction("loadXWikiDoc")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        XWikiDocument providedDocument = (XWikiDocument) invocation.getParameter(0);
                        Map<Locale, XWikiDocument> documentLanguages =
                            documents.get(providedDocument.getDocumentReference());

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<Locale, XWikiDocument>();
                            documents.put((DocumentReference) invocation.getParameter(0), documentLanguages);
                        }

                        XWikiDocument document = documentLanguages.get(providedDocument.getLocale());

                        if (document == null) {
                            document = new XWikiDocument(providedDocument.getDocumentReference());
                            document.setLocale(providedDocument.getLocale());
                            document.setDefaultLocale(providedDocument.getDefaultLocale());
                            document.setTranslation(providedDocument.getTranslation());
                        }

                        return document;
                    }
                });

                allowing(mockXWiki).saveDocument(with(any(XWikiDocument.class)), with(any(String.class)),
                    with(any(XWikiContext.class)));
                will(new CustomAction("saveDocument")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);

                        document.incrementVersion();
                        document.setNew(false);

                        Map<Locale, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        XWikiDocument previousDocument;
                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<Locale, XWikiDocument>();
                            documents.put(document.getDocumentReference(), documentLanguages);
                            previousDocument = null;
                        } else {
                            previousDocument = documentLanguages.get(document.getLocale());
                        }

                        for (XWikiAttachment attachment : document.getAttachmentList()) {
                            if (!attachment.isContentDirty()) {
                                attachment.setAttachment_content(previousDocument.getAttachment(
                                    attachment.getFilename()).getAttachment_content());
                            }
                        }

                        documentLanguages.put(document.getLocale(), document);

                        return null;
                    }
                });

                allowing(mockXWiki).deleteDocument(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(new CustomAction("deleteDocument")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);

                        Map<Locale, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        if (documentLanguages != null) {
                            documentLanguages.remove(document.getLocale());
                        }

                        observation.notify(new DocumentCreatedEvent(document.getDocumentReference()), document,
                            getContext());

                        return null;
                    }
                });

                allowing(mockXWiki).getStore();
                will(returnValue(mockStore));

                allowing(mockXWiki).prepareResources(with(any(XWikiContext.class)));

                allowing(mockXWiki).getLanguagePreference(with(any(XWikiContext.class)));
                will(new CustomAction("getLanguagePreference")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        return getContext().getLanguage();
                    }
                });

                allowing(mockXWiki).getCurrentContentSyntaxId(with(any(String.class)), with(any(XWikiContext.class)));
                will(returnValue("plain/1.0"));

                allowing(mockServletContext).getResourceAsStream("/META-INF/MANIFEST.MF");
                will(returnValue(null));
            }
        });

        this.preferencesDocument =
            new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreferences"));
        this.preferencesObject = new BaseObject();
        this.preferencesObject.setXClassReference(new DocumentReference(getContext().getDatabase(), "XWiki",
            "XWikiPreferences"));
        this.preferencesDocument.addXObject(this.preferencesObject);
        this.mockXWiki.saveDocument(this.preferencesDocument, "", getContext());

        this.defaultWikiTranslation =
            new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "Translations"));
        this.defaultWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        this.mockXWiki.saveDocument(this.defaultWikiTranslation, "", getContext());

        this.observation = getComponentManager().getInstance(ObservationManager.class);

        // MessageTool

        this.tool =
            new XWikiMessageTool(getComponentManager().<ContextualLocalizationManager> getInstance(
                ContextualLocalizationManager.class));
    }

    private void setBundles(String bundles)
    {
        this.preferencesDocument.setOriginalDocument(this.preferencesDocument.clone());

        if (!bundles.equals(this.preferencesObject.getStringValue("documentBundles"))) {
            this.preferencesObject.setStringValue("documentBundles", bundles);

            this.observation.notify(
                new XObjectPropertyUpdatedEvent(new ObjectPropertyReference("documentBundles",
                    new BaseObjectReference(this.preferencesDocument.getDocumentReference(), 0,
                        this.preferencesDocument.getDocumentReference()))), this.preferencesDocument, getContext());
        }
    }

    private void addWikiTranslation(String key, String message, Locale locale) throws XWikiException
    {
        XWikiDocument document = this.defaultWikiTranslation;

        if (locale != null && !locale.equals(Locale.ROOT)) {
            XWikiDocument translatedDocument = document.getTranslatedDocument(locale, getContext());
            if (translatedDocument == document) {
                translatedDocument = new XWikiDocument(document.getDocumentReference());
                translatedDocument.setDefaultLocale(document.getDefaultLocale());
                translatedDocument.setLocale(locale);
                translatedDocument.setSyntax(document.getSyntax());
            }
            document = translatedDocument;
        }

        document.setOriginalDocument(document.clone());

        StringBuilder builder = new StringBuilder(document.getContent());

        builder.append('\n');
        builder.append(key);
        builder.append('=');
        builder.append(message);

        document.setContent(builder.toString());

        boolean isNew = document.isNew();

        this.mockXWiki.saveDocument(document, "", getContext());

        if (isNew) {
            observation.notify(new DocumentCreatedEvent(document.getDocumentReference()), document, getContext());
        } else {
            observation.notify(new DocumentUpdatedEvent(document.getDocumentReference()), document, getContext());
        }

        setBundles(document.getFullName());
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
    public void getWikiTranslation() throws XWikiException
    {
        addWikiTranslation("wiki.translation", "Wiki translation", Locale.ROOT);

        Assert.assertEquals("Wiki translation", this.tool.get("wiki.translation"));
    }

    @Test
    public void getTranslatedWikiTranslation() throws XWikiException
    {
        addWikiTranslation("wiki.translation", "English translation", Locale.ENGLISH);
        addWikiTranslation("wiki.translation", "French translation", Locale.FRENCH);

        getContext().setLocale(Locale.FRENCH);

        Assert.assertEquals("French translation", this.tool.get("wiki.translation"));

        getContext().setLocale(Locale.ENGLISH);

        Assert.assertEquals("English translation", this.tool.get("wiki.translation"));
    }

    @Test
    public void fallackOnParentLocale() throws XWikiException
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        getContext().setLocale(Locale.ENGLISH);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        getContext().setLocale(Locale.US);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        getContext().setLocale(Locale.FRENCH);
        addWikiTranslation("wiki.frenchtranslation", "French translation", Locale.FRENCH);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));

        getContext().setLocale(Locale.FRANCE);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("French translation", this.tool.get("wiki.frenchtranslation"));
    }

    @Test
    public void updateWikiTranslationCache() throws XWikiException
    {
        setBundles(this.defaultWikiTranslation.getFullName());

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        addWikiTranslation("wiki.anothertranslation", "Another translation", Locale.ROOT);

        Assert.assertEquals("Another translation", this.tool.get("wiki.anothertranslation"));

        this.mockXWiki.deleteDocument(this.defaultWikiTranslation, getContext());

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
        Assert.assertEquals("wiki.anothertranslation", this.tool.get("wiki.anothertranslation"));
    }

    @Test
    public void updateWikiPreferencesCache() throws XWikiException
    {
        addWikiTranslation("wiki.defaulttranslation", "Default translation", Locale.ROOT);

        Assert.assertEquals("Default translation", this.tool.get("wiki.defaulttranslation"));

        XWikiDocument otherWikiTranslation =
            new XWikiDocument(new DocumentReference(getContext().getDatabase(), "XWiki", "OtherTranslations"));
        otherWikiTranslation.setSyntax(Syntax.PLAIN_1_0);
        otherWikiTranslation.setContent("wiki.othertranslation=Other translation");
        this.mockXWiki.saveDocument(otherWikiTranslation, "", getContext());

        setBundles(" " + otherWikiTranslation.getFullName());

        Assert.assertEquals("Other translation", this.tool.get("wiki.othertranslation"));

        Assert.assertEquals("wiki.defaulttranslation", this.tool.get("wiki.defaulttranslation"));
    }

    @Test
    public void getTranslationWithParameters() throws XWikiException
    {
        addWikiTranslation("wiki.translation", "{1} {0}", Locale.ROOT);

        Assert.assertEquals("Parameter translation", this.tool.get("wiki.translation", "translation", "Parameter"));
    }

    @Test
    public void getEmptyWikiTranslation() throws XWikiException
    {
        addWikiTranslation("wiki.translation", "", Locale.ROOT);

        Assert.assertEquals("", this.tool.get("wiki.translation"));
    }

    @Test
    public void fallbackOnResource() throws XWikiException
    {
        Assert.assertEquals("Language", this.tool.get("language"));

        this.defaultWikiTranslation.setDefaultLocale(Locale.FRENCH);

        addWikiTranslation("language", "Overwritten language", Locale.ROOT);

        // ROOT language has been overwritten
        getContext().setLocale(Locale.ROOT);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));
        
        // The real locale of ROOT version in FRENCH so it's overwritten too
        getContext().setLocale(Locale.FRENCH);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));

        // GERMAN hasn't been overwritten
        getContext().setLocale(Locale.GERMAN);
        Assert.assertEquals("Sprache", this.tool.get("language"));

        // There is no ENGLISH translation for this key so it fallback on ROOT
        getContext().setLocale(Locale.ENGLISH);
        Assert.assertEquals("Overwritten language", this.tool.get("language"));
    }
}
