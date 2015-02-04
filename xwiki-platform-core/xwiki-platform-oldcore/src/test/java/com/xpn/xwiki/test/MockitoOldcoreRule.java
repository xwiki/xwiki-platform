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
package com.xpn.xwiki.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Test rule to initialize and manipulate various oldcore APIs.
 * 
 * @version $Id$
 * @since 5.2M1
 */
public class MockitoOldcoreRule implements MethodRule
{
    public static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    public static final LocalDocumentReference GROUP_CLASS = new LocalDocumentReference("XWiki", "XWikiGroups");

    private final MethodRule parent;

    private final MockitoComponentManagerRule componentManager;

    private XWikiContext context;

    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private XWikiRightService mockRightService;

    private AuthorizationManager mockAuthorizationManager;

    private ContextualAuthorizationManager mockContextualAuthorizationManager;

    protected Map<DocumentReference, XWikiDocument> documents =
        new ConcurrentHashMap<DocumentReference, XWikiDocument>();

    private boolean notifyDocumentCreatedEvent;

    private boolean notifyDocumentUpdatedEvent;

    private boolean notifyDocumentDeletedEvent;

    public MockitoOldcoreRule()
    {
        this(new MockitoComponentManagerRule());
    }

    public MockitoOldcoreRule(MockitoComponentManagerRule componentManager)
    {
        this(componentManager, componentManager);
    }

    public MockitoOldcoreRule(MockitoComponentManagerRule componentManager, MethodRule parent)
    {
        this.componentManager = componentManager;
        this.parent = parent;
    }

    public MockitoComponentManagerRule getMocker()
    {
        return this.componentManager;
    }

    public void notifyDocumentCreatedEvent(boolean notifyDocumentCreatedEvent)
    {
        this.notifyDocumentCreatedEvent = notifyDocumentCreatedEvent;
    }

    public void notifyDocumentUpdatedEvent(boolean notifyDocumentUpdatedEvent)
    {
        this.notifyDocumentUpdatedEvent = notifyDocumentUpdatedEvent;
    }

    public void notifyDocumentDeletedEvent(boolean notifyDocumentDeletedEvent)
    {
        this.notifyDocumentDeletedEvent = notifyDocumentDeletedEvent;
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target)
    {
        final Statement statement = new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };

        return this.parent != null ? this.parent.apply(statement, method, target) : statement;
    }

    protected void before() throws Exception
    {
        final MockUtil mockUtil = new MockUtil();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(this.componentManager);

        this.context = new XWikiContext();

        this.context.setWikiId("xwiki");
        this.context.setMainXWiki("xwiki");

        this.mockAuthorizationManager = getMocker().registerMockComponent(AuthorizationManager.class);
        this.mockContextualAuthorizationManager =
            getMocker().registerMockComponent(ContextualAuthorizationManager.class);

        this.mockXWiki = mock(XWiki.class);
        getXWikiContext().setWiki(this.mockXWiki);

        this.mockStore = mock(XWikiStoreInterface.class);
        this.mockRightService = mock(XWikiRightService.class);

        when(mockXWiki.getStore()).thenReturn(mockStore);
        when(mockXWiki.getRightService()).thenReturn(mockRightService);

        // We need to initialize the Component Manager so that the components can be looked up
        getXWikiContext().put(ComponentManager.class.getName(), this.componentManager);

        // Make sure a default ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class)) {
            getMocker().registerMemoryConfigurationSource();
        }

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        if (this.componentManager.hasComponent(Environment.class)) {
            ServletEnvironment environment = (ServletEnvironment) this.componentManager.getInstance(Environment.class);

            ServletContext servletContextMock = mock(ServletContext.class);
            environment.setServletContext(servletContextMock);
            when(servletContextMock.getAttribute("javax.servlet.context.tempdir")).thenReturn(
                new File(System.getProperty("java.io.tmpdir")));
        }

        // Initialize the Execution Context
        if (this.componentManager.hasComponent(ExecutionContextManager.class)) {
            ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
            ExecutionContext ec = new ExecutionContext();
            ecm.initialize(ec);
        }

        // Bridge with old XWiki Context, required for old code.
        Execution execution;
        if (this.componentManager.hasComponent(Execution.class)) {
            execution = this.componentManager.getInstance(Execution.class);
        } else {
            execution = this.componentManager.registerMockComponent(Execution.class);
        }
        ExecutionContext econtext;
        if (mockUtil.isMock(execution)) {
            econtext = new ExecutionContext();
            when(execution.getContext()).thenReturn(econtext);
        } else {
            econtext = execution.getContext();
        }
        econtext.setProperty("xwikicontext", this.context);

        // Initialize XWikiContext provider
        if (!this.componentManager.hasComponent(XWikiContext.TYPE_PROVIDER)) {
            Provider<XWikiContext> xcontextProvider = this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
            when(xcontextProvider.get()).thenReturn(this.context);
        } else {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            if (mockUtil.isMock(xcontextProvider)) {
                when(xcontextProvider.get()).thenReturn(this.context);
            }
        }

        // Initialize stub context provider
        if (this.componentManager.hasComponent(XWikiStubContextProvider.class)) {
            XWikiStubContextProvider stubContextProvider =
                this.componentManager.getInstance(XWikiStubContextProvider.class);
            if (!mockUtil.isMock(stubContextProvider)) {
                stubContextProvider.initialize(this.context);
            }
        }

        if (!this.componentManager.hasComponent(CoreConfiguration.class)) {
            CoreConfiguration coreConfigurationMock =
                this.componentManager.registerMockComponent(CoreConfiguration.class);
            when(coreConfigurationMock.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_1_0);
        } else {
            CoreConfiguration coreConfiguration = this.componentManager.registerMockComponent(CoreConfiguration.class);
            if (!mockUtil.isMock(coreConfiguration)) {
                when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_1_0);
            }
        }

        // XWiki

        when(getMockXWiki().getDocument(any(DocumentReference.class), any(XWikiContext.class))).then(
            new Answer<XWikiDocument>()
            {
                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    DocumentReference target = (DocumentReference) invocation.getArguments()[0];

                    if (target.getLocale() == null) {
                        target = new DocumentReference(target, Locale.ROOT);
                    }

                    XWikiDocument document = documents.get(target);

                    if (document == null) {
                        document = new XWikiDocument(target, target.getLocale());
                        document.setSyntax(Syntax.PLAIN_1_0);
                        document.setOriginalDocument(document.clone());
                    }

                    return document;
                }
            });
        when(getMockXWiki().exists(any(DocumentReference.class), any(XWikiContext.class))).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                DocumentReference target = (DocumentReference) invocation.getArguments()[0];

                if (target.getLocale() == null) {
                    target = new DocumentReference(target, Locale.ROOT);
                }

                return documents.containsKey(target);
            }
        });
        Mockito
            .doAnswer(new Answer<Void>()
            {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];
                    String comment = (String) invocation.getArguments()[1];
                    boolean minorEdit = (Boolean) invocation.getArguments()[2];

                    boolean isNew = document.isNew();

                    document.setComment(StringUtils.defaultString(comment));
                    document.setMinorEdit(minorEdit);

                    if (document.isContentDirty() || document.isMetaDataDirty()) {
                        document.setDate(new Date());
                        if (document.isContentDirty()) {
                            document.setContentUpdateDate(new Date());
                            document.setContentAuthorReference(document.getAuthorReference());
                        }
                        document.incrementVersion();

                        document.setContentDirty(false);
                        document.setMetaDataDirty(false);
                    }
                    document.setNew(false);
                    document.setStore(getMockStore());

                    XWikiDocument previousDocument = documents.get(document.getDocumentReferenceWithLocale());

                    if (previousDocument != document) {
                        for (XWikiAttachment attachment : document.getAttachmentList()) {
                            if (!attachment.isContentDirty()) {
                                attachment.setAttachment_content(previousDocument.getAttachment(
                                    attachment.getFilename()).getAttachment_content());
                            }
                        }
                    }

                    XWikiDocument originalDocument = document.getOriginalDocument();
                    if (originalDocument == null) {
                        originalDocument = mockXWiki.getDocument(document.getDocumentReferenceWithLocale(), context);
                        document.setOriginalDocument(originalDocument);
                    }

                    documents.put(document.getDocumentReferenceWithLocale(), document.clone());

                    if (isNew) {
                        if (notifyDocumentCreatedEvent) {
                            getObservationManager().notify(new DocumentCreatedEvent(document.getDocumentReference()),
                                document, getXWikiContext());
                        }
                    } else {
                        if (notifyDocumentUpdatedEvent) {
                            getObservationManager().notify(new DocumentUpdatedEvent(document.getDocumentReference()),
                                document, getXWikiContext());
                        }
                    }

                    document.setOriginalDocument(document.clone());

                    return null;
                }
            }).when(getMockXWiki())
            .saveDocument(any(XWikiDocument.class), any(String.class), anyBoolean(), any(XWikiContext.class));
        Mockito.doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                getMockXWiki().saveDocument((XWikiDocument) invocation.getArguments()[0],
                    (String) invocation.getArguments()[1], false, (XWikiContext) invocation.getArguments()[2]);

                return null;
            }
        }).when(getMockXWiki()).saveDocument(any(XWikiDocument.class), any(String.class), any(XWikiContext.class));
        Mockito.doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                getMockXWiki().saveDocument((XWikiDocument) invocation.getArguments()[0], "", false,
                    (XWikiContext) invocation.getArguments()[1]);

                return null;
            }
        }).when(getMockXWiki()).saveDocument(any(XWikiDocument.class), any(XWikiContext.class));
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                documents.remove(document.getDocumentReferenceWithLocale());

                if (notifyDocumentDeletedEvent) {
                    getObservationManager().notify(new DocumentDeletedEvent(document.getDocumentReference()), document,
                        getXWikiContext());
                }

                return null;
            }
        }).when(getMockXWiki()).deleteDocument(any(XWikiDocument.class), any(Boolean.class), any(XWikiContext.class));
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                mockXWiki.deleteDocument(document, true, context);

                return null;
            }
        }).when(getMockXWiki()).deleteDocument(any(XWikiDocument.class), any(XWikiContext.class));
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                DocumentReference reference = document.getDocumentReference();

                List<Locale> locales = document.getTranslationLocales(context);

                for (Locale locale : locales) {
                    XWikiDocument translation =
                        mockXWiki.getDocument(new DocumentReference(reference, locale), context);
                    mockXWiki.deleteDocument(translation, context);
                }

                mockXWiki.deleteDocument(document, context);

                return null;
            }
        }).when(getMockXWiki()).deleteAllDocuments(any(XWikiDocument.class), any(Boolean.class),
            any(XWikiContext.class));
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                mockXWiki.deleteAllDocuments(document, true, context);

                return null;
            }
        }).when(getMockXWiki()).deleteAllDocuments(any(XWikiDocument.class), any(XWikiContext.class));
        when(getMockXWiki().getXClass(any(DocumentReference.class), any(XWikiContext.class))).then(
            new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    return getMockXWiki().getDocument((DocumentReference) invocation.getArguments()[0],
                        (XWikiContext) invocation.getArguments()[1]).getXClass();
                }
            });
        when(getMockXWiki().getLanguagePreference(any(XWikiContext.class))).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return getXWikiContext().getLanguage();
            }
        });

        // XWikiStoreInterface

        Mockito.when(getMockStore().getTranslationList(any(XWikiDocument.class), any(XWikiContext.class))).then(
            new Answer<List<String>>()
            {
                @Override
                public List<String> answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.getArguments()[0];

                    List<String> translations = new ArrayList<String>();

                    for (XWikiDocument storedDocument : documents.values()) {
                        Locale storedLocale = storedDocument.getLocale();
                        if (!storedLocale.equals(Locale.ROOT)
                            && storedDocument.getDocumentReference().equals(document.getDocumentReference())) {
                            translations.add(storedLocale.toString());
                        }
                    }

                    return translations;
                }
            });
        when(getMockStore().loadXWikiDoc(any(XWikiDocument.class), any(XWikiContext.class))).then(
            new Answer<XWikiDocument>()
            {
                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    return getMockXWiki().getDocument((XWikiDocument) invocation.getArguments()[0],
                        (XWikiContext) invocation.getArguments()[1]);
                }
            });

        // Users

        Mockito.when(getMockXWiki().getUserClass(any(XWikiContext.class))).then(new Answer<BaseClass>()
        {
            @Override
            public BaseClass answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                XWikiDocument userDocument =
                    getMockXWiki().getDocument(
                        new DocumentReference(USER_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                final BaseClass userClass = userDocument.getXClass();

                if (userDocument.isNew()) {
                    userClass.addTextField("first_name", "First Name", 30);
                    userClass.addTextField("last_name", "Last Name", 30);
                    userClass.addEmailField("email", "e-Mail", 30);
                    userClass.addPasswordField("password", "Password", 10);
                    userClass.addBooleanField("active", "Active", "active");
                    userClass.addTextAreaField("comment", "Comment", 40, 5);
                    userClass.addTextField("avatar", "Avatar", 30);
                    userClass.addTextField("phone", "Phone", 30);
                    userClass.addTextAreaField("address", "Address", 40, 3);

                    getMockXWiki().saveDocument(userDocument, xcontext);
                }

                return userClass;
            }
        });
        Mockito.when(getMockXWiki().getGroupClass(any(XWikiContext.class))).then(new Answer<BaseClass>()
        {
            @Override
            public BaseClass answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiContext xcontext = (XWikiContext) invocation.getArguments()[0];

                XWikiDocument groupDocument =
                    getMockXWiki().getDocument(
                        new DocumentReference(GROUP_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                final BaseClass groupClass = groupDocument.getXClass();

                if (groupDocument.isNew()) {
                    groupClass.addTextField("member", "Member", 30);

                    getMockXWiki().saveDocument(groupDocument, xcontext);
                }

                return groupClass;
            }
        });
    }

    protected void after() throws Exception
    {
        Utils.setComponentManager(null);

        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.removeContext();
    }

    public XWikiContext getXWikiContext()
    {
        return this.context;
    }

    public XWiki getMockXWiki()
    {
        return this.mockXWiki;
    }

    public XWikiRightService getMockRightService()
    {
        return this.mockRightService;
    }

    public AuthorizationManager getMockAuthorizationManager()
    {
        return this.mockAuthorizationManager;
    }

    public ContextualAuthorizationManager getMockContextualAuthorizationManager()
    {
        return this.mockContextualAuthorizationManager;
    }

    public XWikiStoreInterface getMockStore()
    {
        return this.mockStore;
    }

    /**
     * @since 6.0RC1
     */
    public ExecutionContext getExecutionContext() throws ComponentLookupException
    {
        return this.componentManager.<Execution>getInstance(Execution.class).getContext();
    }

    /**
     * @since 6.1M2
     */
    public Map<DocumentReference, XWikiDocument> getDocuments()
    {
        return this.documents;
    }

    /**
     * @since 6.1M2
     */
    public ObservationManager getObservationManager() throws ComponentLookupException
    {
        return getMocker().getInstance(ObservationManager.class);
    }
}
