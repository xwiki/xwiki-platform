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
import java.io.InputStream;
import java.net.URL;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;
import javax.script.ScriptContext;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.cfg.Configuration;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.CacheControl;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.internal.CloneableSimpleScriptContext;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.XWikiTempDirUtil;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.URLConfiguration;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiDocument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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

    public static final LocalDocumentReference RIGHTS_CLASS = new LocalDocumentReference("XWiki", "XWikiRights");

    public static final LocalDocumentReference GLOBAL_RIGHTS_CLASS =
        new LocalDocumentReference("XWiki", "XWikiGlobalRights");

    private final MockitoComponentManager componentManager;

    private XWikiContext context;

    private XWiki spyXWiki;

    protected File permanentDirectory;

    protected File temporaryDirectory;

    private HibernateStore mockHibernateStore;

    private XWikiHibernateStore mockXWikiHibernateStore;

    private XWikiVersioningStoreInterface mockVersioningStore;

    private AttachmentVersioningStore mockAttachmentVersioningStore;

    private XWikiRightService mockRightService;

    private XWikiAuthService mockAuthService;

    private XWikiGroupService mockGroupService;

    private AuthorizationManager mockAuthorizationManager;

    private ContextualAuthorizationManager mockContextualAuthorizationManager;

    private QueryManager queryManager;

    private WikiDescriptorManager wikiDescriptorManager;

    protected Map<DocumentReference, XWikiDocument> documents = new ConcurrentHashMap<>();

    protected Map<DocumentReference, XWikiDocumentArchive> documentArchives = new ConcurrentHashMap<>();

    private boolean notifyDocumentCreatedEvent;

    private boolean notifyDocumentUpdatedEvent;

    private boolean notifyDocumentDeletedEvent;

    private MemoryConfigurationSource configurationSource;

    private MemoryConfigurationSource xwikicfgConfigurationSource;

    private MemoryConfigurationSource wikiConfigurationSource;

    private MemoryConfigurationSource spaceConfigurationSource;

    private ScriptContext scriptContext;

    private Environment environment;

    private XWikiEngineContext engineContext;

    private DocumentAccessBridge documentAccessBridge;

    private boolean mockXWiki = true;

    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    private UserPropertiesResolver allUserPropertiesResolver;

    /**
     * @version $Id$
     * @since 15.5RC1
     * @since 14.10.12
     */
    private class TestDocumentUserReference implements UserReference
    {
        private final DocumentReference documentReference;

        TestDocumentUserReference(DocumentReference documentReference)
        {
            this.documentReference = documentReference;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof TestDocumentUserReference) {
                DocumentReference otherDocument =
                    documentReferenceUserReferenceSerializer.serialize((UserReference) obj);

                return Objects.equals(this.documentReference, otherDocument);
            }

            return super.equals(obj);
        }

        @Override
        public String toString()
        {
            return "TestDocumentUserReference: " + Objects.toString(this.documentReference);
        }

        @Override
        public boolean isGlobal()
        {
            return false;
        }
    }

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

    public void mockXWiki(boolean mockXWiki)
    {
        this.mockXWiki = mockXWiki;
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

        this.mockHibernateStore = getMocker().registerMockComponent(HibernateStore.class);
        this.mockXWikiHibernateStore = mock(XWikiHibernateStore.class);
        getMocker().registerComponent(XWikiStoreInterface.class, XWikiHibernateBaseStore.HINT,
            this.mockXWikiHibernateStore);
        this.mockVersioningStore =
            getMocker().registerMockComponent(XWikiVersioningStoreInterface.class, XWikiHibernateBaseStore.HINT);
        this.mockAttachmentVersioningStore =
            getMocker().registerMockComponent(AttachmentVersioningStore.class, XWikiHibernateBaseStore.HINT);
        this.mockRightService = mock(XWikiRightService.class);
        this.mockGroupService = mock(XWikiGroupService.class);
        this.mockAuthService = mock(XWikiAuthService.class);

        doReturn(new Configuration()).when(this.mockHibernateStore).getConfiguration();

        this.spyXWiki.setStore(this.mockXWikiHibernateStore);
        this.spyXWiki.setVersioningStore(this.mockVersioningStore);
        this.spyXWiki.setDefaultAttachmentArchiveStore(this.mockAttachmentVersioningStore);
        this.spyXWiki.setRightService(this.mockRightService);
        this.spyXWiki.setAuthService(this.mockAuthService);
        this.spyXWiki.setGroupService(this.mockGroupService);
        this.spyXWiki.setPluginManager(new XWikiPluginManager());

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

        // Make sure to provide a EntityReferenceFactory
        if (!getMocker().hasComponent(EntityReferenceFactory.class)) {
            EntityReferenceFactory factory = getMocker().registerMockComponent(EntityReferenceFactory.class);
            when(factory.getReference(any())).thenAnswer((invocation) -> invocation.getArgument(0));
        }

        // Make sure a default ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class)) {
            this.configurationSource = getMocker().registerMemoryConfigurationSource();
        }

        // Make sure a "xwikicfg" ConfigurationSource is available
        if (!getMocker().hasComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT)) {
            registerMockXWikiCfg();
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

        // Make sure a CacheControl is available by default
        if (!getMocker().hasComponent(CacheControl.class)) {
            CacheControl cacheControl = getMocker().registerMockComponent(CacheControl.class);
            // Allow caching by default since it's the most common case
            when(cacheControl.isCacheReadAllowed((ChronoLocalDateTime) any())).thenReturn(true);
        }

        // Expose a XWikiStubContextProvider if none is exist
        if (!getMocker().hasComponent(XWikiStubContextProvider.class)) {
            XWikiStubContextProvider conetxtProvider =
                getMocker().registerMockComponent(XWikiStubContextProvider.class);
            when(conetxtProvider.createStubContext()).thenReturn(getXWikiContext());
        }

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        if (getMocker().hasComponent(Environment.class)) {
            this.environment = getMocker().getInstance(Environment.class);

            if (this.environment instanceof ServletEnvironment) {
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
        } else {
            // Automatically register an Environment when none is available since it's a very common need
            registerMockEnvironment();
        }

        // Provide a engineContext
        this.engineContext = this.spyXWiki.getEngineContext();
        if (this.engineContext == null) {
            this.engineContext = mock();
            this.spyXWiki.setEngineContext(this.engineContext);
            when(this.engineContext.getResource(any())).thenAnswer(new Answer<URL>()
            {
                @Override
                public URL answer(InvocationOnMock invocation) throws Throwable
                {
                    return environment.getResource(invocation.getArgument(0));
                }
            });
            when(this.engineContext.getResourceAsStream(any())).thenAnswer(new Answer<InputStream>()
            {
                @Override
                public InputStream answer(InvocationOnMock invocation) throws Throwable
                {
                    return environment.getResourceAsStream(invocation.getArgument(0));
                }
            });
        }

        // Initialize XWikiContext provider
        if (!this.componentManager.hasComponent(XWikiContext.TYPE_PROVIDER)) {
            Provider<XWikiContext> xcontextProvider =
                this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
            when(xcontextProvider.get()).thenReturn(getXWikiContext());
        } else {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            if (MockUtil.isMock(xcontextProvider)) {
                when(xcontextProvider.get()).thenReturn(getXWikiContext());
            }
        }

        // Initialize readonly XWikiContext provider
        if (!this.componentManager.hasComponent(XWikiContext.TYPE_PROVIDER, "readonly")) {
            Provider<XWikiContext> xcontextProvider =
                this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER, "readonly");
            when(xcontextProvider.get()).thenReturn(getXWikiContext());
        } else {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            if (MockUtil.isMock(xcontextProvider)) {
                when(xcontextProvider.get()).thenReturn(getXWikiContext());
            }
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
        econtext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, getXWikiContext());
        this.scriptContext = (ScriptContext) econtext.getProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID);
        if (this.scriptContext == null) {
            this.scriptContext = new CloneableSimpleScriptContext();
            econtext.setProperty(ScriptExecutionContextInitializer.SCRIPT_CONTEXT_ID, this.scriptContext);
        }

        if (!this.componentManager.hasComponent(ScriptContextManager.class)) {
            ScriptContextManager scriptContextManager =
                this.componentManager.registerMockComponent(ScriptContextManager.class);
            when(scriptContextManager.getCurrentScriptContext()).thenReturn(this.scriptContext);
            when(scriptContextManager.getScriptContext()).thenReturn(this.scriptContext);
        }

        // Initialize the initial context in the stub context provider (which is then used for all calls to
        // createStubContext() in the stub context provider). This is to simulate an XWiki runtime, where this is
        // initialized on the first HTTP request.
        if (this.componentManager.hasComponent(XWikiStubContextProvider.class)) {
            XWikiStubContextProvider stubContextProvider =
                this.componentManager.getInstance(XWikiStubContextProvider.class);
            if (!MockUtil.isMock(stubContextProvider)) {
                // TODO: Since we create the XWikiContext in this method and since no request has been set into it at
                // this point, the initial context in XWikiStubContextProvider would normally be initialized with an
                // empty request which would lead to some NPE when ServletRequest is used later on, for example. Thus
                // we force a request in the context here before the call to stubContextProvider.initialize().
                // Note that this needs to be refactored to let the test control what to initialize in the initial
                // context (i.e. before stubContextProvider.initialize() is called).
                // Also note that setting a non null request forces us to set a non null URL as otherwise it would lead
                // to another NPE...
                XWikiRequest originalRequest = getXWikiContext().getRequest();
                if (getXWikiContext().getRequest() == null) {
                    getXWikiContext().setRequest(new XWikiServletRequestStub());
                }
                URL originalURL = getXWikiContext().getURL();
                if (getXWikiContext().getURL() == null) {
                    getXWikiContext().setURL(new URL("http://localhost:8080"));
                }
                stubContextProvider.initialize(getXWikiContext());
                // Make sure we leave the XWikiContext unchanged (since we just temporarily modified the URL and
                // request to set up the initial context in XWikiStubContextProvider).
                getXWikiContext().setURL(originalURL);
                getXWikiContext().setRequest(originalRequest);
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

        // Register mock components for refactoring listener components
        if (!this.componentManager.hasComponent(ModelBridge.class)) {
            this.componentManager.registerMockComponent(ModelBridge.class);
        }

        if (!this.componentManager.hasComponent(ReferenceUpdater.class)) {
            this.componentManager.registerMockComponent(ReferenceUpdater.class);
        }

        // Make sure to a have an URLConfiguration component.
        if (!this.componentManager.hasComponent(URLConfiguration.class)) {
            URLConfiguration mockUrlConfigComponent =
                this.componentManager.registerMockComponent(URLConfiguration.class);
            when(mockUrlConfigComponent.getURLFormatId()).thenReturn("standard");
        }

        if (!this.componentManager.hasComponent(EntityNameValidationManager.class)) {
            this.componentManager.registerMockComponent(EntityNameValidationManager.class);
        }
        if (!this.componentManager.hasComponent(EntityNameValidationConfiguration.class)) {
            this.componentManager.registerMockComponent(EntityNameValidationConfiguration.class);
        }

        boolean supportRevisionStore = this.componentManager.hasComponent(XWikiDocumentFilterUtils.class);

        // Mock getting document revisions using DocumentRevisionProvider.
        if (supportRevisionStore && !this.componentManager.hasComponent(DocumentRevisionProvider.class, "database")) {
            DocumentRevisionProvider documentRevisionProvider =
                this.componentManager.registerMockComponent(DocumentRevisionProvider.class, "database");

            when(documentRevisionProvider.getRevision(any(DocumentReference.class), anyString()))
                .then(invocation -> {
                    DocumentReference reference = invocation.getArgument(0);
                    String revision = invocation.getArgument(1);
                    XWikiDocument document = getSpyXWiki().getDocument(reference, this.context);

                    return documentRevisionProvider.getRevision(document, revision);
                });

            when(documentRevisionProvider.getRevision(anyXWikiDocument(), anyString()))
                .then(invocation -> {
                        XWikiDocument baseDocument = invocation.getArgument(0);
                        String revision = invocation.getArgument(1);
                        return getMockVersioningStore().loadXWikiDoc(baseDocument, revision, this.context);
                    }
                );
        }

        // Set the default revision provider to the database-one when missing as this covers most cases.
        if (supportRevisionStore && !this.componentManager.hasComponent(DocumentRevisionProvider.class)) {
            DocumentRevisionProvider databaseRevisionProvider =
                this.componentManager.getInstance(DocumentRevisionProvider.class, "database");
            this.componentManager.registerComponent(DocumentRevisionProvider.class, databaseRevisionProvider);
        }

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
                DocumentReference reference = invocation.<XWikiDocument>getArgument(0).getDocumentReferenceWithLocale();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                XWikiDocument document = documents.get(reference);

                if (document == null) {
                    document = new XWikiDocument(reference, reference.getLocale());
                    document.setSyntax(Syntax.PLAIN_1_0);
                    document.setOriginalDocument(document.clone());
                }

                return document;
            }
        });
        when(getMockStore().exists(anyXWikiDocument(), anyXWikiContext())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                // The store is based on the context for the wiki
                DocumentReference reference = invocation.<XWikiDocument>getArgument(0).getDocumentReferenceWithLocale();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                return documents.containsKey(reference);
            }
        });
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                // The store is based on the context for the wiki
                DocumentReference reference = invocation.<XWikiDocument>getArgument(0).getDocumentReferenceWithLocale();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                documents.remove(reference);
                documentArchives.remove(reference);

                return null;
            }
        }).when(getMockStore()).deleteXWikiDoc(anyXWikiDocument(), anyXWikiContext());
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = invocation.getArgument(0);

                // The store is based on the context for the wiki
                DocumentReference reference = document.getDocumentReferenceWithLocale();
                XWikiContext xcontext = invocation.getArgument(1);
                if (!xcontext.getWikiReference().equals(reference.getWikiReference())) {
                    reference = reference.setWikiReference(xcontext.getWikiReference());
                }

                saveDocument(reference, document, xcontext);

                return null;
            }
        }).when(getMockStore()).saveXWikiDoc(anyXWikiDocument(), anyXWikiContext());
        when(getMockStore().getLimitSize(any(), any(), any())).thenReturn(255);

        // XWikiVersioningStoreInterface

        when(getMockVersioningStore().getXWikiDocumentArchive(anyXWikiDocument(), anyXWikiContext()))
            .then(new Answer<XWikiDocumentArchive>()
            {
                @Override
                public XWikiDocumentArchive answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = invocation.getArgument(0);

                    // The store is based on the context for the wiki
                    DocumentReference reference = document.getDocumentReferenceWithLocale();

                    XWikiDocumentArchive archiveDoc = documentArchives.get(reference);

                    if (archiveDoc == null) {
                        XWikiContext xcontext = invocation.getArgument(1);
                        String db = xcontext.getWikiId();
                        try {
                            if (reference.getWikiReference().getName() != null) {
                                xcontext.setWikiId(reference.getWikiReference().getName());
                            }
                            archiveDoc = new XWikiDocumentArchive(document.getId());
                            document.setDocumentArchive(archiveDoc);
                            documentArchives.put(reference, archiveDoc);
                        } finally {
                            xcontext.setWikiId(db);
                        }
                    }

                    return archiveDoc;
                }
            });
        when(getMockVersioningStore().getXWikiDocVersions(anyXWikiDocument(), anyXWikiContext()))
            .then(new Answer<Version[]>()
            {
                @Override
                public Version[] answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocumentArchive archive = getMockVersioningStore()
                        .getXWikiDocumentArchive(invocation.getArgument(0), invocation.getArgument(1));

                    if (archive == null) {
                        return new Version[0];
                    }
                    Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
                    Version[] versions = new Version[nodes.size()];
                    Iterator<XWikiRCSNodeInfo> it = nodes.iterator();
                    for (int i = 0; i < versions.length; i++) {
                        XWikiRCSNodeInfo node = it.next();
                        versions[versions.length - 1 - i] = node.getId().getVersion();
                    }

                    return versions;
                }
            });
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                XWikiDocument document = invocation.getArgument(0);
                XWikiContext xcontext = invocation.getArgument(2);

                XWikiDocumentArchive archiveDoc = getMockVersioningStore().getXWikiDocumentArchive(document, xcontext);
                archiveDoc.updateArchive(document, document.getAuthor(), document.getDate(), document.getComment(),
                    document.getRCSVersion(), xcontext);
                document.setRCSVersion(archiveDoc.getLatestVersion());

                return null;
            }
        }).when(getMockVersioningStore()).updateXWikiDocArchive(any(), anyBoolean(), any());

        doAnswer(invocation -> {
            XWikiDocument baseDocument = invocation.getArgument(0);
            String revision = invocation.getArgument(1);
            XWikiContext xContext = invocation.getArgument(2);

            XWikiDocumentArchive archive = getMockVersioningStore().getXWikiDocumentArchive(baseDocument, xContext);
            Version version = new Version(revision);

            XWikiDocument doc = archive.loadDocument(version, xContext);
            if (doc == null) {
                Object[] args = { baseDocument.getDocumentReferenceWithLocale(), version.toString() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
                    "Version {1} does not exist while reading document {0}", null, args);
            }

            doc.setStore(baseDocument.getStore());

            // Make sure the attachment of the revision document have the right store
            for (XWikiAttachment revisionAttachment : doc.getAttachmentList()) {
                XWikiAttachment attachment = baseDocument.getAttachment(revisionAttachment.getFilename());

                if (attachment != null) {
                    revisionAttachment.setContentStore(attachment.getContentStore());
                    revisionAttachment.setArchiveStore(attachment.getArchiveStore());
                }
            }

            return doc;
        }).when(getMockVersioningStore()).loadXWikiDoc(anyXWikiDocument(), anyString(), anyXWikiContext());

        // XWiki

        if (this.mockXWiki) {
            if (!supportRevisionStore) {
                doAnswer((Answer<XWikiDocument>) invocation -> {
                    XWikiDocument doc = invocation.getArgument(0);
                    String revision = invocation.getArgument(1);

                    if (StringUtils.equals(revision, doc.getVersion())) {
                        return doc;
                    }

                    return new XWikiDocument(doc.getDocumentReference());
                }).when(getSpyXWiki()).getDocument(anyXWikiDocument(), any(), anyXWikiContext());
            }
            doAnswer(new Answer<XWikiDocument>()
            {
                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = invocation.getArgument(0);

                    String currentWiki = getXWikiContext().getWikiId();
                    try {
                        getXWikiContext().setWikiId(document.getDocumentReference().getWikiReference().getName());

                        return getMockStore().loadXWikiDoc(document, getXWikiContext());
                    } finally {
                        getXWikiContext().setWikiId(currentWiki);
                    }
                }
            }).when(getSpyXWiki()).getDocument(anyXWikiDocument(), any(XWikiContext.class));
            doAnswer(new Answer<Void>()
            {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = invocation.getArgument(0);
                    String comment = invocation.getArgument(1);
                    boolean minorEdit = invocation.getArgument(2);
                    XWikiContext xcontext = invocation.getArgument(3);

                    boolean isNew = document.isNew();

                    document.setComment(StringUtils.defaultString(comment));
                    document.setMinorEdit(minorEdit);

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
                        originalDocument =
                            spyXWiki.getDocument(document.getDocumentReferenceWithLocale(), getXWikiContext());
                        document.setOriginalDocument(originalDocument);
                    }

                    saveDocument(document.getDocumentReferenceWithLocale(), document, xcontext);

                    XWikiDocument newOriginal = document.getOriginalDocument();

                    try {
                        document.setOriginalDocument(originalDocument);

                        if (notifyDocumentCreatedEvent) {
                            if (isNew) {
                                getObservationManager().notify(
                                    new DocumentCreatedEvent(document.getDocumentReference()), document,
                                    getXWikiContext());
                            } else {
                                getObservationManager().notify(
                                    new DocumentUpdatedEvent(document.getDocumentReference()), document,
                                    getXWikiContext());
                            }
                        }
                    } finally {
                        document.setOriginalDocument(newOriginal);
                    }

                    return null;
                }
            }).when(getSpyXWiki()).saveDocument(anyXWikiDocument(), any(String.class), anyBoolean(), anyXWikiContext());
            doNothing().when(getSpyXWiki()).checkSavingDocument(any(DocumentReference.class), anyXWikiDocument(),
                any(String.class), anyBoolean(), anyXWikiContext());
            doAnswer(new Answer<Void>()
            {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = invocation.getArgument(0);

                    String currentWiki = null;

                    currentWiki = getXWikiContext().getWikiId();
                    try {
                        getXWikiContext().setWikiId(document.getDocumentReference().getWikiReference().getName());

                        getMockStore().deleteXWikiDoc(document, getXWikiContext());

                        if (notifyDocumentDeletedEvent) {
                            getObservationManager().notify(new DocumentDeletedEvent(document.getDocumentReference()),
                                document, getXWikiContext());
                        }
                    } finally {
                        getXWikiContext().setWikiId(currentWiki);
                    }

                    return null;
                }
            }).when(getSpyXWiki()).deleteDocument(anyXWikiDocument(), any(Boolean.class), anyXWikiContext());
            doNothing().when(getSpyXWiki()).checkDeletingDocument(any(DocumentReference.class), anyXWikiDocument(),
                anyXWikiContext());
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

            // Users

            doAnswer(new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = invocation.getArgument(0);

                    XWikiDocument userDocument = getSpyXWiki().getDocument(
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

                    XWikiDocument groupDocument = getSpyXWiki().getDocument(
                        new DocumentReference(GROUP_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                    final BaseClass groupClass = groupDocument.getXClass();

                    if (groupDocument.isNew()) {
                        groupClass.addTextField("member", "Member", 30);

                        getSpyXWiki().saveDocument(groupDocument, xcontext);
                    }

                    return groupClass;
                }
            }).when(getSpyXWiki()).getGroupClass(anyXWikiContext());

            // RightsClass
            doAnswer(new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = invocation.getArgument(0);

                    XWikiDocument rightDocument = getSpyXWiki().getDocument(
                        new DocumentReference(RIGHTS_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                    final BaseClass rightClass = rightDocument.getXClass();

                    if (rightDocument.isNew()) {
                        rightClass.addTextField("groups", "groups", 80);
                        rightClass.addTextField("levels", "Access Levels", 80);
                        rightClass.addTextField("users", "Users", 80);
                        rightClass.addBooleanField("allow", "Allow/Deny", "allow");
                        getSpyXWiki().saveDocument(rightDocument, xcontext);
                    }

                    return rightClass;
                }
            }).when(getSpyXWiki()).getRightsClass(anyXWikiContext());

            // GlobalRightsClass
            doAnswer(new Answer<BaseClass>()
            {
                @Override
                public BaseClass answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiContext xcontext = invocation.getArgument(0);

                    XWikiDocument globalRightDocument = getSpyXWiki().getDocument(
                        new DocumentReference(GLOBAL_RIGHTS_CLASS, new WikiReference(xcontext.getWikiId())), xcontext);

                    final BaseClass globalRightClass = globalRightDocument.getXClass();

                    if (globalRightDocument.isNew()) {
                        globalRightClass.addTextField("groups", "groups", 80);
                        globalRightClass.addTextField("levels", "Access Levels", 80);
                        globalRightClass.addTextField("users", "Users", 80);
                        globalRightClass.addBooleanField("allow", "Allow/Deny", "allow");
                        getSpyXWiki().saveDocument(globalRightDocument, xcontext);
                    }

                    return globalRightClass;
                }
            }).when(getSpyXWiki()).getGlobalRightsClass(anyXWikiContext());
        }

        // DocumentAccessBridge
        if (!this.componentManager.hasComponent(DocumentAccessBridge.class)) {
            this.documentAccessBridge = this.componentManager.registerMockComponent(DocumentAccessBridge.class);
        } else {
            this.documentAccessBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        }
        if (MockUtil.isMock(this.documentAccessBridge)) {
            when(this.documentAccessBridge.exists(any(DocumentReference.class))).thenAnswer(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable
                {
                    DocumentReference documentReference = invocation.getArgument(0);

                    return spyXWiki.exists(documentReference, context);
                }
            });
        }

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

        // A default implementation of UserReferenceResolver<CurrentUserReference> is expected by
        // EffectiveAuthorSetterListener which otherwise fails to be registered as a component when @AllComponents
        // annotation is used in tests that depend on oldcore. We register a mock implementation in case there's none
        // registered already.
        DefaultParameterizedType currentUserReferenceResolverType =
            new DefaultParameterizedType(null, UserReferenceResolver.class, CurrentUserReference.class);
        if (!this.componentManager.hasComponent(currentUserReferenceResolverType)) {
            getMocker().registerMockComponent(currentUserReferenceResolverType);
        }

        DefaultParameterizedType userReferenceDocumentReferenceResolverType =
            new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class);
        if (!this.componentManager.hasComponent(userReferenceDocumentReferenceResolverType, "document")) {
            UserReferenceResolver<DocumentReference> userReferenceResolver =
                getMocker().registerMockComponent(userReferenceDocumentReferenceResolverType, "document");

            DefaultParameterizedType userReferenceDocumentReferenceSerializer =
                new DefaultParameterizedType(null, UserReferenceSerializer.class, DocumentReference.class);
            if (!this.componentManager.hasComponent(userReferenceDocumentReferenceSerializer, "document")) {
                this.documentReferenceUserReferenceSerializer =
                    getMocker().registerMockComponent(userReferenceDocumentReferenceSerializer, "document");
            } else {
                this.documentReferenceUserReferenceSerializer =
                    getMocker().getInstance(userReferenceDocumentReferenceSerializer, "document");
            }

            if (!this.componentManager.hasComponent(UserPropertiesResolver.class, "all")) {
                this.allUserPropertiesResolver = getMocker().registerMockComponent(UserPropertiesResolver.class, "all");
            } else {
                this.allUserPropertiesResolver = getMocker().getInstance(UserPropertiesResolver.class, "all");
            }

            // we ensure that when trying to resolve a DocumentReference to UserReference, then the returned mock
            // will return the original DocumentReference when resolved back to DocumentReference.
            when(userReferenceResolver.resolve(any()))
                .then(invocationOnMock -> new TestDocumentUserReference(invocationOnMock.getArgument(0)));
            when(this.documentReferenceUserReferenceSerializer.serialize(any(TestDocumentUserReference.class)))
                .then(invocationOnMock -> invocationOnMock.<TestDocumentUserReference>getArgument(0).documentReference);
        }
    }

    private void saveDocument(DocumentReference reference, XWikiDocument document, XWikiContext xcontext)
        throws XWikiException
    {
        boolean supportRevisionStore = this.componentManager.hasComponent(XWikiDocumentFilterUtils.class);

        if (document.isContentDirty() || document.isMetaDataDirty()) {
            document.setDate(new Date());
            if (document.isContentDirty()) {
                document.setContentUpdateDate(new Date());
                DocumentAuthors authors = document.getAuthors();
                authors.setContentAuthor(authors.getEffectiveMetadataAuthor());
            }
            document.incrementVersion();

            document.setContentDirty(false);
            document.setMetaDataDirty(false);

            if (supportRevisionStore) {
                // Save the document in the document archive.
                getMockVersioningStore().updateXWikiDocArchive(document, true, xcontext);
            }
        } else {
            if (supportRevisionStore) {
                if (document.getDocumentArchive() != null) {
                    getMockVersioningStore().saveXWikiDocArchive(document.getDocumentArchive(), false, xcontext);

                    if (!containsVersion(document, document.getRCSVersion(), xcontext)) {
                        getMockVersioningStore().updateXWikiDocArchive(document, false, xcontext);
                    }
                } else {
                    try {
                        document.getDocumentArchive(xcontext);

                        if (!containsVersion(document, document.getRCSVersion(), xcontext)) {
                            getMockVersioningStore().updateXWikiDocArchive(document, false, xcontext);
                        }
                    } catch (XWikiException e) {
                        // this is a non critical error
                    }
                }
            }
        }
        document.setNew(false);
        document.setStore(getMockStore());

        // Make sure the document is not restricted.
        document.setRestricted(false);

        XWikiDocument savedDocument = document.clone();

        documents.put(document.getDocumentReferenceWithLocale(), savedDocument);

        // Set the document as it's original document
        savedDocument.setOriginalDocument(savedDocument.clone());
    }

    private boolean containsVersion(XWikiDocument doc, Version targetversion, XWikiContext context)
        throws XWikiException
    {
        for (Version version : doc.getRevisions(context)) {
            if (version.equals(targetversion)) {
                return true;
            }
        }

        return false;
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

    /**
     * @since 15.0RC1
     * @since 14.10.2
     */
    public DocumentAccessBridge getDocumentAccessBridge()
    {
        return this.documentAccessBridge;
    }

    public XWikiRightService getMockRightService()
    {
        return this.mockRightService;
    }

    public XWikiAuthService getMockAuthService()
    {
        return this.mockAuthService;
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
        return this.mockXWikiHibernateStore;
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
     * Return the default configuration, which is also registered for xwikiproperties.
     * @since 7.1M1
     */
    public MemoryConfigurationSource getConfigurationSource()
    {
        return this.configurationSource;
    }

    /**
     * @since 15.9RC1
     */
    public MemoryConfigurationSource registerMockXWikiCfg()
    {
        this.xwikicfgConfigurationSource = new MockConfigurationSource();
        getMocker().registerComponent(MockConfigurationSource.getDescriptor(XWikiCfgConfigurationSource.ROLEHINT),
            this.xwikicfgConfigurationSource);

        return this.xwikicfgConfigurationSource;
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
        File testDirectory = XWikiTempDirUtil.createTemporaryDirectory();

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

    public UserPropertiesResolver getMockAllUserPropertiesResolver()
    {
        return this.allUserPropertiesResolver;
    }
}
