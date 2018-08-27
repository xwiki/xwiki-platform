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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiDocument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test setup to initialize and manipulate various oldcore APIs.
 * 
 * @version $Id$
 * @since 10.2
 */
public class MockitoOldcore
{
    public static final LocalDocumentReference USER_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    public static final LocalDocumentReference GROUP_CLASS = new LocalDocumentReference("XWiki", "XWikiGroups");

    private final MockitoComponentManager componentManager;

    private XWikiContext context;

    private XWiki spyXWiki;

    protected File permanentDirectory;

    protected File temporaryDirectory;

    private XWikiHibernateStore mockHibernateStore;

    private XWikiVersioningStoreInterface mockVersioningStore;

    private XWikiRightService mockRightService;

    private XWikiGroupService mockGroupService;

    private AuthorizationManager mockAuthorizationManager;

    private ContextualAuthorizationManager mockContextualAuthorizationManager;

    private QueryManager queryManager;

    private WikiDescriptorManager wikiDescriptorManager;

    protected Map<DocumentReference, XWikiDocument> documents = new ConcurrentHashMap<>();

    private boolean notifyDocumentCreatedEvent;

    private boolean notifyDocumentUpdatedEvent;

    private boolean notifyDocumentDeletedEvent;

    private MemoryConfigurationSource configurationSource;

    private MemoryConfigurationSource xwikicfgConfigurationSource;

    private MemoryConfigurationSource wikiConfigurationSource;

    private MemoryConfigurationSource spaceConfigurationSource;

    private ScriptContext scriptContext;

    private Environment environment;

    public MockitoOldcore(MockitoComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public MockitoComponentManager getMocker()
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

    /**
     * Enabled notification of component descriptor registration/unregistration.
     * 
     * @throws ComponentLookupException when failing to lookup {@link ObservationManager} component
     */
    public void notifyComponentDescriptorEvent() throws ComponentLookupException
    {
        getMocker().notifyComponentDescriptorEvent();
    }

    public void before(Class<?> testClass) throws Exception
    {
        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getMocker());

        this.context = new XWikiContext();

        getXWikiContext().setWikiId("xwiki");
        getXWikiContext().setMainXWiki("xwiki");

        this.spyXWiki = spy(new XWiki());
        getXWikiContext().setWiki(this.spyXWiki);

        this.mockHibernateStore = mock(XWikiHibernateStore.class);
        this.mockVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockRightService = mock(XWikiRightService.class);
        this.mockGroupService = mock(XWikiGroupService.class);

        doReturn(this.mockHibernateStore).when(this.spyXWiki).getStore();
        doReturn(this.mockHibernateStore).when(this.spyXWiki).getHibernateStore();
        doReturn(this.mockVersioningStore).when(this.spyXWiki).getVersioningStore();
        doReturn(this.mockRightService).when(this.spyXWiki).getRightService();
        doReturn(this.mockGroupService).when(this.spyXWiki).getGroupService(getXWikiContext());

        // We need to initialize the Component Manager so that the components can be looked up
        getXWikiContext().put(ComponentManager.class.getName(), getMocker());

        if (testClass.getAnnotation(AllComponents.class) != null) {
            // If @AllComponents is enabled force mocking AuthorizationManager and ContextualAuthorizationManager if not
            // already mocked
            this.mockAuthorizationManager = getMocker().registerMockComponent(AuthorizationManager.class, false);
            this.mockContextualAuthorizationManager =
                getMocker().registerMockComponent(ContextualAuthorizationManager.class, false);
        } else {
            // Make sure an AuthorizationManager and a ContextualAuthorizationManager is available
            if (!getMocker().hasComponent(AuthorizationManager.class)) {
                this.mockAuthorizationManager = getMocker().registerMockComponent(AuthorizationManager.class);
            }
            if (!getMocker().hasComponent(ContextualAuthorizationManager.class)) {
                this.mockContextualAuthorizationManager =
                    getMocker().registerMockComponent(ContextualAuthorizationManager.class);
            }
        }

        // Make sure a default ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class)) {
            this.configurationSource = getMocker().registerMemoryConfigurationSource();
        }

