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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;
import org.xwiki.localization.wiki.internal.TranslationDocumentModel.Scope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

public class DocumentTranslationBundleFactoryTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private QueryManager mockQueryManager;

    private Query mockQuery;

    private AuthorizationManager mockAuthorizationManager;

    private Map<DocumentReference, Map<Locale, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<Locale, XWikiDocument>>();

    private ObservationManager observation;

    private LocalizationManager localization;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");

        this.mockStore = getMockery().mock(XWikiStoreInterface.class);

        this.mockQuery = getMockery().mock(Query.class);

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
                            document.setSyntax(Syntax.PLAIN_1_0);
                            document.setOriginalDocument(document.clone());
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
                            document.setStore(mockStore);
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

                        boolean isNew = document.isNew();

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

                        if (isNew) {
                            observation.notify(new DocumentCreatedEvent(document.getDocumentReference()), document,
                                getContext());
                        } else {
                            observation.notify(new DocumentUpdatedEvent(document.getDocumentReference()), document,
                                getContext());
                        }

                        document.setOriginalDocument(document.clone());

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

                        return null;
                    }
                });

                allowing(mockXWiki).isVirtualMode();
                will(returnValue(true));

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

                allowing(mockXWiki).getVirtualWikisDatabaseNames(with(any(XWikiContext.class)));
                will(returnValue(Collections.EMPTY_LIST));

                allowing(mockQueryManager).createQuery(with(any(String.class)), with(any(String.class)));
                will(returnValue(mockQuery));

                allowing(mockQuery).setWiki(with(any(String.class)));

                allowing(mockQuery).execute();
                will(returnValue(Collections.EMPTY_LIST));

                allowing(mockAuthorizationManager).checkAccess(with(any(Right.class)),
                    (DocumentReference) with(anything()), with(any(EntityReference.class)));
            }
        });

        this.observation = getComponentManager().getInstance(ObservationManager.class);

        // Initialiaze document bundle factory
        getComponentManager().getInstance(TranslationBundleFactory.class, DocumentTranslationBundleFactory.ID);

        this.localization = getComponentManager().getInstance(LocalizationManager.class);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.mockQueryManager = registerMockComponent(QueryManager.class);
        this.mockAuthorizationManager = registerMockComponent(AuthorizationManager.class);
    }

    private void addTranslation(String key, String message, DocumentReference reference, Locale locale, Scope scope)
        throws XWikiException
    {
        XWikiDocument document = this.mockXWiki.getDocument(reference, getContext());

        if (document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE) == null) {
            BaseObject translationObject = new BaseObject();
            translationObject.setXClassReference(new DocumentReference(getContext().getDatabase(), "XWiki",
                "TranslationDocumentClass"));
            if (scope != null) {
                translationObject
                    .setStringValue(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE, scope.toString());
            }
            document.addXObject(translationObject);

            if (!locale.equals(Locale.ROOT)) {
                this.mockXWiki.saveDocument(document, "", getContext());
            }
        }

        if (!locale.equals(Locale.ROOT)) {
            XWikiDocument tdocument = document.getTranslatedDocument(locale, getContext());
            if (tdocument == document) {
                tdocument = new XWikiDocument(document.getDocumentReference());
                tdocument.setLocale(locale);
                tdocument.setTranslation(1);
                tdocument.setStore(mockStore);
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

        this.mockXWiki.saveDocument(document, "", getContext());
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

    // tests

    @Test
    public void getTranslationScopeWiki() throws XWikiException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        addTranslation("wiki.translation", "Wiki translation", new DocumentReference(getContext().getDatabase(),
            "space", "translation"), Locale.ROOT, Scope.WIKI);

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }

    @Test
    public void getTranslationScopeONDemand() throws XWikiException, TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException
    {
        assertTranslation("wiki.translation", null, Locale.ROOT);

        DocumentReference translationDocument =
            new DocumentReference(getContext().getDatabase(), "space", "translation");

        addTranslation("wiki.translation", "Wiki translation", translationDocument, Locale.ROOT, Scope.ON_DEMAND);

        this.localization.use(DocumentTranslationBundleFactory.ID, translationDocument.toString());

        assertTranslation("wiki.translation", "Wiki translation", Locale.ROOT);
    }
}