        // Make sure a "xwikicfg" ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT)) {
            this.xwikicfgConfigurationSource = new MockConfigurationSource();
            getMocker().registerComponent(MockConfigurationSource.getDescriptor(XWikiCfgConfigurationSource.ROLEHINT),
                this.xwikicfgConfigurationSource);
        }
        // Make sure a "wiki" ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class, "wiki")) {
            this.wikiConfigurationSource = new MockConfigurationSource();
            getMocker().registerComponent(MockConfigurationSource.getDescriptor("wiki"), this.wikiConfigurationSource);
        }

        // Make sure a "space" ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class, "space")) {
            this.spaceConfigurationSource = new MockConfigurationSource();
            getMocker().registerComponent(MockConfigurationSource.getDescriptor("space"),
                this.spaceConfigurationSource);
        }

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        if (getMocker().hasComponent(Environment.class)
            && getMocker().getInstance(Environment.class) instanceof ServletEnvironment) {
            ServletEnvironment servletEnvironment = getMocker().getInstance(Environment.class);

            ServletContext servletContextMock = mock(ServletContext.class);
            servletEnvironment.setServletContext(servletContextMock);
            when(servletContextMock.getAttribute("javax.servlet.context.tempdir"))
                .thenReturn(new File(System.getProperty("java.io.tmpdir")));

            initEnvironmentDirectories();

            servletEnvironment.setTemporaryDirectory(this.temporaryDirectory);
            servletEnvironment.setPermanentDirectory(this.permanentDirectory);

            this.environment = servletEnvironment;
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
        if (MockUtil.isMock(execution)) {
            econtext = new ExecutionContext();
            when(execution.getContext()).thenReturn(econtext);
        } else {
            econtext = execution.getContext();
        }

        // Set a few standard things in the ExecutionContext
        econtext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.context);
        this.scriptContext = (ScriptContext) econtext.getProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID);
        if (this.scriptContext == null) {
            this.scriptContext = new SimpleScriptContext();
            econtext.setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID, this.scriptContext);
        }

        if (!this.componentManager.hasComponent(ScriptContextManager.class)) {
            ScriptContextManager scriptContextManager =
                this.componentManager.registerMockComponent(ScriptContextManager.class);
            when(scriptContextManager.getCurrentScriptContext()).thenReturn(this.scriptContext);
            when(scriptContextManager.getScriptContext()).thenReturn(this.scriptContext);
        }

        // Initialize XWikiContext provider
        if (!this.componentManager.hasComponent(XWikiContext.TYPE_PROVIDER)) {
            Provider<XWikiContext> xcontextProvider =
                this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
            when(xcontextProvider.get()).thenReturn(this.context);
        } else {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            if (MockUtil.isMock(xcontextProvider)) {
                when(xcontextProvider.get()).thenReturn(this.context);
            }
        }

        // Initialize readonly XWikiContext provider
        if (!this.componentManager.hasComponent(XWikiContext.TYPE_PROVIDER, "readonly")) {
            Provider<XWikiContext> xcontextProvider =
                this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER, "readonly");
            when(xcontextProvider.get()).thenReturn(this.context);
        } else {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            if (MockUtil.isMock(xcontextProvider)) {
                when(xcontextProvider.get()).thenReturn(this.context);
            }
        }

        // Initialize stub context provider
        if (this.componentManager.hasComponent(XWikiStubContextProvider.class)) {
            XWikiStubContextProvider stubContextProvider =
                this.componentManager.getInstance(XWikiStubContextProvider.class);
            if (!MockUtil.isMock(stubContextProvider)) {
                stubContextProvider.initialize(this.context);
            }
        }

        // Make sure to have a mocked CoreConfiguration (even if one already exist)
        if (!this.componentManager.hasComponent(CoreConfiguration.class)) {
            CoreConfiguration coreConfigurationMock =
                this.componentManager.registerMockComponent(CoreConfiguration.class);
            when(coreConfigurationMock.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_2_1);
        } else {
            CoreConfiguration coreConfiguration =
                this.componentManager.registerMockComponent(CoreConfiguration.class, false);
            if (MockUtil.isMock(coreConfiguration)) {
                when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_2_1);
            }
        }

        // Set a context ComponentManager if none exist
        if (!this.componentManager.hasComponent(ComponentManager.class, "context")) {
            DefaultComponentDescriptor<ComponentManager> componentManagerDescriptor =
                new DefaultComponentDescriptor<>();
            componentManagerDescriptor.setRoleHint("context");
            componentManagerDescriptor.setRoleType(ComponentManager.class);
            this.componentManager.registerComponent(componentManagerDescriptor, this.componentManager);
        }

        // XWiki

        doAnswer(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument doc = invocation.getArgument(0);
                String revision = invocation.getArgument(1);

                if (StringUtils.equals(revision, doc.getVersion())) {
                    return doc;
                }

                // TODO: implement version store mocking
                return new XWikiDocument(doc.getDocumentReference());
            }
        }).when(getSpyXWiki()).getDocument(anyXWikiDocument(), any(), anyXWikiContext());
        doAnswer(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                DocumentReference target = invocation.getArgument(0);

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
        }).when(getSpyXWiki()).getDocument(any(DocumentReference.class), anyXWikiContext());
        doAnswer(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument target = invocation.getArgument(0);

                return getSpyXWiki().getDocument(target.getDocumentReferenceWithLocale(), invocation.getArgument(1));
            }
        }).when(getSpyXWiki()).getDocument(anyXWikiDocument(), any(XWikiContext.class));
        doAnswer(new Answer<Boolean>()
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
        }).when(getSpyXWiki()).exists(any(DocumentReference.class), anyXWikiContext());
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = invocation.getArgument(0);
                String comment = invocation.getArgument(1);
                boolean minorEdit = invocation.getArgument(2);

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

                if (previousDocument != null && previousDocument != document) {
                    for (XWikiAttachment attachment : document.getAttachmentList()) {
                        if (!attachment.isContentDirty()) {
                            attachment.setAttachment_content(
                                previousDocument.getAttachment(attachment.getFilename()).getAttachment_content());
                        }
                    }
                }

                XWikiDocument originalDocument = document.getOriginalDocument();
                if (originalDocument == null) {
                    originalDocument = spyXWiki.getDocument(document.getDocumentReferenceWithLocale(), context);
                    document.setOriginalDocument(originalDocument);
                }

                XWikiDocument savedDocument = document.clone();

                documents.put(document.getDocumentReferenceWithLocale(), savedDocument);

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

                // Set the document as it's original document
                savedDocument.setOriginalDocument(savedDocument.clone());

                return null;
            }
        }).when(getSpyXWiki()).saveDocument(anyXWikiDocument(), any(String.class), anyBoolean(), anyXWikiContext());
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = invocation.getArgument(0);

                documents.remove(document.getDocumentReferenceWithLocale());

                if (notifyDocumentDeletedEvent) {
                    getObservationManager().notify(new DocumentDeletedEvent(document.getDocumentReference()), document,
                        getXWikiContext());
                }

                return null;
            }
        }).when(getSpyXWiki()).deleteDocument(anyXWikiDocument(), any(Boolean.class), anyXWikiContext());
        doAnswer(new Answer<BaseClass>()
        {
            @Override
            public BaseClass answer(InvocationOnMock invocation) throws Throwable
            {
                return getSpyXWiki()
                    .getDocument((DocumentReference) invocation.getArguments()[0], invocation.getArgument(1))
                    .getXClass();
            }
        }).when(getSpyXWiki()).getXClass(any(DocumentReference.class), anyXWikiContext());
        doAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return getXWikiContext().getLanguage();
            }
        }).when(getSpyXWiki()).getLanguagePreference(anyXWikiContext());

        getXWikiContext().setLocale(Locale.ENGLISH);

        // XWikiStoreInterface

        when(getMockStore().getTranslationList(anyXWikiDocument(), anyXWikiContext())).then(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = invocation.getArgument(0);

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
        when(getMockStore().loadXWikiDoc(anyXWikiDocument(), anyXWikiContext())).then(new Answer<XWikiDocument>()
        {
            @Override
            public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
            {
                // The store is based on the contex for the wiki
                DocumentReference reference = invocation.<XWikiDocument>getArgument(0).getDocumentReference();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                return getSpyXWiki().getDocument(reference, xcontext);
            }
        });
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                // The store is based on the contex for the wiki
                DocumentReference reference = invocation.<XWikiDocument>getArgument(0).getDocumentReferenceWithLocale();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                documents.remove(reference);

                return null;
            }
        }).when(getMockStore()).deleteXWikiDoc(anyXWikiDocument(), anyXWikiContext());

        // Users

        doAnswer(new Answer<BaseClass>()
        {
            @Override
            public BaseClass answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiContext xcontext = invocation.getArgument(0);

                XWikiDocument userDocument = getSpyXWiki()
                    .getDocument(new DocumentReference(USER_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

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

                    getSpyXWiki().saveDocument(userDocument, xcontext);
                }

                return userClass;
            }
        }).when(getSpyXWiki()).getUserClass(anyXWikiContext());
        doAnswer(new Answer<BaseClass>()
        {
            @Override
            public BaseClass answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiContext xcontext = invocation.getArgument(0);

                XWikiDocument groupDocument = getSpyXWiki()
                    .getDocument(new DocumentReference(GROUP_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                final BaseClass groupClass = groupDocument.getXClass();

                if (groupDocument.isNew()) {
                    groupClass.addTextField("member", "Member", 30);

                    getSpyXWiki().saveDocument(groupDocument, xcontext);
                }

                return groupClass;
            }
        }).when(getSpyXWiki()).getGroupClass(anyXWikiContext());

        // Query Manager
        // If there's already a Query Manager registered, use it instead.
        // This allows, for example, using @ComponentList to use the real Query Manager, in integration tests.
        if (!this.componentManager.hasComponent(QueryManager.class)) {
            mockQueryManager();
        }
        when(getMockStore().getQueryManager()).then(new Answer<QueryManager>()
        {

            @Override
            public QueryManager answer(InvocationOnMock invocation) throws Throwable
            {
                return getQueryManager();
            }
        });

        // WikiDescriptorManager
        // If there's already a WikiDescriptorManager registered, use it instead.
        // This allows, for example, using @ComponentList to use the real WikiDescriptorManager, in integration tests.
        if (!this.componentManager.hasComponent(WikiDescriptorManager.class)) {
            this.wikiDescriptorManager = getMocker().registerMockComponent(WikiDescriptorManager.class);
            when(this.wikiDescriptorManager.getMainWikiId()).then(new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {
                    return getXWikiContext().getMainXWiki();
                }
            });
            when(this.wikiDescriptorManager.getCurrentWikiId()).then(new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {
                    return getXWikiContext().getWikiId();
                }
            });
        }
    }

    protected DocumentReference resolveDocument(String documentName) throws ComponentLookupException
    {
        DocumentReferenceResolver<String> resolver =
            getMocker().getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        return resolver.resolve(documentName);
    }

    public void after() throws Exception
    {
        Utils.setComponentManager(null);

        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.removeContext();
    }

    public XWikiContext getXWikiContext()
    {
        return this.context;
    }

    /**
     * @since 7.3RC1
     */
    public XWiki getSpyXWiki()
    {
        return this.spyXWiki;
    }

    /**
     * @deprecated since 7.3RC1, use {@link #getSpyXWiki()} instead
     */
    @Deprecated
    public XWiki getMockXWiki()
    {
        return getSpyXWiki();
    }

    public File getPermanentDirectory()
    {
        return this.permanentDirectory;
    }

    public File getTemporaryDirectory()
    {
        return this.temporaryDirectory;
    }

    public XWikiRightService getMockRightService()
    {
        return this.mockRightService;
    }

    public XWikiGroupService getMockGroupService()
    {
        return this.mockGroupService;
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
        return this.mockHibernateStore;
    }

    /**
     * @since 7.2M2
     */
    public XWikiVersioningStoreInterface getMockVersioningStore()
    {
        return this.mockVersioningStore;
    }

    /**
     * @since 6.0RC1
     */
    public ExecutionContext getExecutionContext() throws ComponentLookupException
    {
        return this.componentManager.<Execution>getInstance(Execution.class).getContext();
    }

    /**
     * @since 8.3M1
     */
    public ScriptContext getScriptContext()
    {
        return this.scriptContext;
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

    /**
     * @since 7.0RC1
     */
    public QueryManager getQueryManager() throws ComponentLookupException
    {
        if (this.queryManager == null) {
            this.queryManager = this.componentManager.getInstance(QueryManager.class);
        }

        return this.queryManager;
    }

    /**
     * Force mocking query manager.
     * 
     * @return 7.2M1
     */
    public QueryManager mockQueryManager() throws Exception
    {
        this.queryManager = getMocker().registerMockComponent(QueryManager.class);

        return this.queryManager;
    }

    /**
     * @since 7.2M1
     */
    public WikiDescriptorManager getWikiDescriptorManager() throws ComponentLookupException
    {
        if (this.wikiDescriptorManager == null) {
            // Avoid initializing it if not needed
            if (this.componentManager.hasComponent(WikiDescriptorManager.class)) {
                this.wikiDescriptorManager = this.componentManager.getInstance(WikiDescriptorManager.class);
            }
        }

        return this.wikiDescriptorManager;
    }

    /**
     * @since 7.1M1
     */
    public MemoryConfigurationSource getConfigurationSource()
    {
        return this.configurationSource;
    }

    /**
     * @since 7.2M2
     */
    public MemoryConfigurationSource getMockXWikiCfg()
    {
        return this.xwikicfgConfigurationSource;
    }

    /**
     * @since 7.2RC1
     */
    public MemoryConfigurationSource getMockWikiConfigurationSource()
    {
        return this.wikiConfigurationSource;
    }

    private void initEnvironmentDirectories()
    {
        File testDirectory = new File("target/test-" + new Date().getTime()).getAbsoluteFile();

        this.temporaryDirectory = new File(testDirectory, "temporary");
        this.permanentDirectory = new File(testDirectory, "permanent-dir");
    }

    /**
     * @since 7.2M2
     */
    public void registerMockEnvironment() throws Exception
    {
        this.environment = getMocker().registerMockComponent(Environment.class);

        initEnvironmentDirectories();

        when(this.environment.getTemporaryDirectory()).thenReturn(this.temporaryDirectory);
        when(this.environment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
    }
}
