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
package com.xpn.xwiki;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;
import javax.script.ScriptContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.velocity.VelocityContext;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentRolledBackEvent;
import org.xwiki.bridge.event.DocumentRollingBackEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.annotation.Serializable;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailStatusResultSerializer;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.refactoring.batch.BatchOperationExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.criteria.api.XWikiCriteriaService;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocument.XWikiAttachmentToRemove;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.internal.WikiInitializerJob;
import com.xpn.xwiki.internal.WikiInitializerRequest;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.internal.XWikiConfigDelegate;
import com.xpn.xwiki.internal.XWikiInitializerJob;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.internal.render.OldRendering;
import com.xpn.xwiki.internal.render.groovy.ParseGroovyFromString;
import com.xpn.xwiki.internal.skin.InternalSkinConfiguration;
import com.xpn.xwiki.internal.skin.InternalSkinManager;
import com.xpn.xwiki.internal.skin.WikiSkin;
import com.xpn.xwiki.internal.skin.WikiSkinUtils;
import com.xpn.xwiki.internal.velocity.VelocityEvaluator;
import com.xpn.xwiki.job.JobRequestContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.groovy.XWikiPageClassLoader;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.SearchEngineRule;
import com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;
import com.xpn.xwiki.web.XWikiURLFactoryServiceImpl;
import com.xpn.xwiki.web.includeservletasstring.IncludeServletAsString;

@Serializable(false)
public class XWiki implements EventListener
{
    /** Name of the default wiki. */
    public static final String DEFAULT_MAIN_WIKI = "xwiki";

    /** Name of the default home space. */
    public static final String DEFAULT_HOME_SPACE = "Main";

    /** Name of the default system space. */
    public static final String SYSTEM_SPACE = "XWiki";

    /** Name of the default space homepage. */
    public static final String DEFAULT_SPACE_HOMEPAGE = "WebHome";

    public static final String CKEY_SKIN = InternalSkinManager.CKEY_SKIN;

    public static final String CKEY_BASESKIN = InternalSkinManager.CKEY_PARENTSKIN;

    public static final String DEFAULT_SKIN = InternalSkinConfiguration.DEFAULT_SKIN;

    /** Logging helper object. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    /** Frequently used Document reference, the class which holds virtual wiki definitions. */
    private static final DocumentReference VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE =
        new DocumentReference(DEFAULT_MAIN_WIKI, SYSTEM_SPACE, "XWikiServerClass");

    /** The default encoding, and the internally used encoding when dealing with byte representation of strings. */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** Represents no value (ie the default value will be used) in xproperties */
    private static final String NO_VALUE = "---";

    /** The main document storage. */
    private XWikiStoreInterface store;

    /** The attachment content storage. */
    private XWikiAttachmentStoreInterface defaultAttachmentContentStore;

    /** The attachment archive storage. */
    private AttachmentVersioningStore defaultAttachmentArchiveStore;

    /** Document versioning storage. */
    private XWikiVersioningStoreInterface versioningStore;

    /** Deleted documents storage. */
    private XWikiRecycleBinStoreInterface recycleBinStore;

    private AttachmentRecycleBinStore attachmentRecycleBinStore;

    private XWikiPluginManager pluginManager;

    private XWikiAuthService authService;

    private XWikiRightService rightService;

    private XWikiGroupService groupService;

    private XWikiStatsService statsService;

    private XWikiURLFactoryService urlFactoryService;

    private XWikiCriteriaService criteriaService;

    /** Lock object used for the lazy initialization of the authentication service. */
    private final Object AUTH_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the authorization service. */
    private final Object RIGHT_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the group management service. */
    private final Object GROUP_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the statistics service. */
    private final Object STATS_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the URL Factory service. */
    private final Object URLFACTORY_SERVICE_LOCK = new Object();

    private MetaClass metaclass;

    private String version;

    private XWikiEngineContext engine_context;

    private String database;

    private String fullNameSQL;

    /**
     * The list of initialized wikis.
     */
    private Map<String, WikiInitializerJob> initializedWikis = new ConcurrentHashMap<>();

    private boolean isReadOnly = false;

    /**
     * @deprecated since 6.1M2, use {@link XWikiCfgConfigurationSource#CFG_ENV_NAME} instead
     */
    @Deprecated
    public static final String CFG_ENV_NAME = XWikiCfgConfigurationSource.CFG_ENV_NAME;

    public static final String MACROS_FILE = "/templates/macros.txt";

    /**
     * File containing XWiki's version, in the format: <version name>.<SVN revision number>.
     */
    private static final String VERSION_FILE = "/WEB-INF/version.properties";

    /**
     * Property containing the version value in the {@link #VERSION_FILE} file.
     */
    private static final String VERSION_FILE_PROPERTY = "version";

    private static XWikiInitializerJob job;

    /** List of configured syntax ids. */
    private List<String> configuredSyntaxes;

    /** Used to convert a proper Document Reference to string (standard form). */
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    private DocumentReferenceResolver<EntityReference> currentReferenceDocumentReferenceResolver;

    private EntityReferenceResolver<String> currentMixedEntityReferenceResolver;

    private EntityReferenceResolver<String> relativeEntityReferenceResolver;

    private EntityReferenceSerializer<String> localStringEntityReferenceSerializer;

    private ResourceReferenceManager resourceReferenceManager;

    private JobExecutor jobExecutor;

    private InternalSkinManager internalSkinManager;

    private TemplateManager templateManager;

    private RenderingContext renderingContext;

    private VelocityEvaluator velocityEvaluator;

    /**
     * Whether backlinks are enabled or not (cached for performance).
     *
     * @since 3.2M2
     */
    private Boolean hasBacklinks;

    private ConfigurationSource xwikicfg;

    private ConfigurationSource wikiConfiguration;

    private ConfigurationSource userConfiguration;

    private ConfigurationSource spaceConfiguration;

    private EditConfiguration editConfiguration;

    private ObservationManager observationManager;

    private Provider<XWikiContext> xcontextProvider;

    private ContextualLocalizationManager localization;

    private Provider<OldRendering> oldRenderingProvider;

    private ParseGroovyFromString parseGroovyFromString;

    private JobProgressManager progress;

    private Provider<DocumentReference> defaultDocumentReferenceProvider;

    private DocumentReferenceResolver<EntityReference> currentgetdocumentResolver;

    private AttachmentReferenceResolver<EntityReference> currentAttachmentReferenceResolver;

    private WikiSkinUtils wikiSkinUtils;

    private DocumentRevisionProvider documentRevisionProvider;

    /**
     * List of top level space names that can be used in the fake context document created when accessing a resource
     * with the 'skin' action.
     */
    private List<String> SKIN_RESOURCE_SPACE_NAMES = Arrays.asList("skins", "resources");

    private ConfigurationSource getConfiguration()
    {
        if (this.xwikicfg == null) {
            this.xwikicfg = Utils.getComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);
        }

        return this.xwikicfg;
    }

    private ConfigurationSource getWikiConfiguration()
    {
        if (this.wikiConfiguration == null) {
            this.wikiConfiguration = Utils.getComponent(ConfigurationSource.class, "wiki");
        }

        return this.wikiConfiguration;
    }

    private ConfigurationSource getSpaceConfiguration()
    {
        if (this.spaceConfiguration == null) {
            this.spaceConfiguration = Utils.getComponent(ConfigurationSource.class, "space");
        }

        return this.spaceConfiguration;
    }

    private ConfigurationSource getUserConfiguration()
    {
        if (this.userConfiguration == null) {
            this.userConfiguration = Utils.getComponent(ConfigurationSource.class, "user");
        }

        return this.userConfiguration;
    }

    private EditConfiguration getEditConfiguration()
    {
        if (this.editConfiguration == null) {
            this.editConfiguration = Utils.getComponent(EditConfiguration.class);
        }

        return this.editConfiguration;
    }

    private InternalSkinManager getInternalSkinManager()
    {
        if (this.internalSkinManager == null) {
            this.internalSkinManager = Utils.getComponent(InternalSkinManager.class);
        }

        return this.internalSkinManager;
    }

    private TemplateManager getTemplateManager()
    {
        if (this.templateManager == null) {
            this.templateManager = Utils.getComponent(TemplateManager.class);
        }

        return this.templateManager;
    }

    private RenderingContext getRenderingContext()
    {
        if (this.renderingContext == null) {
            this.renderingContext = Utils.getComponent(RenderingContext.class);
        }

        return this.renderingContext;
    }

    private MutableRenderingContext getMutableRenderingContext()
    {
        return getRenderingContext() instanceof MutableRenderingContext
            ? (MutableRenderingContext) getRenderingContext() : null;
    }

    private VelocityEvaluator getVelocityEvaluator()
    {
        if (this.velocityEvaluator == null) {
            this.velocityEvaluator = Utils.getComponent(VelocityEvaluator.class);
        }

        return this.velocityEvaluator;
    }

    private ObservationManager getObservationManager()
    {
        if (this.observationManager == null) {
            this.observationManager = Utils.getComponent(ObservationManager.class);
        }

        return this.observationManager;
    }

    private XWikiContext getXWikiContext()
    {
        if (this.xcontextProvider == null) {
            this.xcontextProvider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);
        }

        return this.xcontextProvider.get();
    }

    private ContextualLocalizationManager getLocalization()
    {
        if (this.localization == null) {
            this.localization = Utils.getComponent(ContextualLocalizationManager.class);
        }

        return this.localization;
    }

    private OldRendering getOldRendering()
    {
        if (this.oldRenderingProvider == null) {
            this.oldRenderingProvider = Utils.getComponent(OldRendering.TYPE_PROVIDER);
        }

        return this.oldRenderingProvider.get();
    }

    private ParseGroovyFromString getParseGroovyFromString()
    {
        if (this.parseGroovyFromString == null) {
            this.parseGroovyFromString = Utils.getComponent(ParseGroovyFromString.class);
        }

        return this.parseGroovyFromString;
    }

    private JobProgressManager getProgress()
    {
        if (this.progress == null) {
            this.progress = Utils.getComponent(JobProgressManager.class);
        }

        return this.progress;
    }

    private Provider<DocumentReference> getDefaultDocumentReferenceProvider()
    {
        if (this.defaultDocumentReferenceProvider == null) {
            this.defaultDocumentReferenceProvider = Utils.getComponent(DocumentReference.TYPE_PROVIDER);
        }

        return this.defaultDocumentReferenceProvider;
    }

    private DocumentReferenceResolver<EntityReference> getCurrentGetDocumentResolver()
    {
        if (this.currentgetdocumentResolver == null) {
            this.currentgetdocumentResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, "currentgetdocument");
        }

        return this.currentgetdocumentResolver;
    }

    private AttachmentReferenceResolver<EntityReference> getCurrentAttachmentResolver()
    {
        if (this.currentAttachmentReferenceResolver == null) {
            this.currentAttachmentReferenceResolver =
                Utils.getComponent(AttachmentReferenceResolver.TYPE_REFERENCE, "current");
        }

        return this.currentAttachmentReferenceResolver;
    }

    private EntityReferenceSerializer<String> getDefaultEntityReferenceSerializer()
    {
        if (this.defaultEntityReferenceSerializer == null) {
            this.defaultEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        }

        return this.defaultEntityReferenceSerializer;
    }

    private DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        if (this.currentMixedDocumentReferenceResolver == null) {
            this.currentMixedDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        }

        return this.currentMixedDocumentReferenceResolver;
    }

    private DocumentReferenceResolver<EntityReference> getCurrentReferenceDocumentReferenceResolver()
    {
        if (this.currentReferenceDocumentReferenceResolver == null) {
            this.currentReferenceDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        }

        return this.currentReferenceDocumentReferenceResolver;
    }

    private EntityReferenceResolver<String> getCurrentMixedEntityReferenceResolver()
    {
        if (this.currentMixedEntityReferenceResolver == null) {
            this.currentMixedEntityReferenceResolver =
                Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "currentmixed");
        }

        return this.currentMixedEntityReferenceResolver;
    }

    private EntityReferenceResolver<String> getRelativeEntityReferenceResolver()
    {
        if (this.relativeEntityReferenceResolver == null) {
            this.relativeEntityReferenceResolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        }

        return this.relativeEntityReferenceResolver;
    }

    private EntityReferenceSerializer<String> getLocalStringEntityReferenceSerializer()
    {
        if (this.localStringEntityReferenceSerializer == null) {
            this.localStringEntityReferenceSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }

        return this.localStringEntityReferenceSerializer;
    }

    private ResourceReferenceManager getResourceReferenceManager()
    {
        if (this.resourceReferenceManager == null) {
            this.resourceReferenceManager = Utils.getComponent(ResourceReferenceManager.class);
        }

        return this.resourceReferenceManager;
    }

    private JobExecutor getJobExecutor()
    {
        if (this.jobExecutor == null) {
            this.jobExecutor = Utils.getComponent(JobExecutor.class);
        }

        return this.jobExecutor;
    }

    private DocumentReference getDefaultDocumentReference()
    {
        return getDefaultDocumentReferenceProvider().get();
    }

    private WikiSkinUtils getWikiSkinUtils()
    {
        if (this.wikiSkinUtils == null) {
            this.wikiSkinUtils = Utils.getComponent(WikiSkinUtils.class);
        }

        return this.wikiSkinUtils;
    }

    private DocumentRevisionProvider getDocumentRevisionProvider()
    {
        if (this.documentRevisionProvider == null) {
            this.documentRevisionProvider = Utils.getComponent(DocumentRevisionProvider.class);
        }

        return this.documentRevisionProvider;
    }

    private String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(getLocalization().getTranslationPlain(key, parameters), key);
    }

    /**
     * @param context see {@link XWikiContext}
     */
    public static XWiki getMainXWiki(XWikiContext context) throws XWikiException
    {
        return getMainXWiki(true, context);
    }

    /**
     * @param wait true if the method should way for {@link XWiki} instance to be initialized
     * @param context see {@link XWikiContext}
     */
    public static XWiki getMainXWiki(boolean wait, XWikiContext context) throws XWikiException
    {
        String xwikiname = DEFAULT_MAIN_WIKI;

        context.setMainXWiki(xwikiname);

        XWiki xwiki;

        try {
            XWikiEngineContext econtext = context.getEngineContext();

            xwiki = (XWiki) econtext.getAttribute(xwikiname);
            if (xwiki == null) {
                // Start XWiki initialization
                synchronized (XWiki.class) {
                    xwiki = (XWiki) econtext.getAttribute(xwikiname);
                    if (xwiki == null && job == null) {
                        job = Utils.getComponent((Type) Job.class, XWikiInitializerJob.JOBTYPE);

                        if (job.getStatus() == null) {
                            // "Pre-initialize" XWikiStubContextProvider so that XWiki initializer can find one
                            Utils.<XWikiStubContextProvider>getComponent(XWikiStubContextProvider.class)
                                .initialize(context);

                            job.startAsync();
                        }
                    }
                }

                // Wait until XWiki is initialized
                if (wait) {
                    job.join();
                    xwiki = (XWiki) econtext.getAttribute(xwikiname);
                }
            }

            context.setWiki(xwiki);

            return xwiki;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
                "Could not initialize main XWiki instance", e);
        }
    }

    public static boolean isInitializing(XWikiContext xcontext)
    {
        return Boolean.TRUE.equals(xcontext.getEngineContext().getAttribute("xwiki.init"));
    }

    /**
     * Return the XWiki object (as in "the Wiki API") corresponding to the requested wiki.
     *
     * @param context see {@link XWikiContext}
     * @return an XWiki object configured for the wiki corresponding to the current request
     * @throws XWikiException if the requested URL does not correspond to a real wiki, or if there's an error in the
     *             storage
     */
    public static XWiki getXWiki(XWikiContext context) throws XWikiException
    {
        return getXWiki(true, context);
    }

    /**
     * Return the XWiki object (as in "the Wiki API") corresponding to the requested wiki.
     * <p>
     * Unless <code>wait</code> is false the method return right away null if XWiki is not yet initialized.
     *
     * @param wait wait until XWiki is initialized
     * @param xcontext see {@link XWikiContext}
     * @return an XWiki object configured for the wiki corresponding to the current request
     * @throws XWikiException if the requested URL does not correspond to a real wiki, or if there's an error in the
     *             storage
     */
    public static XWiki getXWiki(boolean wait, XWikiContext xcontext) throws XWikiException
    {
        XWiki xwiki = getMainXWiki(wait, xcontext);

        if (xwiki == null) {
            return null;
        }

        // Extract Entity Resource from URL and put it in the Execution Context
        EntityResourceReference entityResourceReference = initializeResourceFromURL(xcontext);

        // Get the wiki id
        String wikiId = entityResourceReference.getEntityReference().extractReference(EntityType.WIKI).getName();
        if (wikiId.equals(xcontext.getMainXWiki())) {
            // The main wiki was requested.
            return xwiki;
        }

        // Check if the wiki exists by checking if a descriptor exists for the wiki id.
        WikiDescriptorManager wikiDescriptorManager = Utils.getComponent(WikiDescriptorManager.class);
        WikiDescriptor descriptor;
        try {
            descriptor = wikiDescriptorManager.getById(wikiId);
        } catch (WikiManagerException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_STORE_MISC,
                String.format("Failed find wiki descriptor for wiki id [%s]", wikiId), e);
        }
        if (descriptor == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                String.format("The wiki [%s] does not exist", wikiId));
        }

        // Initialize wiki

        xcontext.setWikiId(wikiId);
        xcontext.setOriginalWikiId(wikiId);

        if (!xwiki.initializeWiki(wikiId, wait, xcontext)) {
            // The wiki is still initializing
            return null;
        }

        return xwiki;
    }

    /**
     * @param wikiId the identifier of the wiki
     * @return the current {@link WikiInitializerJob} associated to the passed wiki or null if there is none
     */
    public Job getWikiInitializerJob(String wikiId)
    {
        return this.initializedWikis.get(wikiId);
    }

    /**
     * Make sure the wiki is initializing or wait for it.
     * 
     * @param wikiId the identifier of the wiki to initialize
     * @param wait true if the method should return only when the wiki is fully initialized
     * @return true if the wiki is fully initialized
     * @param xcontext the XWiki context
     * @throws XWikiException when the initialization failed
     * @since 8.4RC1
     */
    public boolean initializeWiki(String wikiId, boolean wait, XWikiContext xcontext) throws XWikiException
    {
        Job wikiJob = this.initializedWikis.get(wikiId);

        // Create and start the job if it does not exist
        if (wikiJob == null) {
            try {
                wikiJob = initializeWiki(wikiId, xcontext);
            } catch (JobException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
                    "Could not start [" + wikiId + "] wiki initialization", e);
            }
        }

        // Check if the job is done
        if (wikiJob.getStatus().getState() == State.FINISHED) {
            return true;
        }

        // Wait until the job is finished if asked to
        if (wait) {
            try {
                wikiJob.join();
            } catch (InterruptedException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
                    "Wiki [" + wikiId + "] initialization was interrupted unexpectedly", e);
            }

            if (wikiJob.getStatus().getError() != null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
                    "Wiki [" + wikiId + "] initialization failed", wikiJob.getStatus().getError());
            }

            return true;
        }

        // Still initializing
        return false;
    }

    private Job initializeWiki(String wikiId, XWikiContext xcontext) throws JobException
    {
        synchronized (this.initializedWikis) {
            WikiInitializerJob wikiJob = this.initializedWikis.get(wikiId);

            if (wikiJob == null) {
                WikiInitializerRequest request = new WikiInitializerRequest(wikiId);

                JobRequestContext.set(request, xcontext);

                wikiJob = (WikiInitializerJob) getJobExecutor().execute(WikiInitializerJob.JOBTYPE, request);
                this.initializedWikis.put(wikiId, wikiJob);
            }

            return wikiJob;
        }
    }

    private static EntityResourceReference initializeResourceFromURL(XWikiContext context) throws XWikiException
    {
        // Extract the Entity Resource from the URL
        // TODO: This code should be put in an ExecutionContextInitializer but we couldn't do yet since this code
        // requires that the XWiki object be initialized first (the line above). Thus we'll be able to to move it only
        // after the XWiki init is done also in an ExecutionContextInitializer (and with priorities).
        @SuppressWarnings("deprecation")
        EntityResourceReference entityResourceReference;
        URL url = context.getURL();
        try {
            ExtendedURL extendedURL = new ExtendedURL(url, context.getRequest().getContextPath());
            ResourceTypeResolver<ExtendedURL> typeResolver =
                Utils.getComponent(new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
            ResourceType type = typeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());
            ResourceReferenceResolver<ExtendedURL> resourceResolver = Utils
                .getComponent(new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
            entityResourceReference = (EntityResourceReference) resourceResolver.resolve(extendedURL, type,
                Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                String.format("Failed to extract Entity Resource Reference from URL [%s]", url), e);
        }
        Utils.getComponent(Execution.class).getContext().setProperty(ResourceReferenceManager.RESOURCE_CONTEXT_PROPERTY,
            entityResourceReference);

        return entityResourceReference;
    }

    public static URL getRequestURL(XWikiRequest request) throws XWikiException
    {
        try {
            StringBuffer requestURL = request.getRequestURL();
            String qs = request.getQueryString();
            if ((qs != null) && (!qs.equals(""))) {
                return new URL(requestURL.toString() + "?" + qs);
            } else {
                return new URL(requestURL.toString());
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                "Exception while getting URL from request", e);
        }
    }

    public static Object callPrivateMethod(Object obj, String methodName)
    {
        return callPrivateMethod(obj, methodName, null, null);
    }

    public static Object callPrivateMethod(Object obj, String methodName, Class<?>[] classes, Object[] args)
    {
        try {
            Method method = obj.getClass().getDeclaredMethod(methodName, classes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to call private method [{}]: [{}]", methodName, e);

            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            LOGGER.error("Private method [{}] failed: [{}]", methodName, e);

            return null;
        }
    }

    public static HttpClient getHttpClient(int timeout, String userAgent)
    {
        HttpClient client = new HttpClient();

        if (timeout != 0) {
            client.getParams().setSoTimeout(timeout);
            client.getParams().setParameter("http.connection.timeout", Integer.valueOf(timeout));
        }

        client.getParams().setParameter("http.useragent", userAgent);

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if ((proxyHost != null) && (!proxyHost.equals(""))) {
            int port = 3128;
            if ((proxyPort != null) && (!proxyPort.equals(""))) {
                port = Integer.parseInt(proxyPort);
            }
            client.getHostConfiguration().setProxy(proxyHost, port);
        }

        String proxyUser = System.getProperty("http.proxyUser");
        if ((proxyUser != null) && (!proxyUser.equals(""))) {
            String proxyPassword = System.getProperty("http.proxyPassword");
            Credentials defaultcreds = new UsernamePasswordCredentials(proxyUser, proxyPassword);
            client.getState().setProxyCredentials(AuthScope.ANY, defaultcreds);
        }

        return client;
    }

    /**
     * Using reflection, read the private value of the passed field name for the passed object.
     *
     * @param obj the java object on which to read the private field value
     * @param fieldName the object member field for which to read the value
     * @return the private value for the field
     * @deprecated use {@link FieldUtils#readDeclaredField(Object, String, boolean)} instead
     */
    @Deprecated
    public static Object getPrivateField(Object obj, String fieldName)
    {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to get private field with name [{}]: [{}]", fieldName, e);

            return null;
        } finally {
        }
    }

    public static String getServerWikiPage(String servername)
    {
        return "XWiki.XWikiServer" + StringUtils.capitalize(servername);
    }

    /**
     * @param content the content of the text area
     * @param context see {@link XWikiContext}
     */
    public static String getTextArea(String content, XWikiContext context)
    {
        StringBuilder result = new StringBuilder();

        // Forcing a new line after the <textarea> tag, as
        // http://www.w3.org/TR/html4/appendix/notes.html#h-B.3.1 causes an empty line at the start
        // of the document content to be trimmed.
        result.append("<textarea name=\"content\" id=\"content\" rows=\"25\" cols=\"80\">\n");
        result.append(XMLUtils.escape(content));
        result.append("</textarea>");

        return result.toString();
    }

    /**
     * This provide a way to create an XWiki object without initializing the whole XWiki (including plugins, storage,
     * etc.).
     * <p>
     * Needed for tools or tests which need XWiki because it is used everywhere in the API.
     */
    public XWiki()
    {
        // Empty voluntarily
    }

    /**
     * Initialize all xwiki subsystems.
     *
     * @param context see {@link XWikiContext}
     * @param engineContext the XWiki object wrapping the {@link javax.servlet.ServletContext} and which allows to set
     *            data that live on as long as the XWiki webapp is not stopped in the Servlet Container
     * @param noupdate true if the whole initialization should be done (create mandatory xlcasses, initialize stats
     *            service), i.e. if this is not an update, and false otherwise
     * @throws XWikiException if an error happened during initialization (failure to initialize some cache for example)
     */
    public XWiki(XWikiContext context, XWikiEngineContext engineContext, boolean noupdate) throws XWikiException
    {
        initXWiki(context, engineContext, noupdate);
    }

    /**
     * Initialize all xwiki subsystems.
     *
     * @param context see {@link XWikiContext}
     * @throws XWikiException if an error happened during initialization (failure to initialize some cache for example)
     */
    public XWiki(XWikiContext context) throws XWikiException
    {
        this(context, null, false);
    }

    /**
     * Initialize all xwiki subsystems.
     *
     * @param context see {@link XWikiContext}
     * @param engineContext the XWiki object wrapping the {@link javax.servlet.ServletContext} and which allows to set
     *            data that live on as long as the XWiki webapp is not stopped in the Servlet Container
     * @param noupdate true if the whole initialization should be done (create mandatory xlcasses, initialize stats
     *            service), i.e. if this is not an update, and false otherwise
     * @throws XWikiException if an error happened during initialization (failure to initialize some cache for example)
     */
    public void initXWiki(XWikiContext context, XWikiEngineContext engineContext, boolean noupdate)
        throws XWikiException
    {
        initXWiki(null, context, engineContext, noupdate);
    }

    /**
     * Initialize all xwiki subsystems.
     *
     * @param config the object holding the XWiki configuration read from {@code xwiki.cfg}
     * @param context see {@link XWikiContext}
     * @param engineContext the XWiki object wrapping the {@link javax.servlet.ServletContext} and which allows to set
     *            data that live on as long as the XWiki webapp is not stopped in the Servlet Container
     * @param noupdate true if the whole initialization should be done (create mandatory xlcasses, initialize stats
     *            service), i.e. if this is not an update, and false otherwise
     * @throws XWikiException if an error happened during initialization (failure to initialize some cache for example)
     * @deprecated since 6.1M2, use {@link #initXWiki(XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public void initXWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engineContext, boolean noupdate)
        throws XWikiException
    {
        getProgress().pushLevelProgress(4, this);

        try {
            getProgress().startStep(this);

            setDatabase(context.getMainXWiki());

            setEngineContext(engineContext);
            context.setWiki(this);

            // "Pre-initialize" XWikiStubContextProvider with a XWikiContext containing a XWiki instance as soon as
            // possible
            Utils.<XWikiStubContextProvider>getComponent(XWikiStubContextProvider.class).initialize(context);

            // Prepare the store
            if (config != null) {
                setConfig(config);
            }

            XWikiStoreInterface mainStore = Utils.getComponent(XWikiStoreInterface.class,
                getConfiguration().getProperty("xwiki.store.main.hint", XWikiHibernateBaseStore.HINT));

            // Check if we need to use the cache store..
            boolean nocache = "0".equals(getConfiguration().getProperty("xwiki.store.cache", "1"));
            if (!nocache) {
                XWikiCacheStoreInterface cachestore = new XWikiCacheStore(mainStore, context);
                setStore(cachestore);
            } else {
                setStore(mainStore);
            }

            setCriteriaService((XWikiCriteriaService) createClassFromConfig("xwiki.criteria.class",
                "com.xpn.xwiki.criteria.impl.XWikiCriteriaServiceImpl", context));

            setDefaultAttachmentContentStore(
                Utils.<XWikiAttachmentStoreInterface>getComponent(XWikiAttachmentStoreInterface.class,
                    getConfiguration().getProperty("xwiki.store.attachment.hint", XWikiHibernateBaseStore.HINT)));

            setVersioningStore(Utils.<XWikiVersioningStoreInterface>getComponent(XWikiVersioningStoreInterface.class,
                getConfiguration().getProperty("xwiki.store.versioning.hint", XWikiHibernateBaseStore.HINT)));

            setDefaultAttachmentArchiveStore(Utils.<AttachmentVersioningStore>getComponent(
                AttachmentVersioningStore.class, hasAttachmentVersioning(context) ? getConfiguration()
                    .getProperty("xwiki.store.attachment.versioning.hint", XWikiHibernateBaseStore.HINT) : "void"));

            if (hasRecycleBin(context)) {
                setRecycleBinStore(
                    Utils.<XWikiRecycleBinStoreInterface>getComponent(XWikiRecycleBinStoreInterface.class,
                        getConfiguration().getProperty("xwiki.store.recyclebin.hint", XWikiHibernateBaseStore.HINT)));
            }

            if (hasAttachmentRecycleBin(context)) {
                setAttachmentRecycleBinStore(
                    Utils.<AttachmentRecycleBinStore>getComponent(AttachmentRecycleBinStore.class, getConfiguration()
                        .getProperty("xwiki.store.attachment.recyclebin.hint", XWikiHibernateBaseStore.HINT)));
            }

            // "Pre-initialize" XWikiStubContextProvider so that rendering engine, plugins or listeners reacting to
            // potential document changes can use it
            Utils.<XWikiStubContextProvider>getComponent(XWikiStubContextProvider.class).initialize(context);

            getProgress().endStep(this);

            getProgress().startStep(this);

            // Make sure these classes exists
            if (noupdate) {
                initializeMandatoryDocuments(context);
                getStatsService(context);
            }

            getProgress().endStep(this);

            getProgress().startStep(this);

            // Prepare the Plugin Engine
            preparePlugins(context);

            getProgress().endStep(this);

            getProgress().startStep(this);

            String ro = getConfiguration().getProperty("xwiki.readonly", "no");
            this.isReadOnly = ("yes".equalsIgnoreCase(ro) || "true".equalsIgnoreCase(ro) || "1".equalsIgnoreCase(ro));

            // Save the configured syntaxes
            String syntaxes = getConfiguration().getProperty("xwiki.rendering.syntaxes", "xwiki/1.0");
            this.configuredSyntaxes = Arrays.asList(StringUtils.split(syntaxes, " ,"));

            getObservationManager().addListener(this);
        } finally {
            getProgress().popLevelProgress(this);
        }
    }

    /**
     * Ensure that mandatory classes (ie classes XWiki needs to work properly) exist and create them if they don't
     * exist.
     *
     * @param context see {@link XWikiContext}
     */
    public void initializeMandatoryDocuments(XWikiContext context)
    {
        if (context.get("initdone") == null) {
            @SuppressWarnings("deprecation")
            List<MandatoryDocumentInitializer> initializers =
                Utils.getComponentList(MandatoryDocumentInitializer.class);

            // Sort the initializers based on priority. Lower priority values are first.
            Collections.sort(initializers, new Comparator<MandatoryDocumentInitializer>()
            {
                @Override
                public int compare(MandatoryDocumentInitializer left, MandatoryDocumentInitializer right)
                {
                    Priority leftPriority = left.getClass().getAnnotation(Priority.class);
                    int leftPriorityValue =
                        leftPriority != null ? leftPriority.value() : MandatoryDocumentInitializer.DEFAULT_PRIORITY;

                    Priority rightPriority = right.getClass().getAnnotation(Priority.class);
                    int rightPriorityValue =
                        rightPriority != null ? rightPriority.value() : MandatoryDocumentInitializer.DEFAULT_PRIORITY;

                    // Compare the two.
                    return leftPriorityValue - rightPriorityValue;
                }
            });

            for (MandatoryDocumentInitializer initializer : initializers) {
                initializeMandatoryDocument(initializer, context);
            }
        }
    }

    private void initializeMandatoryDocument(String wiki, MandatoryDocumentInitializer initializer,
        XWikiContext context)
    {
        String currentWiki = context.getWikiId();

        try {
            context.setWikiId(wiki);

            initializeMandatoryDocument(initializer, context);
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    private void initializeMandatoryDocument(MandatoryDocumentInitializer initializer, XWikiContext context)
    {
        try {
            DocumentReference documentReference =
                getCurrentReferenceDocumentReferenceResolver().resolve(initializer.getDocumentReference());

            if (documentReference.getWikiReference().getName().equals(context.getWikiId())) {
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);

                if (initializer.updateDocument(document)) {
                    saveDocument(document,
                        localizePlainOrKey("core.model.xclass.mandatoryUpdateProperty.versionSummary"), context);
                }
            }
        } catch (XWikiException e) {
            LOGGER.error("Failed to initialize mandatory document", e);
        }
    }

    public XWikiStoreInterface getNotCacheStore()
    {
        XWikiStoreInterface store = getStore();
        if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
        }
        return store;
    }

    public XWikiHibernateStore getHibernateStore()
    {
        XWikiStoreInterface store = getStore();
        if (store instanceof XWikiHibernateStore) {
            return (XWikiHibernateStore) store;
        } else if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
            if (store instanceof XWikiHibernateStore) {
                return (XWikiHibernateStore) store;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * @param wikiId the id of the wiki
     * @param context see {@link XWikiContext}
     * @deprecated since 8.4RC1, use {@link #initializeWiki(String, boolean, XWikiContext)} instead
     */
    @Deprecated
    public void updateDatabase(String wikiId, XWikiContext context) throws HibernateException, XWikiException
    {
        updateDatabase(wikiId, false, context);
    }

    /**
     * @param wikiId the id of the wiki
     * @param context see {@link XWikiContext}
     * @deprecated since 8.4RC1, use {@link #initializeWiki(String, boolean, XWikiContext)} instead
     */
    @Deprecated
    public void updateDatabase(String wikiId, boolean force, XWikiContext context)
        throws HibernateException, XWikiException
    {
        updateDatabase(wikiId, force, true, context);
    }

    /**
     * @param wikiId the id of the wiki
     * @param force if the update of the databse should be forced
     * @param initDocuments if mandatory document and plugin should be initialized for passed wiki
     * @param context see {@link XWikiContext}
     * @deprecated since 8.4RC1, use {@link #initializeWiki(String, boolean, XWikiContext)} instead
     */
    @Deprecated
    public void updateDatabase(String wikiId, boolean force, boolean initDocuments, XWikiContext context)
        throws HibernateException, XWikiException
    {
        initializeWiki(wikiId, true, context);
    }

    /**
     * @return a cached list of all active virtual wikis (i.e. wikis who have been hit by a user request). To get a full
     *         list of all virtual wikis database names use {@link WikiDescriptorManager#getAllIds()}.
     * @deprecated
     */
    @Deprecated
    public List<String> getVirtualWikiList()
    {
        return new ArrayList<>(this.initializedWikis.keySet());
    }

    /**
     * @param context see {@link XWikiContext}
     * @return the full list of all wiki names of all defined wikis. The wiki names are computed from the names of
     *         documents having a {@code XWiki.XWikiServerClass} object attached to them by removing the
     *         {@code XWiki.XWikiServer} prefix and making it lower case. For example a page named
     *         {@code XWiki.XWikiServerMyDatabase} would return {@code mydatabase} as the wiki name. This list will also
     *         contain the main wiki.
     *         <p>
     *         Note: the wiki name is commonly also the name of the database where the wiki's data is stored. However,
     *         if configured accordingly, the database can be diferent from the wiki name, like for example when setting
     *         a wiki database prefix.
     * @deprecated since 5.3, use {@link WikiDescriptorManager#getAllIds()} instead
     */
    @Deprecated
    public List<String> getVirtualWikisDatabaseNames(XWikiContext context) throws XWikiException
    {
        WikiDescriptorManager descriptorManager = Utils.getComponent(WikiDescriptorManager.class);

        try {
            return new ArrayList<String>(descriptorManager.getAllIds());
        } catch (WikiManagerException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to get the list of wikis", e);
        }
    }

    /**
     * @return the cache containing the names of the wikis already initialized.
     * @since 1.5M2.
     * @deprecated
     */
    @Deprecated
    public Cache<DocumentReference> getVirtualWikiCache()
    {
        return null;
    }

    /**
     * Get the reference of the owner for the provider wiki.
     *
     * @param wikiName the technical name of the wiki
     * @param context see {@link XWikiContext}
     * @return the wiki owner or null if none is set
     * @throws XWikiException failed to get wiki descriptor document
     */
    public String getWikiOwner(String wikiName, XWikiContext context) throws XWikiException
    {
        String wikiOwner;

        String currentdatabase = context.getWikiId();
        try {
            context.setWikiId(context.getMainXWiki());

            String serverwikipage = getServerWikiPage(wikiName);
            XWikiDocument doc = getDocument(serverwikipage, context);

            if (doc.isNew()) {
                if (!context.isMainWiki(wikiName)) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                        "The wiki " + wikiName + " does not exist");
                } else {
                    wikiOwner = null;
                }
            } else {
                wikiOwner = doc.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "owner");
                if (wikiOwner.indexOf(':') == -1) {
                    wikiOwner = context.getMainXWiki() + ":" + wikiOwner;
                }
            }
        } finally {
            context.setWikiId(currentdatabase);
        }

        return wikiOwner;
    }

    /**
     * @param context see {@link XWikiContext}
     */
    protected Object createClassFromConfig(String param, String defClass, XWikiContext context) throws XWikiException
    {
        String storeclass = getConfiguration().getProperty(param, defClass);
        try {
            Class<?>[] classes = new Class<?>[] { XWikiContext.class };
            Object[] args = new Object[] { context };
            Object result = Class.forName(storeclass).getConstructor(classes).newInstance(args);
            return result;
        } catch (Exception e) {
            Throwable ecause = e;
            if (e instanceof InvocationTargetException) {
                ecause = ((InvocationTargetException) e).getTargetException();
            }
            Object[] args = { param, storeclass };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR, "Cannot load class {1} from param {0}", ecause,
                args);
        }
    }

    private void preparePlugins(XWikiContext context)
    {
        setPluginManager(new XWikiPluginManager(getXWikiPreference("plugins", context), context));
        String plugins = getConfiguration().getProperty("xwiki.plugins", "");
        if (!plugins.equals("")) {
            getPluginManager().addPlugins(StringUtils.split(plugins, " ,"), context);
        }
    }

    /**
     * @return the XWiki core version as specified in the {@link #VERSION_FILE} file
     */
    @SuppressWarnings("deprecation")
    public String getVersion()
    {
        if (this.version == null) {
            try {
                InputStream is = getResourceAsStream(VERSION_FILE);
                try {
                    XWikiConfig properties = new XWikiConfig(is);
                    this.version = properties.getProperty(VERSION_FILE_PROPERTY);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            } catch (Exception e) {
                // Failed to retrieve the version, log a warning and default to "Unknown"
                LOGGER.warn("Failed to retrieve XWiki's version from [" + VERSION_FILE + "], using the ["
                    + VERSION_FILE_PROPERTY + "] property.", e);
                this.version = "Unknown version";
            }
        }
        return this.version;
    }

    public URL getResource(String s) throws MalformedURLException
    {
        return getEngineContext().getResource(s);
    }

    public InputStream getResourceAsStream(String s) throws MalformedURLException
    {
        InputStream is = getEngineContext().getResourceAsStream(s);
        if (is == null) {
            is = getEngineContext().getResourceAsStream("/" + s);
        }
        return is;
    }

    public String getResourceContent(String name) throws IOException
    {
        if (getEngineContext() != null) {
            try (InputStream is = getResourceAsStream(name)) {
                if (is != null) {
                    return IOUtils.toString(is, DEFAULT_ENCODING);
                }
            }
        }
        // Resources should always be encoded as UTF-8, to reduce the dependency on the system encoding
        return FileUtils.readFileToString(new File(name), DEFAULT_ENCODING);
    }

    public Date getResourceLastModificationDate(String name)
    {
        try {
            if (getEngineContext() != null) {
                return Util.getFileLastModificationDate(getEngineContext().getRealPath(name));
            }
        } catch (Exception ex) {
            // Probably a SecurityException or the file is not accessible (inside a war)
            LOGGER.info("Failed to get file modification date: " + ex.getMessage());
        }
        return new Date();
    }

    public byte[] getResourceContentAsBytes(String name) throws IOException
    {
        if (getEngineContext() != null) {
            try (InputStream is = getResourceAsStream(name)) {
                if (is != null) {
                    return IOUtils.toByteArray(is);
                }
            } catch (Exception e) {
            }
        }
        return FileUtils.readFileToByteArray(new File(name));
    }

    public boolean resourceExists(String name)
    {
        if (getEngineContext() != null) {
            try {
                if (getResource(name) != null) {
                    return true;
                }
            } catch (IOException e) {
            }
        }
        try {
            File file = new File(name);
            return file.exists();
        } catch (Exception e) {
            // Could be running under -security, which prevents calling file.exists().
        }
        return false;
    }

    public String getRealPath(String path)
    {
        return getEngineContext().getRealPath(path);
    }

    public String ParamAsRealPath(String key)
    {
        String param = getConfiguration().getProperty(key);
        try {
            return getRealPath(param);
        } catch (Exception e) {
            return param;
        }
    }

    /**
     * @param context see {@link XWikiContext}
     */
    public String ParamAsRealPath(String key, XWikiContext context)
    {
        return ParamAsRealPath(key);
    }

    public String ParamAsRealPathVerified(String param)
    {
        String path;
        File fpath;

        path = getConfiguration().getProperty(param);
        if (path == null) {
            return null;
        }

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        }

        path = getRealPath(path);
        if (path == null) {
            return null;
        }

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        } else {
        }
        return null;
    }

    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    /**
     * @deprecated since 9.9RC1, use {@link #getDefaultAttachmentContentStore()} instead
     */
    @Deprecated
    public XWikiAttachmentStoreInterface getAttachmentStore()
    {
        return getDefaultAttachmentContentStore();
    }

    /**
     * @return the store to use by default when saving a new attachment content
     * @since 9.10RC1
     */
    public XWikiAttachmentStoreInterface getDefaultAttachmentContentStore()
    {
        return this.defaultAttachmentContentStore;
    }

    /**
     * @return the store to use by default when saving a new attachment archive
     * @since 9.10RC1
     */
    public AttachmentVersioningStore getDefaultAttachmentArchiveStore()
    {
        return this.defaultAttachmentArchiveStore;
    }

    /**
     * @deprecated since 9.9RC1, use {@link #getDefaultAttachmentArchiveStore()} instead
     */
    @Deprecated
    public AttachmentVersioningStore getAttachmentVersioningStore()
    {
        return getDefaultAttachmentArchiveStore();
    }

    public XWikiVersioningStoreInterface getVersioningStore()
    {
        return this.versioningStore;
    }

    public XWikiRecycleBinStoreInterface getRecycleBinStore()
    {
        return this.recycleBinStore;
    }

    public AttachmentRecycleBinStore getAttachmentRecycleBinStore()
    {
        return this.attachmentRecycleBinStore;
    }

    /**
     * @param doc the document to save
     * @param context see {@link XWikiContext}
     */
    public void saveDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        // If no comment is provided we should use an empty comment
        saveDocument(doc, "", context);
    }

    /**
     * @param doc the document to save
     * @param comment the comment to associated to the new version of the saved document
     * @param context see {@link XWikiContext}
     */
    public void saveDocument(XWikiDocument doc, String comment, XWikiContext context) throws XWikiException
    {
        saveDocument(doc, comment, false, context);
    }

    /**
     * @param document the document to save
     * @param comment the comment to associated to the new version of the saved document
     * @param isMinorEdit true if the new version is a minor version
     * @param context see {@link XWikiContext}
     */
    public void saveDocument(XWikiDocument document, String comment, boolean isMinorEdit, XWikiContext context)
        throws XWikiException
    {
        String currentWiki = context.getWikiId();

        try {
            // Switch to document wiki
            context.setWikiId(document.getDocumentReference().getWikiReference().getName());

            // Setting comment & minor edit before saving
            document.setComment(StringUtils.defaultString(comment));
            document.setMinorEdit(isMinorEdit);

            // We need to save the original document since saveXWikiDoc() will reset it and we
            // need that original document for the notification below.
            XWikiDocument originalDocument = document.getOriginalDocument();

            // Make sure to always have an original document for listeners that need to compare with it.
            // The only case where we have a null original document is supposedly when the document
            // instance has been crafted and passed #saveDocument without using #getDocument
            // (which is not a good practice)
            if (originalDocument == null) {
                originalDocument =
                    getDocument(new DocumentReference(document.getDocumentReference(), document.getLocale()), context);
                document.setOriginalDocument(originalDocument);
            }

            ObservationManager om = getObservationManager();

            // Notify listeners about the document about to be created or updated

            // Note that for the moment the event being send is a bridge event, as we are still passing around
            // an XWikiDocument as source and an XWikiContext as data.

            if (om != null) {
                CancelableEvent documentEvent;
                if (originalDocument.isNew()) {
                    documentEvent = new DocumentCreatingEvent(document.getDocumentReference());
                } else {
                    documentEvent = new DocumentUpdatingEvent(document.getDocumentReference());
                }
                om.notify(documentEvent, document, context);

                // If the action has been canceled by the user then don't perform any save and throw an exception
                if (documentEvent.isCanceled()) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
                        String.format("An Event Listener has cancelled the document save for [%s]. Reason: [%s]",
                            document.getDocumentReference(), documentEvent.getReason()));
                }
            }

            // Put attachments to remove in recycle bin
            if (hasAttachmentRecycleBin(context)) {
                for (XWikiAttachmentToRemove attachment : document.getAttachmentsToRemove()) {
                    if (attachment.isToRecycleBin()) {
                        getAttachmentRecycleBinStore().saveToRecycleBin(attachment.getAttachment(), context.getUser(),
                            new Date(), context, true);
                    }
                }
            }

            // Actually save the document.
            getStore().saveXWikiDoc(document, context);

            // Since the store#saveXWikiDoc resets originalDocument, we need to temporarily put it
            // back to send notifications.
            XWikiDocument newOriginal = document.getOriginalDocument();

            try {
                document.setOriginalDocument(originalDocument);

                // Notify listeners about the document having been created or updated

                // First the legacy notification mechanism

                // Then the new observation module
                // Note that for the moment the event being send is a bridge event, as we are still passing around
                // an XWikiDocument as source and an XWikiContext as data.
                // The old version is made available using doc.getOriginalDocument()

                if (om != null) {
                    if (originalDocument.isNew()) {
                        om.notify(new DocumentCreatedEvent(document.getDocumentReference()), document, context);
                    } else {
                        om.notify(new DocumentUpdatedEvent(document.getDocumentReference()), document, context);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to send document save notification for document ["
                    + getDefaultEntityReferenceSerializer().serialize(document.getDocumentReference()) + "]", ex);
            } finally {
                document.setOriginalDocument(newOriginal);
            }
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    /**
     * Loads a XWikiDocument from the store.
     * <p>
     * Before 7.2M1 the reference is assumed to be a complete or incomplete document reference.
     * <p>
     * Since 7.2M1, the passed reference can be anything. If if a document child, the document reference will be
     * extracted from it. If it's a document parent it will be completed with the necessary default references (for
     * example if it's a space reference it will load the space home page).
     *
     * @param reference the reference of teh document
     * @param context see {@link XWikiContext}
     * @since 5.0M1
     */
    public XWikiDocument getDocument(EntityReference reference, XWikiContext context) throws XWikiException
    {
        return getDocument(getCurrentGetDocumentResolver().resolve(reference), context);
    }

    /**
     * @param doc the document
     * @param context see {@link XWikiContext}
     */
    public XWikiDocument getDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String currentWiki = context.getWikiId();
        try {
            context.setWikiId(doc.getDocumentReference().getWikiReference().getName());

            return getStore().loadXWikiDoc(doc, context);
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    /**
     * @param reference the reference of the document to load
     * @param revision the revision of the document to load
     * @param context the XWiki context
     * @return the document corresponding to the passed revision or a new XWikiDocument instance if none can be found
     * @throws XWikiException when failing to load the document revision
     * @since 9.4RC1
     * @deprecated sine 9.10RC1, use {@link DocumentRevisionProvider#getRevision(DocumentReference, String)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(DocumentReference reference, String revision, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument revisionDocument = getDocumentRevisionProvider().getRevision(reference, revision);

        if (revisionDocument == null && (revision.equals("1.1") || revision.equals("1.0"))) {
            revisionDocument = new XWikiDocument(reference);
        }

        return revisionDocument;
    }

    /**
     * @param document the reference document
     * @param revision the revision of the document to load
     * @param context the XWiki context
     * @return the document corresponding to the passed revision or a new XWikiDocument instance if none can be found
     * @throws XWikiException when failing to load the document revision
     * @deprecated sine 9.10RC1, use {@link DocumentRevisionProvider#getRevision(XWikiDocument, String)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(XWikiDocument document, String revision, XWikiContext context) throws XWikiException
    {
        XWikiDocument revisionDocument = getDocumentRevisionProvider().getRevision(document, revision);

        if (revisionDocument == null && (revision.equals("1.1") || revision.equals("1.0"))) {
            revisionDocument = new XWikiDocument(document.getDocumentReference());
        }

        return revisionDocument;
    }

    /**
     * @param reference the reference of the document
     * @param context see {@link XWikiContext}
     * @since 2.2M1
     */
    public XWikiDocument getDocument(DocumentReference reference, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(
            reference.getLocale() != null ? new DocumentReference(reference, null) : reference, reference.getLocale());

        doc.setContentDirty(true);

        return getDocument(doc, context);
    }

    /**
     * @param fullname the reference of the document as String
     * @param context see {@link XWikiContext}
     * @deprecated since 2.2M1 use {@link #getDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);
        return getDocument(doc, context);
    }

    /**
     * @param spaces the reference of the space as String
     * @param fullname the reference of the document as String
     * @param context see {@link XWikiContext}
     * @deprecated since 2.2M1 use {@link #getDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(String spaces, String fullname, XWikiContext context) throws XWikiException
    {
        int dotPosition = fullname.lastIndexOf('.');
        if (dotPosition != -1) {
            String spaceFromFullname = fullname.substring(0, dotPosition);
            String name = fullname.substring(dotPosition + 1);
            if (name.equals("")) {
                name = getDefaultPage(context);
            }
            return getDocument(spaceFromFullname + "." + name, context);
        } else {
            return getDocument(spaces + "." + fullname, context);
        }
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocuments(String, String)
     */
    public XWikiDeletedDocument[] getDeletedDocuments(String fullname, String locale, XWikiContext context)
        throws XWikiException
    {
        if (hasRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(getCurrentMixedDocumentReferenceResolver().resolve(fullname));
            doc.setLanguage(locale);
            return getRecycleBinStore().getAllDeletedDocuments(doc, context, true);
        } else {
            return null;
        }
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocuments(String)
     * @since 9.4RC1
     */
    public XWikiDeletedDocument[] getDeletedDocuments(String batchId, XWikiContext context) throws XWikiException
    {
        if (hasRecycleBin(context)) {
            return getRecycleBinStore().getAllDeletedDocuments(batchId, context, true);
        } else {
            return null;
        }
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocument(String, String, String)
     * @deprecated since 9.4RC1. Use {@link #getDeletedDocument(long, XWikiContext)} instead.
     */
    @Deprecated
    public XWikiDeletedDocument getDeletedDocument(String fullname, String locale, int index, XWikiContext context)
        throws XWikiException
    {
        return getDeletedDocument(index, context);
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocument(String)
     * @since 9.4RC1
     */
    public XWikiDeletedDocument getDeletedDocument(long index, XWikiContext context) throws XWikiException
    {
        if (hasRecycleBin(context)) {
            return getRecycleBinStore().getDeletedDocument(index, context, true);
        } else {
            return null;
        }
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document. Note that this does not distinguish
     * between different incarnations of a document name, and it does not require that the document still exists, it
     * returns all the attachments that at the time of their deletion had a document with the specified name as their
     * owner.
     *
     * @param docName the {@link XWikiDocument#getFullName() name} of the owner document
     * @param context see {@link XWikiContext}
     * @return A list with all the deleted attachments which belonged to the specified document. If no such attachments
     *         are found in the trash, an empty list is returned.
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName, XWikiContext context) throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(getCurrentMixedDocumentReferenceResolver().resolve(docName));
            return getAttachmentRecycleBinStore().getAllDeletedAttachments(doc, context, true);
        }
        return null;
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document and had the specified name. Multiple
     * versions can be returned since the same file can be uploaded and deleted several times, creating different
     * instances in the trash. Note that this does not distinguish between different incarnations of a document name,
     * and it does not require that the document still exists, it returns all the attachments that at the time of their
     * deletion had a document with the specified name as their owner.
     *
     * @param docName the {@link DeletedAttachment#getDocName() name of the document} the attachment belonged to
     * @param filename the {@link DeletedAttachment#getFilename() name} of the attachment to search for
     * @param context see {@link XWikiContext}
     * @return A list with all the deleted attachments which belonged to the specified document and had the specified
     *         filename. If no such attachments are found in the trash, an empty list is returned.
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName, String filename, XWikiContext context)
        throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(getCurrentMixedDocumentReferenceResolver().resolve(docName));
            XWikiAttachment attachment = new XWikiAttachment(doc, filename);
            return getAttachmentRecycleBinStore().getAllDeletedAttachments(attachment, context, true);
        }
        return null;
    }

    /**
     * Retrieve a specific attachment from the trash.
     *
     * @param id the unique identifier of the entry in the trash
     * @param context the XWiki context
     * @return specified attachment from the trash, {@code null} if not found
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public DeletedAttachment getDeletedAttachment(String id, XWikiContext context) throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            return getAttachmentRecycleBinStore().getDeletedAttachment(NumberUtils.toLong(id), context, true);
        }
        return null;
    }

    public MetaClass getMetaclass()
    {
        if (this.metaclass == null) {
            this.metaclass = MetaClass.getMetaClass();
        }
        return this.metaclass;
    }

    public void setMetaclass(MetaClass metaclass)
    {
        this.metaclass = metaclass;
    }

    /**
     * @param context see {@link XWikiContext}
     */
    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        List<String> result = getStore().getClassList(context);
        Collections.sort(result);
        return result;
    }

    /**
     * @param sql the sql query to execute
     * @param context see {@link XWikiContext}
     */
    public <T> List<T> search(String sql, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, context);
    }

    /**
     * @param sql the sql query to execute
     * @param nb limit the number of results to return
     * @param start the offset from which to start return results
     * @param context see {@link XWikiContext}
     */
    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, nb, start, context);
    }

    /**
     * @param sql the sql query to execute
     * @param context see {@link XWikiContext}
     */
    public <T> List<T> search(String sql, Object[][] whereParams, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, whereParams, context);
    }

    /**
     * @param sql the sql query to execute
     * @param nb limit the number of results to return
     * @param start the offset from which to start return results
     * @param context see {@link XWikiContext}
     */
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return getStore().search(sql, nb, start, whereParams, context);
    }

    /**
     * @param content the content to parse
     * @param context see {@link XWikiContext}
     * @deprecated Since 7.2M1. Use specific rendering/parsing options for the content type you want to parse/render.
     */
    @Deprecated
    public String parseContent(String content, XWikiContext context)
    {
        return getOldRendering().parseContent(content, context);
    }

    /**
     * @param template the name of the template
     * @param context see {@link XWikiContext}
     * @deprecated use {@link #evaluateTemplate(String, XWikiContext)} instead
     */
    @Deprecated
    public String parseTemplate(String template, XWikiContext context)
    {
        String result = "";

        try {
            result = evaluateTemplate(template, context);
        } catch (Exception e) {
            LOGGER.debug("Exception while parsing template [{}] from /templates/", template, e);
        }

        return result;
    }

    /**
     * Evaluate provided template content using velocity engine.
     *
     * @param template the template to evaluate
     * @param context see {@link XWikiContext}
     * @return the return of the velocity script
     * @throws IOException failed to get the template content
     * @since 2.2.2
     * @deprecated since 7.0M1, use {@link TemplateManager#render(String)} instead
     */
    @Deprecated
    public String evaluateTemplate(String template, XWikiContext context) throws IOException
    {
        try {
            return getTemplateManager().render(template);
        } catch (Exception e) {
            LOGGER.error("Error while evaluating velocity template [{}]", template, e);

            Object[] args = { template };
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION, "Error while evaluating velocity template {0}",
                e, args);

            return Util.getHTMLExceptionMessage(xe, context);
        }
    }

    /**
     * @param template the name of the template
     * @param skinId the id of the skin from which to load the template
     * @param context see {@link XWikiContext}
     * @deprecated since 7.0M1, use {@link TemplateManager#renderFromSkin(String, Skin)} instead
     */
    @Deprecated
    public String parseTemplate(String template, String skinId, XWikiContext context)
    {
        MutableRenderingContext mutableRenderingContext = getMutableRenderingContext();

        Syntax currentTargetSyntax = mutableRenderingContext.getTargetSyntax();
        try {
            // Force rendering with XHTML 1.0 syntax for retro-compatibility
            mutableRenderingContext.setTargetSyntax(Syntax.XHTML_1_0);

            Skin skin = getInternalSkinManager().getSkin(skinId);
            return getTemplateManager().renderFromSkin(template, skin);
        } catch (Exception e) {
            LOGGER.error("Error while evaluating velocity template [{}] skin [{}]", template, skinId, e);

            Object[] args = { template, skinId };
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION,
                "Error while evaluating velocity template [{0}] from skin [{1}]", e, args);

            return Util.getHTMLExceptionMessage(xe, context);
        } finally {
            mutableRenderingContext.setTargetSyntax(currentTargetSyntax);
        }
    }

    /**
     * @param template the name of the template
     * @param skin the id of the skin from which to load the template
     * @param context see {@link XWikiContext}
     */
    public String renderTemplate(String template, String skin, XWikiContext context)
    {
        try {
            return getOldRendering().renderTemplate(template, skin, context);
        } catch (Exception ex) {
            LOGGER.error("Failed to render template [" + template + "] for skin [" + skin + "]", ex);
            return parseTemplate(template, skin, context);
        }
    }

    /**
     * @param template the name of the template
     * @param context see {@link XWikiContext}
     */
    public String renderTemplate(String template, XWikiContext context)
    {
        try {
            return getOldRendering().renderTemplate(template, context);
        } catch (Exception ex) {
            LOGGER.error("Failed to render template [" + template + "]", ex);
            return parseTemplate(template, context);
        }
    }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity templates; works by creating a
     * RequestDispatcher, buffering the output, then returning it as a string.
     */
    public String invokeServletAndReturnAsString(String url, XWikiContext xwikiContext)
    {

        HttpServletRequest servletRequest = xwikiContext.getRequest();
        HttpServletResponse servletResponse = xwikiContext.getResponse();

        try {
            return IncludeServletAsString.invokeServletAndReturnAsString(url, servletRequest, servletResponse);
        } catch (Exception e) {
            LOGGER.warn("Exception including url: " + url, e);
            return "Exception including \"" + url + "\", see logs for details.";
        }

    }

    /**
     * @param iconName the standard name of an icon (it's not the name of the file on the filesystem, it's a generic
     *            name, for example "success" for a success icon
     * @param context see {@link XWikiContext}
     * @return the URL to the icon resource
     * @since 2.6M1
     */
    public String getIconURL(String iconName, XWikiContext context)
    {
        // TODO: Do a better mapping between generic icon name and physical resource name, especially to be independent
        // of the underlying icon library. Right now we assume it's the Silk icon library.
        return getSkinFile("icons/silk/" + iconName + ".png", context);
    }

    public String getSkinFile(String filename, XWikiContext context)
    {
        return getSkinFile(filename, false, context);
    }

    public String getSkinFile(String filename, boolean forceSkinAction, XWikiContext context)
    {
        XWikiURLFactory urlf = context.getURLFactory();

        try {
            // Try in the specified skin
            Skin skin = getInternalSkinManager().getCurrentSkin(true);
            if (skin != null) {
                Resource<?> resource = skin.getResource(filename);
                if (resource != null) {
                    return resource.getURL(forceSkinAction);
                }
            } else {
                // Try in the current parent skin
                Skin parentSkin = getInternalSkinManager().getCurrentParentSkin(true);
                if (parentSkin != null) {
                    Resource<?> resource = parentSkin.getResource(filename);
                    if (resource != null) {
                        return resource.getURL(forceSkinAction);
                    }
                }
            }

            // Look for a resource file
            if (resourceExists("/resources/" + filename)) {
                URL url = urlf.createResourceURL(filename, forceSkinAction, context);
                return urlf.getURL(url, context);
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while getting skin file [" + filename + "]", e);
            }
        }

        // If all else fails, use the default base skin, even if the URLs could be invalid.
        URL url;
        if (forceSkinAction) {
            url = urlf.createSkinURL(filename, "skins", getDefaultBaseSkin(context), context);
        } else {
            url = urlf.createSkinURL(filename, getDefaultBaseSkin(context), context);
        }
        return urlf.getURL(url, context);
    }

    public String getSkinFile(String filename, String skin, XWikiContext context)
    {
        return getSkinFile(filename, skin, false, context);
    }

    public String getSkinFile(String filename, String skinId, boolean forceSkinAction, XWikiContext context)
    {
        try {
            Skin skin = getInternalSkinManager().getSkin(skinId);

            Resource<?> resource = skin.getLocalResource(filename);

            if (resource != null) {
                return resource.getURL(forceSkinAction);
            }

            // Look for a resource file
            if (resourceExists("/resources/" + filename)) {
                XWikiURLFactory urlf = context.getURLFactory();

                URL url = urlf.createResourceURL(filename, forceSkinAction, context);
                return urlf.getURL(url, context);
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while getting skin file [{}] from skin [{}]", filename, skinId, e);
            }
        }

        return null;
    }

    /**
     * @deprecated since 7.0M1, use {@link SkinManager#getCurrentSkin(boolean)} instead
     */
    @Deprecated
    public String getSkin(XWikiContext context)
    {
        String skin;

        try {
            skin = getInternalSkinManager().getCurrentSkinId(true);
        } catch (Exception e) {
            LOGGER.debug("Exception while determining current skin", e);
            skin = getDefaultBaseSkin(context);
        }

        return skin;
    }

    public String getSkinPreference(String prefname, XWikiContext context)
    {
        return getSkinPreference(prefname, "", context);
    }

    public String getSkinPreference(String prefname, String defaultValue, XWikiContext context)
    {
        for (Skin skin = getInternalSkinManager().getCurrentSkin(true); skin != null; skin = skin.getParent()) {
            if (skin instanceof WikiSkin) {
                String value = getWikiSkinUtils().getSkinProperty(skin.getId(), prefname);

                // TODO: remove the NO_VALUE test when XWIKI-10853 is fixed
                if (!StringUtils.isEmpty(value) && !NO_VALUE.equals(value)) {
                    return value;
                }
            }
        }

        return defaultValue;
    }

    /**
     * @deprecated since 7.0M1, use {@link SkinManager#getDefaultParentSkin()} instead
     */
    @Deprecated
    public String getDefaultBaseSkin(XWikiContext context)
    {
        return getInternalSkinManager().getDefaultParentSkinId();
    }

    /**
     * @deprecated since 7.0M1
     */
    @Deprecated
    public String getBaseSkin(XWikiContext context)
    {
        return getBaseSkin(context, false);
    }

    /**
     * @deprecated since 7.0M1
     */
    @Deprecated
    public String getBaseSkin(XWikiContext context, boolean fromRenderSkin)
    {
        String baseskin = "";
        try {
            return getInternalSkinManager().getCurrentParentSkinId(false);
        } catch (Exception e) {
            baseskin = getDefaultBaseSkin(context);

            LOGGER.debug("Exception while determining base skin", e);
        }

        return baseskin;
    }

    /**
     * @param skin the name of the skin for which to return the base skin. For example : <tt>XWiki.DefaultSkin</tt>
     * @param context see {@link XWikiContext}
     * @return if found, the name of the base skin the asked skin inherits from. If not found, returns an empty string.
     * @since 2.0.2
     * @since 2.1M1
     * @deprecated since 7.0M1, use {@link SkinManager#getCurrentSkin(boolean)} and {@link Skin#getParent()} instead
     */
    @Deprecated
    public String getBaseSkin(String skin, XWikiContext context)
    {
        String baseSkin = getInternalSkinManager().getParentSkin(skin);

        return baseSkin != null ? baseSkin : "";
    }

    public String getSpaceCopyright(XWikiContext context)
    {
        return getSpacePreference("webcopyright", "", context);
    }

    public String getXWikiPreference(String prefname, XWikiContext context)
    {
        return getXWikiPreference(prefname, "", context);
    }

    /**
     * Obtain a preference value for the wiki, looking up first in the XWiki.XWikiPreferences document, then fallbacking
     * on a config parameter when the first lookup gives an empty string, then returning the default value if the config
     * parameter returned itself an empty string.
     *
     * @param prefname the parameter to look for in the XWiki.XWikiPreferences object in the XWiki.XWikiPreferences
     *            document of the wiki.
     * @param fallbackParam the parameter in xwiki.cfg to fallback on, in case the XWiki.XWikiPreferences object gave no
     *            result
     * @param defaultValue the default value to fallback on, in case both XWiki.XWikiPreferences and the fallback
     *            xwiki.cfg parameter gave no result
     */
    public String getXWikiPreference(String prefname, String fallbackParam, String defaultValue, XWikiContext context)
    {
        String result = getWikiConfiguration().getProperty(prefname, String.class);

        if (StringUtils.isEmpty(result)) {
            result = getConfiguration().getProperty(fallbackParam, defaultValue);
        }

        return result != null ? result : "";
    }

    /**
     * Obtain a preference value for the wiki, looking up first in the XWiki.XWikiPreferences document, then fallbacking
     * on a config parameter when the first lookup gives an empty string, then returning the default value if the config
     * parameter returned itself an empty string.
     *
     * @param prefname the parameter to look for in the XWiki.XWikiPreferences object in the XWiki.XWikiPreferences
     *            document of the wiki.
     * @param wiki the wiki to get preference from
     * @param fallbackParam the parameter in xwiki.cfg to fallback on, in case the XWiki.XWikiPreferences object gave no
     *            result
     * @param defaultValue the default value to fallback on, in case both XWiki.XWikiPreferences and the fallback
     *            xwiki.cfg parameter gave no result
     * @since 7.4M1
     */
    public String getXWikiPreference(String prefname, String wiki, String fallbackParam, String defaultValue,
        XWikiContext xcontext)
    {
        String currentWiki = xcontext.getWikiId();

        try {
            xcontext.setWikiId(wiki);

            return getXWikiPreference(prefname, fallbackParam, defaultValue, xcontext);
        } finally {
            xcontext.setWikiId(currentWiki);
        }
    }

    public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
    {
        return getXWikiPreference(prefname, "", defaultValue, context);
    }

    public String getSpacePreference(String preference, XWikiContext context)
    {
        return getSpacePreference(preference, "", context);
    }

    public String getSpacePreference(String preference, String defaultValue, XWikiContext context)
    {
        return getSpacePreference(preference, (SpaceReference) null, defaultValue, context);
    }

    /**
     * @deprecated since 7.4M1, use {@link #getSpacePreference(String, SpaceReference, String, XWikiContext)} instead
     */
    @Deprecated
    public String getSpacePreference(String preference, String space, String defaultValue, XWikiContext context)
    {
        return getSpacePreference(preference, new SpaceReference(space, context.getWikiReference()), defaultValue,
            context);
    }

    /**
     * Get the reference of the space and fallback on parent space or wiki in case nothing is found.
     * <p>
     * If the property is not set on any level then empty String is returned.
     * 
     * @param preferenceKey the name of the preference key
     * @param spaceReference the reference of the space
     * @param context see {@link XWikiContext}
     * @return the value of the preference or empty String if it could not be found
     * @since 7.4M1
     */
    public String getSpacePreference(String preferenceKey, SpaceReference spaceReference, XWikiContext context)
    {
        return getSpacePreference(preferenceKey, spaceReference, "", context);
    }

    /**
     * Get the reference of the space and fallback on parent space or wiki in case nothing is found.
     * <p>
     * If the property is not set on any level then <code>defaultValue</code> is returned.
     * 
     * @param preferenceKey the name of the preference key
     * @param spaceReference the reference of the space
     * @param defaultValue the value to return if the preference can't be found
     * @param context see {@link XWikiContext}
     * @return the value of the preference or <code>defaultValue</code> if it could not be found
     * @since 7.4M1
     */
    public String getSpacePreference(String preferenceKey, SpaceReference spaceReference, String defaultValue,
        XWikiContext context)
    {
        XWikiDocument currentDocument = context.getDoc();

        try {
            if (spaceReference != null) {
                context.setDoc(new XWikiDocument(new DocumentReference("WebPreferences", spaceReference)));
            } else if (currentDocument != null) {
                spaceReference = currentDocument.getDocumentReference().getLastSpaceReference();
            }

            String result = getSpaceConfiguration().getProperty(preferenceKey, String.class);

            if (StringUtils.isEmpty(result)) {
                if (spaceReference == null) {
                    result = getXWikiPreference(preferenceKey, defaultValue, context);
                } else if (spaceReference.getParent() instanceof SpaceReference) {
                    result = getSpacePreference(preferenceKey, (SpaceReference) spaceReference.getParent(),
                        defaultValue, context);
                } else if (spaceReference.getParent() instanceof WikiReference) {
                    result =
                        getXWikiPreference(preferenceKey, spaceReference.getParent().getName(), defaultValue, context);
                }
            }

            return result != null ? result : defaultValue;
        } finally {
            context.setDoc(currentDocument);
        }
    }

    public String getUserPreference(String prefname, XWikiContext context)
    {
        String result = getUserConfiguration().getProperty(prefname, String.class);

        if (StringUtils.isEmpty(result)) {
            result = getSpacePreference(prefname, context);
        }

        return result != null ? result : "";
    }

    public String getUserPreferenceFromCookie(String prefname, XWikiContext context)
    {
        Cookie[] cookies = context.getRequest().getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if (name.equals(prefname)) {
                String value = cookie.getValue();
                if (!value.trim().equals("")) {
                    return value;
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public String getUserPreference(String prefname, boolean useCookie, XWikiContext context)
    {
        // First we look in the cookies
        if (useCookie) {
            String result = Util.normalizeLanguage(getUserPreferenceFromCookie(prefname, context));
            if (result != null) {
                return result;
            }
        }
        return getUserPreference(prefname, context);
    }

    /**
     * First try to find the current locale in use from the XWiki context. If none is used and if the wiki is not
     * multilingual use the default locale defined in the XWiki preferences. If the wiki is multilingual try to get the
     * locale passed in the request. If none was passed try to get it from a cookie. If no locale cookie exists then use
     * the user default locale and barring that use the browser's "Accept-Language" header sent in HTTP request. If none
     * is defined use the default locale.
     *
     * @return the locale to use
     * @since 8.0M1
     */
    public Locale getLocalePreference(XWikiContext context)
    {
        String language = getLanguagePreference(context);

        return LocaleUtils.toLocale(language);
    }

    /**
     * First try to find the current locale in use from the XWiki context. If none is used and if the wiki is not
     * multilingual use the default locale defined in the XWiki preferences. If the wiki is multilingual try to get the
     * locale passed in the request. If none was passed try to get it from a cookie. If no locale cookie exists then use
     * the user default locale and barring that use the browser's "Accept-Language" header sent in HTTP request. If none
     * is defined use the default locale.
     *
     * @return the locale to use
     * @deprecated since 8.0M1, use {@link #getLocalePreference(XWikiContext)} instead
     */
    @Deprecated
    // TODO: move implementation to #getLocalePreference
    public String getLanguagePreference(XWikiContext context)
    {
        // First we try to get the language from the XWiki Context. This is the current language
        // being used.
        String language = context.getLanguage();
        if (language != null) {
            return language;
        }

        String defaultLanguage = getDefaultLanguage(context);

        // If the wiki is non multilingual then the language is the default language.
        if (!context.getWiki().isMultiLingual(context)) {
            language = defaultLanguage;
            context.setLanguage(language);
            return language;
        }

        // As the wiki is multilingual try to find the language to use from the request by looking
        // for a language parameter. If the language value is "default" use the default language
        // from the XWiki preferences settings. Otherwise set a cookie to remember the language
        // in use.
        try {
            language = Util.normalizeLanguage(context.getRequest().getParameter("language"));
            if ((language != null) && (!language.equals(""))) {
                if (language.equals("default")) {
                    // forgetting language cookie
                    Cookie cookie = new Cookie("language", "");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    context.getResponse().addCookie(cookie);
                    language = defaultLanguage;
                } else {
                    // setting language cookie
                    Cookie cookie = new Cookie("language", language);
                    cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                    cookie.setPath("/");
                    context.getResponse().addCookie(cookie);
                }
                context.setLanguage(language);
                return language;
            }
        } catch (Exception e) {
        }

        // As no language parameter was passed in the request, try to get the language to use
        // from a cookie.
        try {
            // First we get the language from the cookie
            language = Util.normalizeLanguage(getUserPreferenceFromCookie("language", context));
            if ((language != null) && (!language.equals(""))) {
                context.setLanguage(language);
                return language;
            }
        } catch (Exception e) {
        }

        // Next from the default user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = null;
            userdoc = getDocument(user, context);
            if (userdoc != null) {
                language = Util.normalizeLanguage(userdoc.getStringValue("XWiki.XWikiUsers", "default_language"));
                if (!language.equals("")) {
                    context.setLanguage(language);
                    return language;
                }
            }
        } catch (XWikiException e) {
        }

        // If the default language is preferred, and since the user didn't explicitly ask for a
        // language already, then use the default wiki language.
        if (getConfiguration().getProperty("xwiki.language.preferDefault", "0").equals("1")
            || getSpacePreference("preferDefaultLanguage", "0", context).equals("1")) {
            language = defaultLanguage;
            context.setLanguage(language);
            return language;
        }

        // Then from the navigator language setting
        if (context.getRequest() != null) {
            String acceptHeader = context.getRequest().getHeader("Accept-Language");
            // If the client didn't specify some languages, skip this phase
            if ((acceptHeader != null) && (!acceptHeader.equals(""))) {
                List<String> acceptedLanguages = getAcceptedLanguages(context.getRequest());
                // We can force one of the configured languages to be accepted
                if (getConfiguration().getProperty("xwiki.language.forceSupported", "0").equals("1")) {
                    List<String> available = Arrays.asList(getXWikiPreference("languages", context).split("[, |]"));
                    // Filter only configured languages
                    acceptedLanguages.retainAll(available);
                }
                if (acceptedLanguages.size() > 0) {
                    // Use the "most-preferred" language, as requested by the client.
                    context.setLanguage(acceptedLanguages.get(0));
                    return acceptedLanguages.get(0);
                }
                // If none of the languages requested by the client is acceptable, skip to next
                // phase (use default language).
            }
        }

        // Finally, use the default language from the global preferences.
        context.setLanguage(defaultLanguage);
        return defaultLanguage;
    }

    /**
     * Construct a list of language codes (ISO 639-1) from the Accept-Languages header. This method filters out some
     * bugs in different browsers or containers, like returning '*' as a language (Jetty) or using '_' as a
     * language--country delimiter (some versions of Opera).
     *
     * @param request The client request.
     * @return A list of language codes, in the client preference order; might be empty if the header is not well
     *         formed.
     */
    private List<String> getAcceptedLanguages(XWikiRequest request)
    {
        List<String> result = new ArrayList<String>();
        Enumeration<Locale> e = request.getLocales();
        while (e.hasMoreElements()) {
            String language = e.nextElement().getLanguage().toLowerCase();
            // All language codes should have 2 letters.
            if (StringUtils.isAlpha(language)) {
                result.add(language);
            }
        }
        return result;
    }

    /**
     * @deprecated since 5.1M2 use {@link #getDefaultLocale(XWikiContext)} instead
     */
    @Deprecated
    public String getDefaultLanguage(XWikiContext xcontext)
    {
        return getDefaultLocale(xcontext).toString();
    }

    /**
     * The default locale in the preferences.
     *
     * @param xcontext the XWiki context.
     * @return the default locale
     * @since 5.1M2
     */
    public Locale getDefaultLocale(XWikiContext xcontext)
    {
        // Find out what is the default language from the XWiki preferences settings.
        String defaultLanguage = xcontext.getWiki().getXWikiPreference("default_language", "", xcontext);

        Locale defaultLocale;

        if (StringUtils.isBlank(defaultLanguage)) {
            defaultLocale = Locale.ENGLISH;
        } else {
            try {
                defaultLocale = LocaleUtils.toLocale(Util.normalizeLanguage(defaultLanguage));
            } catch (Exception e) {
                LOGGER.warn("Invalid locale [{}] set as default locale in the preferences", defaultLanguage);
                defaultLocale = Locale.ENGLISH;
            }
        }

        return defaultLocale;
    }

    /**
     * Get the available locales according to the preferences.
     *
     * @param xcontext the XWiki context
     * @return all the available locales
     * @since 5.1M2
     */
    public List<Locale> getAvailableLocales(XWikiContext xcontext)
    {
        String[] languages = StringUtils.split(xcontext.getWiki().getXWikiPreference("languages", xcontext), ", |");

        List<Locale> locales = new ArrayList<Locale>(languages.length);

        for (String language : languages) {
            if (StringUtils.isNotBlank(language)) {
                try {
                    locales.add(LocaleUtils.toLocale(language));
                } catch (Exception e) {
                    LOGGER.warn("Invalid locale [{}] listed as available in the preferences", language);
                }
            }
        }

        // Add default language in case it's not listed as available (which is wrong but it happen)
        Locale defaultocale = getDefaultLocale(xcontext);
        if (!locales.contains(defaultocale)) {
            locales.add(defaultocale);
        }

        return locales;
    }

    /**
     * @since 8.0M1
     */
    public Locale getDocLocalePreferenceNew(XWikiContext context)
    {
        String language = getDocLanguagePreferenceNew(context);

        return LocaleUtils.toLocale(language);
    }

    /**
     * @deprecated since 8.0M1, use {@link #getDocLocalePreferenceNew(XWikiContext)} instead
     */
    @Deprecated
    // TODO: move implementation to #getDocLocalePreferenceNew
    public String getDocLanguagePreferenceNew(XWikiContext context)
    {
        // Get context language
        String contextLanguage = context.getLanguage();
        // If the language exists in the context, it was previously set by another call
        if (contextLanguage != null && contextLanguage != "") {
            return contextLanguage;
        }

        String language = "", requestLanguage = "", userPreferenceLanguage = "", navigatorLanguage = "",
            cookieLanguage = "";
        boolean setCookie = false;

        if (!context.getWiki().isMultiLingual(context)) {
            language = context.getWiki().getXWikiPreference("default_language", "", context);
            context.setLanguage(language);
            return language;
        }

        // Get request language
        try {
            requestLanguage = Util.normalizeLanguage(context.getRequest().getParameter("language"));
        } catch (Exception ex) {
        }

        // Get user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = getDocument(user, context);
            if (userdoc != null) {
                userPreferenceLanguage = userdoc.getStringValue("XWiki.XWikiUsers", "default_language");
            }
        } catch (XWikiException e) {
        }

        // Get navigator language setting
        if (context.getRequest() != null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept != null) && (!accept.equals(""))) {
                String[] alist = StringUtils.split(accept, ",;-");
                if ((alist != null) && !(alist.length == 0)) {
                    context.setLanguage(alist[0]);
                    navigatorLanguage = alist[0];
                }
            }
        }

        // Get language from cookie
        try {
            cookieLanguage = Util.normalizeLanguage(getUserPreferenceFromCookie("language", context));
        } catch (Exception e) {
        }

        // Determine which language to use
        // First we get the language from the request
        if (StringUtils.isNotEmpty(requestLanguage)) {
            if (requestLanguage.equals("default")) {
                setCookie = true;
            } else {
                language = requestLanguage;
                context.setLanguage(language);
                Cookie cookie = new Cookie("language", language);
                cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                cookie.setPath("/");
                context.getResponse().addCookie(cookie);
                return language;
            }
        }
        // Next we get the language from the cookie
        if (StringUtils.isNotEmpty(cookieLanguage)) {
            language = cookieLanguage;
        }
        // Next from the default user preference
        else if (StringUtils.isNotEmpty(userPreferenceLanguage)) {
            language = userPreferenceLanguage;
        }
        // Then from the navigator language setting
        else if (StringUtils.isNotEmpty(navigatorLanguage)) {
            language = navigatorLanguage;
        }
        context.setLanguage(language);
        if (setCookie) {
            Cookie cookie = new Cookie("language", language);
            cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
            cookie.setPath("/");
            context.getResponse().addCookie(cookie);
        }
        return language;
    }

    /**
     * @since 8.0M1
     */
    public Locale getInterfaceLocalePreference(XWikiContext context)
    {
        String language = getInterfaceLanguagePreference(context);

        return LocaleUtils.toLocale(language);
    }

    /**
     * @deprecated since 8.0M1, use {@link #getInterfaceLocalePreference(XWikiContext)} instead
     */
    @Deprecated
    // TODO: move implementation to #getInterfaceLocalePreference
    public String getInterfaceLanguagePreference(XWikiContext context)
    {
        String language = "", requestLanguage = "", userPreferenceLanguage = "", navigatorLanguage = "",
            cookieLanguage = "", contextLanguage = "";
        boolean setCookie = false;

        if (!context.getWiki().isMultiLingual(context)) {
            language = Util.normalizeLanguage(context.getWiki().getXWikiPreference("default_language", "", context));
            context.setInterfaceLanguage(language);
            return language;
        }

        // Get request language
        try {
            requestLanguage = Util.normalizeLanguage(context.getRequest().getParameter("interfacelanguage"));
        } catch (Exception ex) {
        }

        // Get context language
        contextLanguage = context.getInterfaceLanguage();

        // Get user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = null;
            userdoc = getDocument(user, context);
            if (userdoc != null) {
                userPreferenceLanguage = userdoc.getStringValue("XWiki.XWikiUsers", "default_interface_language");
            }
        } catch (XWikiException e) {
        }

        // Get navigator language setting
        if (context.getRequest() != null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept != null) && (!accept.equals(""))) {
                String[] alist = StringUtils.split(accept, ",;-");
                if ((alist != null) && !(alist.length == 0)) {
                    context.setLanguage(alist[0]);
                    navigatorLanguage = alist[0];
                }
            }
        }

        // Get language from cookie
        try {
            cookieLanguage = Util.normalizeLanguage(getUserPreferenceFromCookie("interfacelanguage", context));
        } catch (Exception e) {
        }

        // Determine which language to use
        // First we get the language from the request
        if ((requestLanguage != null) && (!requestLanguage.equals(""))) {
            if (requestLanguage.equals("default")) {
                setCookie = true;
            } else {
                language = requestLanguage;
                context.setLanguage(language);
                Cookie cookie = new Cookie("interfacelanguage", language);
                cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                cookie.setPath("/");
                context.getResponse().addCookie(cookie);
                return language;
            }
        }
        // Next we get the language from the context
        if (contextLanguage != null && contextLanguage != "") {
            language = contextLanguage;
        }
        // Next we get the language from the cookie
        else if (StringUtils.isNotEmpty(cookieLanguage)) {
            language = cookieLanguage;
        }
        // Next from the default user preference
        else if (StringUtils.isNotEmpty(userPreferenceLanguage)) {
            language = userPreferenceLanguage;
        }
        // Then from the navigator language setting
        else if (StringUtils.isNotEmpty(navigatorLanguage)) {
            language = navigatorLanguage;
        }
        context.setLanguage(language);
        if (setCookie) {
            Cookie cookie = new Cookie("interfacelanguage", language);
            cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
            cookie.setPath("/");
            context.getResponse().addCookie(cookie);
        }
        return language;
    }

    public long getXWikiPreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getXWikiPreference(preference, context));
    }

    public long getSpacePreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getSpacePreference(preference, context));
    }

    public long getXWikiPreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return NumberUtils.toLong((getXWikiPreference(preference, context)), defaultValue);
    }

    public long getXWikiPreferenceAsLong(String preference, String fallbackParameter, long defaultValue,
        XWikiContext context)
    {
        return NumberUtils.toLong(getXWikiPreference(preference, fallbackParameter, "", context), defaultValue);
    }

    public long getSpacePreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return NumberUtils.toLong(getSpacePreference(preference, context), defaultValue);
    }

    public long getUserPreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getUserPreference(preference, context));
    }

    public int getXWikiPreferenceAsInt(String preference, XWikiContext context)
    {
        return Integer.parseInt(getXWikiPreference(preference, context));
    }

    public int getSpacePreferenceAsInt(String preference, XWikiContext context)
    {
        return Integer.parseInt(getSpacePreference(preference, context));
    }

    public int getXWikiPreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return NumberUtils.toInt(getXWikiPreference(preference, context), defaultValue);
    }

    public int getXWikiPreferenceAsInt(String preference, String fallbackParameter, int defaultValue,
        XWikiContext context)
    {
        return NumberUtils.toInt(getXWikiPreference(preference, fallbackParameter, "", context), defaultValue);
    }

    public int getSpacePreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return NumberUtils.toInt(getSpacePreference(preference, context), defaultValue);
    }

    public int getUserPreferenceAsInt(String prefname, XWikiContext context)
    {
        return Integer.parseInt(getUserPreference(prefname, context));
    }

    public void flushCache(XWikiContext context)
    {
        // We need to flush the virtual wiki list
        this.initializedWikis = new ConcurrentHashMap<>();

        // We need to flush the group service cache
        if (this.groupService != null) {
            this.groupService.flushCache();
        }

        // If we use the Cache Store layer.. we need to flush it
        XWikiStoreInterface store = getStore();
        if ((store != null) && (store instanceof XWikiCacheStoreInterface)) {
            ((XWikiCacheStoreInterface) getStore()).flushCache();
        }
        // Flush renderers.. Groovy renderer has a cache
        getOldRendering().flushCache();
        getParseGroovyFromString().flushCache();

        XWikiPluginManager pmanager = getPluginManager();
        if (pmanager != null) {
            pmanager.flushCache(context);
        }

        // Make sure we call all classes flushCache function
        try {
            List<String> classes = getClassList(context);
            for (int i = 0; i < classes.size(); i++) {
                String className = classes.get(i);
                try {
                    getClass(className, context).flushCache();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

    }

    public XWikiPluginManager getPluginManager()
    {
        return this.pluginManager;
    }

    public void setPluginManager(XWikiPluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    /**
     * @param attachmentContentStore the store to use by default when saving a new attachment content
     * @since 9.10RC1
     */
    public void setDefaultAttachmentContentStore(XWikiAttachmentStoreInterface attachmentContentStore)
    {
        this.defaultAttachmentContentStore = attachmentContentStore;
    }

    /**
     * @deprecated since 9.9RC1, use {@link #setDefaultAttachmentContentStore(XWikiAttachmentStoreInterface)} instead
     */
    @Deprecated
    public void setAttachmentStore(XWikiAttachmentStoreInterface attachmentStore)
    {
        this.defaultAttachmentContentStore = attachmentStore;
    }

    /**
     * @param attachmentArchiveStore the store to use by default when saving a new attachment archive
     * @since 9.10RC1
     */
    public void setDefaultAttachmentArchiveStore(AttachmentVersioningStore attachmentArchiveStore)
    {
        this.defaultAttachmentArchiveStore = attachmentArchiveStore;
    }

    /**
     * @deprecated since 9.10RC1, use {@link #setDefaultAttachmentArchiveStore(AttachmentVersioningStore)} instead
     */
    @Deprecated
    public void setAttachmentVersioningStore(AttachmentVersioningStore attachmentArchiveStore)
    {
        setDefaultAttachmentArchiveStore(attachmentArchiveStore);
    }

    public void setVersioningStore(XWikiVersioningStoreInterface versioningStore)
    {
        this.versioningStore = versioningStore;
    }

    public void setRecycleBinStore(XWikiRecycleBinStoreInterface recycleBinStore)
    {
        this.recycleBinStore = recycleBinStore;
    }

    public void setAttachmentRecycleBinStore(AttachmentRecycleBinStore attachmentRecycleBinStore)
    {
        this.attachmentRecycleBinStore = attachmentRecycleBinStore;
    }

    public void setCriteriaService(XWikiCriteriaService criteriaService)
    {
        this.criteriaService = criteriaService;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * Verify if the provided xclass page exists and that it contains all the required configuration properties to make
     * the tag feature work properly. If some properties are missing they are created and saved in the database.
     *
     * @param context see {@link XWikiContext}
     * @param classReference the reference of the document containing the class
     * @return the Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the database
     */
    private BaseClass getMandatoryClass(XWikiContext context, DocumentReference classReference) throws XWikiException
    {
        XWikiDocument document = getDocument(classReference, context);

        if (context.get("initdone") == null) {
            @SuppressWarnings("deprecation")
            MandatoryDocumentInitializer initializer =
                Utils.getComponent(MandatoryDocumentInitializer.class, document.getFullName());

            if (initializer.updateDocument(document)) {
                saveDocument(document, localizePlainOrKey("core.model.xclass.mandatoryUpdateProperty.versionSummary"),
                    context);
            }
        }

        return document.getXClass();
    }

    /**
     * Verify if the <code>XWiki.TagClass</code> page exists and that it contains all the required configuration
     * properties to make the tag feature work properly. If some properties are missing they are created and saved in
     * the database.
     *
     * @param context see {@link XWikiContext}
     * @return the TagClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getTagClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "TagClass"));
    }

    /**
     * Verify if the <code>XWiki.SheetClass</code> page exists and that it contains all the required configuration
     * properties to make the sheet feature work properly. If some properties are missing they are created and saved in
     * the database. SheetClass is used to a page as a sheet. When a page is tagged as a sheet and that page is included
     * in another page using the include macro then editing it triggers automatic inline edition (for XWiki Syntax 2.0
     * only - for XWiki Syntax 1.0 automatic inline edition is triggered using #includeForm).
     *
     * @param context see {@link XWikiContext}
     * @return the SheetClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the database
     * @deprecated since 3.1M2 edit mode class should be used for this purpose, not the sheet class
     * @see #getEditModeClass(XWikiContext)
     */
    @Deprecated
    public BaseClass getSheetClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "SheetClass"));
    }

    /**
     * Verify if the {@code XWiki.EditModeClass} page exists and that it contains all the required configuration
     * properties to make the edit mode feature work properly. If some properties are missing they are created and saved
     * in the database. EditModeClass is used to specify the default edit mode of a page. It can also be used to mark a
     * page as a sheet. When a page is marked as a sheet and that page is included in another page using the include
     * macro then editing it triggers automatic inline edition (for XWiki Syntax 2.0 only - for XWiki Syntax 1.0
     * automatic inline edition is triggered using #includeForm). It replaces and enhances the SheetClass mechanism (see
     * {@link #getSheetClass(XWikiContext)}).
     *
     * @param context see {@link XWikiContext}
     * @return the EditModeClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the database
     * @since 3.1M2
     */
    public BaseClass getEditModeClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(
            new LocalDocumentReference(XWikiConstant.EDIT_MODE_CLASS), new WikiReference(context.getWikiId())));
    }

    /**
     * Verify if the <code>XWiki.XWikiUsers</code> page exists and that it contains all the required configuration
     * properties to make the user feature work properly. If some properties are missing they are created and saved in
     * the database.
     *
     * @param context see {@link XWikiContext}
     * @return the XWikiUsers Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getUserClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "XWikiUsers"));
    }

    /**
     * Verify if the <code>XWiki.GlobalRedirect</code> page exists and that it contains all the required configuration
     * properties to make the redirection feature work properly. If some properties are missing they are created and
     * saved in the database.
     *
     * @param context see {@link XWikiContext}
     * @return the GlobalRedirect Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getRedirectClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "GlobalRedirect"));
    }

    /**
     * Verify if the <code>XWiki.XWikiPreferences</code> page exists and that it contains all the required configuration
     * properties to make XWiki work properly. If some properties are missing they are created and saved in the
     * database.
     *
     * @param context see {@link XWikiContext}
     * @return the XWiki Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getPrefsClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, getPreferencesDocumentReference(context));
    }

    public BaseClass getGroupClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "XWikiGroups"));
    }

    public BaseClass getRightsClass(String pagename, XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, pagename));
    }

    public BaseClass getRightsClass(XWikiContext context) throws XWikiException
    {
        return getRightsClass("XWikiRights", context);
    }

    public BaseClass getGlobalRightsClass(XWikiContext context) throws XWikiException
    {
        return getRightsClass("XWikiGlobalRights", context);
    }

    public BaseClass getCommentsClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "XWikiComments"));
    }

    public BaseClass getSkinClass(XWikiContext context) throws XWikiException
    {
        return getMandatoryClass(context, new DocumentReference(context.getWikiId(), SYSTEM_SPACE, "XWikiSkins"));
    }

    public int createUser(XWikiContext context) throws XWikiException
    {
        return createUser(false, "edit", context);
    }

    public int validateUser(boolean withConfirmEmail, XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            // Get the user document
            String username = convertUsername(request.getParameter("xwikiname"), context);
            if (username.indexOf('.') == -1) {
                username = "XWiki." + username;
            }
            XWikiDocument userDocument = getDocument(username, context);

            // Get the stored validation key
            BaseObject userObject = userDocument.getObject("XWiki.XWikiUsers", 0);
            String storedKey = userObject.getStringValue("validkey");

            // Get the validation key from the URL
            String validationKey = request.getParameter("validkey");
            PropertyInterface validationKeyClass = getClass("XWiki.XWikiUsers", context).get("validkey");
            if (validationKeyClass instanceof PasswordClass) {
                validationKey = ((PasswordClass) validationKeyClass).getEquivalentPassword(storedKey, validationKey);
            }

            // Compare the two keys
            if ((!storedKey.equals("") && (storedKey.equals(validationKey)))) {
                userObject.setIntValue("active", 1);
                saveDocument(userDocument, context);

                if (withConfirmEmail) {
                    String email = userObject.getStringValue("email");
                    String password = userObject.getStringValue("password");
                    sendValidationEmail(username, password, email, request.getParameter("validkey"),
                        "confirmation_email_content", context);
                }

                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_VALIDATE_USER,
                "Exception while validating user", e, null);
        }
    }

    public int createUser(boolean withValidation, String userRights, XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            Map<String, String[]> map = Util.getObject(request, "register");

            String content = "";
            Syntax syntax = getDefaultDocumentSyntaxInternal();

            // Read the values from the request.
            String xwikiname = request.getParameter("xwikiname");
            String password2 = request.getParameter("register2_password");
            String password = (map.get("password"))[0];
            String email = (map.get("email"))[0];
            String template = request.getParameter("template");
            String parent = request.getParameter("parent");
            String validkey = null;

            // Validate the values.
            if (XWikiRightService.SUPERADMIN_USER.equalsIgnoreCase(xwikiname)) {
                return -8;
            }
            try {
                if (!context.getUtil().match(getConfiguration().getProperty("xwiki.validusername", "/^[a-zA-Z0-9_]+$/"),
                    xwikiname)) {
                    return -4;
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Invalid regular expression for xwiki.validusername", ex);
                if (!context.getUtil().match("/^[a-zA-Z0-9_]+$/", xwikiname)) {
                    return -4;
                }
            }

            if ((!password.equals(password2)) || (password.trim().equals(""))) {
                // TODO: throw wrong password exception
                return -2;
            }

            if ((template != null) && (!template.equals(""))) {
                XWikiDocument tdoc = getDocument(template, context);
                if ((!tdoc.isNew())) {
                    // FIXME: This ignores template objects, attachments, etc.
                    content = tdoc.getContent();
                    syntax = tdoc.getSyntax();
                }
            }

            if ((parent == null) || (parent.equals(""))) {
                parent = "XWiki.XWikiUsers";
            }

            // Mark the user as active or waiting email validation.
            if (withValidation) {
                map.put("active", new String[] { "0" });
                validkey = generateValidationKey(16);
                map.put("validkey", new String[] { validkey });

            } else {
                // Mark user active
                map.put("active", new String[] { "1" });
            }

            // Create the user.
            int result =
                createUser(xwikiname, map, getRelativeEntityReferenceResolver().resolve(parent, EntityType.DOCUMENT),
                    content, syntax, userRights, context);

            // Send validation mail, if needed.
            if ((result > 0) && (withValidation)) {
                // Send the validation email
                try {
                    sendValidationEmail(xwikiname, password, email, validkey, "validation_email_content", context);
                } catch (XWikiException e) {
                    LOGGER.warn("User created. Failed to send the mail to the created user.", e);
                    return -11;
                }

            }

            return result;
        } catch (XWikiException e) {
            LOGGER.error(e.getMessage(), e);

            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_CREATE_USER,
                "Exception while creating user", e, null);
        }
    }

    /**
     * Method allows to create an empty user with no password (he won't be able to login) This method is usefull for
     * authentication like LDAP or App Server trusted
     *
     * @param xwikiname
     * @param userRights
     * @param context see {@link XWikiContext}
     * @return true if success
     * @throws XWikiException
     */
    public boolean createEmptyUser(String xwikiname, String userRights, XWikiContext context) throws XWikiException
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("active", "1");
        map.put("first_name", xwikiname);

        if (createUser(xwikiname, map, userRights, context) == 1) {
            return true;
        } else {
            return false;
        }
    }

    public void sendConfirmationEmail(String xwikiname, String password, String email, String message,
        String contentfield, XWikiContext context) throws XWikiException
    {
        sendValidationEmail(xwikiname, password, email, "message", message, contentfield, context);
    }

    public void sendValidationEmail(String xwikiname, String password, String email, String validkey,
        String contentfield, XWikiContext context) throws XWikiException
    {
        sendValidationEmail(xwikiname, password, email, "validkey", validkey, contentfield, context);
    }

    public void sendValidationEmail(String xwikiname, String password, String email, String addfieldname,
        String addfieldvalue, String contentfield, XWikiContext context) throws XWikiException
    {
        MailSenderConfiguration configuration = Utils.getComponent(MailSenderConfiguration.class);

        String sender;
        String content;
        try {
            sender = configuration.getFromAddress();
            if (StringUtils.isBlank(sender)) {
                String server = context.getRequest().getServerName();
                if (server.matches("\\[.*\\]|(\\d{1,3}+\\.){3}+\\d{1,3}+")) {
                    sender = "noreply@domain.net";
                } else {
                    sender = "noreply@" + server;
                }
            }
            content = getXWikiPreference(contentfield, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG,
                "Exception while reading the validation email config", e, null);

        }

        try {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put(addfieldname, addfieldvalue);
            vcontext.put("email", email);
            vcontext.put("password", password);
            vcontext.put("sender", sender);
            vcontext.put("xwikiname", xwikiname);
            content = parseContent(content, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL,
                "Exception while preparing the validation email", e, null);

        }

        // Let's now send the message
        try {
            Session session =
                Session.getInstance(configuration.getAllProperties(), new XWikiAuthenticator(configuration));
            InputStream is = new ByteArrayInputStream(content.getBytes());
            MimeMessage message = new MimeMessage(session, is);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, email);
            message.setHeader("X-MailType", "Account Validation");
            MailSender mailSender = Utils.getComponent(MailSender.class);
            MailListener mailListener = Utils.getComponent(MailListener.class, "database");
            mailSender.sendAsynchronously(Arrays.asList(message), session, mailListener);
            mailListener.getMailStatusResult().waitTillProcessed(Long.MAX_VALUE);
            String errorMessage = MailStatusResultSerializer.serializeErrors(mailListener.getMailStatusResult());
            if (errorMessage != null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL,
                    String.format("Error while sending the validation email. %s", errorMessage));
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL, "Error while sending the validation email", e);
        }
    }

    public String generateRandomString(int size)
    {
        return RandomStringUtils.randomAlphanumeric(size);
    }

    public String generateValidationKey(int size)
    {
        return generateRandomString(size);
    }

    /**
     * Create a new user.
     *
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param context see {@link XWikiContext}
     * @return
     *         <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ?> map, XWikiContext context) throws XWikiException
    {
        return createUser(userName, map, "edit", context);
    }

    /**
     * Create a new user.
     *
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param userRights the right of the user on his own profile page
     * @param context see {@link XWikiContext}
     * @return
     *         <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ?> map, String userRights, XWikiContext context)
        throws XWikiException
    {
        BaseClass userClass = getUserClass(context);

        String content = "";
        Syntax syntax = getDefaultDocumentSyntaxInternal();

        return createUser(userName, map,
            new EntityReference(userClass.getDocumentReference().getName(), EntityType.DOCUMENT), content, syntax,
            userRights, context);
    }

    /**
     * @deprecated since 2.4RC1 use
     *             {@link #createUser(String, Map, EntityReference, String, Syntax, String, XWikiContext)} instead
     */
    @Deprecated
    public int createUser(String userName, Map<String, ?> map, String parent, String content, String syntaxId,
        String userRights, XWikiContext context) throws XWikiException
    {
        Syntax syntax;

        try {
            syntax = Syntax.valueOf(syntaxId);
        } catch (ParseException e) {
            syntax = getDefaultDocumentSyntaxInternal();
        }

        return createUser(userName, map, getRelativeEntityReferenceResolver().resolve(parent, EntityType.DOCUMENT),
            content, syntax, userRights, context);
    }

    /**
     * Create a new user.
     *
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param parentReference the parent of the user profile
     * @param content the content of the user profile
     * @param syntax the syntax of the provided content
     * @param userRights the right of the user on his own profile page
     * @param context see {@link XWikiContext}
     * @return
     *         <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ?> map, EntityReference parentReference, String content,
        Syntax syntax, String userRights, XWikiContext context) throws XWikiException
    {
        BaseClass userClass = getUserClass(context);

        try {
            // TODO: Verify existing user
            XWikiDocument doc = getDocument(new DocumentReference(context.getWikiId(), "XWiki", userName), context);

            if (!doc.isNew()) {
                // TODO: throws Exception
                return -3;
            }

            DocumentReference userClassReference = userClass.getDocumentReference();
            BaseObject userObject =
                doc.newXObject(userClassReference.removeParent(userClassReference.getWikiReference()), context);
            userClass.fromMap(map, userObject);

            doc.setParentReference(parentReference);
            doc.setContent(content);
            doc.setSyntax(syntax);

            // Set the user itself as the creator of the document, so that she has the CREATOR right on her user page.
            doc.setCreatorReference(doc.getDocumentReference());

            // However, we use the context user for the author to see in the history who has really created the user
            // (it may be an administrator).
            if (context.getUserReference() != null) {
                doc.setAuthorReference(context.getUserReference());
            } else {
                // Except if the current user is guest (which means the user registered herself)
                doc.setAuthorReference(doc.getDocumentReference());
            }

            // The information from the user profile needs to be indexed using the proper locale. If multilingual is
            // enabled then the user can choose the desired locale (from the list of supported locales) before
            // registering. An administrator registering users can do the same. Otherwise, if there is only one locale
            // supported then that langage will be used.
            doc.setDefaultLocale(context.getLocale());

            protectUserPage(doc.getFullName(), userRights, doc, context);

            saveDocument(doc, localizePlainOrKey("core.comment.createdUser"), context);

            // Now let's add the user to XWiki.XWikiAllGroup
            setUserDefaultGroup(doc.getFullName(), context);

            return 1;
        } catch (Exception e) {
            Object[] args = { "XWiki." + userName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_CREATE,
                "Cannot create user {0}", e, args);
        }
    }

    /**
     * @deprecated starting with XE 1.8.1 use
     *             {@link #createUser(String, Map, String, String, String, String, XWikiContext)} instead
     */
    @Deprecated
    public int createUser(String xwikiname, Map<String, ?> map, String parent, String content, String userRights,
        XWikiContext context) throws XWikiException
    {
        return createUser(xwikiname, map, parent, content, Syntax.XWIKI_1_0.toIdString(), userRights, context);
    }

    public void setUserDefaultGroup(String fullwikiname, XWikiContext context) throws XWikiException
    {
        String groupsPreference = isAllGroupImplicit() ? getConfiguration().getProperty("xwiki.users.initialGroups")
            : getConfiguration().getProperty("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        if (groupsPreference != null) {
            String[] groups = groupsPreference.split(",");
            for (String groupName : groups) {
                if (StringUtils.isNotBlank(groupName)) {
                    addUserToGroup(fullwikiname, groupName.trim(), context);
                }
            }
        }
    }

    protected void addUserToGroup(String userName, String groupName, XWikiContext context) throws XWikiException
    {
        XWikiDocument groupDoc = getDocument(groupName, context);

        DocumentReference groupClassReference = getGroupClass(context).getDocumentReference();
        BaseObject memberObject =
            groupDoc.newXObject(groupClassReference.removeParent(groupClassReference.getWikiReference()), context);

        memberObject.setStringValue("member", userName);

        this.saveDocument(groupDoc, localizePlainOrKey("core.comment.addedUserToGroup"), context);
    }

    public void protectUserPage(String userName, String userRights, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        DocumentReference rightClassReference = getRightsClass(context).getDocumentReference();

        EntityReference relativeRightClassReference =
            rightClassReference.removeParent(rightClassReference.getWikiReference());

        // Allow users to edit their own profiles
        BaseObject newuserrightsobject = doc.newXObject(relativeRightClassReference, context);
        newuserrightsobject.setLargeStringValue("users", userName);
        newuserrightsobject.setStringValue("levels", userRights);
        newuserrightsobject.setIntValue("allow", 1);
    }

    public User getUser(XWikiContext context)
    {
        XWikiUser xwikiUser = context.getXWikiUser();
        User user = new User(xwikiUser, context);
        return user;
    }

    public User getUser(String username, XWikiContext context)
    {
        XWikiUser xwikiUser = new XWikiUser(username);
        User user = new User(xwikiUser, context);
        return user;
    }

    /**
     * Prepares the localized resources, according to the selected locale. Set context "msg" and locale.
     *
     * @param context see {@link XWikiContext}
     */
    public void prepareResources(XWikiContext context)
    {
        if (context.get("msg") == null) {
            Locale locale = getLocalePreference(context);
            context.setLocale(locale);
            if (context.getResponse() != null) {
                context.getResponse().setLocale(locale);
            }
            XWikiMessageTool msg = new XWikiMessageTool(Utils.getComponent(ContextualLocalizationManager.class));
            context.put("msg", msg);
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        return getAuthService().checkAuth(context);
    }

    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        // Handle the 'skin' action specially so that resources don`t require special (or even 'view') rights.
        String firstSpaceName = doc.getDocumentReference().getSpaceReferences().get(0).getName();
        if (action.equals("skin") && SKIN_RESOURCE_SPACE_NAMES.contains(firstSpaceName)) {
            // We still need to call checkAuth to set the proper user.
            XWikiUser user = checkAuth(context);
            if (user != null) {
                context.setUser(user.getUser());
            }

            // Always allow.
            return true;
        }

        return getRightService().checkAccess(action, doc, context);
    }

    public String include(String topic, boolean isForm, XWikiContext context) throws XWikiException
    {
        String database = null, incdatabase = null;
        String prefixedTopic, localTopic;

        // Save current documents in script context
        Document currentAPIdoc = null, currentAPIcdoc = null, currentAPItdoc = null;
        ScriptContextManager scritContextManager = Utils.getComponent(ScriptContextManager.class);
        ScriptContext scontext = scritContextManager.getScriptContext();
        String currentDocName = context.getWikiId() + ":" + context.getDoc().getFullName();
        if (scontext != null) {
            currentAPIdoc = (Document) scontext.getAttribute("doc");
            currentAPIcdoc = (Document) scontext.getAttribute("cdoc");
            currentAPItdoc = (Document) scontext.getAttribute("tdoc");
        }

        try {
            int i0 = topic.indexOf(':');
            if (i0 != -1) {
                incdatabase = topic.substring(0, i0);
                database = context.getWikiId();
                context.setWikiId(incdatabase);
                prefixedTopic = topic;
                localTopic = topic.substring(i0 + 1);
            } else {
                prefixedTopic = context.getWikiId() + ":" + topic;
                localTopic = topic;
            }

            XWikiDocument doc = null;
            try {
                LOGGER.debug("Including Topic " + topic);
                try {
                    @SuppressWarnings("unchecked")
                    Set<String> includedDocs = (Set<String>) context.get("included_docs");
                    if (includedDocs == null) {
                        includedDocs = new HashSet<String>();
                        context.put("included_docs", includedDocs);
                    }

                    if (includedDocs.contains(prefixedTopic) || currentDocName.equals(prefixedTopic)) {
                        LOGGER.warn("Error on too many recursive includes for topic " + topic);
                        return "Cannot make recursive include";
                    }
                    includedDocs.add(prefixedTopic);
                } catch (Exception e) {
                }

                // Get document to include
                DocumentReference targetDocumentReference =
                    getCurrentMixedDocumentReferenceResolver().resolve(localTopic);
                doc = getDocument(targetDocumentReference, context);

                if (checkAccess("view", doc, context) == false) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED, "Access to this document is denied: " + doc);
                }
            } catch (XWikiException e) {
                LOGGER.warn("Exception Including Topic " + topic, e);
                return "Topic " + topic + " does not exist";
            }

            XWikiDocument contentdoc = doc.getTranslatedDocument(context);

            String result;
            if (isForm) {
                // We do everything in the context of the including document
                if (database != null) {
                    context.setWikiId(database);
                }

                // Note: the Script macro in the new rendering checks for programming rights for the document in
                // the xwiki context.
                result = getRenderedContent(contentdoc, (XWikiDocument) context.get("doc"), context);
            } else {
                // We stay in the included document context

                // Since the Script macro checks for programming rights in the current document, we need to
                // temporarily set the contentdoc as the current doc before rendering it.
                XWikiDocument originalDoc = null;
                try {
                    originalDoc = context.getDoc();
                    context.put("doc", doc);
                    result = getRenderedContent(contentdoc, doc, context);
                } finally {
                    context.put("doc", originalDoc);
                }
            }
            try {
                @SuppressWarnings("unchecked")
                Set<String> includedDocs = (Set<String>) context.get("included_docs");
                if (includedDocs != null) {
                    includedDocs.remove(prefixedTopic);
                }
            } catch (Exception e) {
            }
            return result;
        } finally {
            if (database != null) {
                context.setWikiId(database);
            }

            if (currentAPIdoc != null) {
                if (scontext != null) {
                    scontext.setAttribute("doc", currentAPIdoc, ScriptContext.ENGINE_SCOPE);
                }
            }
            if (currentAPIcdoc != null) {
                if (scontext != null) {
                    scontext.setAttribute("cdoc", currentAPIcdoc, ScriptContext.ENGINE_SCOPE);
                }
            }
            if (currentAPItdoc != null) {
                if (scontext != null) {
                    scontext.setAttribute("tdoc", currentAPItdoc, ScriptContext.ENGINE_SCOPE);
                }
            }
        }
    }

    /**
     * Render content from the passed included document, setting the correct security doc (sdoc) and including doc
     * (idoc).
     *
     * @since 2.2M2
     */
    private String getRenderedContent(XWikiDocument includedDoc, XWikiDocument includingDoc, XWikiContext context)
        throws XWikiException
    {
        String result;
        XWikiDocument idoc = (XWikiDocument) context.get("idoc");
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");

        context.put("idoc", includingDoc);
        context.put("sdoc", includedDoc);
        try {
            result = includedDoc.getRenderedContent(Utils.getComponent(RenderingContext.class).getTargetSyntax(), false,
                context);
        } finally {
            // Remove including doc or set the previous one
            if (idoc == null) {
                context.remove("idoc");
            } else {
                context.put("idoc", idoc);
            }

            // Remove security doc or set the previous one
            if (sdoc == null) {
                context.remove("sdoc");
            } else {
                context.put("sdoc", sdoc);
            }
        }

        return result;
    }

    public void deleteDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        deleteDocument(doc, true, context);
    }

    public void deleteDocument(XWikiDocument doc, boolean totrash, XWikiContext context) throws XWikiException
    {
        String currentWiki = null;

        currentWiki = context.getWikiId();
        try {
            context.setWikiId(doc.getDocumentReference().getWikiReference().getName());

            // The source document is a new empty XWikiDocument to follow
            // DocumentUpdatedEvent policy: source document in new document and the old version is available using
            // doc.getOriginalDocument()
            XWikiDocument blankDoc = new XWikiDocument(doc.getDocumentReference());
            // Again to follow general event policy, new document author is the user who modified the document
            // (here the modification is delete)
            blankDoc.setOriginalDocument(doc.getOriginalDocument());
            blankDoc.setAuthorReference(context.getUserReference());
            blankDoc.setContentAuthorReference(context.getUserReference());

            ObservationManager om = getObservationManager();

            // Inform notification mechanisms that a document is about to be deleted
            // Note that for the moment the event being send is a bridge event, as we are still passing around
            // an XWikiDocument as source and an XWikiContext as data.
            if (om != null) {
                om.notify(new DocumentDeletingEvent(doc.getDocumentReference()), blankDoc, context);
            }

            if (hasRecycleBin(context) && totrash) {
                // Extract any existing batchId from the context.
                String batchId = Utils.getComponent(BatchOperationExecutor.class).getCurrentBatchId();

                // Save to recycle bin together with any determined batch ID.
                getRecycleBinStore().saveToRecycleBin(doc, context.getUser(), new Date(), batchId, context, true);
            }

            getStore().deleteXWikiDoc(doc, context);

            try {
                // Inform notification mechanisms that a document has been deleted
                // Note that for the moment the event being send is a bridge event, as we are still passing around
                // an XWikiDocument as source and an XWikiContext as data.
                if (om != null) {
                    om.notify(new DocumentDeletedEvent(doc.getDocumentReference()), blankDoc, context);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to send document delete notifications for document [{}]",
                    doc.getDocumentReference(), ex);
            }
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    public String getDatabase()
    {
        return this.database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public void gc()
    {
        System.gc();
    }

    public long freeMemory()
    {
        return Runtime.getRuntime().freeMemory();
    }

    public long totalMemory()
    {
        return Runtime.getRuntime().totalMemory();
    }

    public long maxMemory()
    {
        return Runtime.getRuntime().maxMemory();
    }

    public String[] split(String str, String sep)
    {
        return StringUtils.split(str, sep);
    }

    /**
     * @deprecated use {@link ExceptionUtils#getStackTrace(Throwable)} instead
     */
    @Deprecated
    public String printStrackTrace(Throwable e)
    {
        StringWriter strwriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strwriter);
        e.printStackTrace(writer);

        return strwriter.toString();
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, true, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        boolean reset, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, reset, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        boolean reset, boolean force, boolean resetCreationData, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, reset, force, resetCreationData,
            context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilocale, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilocale, true, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilocale, boolean reset, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilocale, reset, false, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilocale, boolean reset, boolean force, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilocale, reset, force, false, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilocale, boolean reset, boolean force, boolean resetCreationData, XWikiContext context)
        throws XWikiException
    {
        String db = context.getWikiId();
        String sourceWiki = sourceDocumentReference.getWikiReference().getName();
        String targetWiki = targetDocumentReference.getWikiReference().getName();

        String sourceStringReference = getDefaultEntityReferenceSerializer().serialize(sourceDocumentReference);

        try {
            context.setWikiId(sourceWiki);
            XWikiDocument sdoc = getDocument(sourceDocumentReference, context);
            if (!sdoc.isNew()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                        "Copying document [" + sourceDocumentReference + "] to [" + targetDocumentReference + "]");
                }

                // Let's switch to the other database to verify if the document already exists
                context.setWikiId(targetWiki);
                XWikiDocument tdoc = getDocument(targetDocumentReference, context);
                // There is already an existing document
                if (!tdoc.isNew()) {
                    if (force) {
                        // We need to delete the previous document
                        deleteDocument(tdoc, context);
                    } else {
                        return false;
                    }
                }

                // Let's switch back again to the original db
                context.setWikiId(sourceWiki);

                if (wikilocale == null) {
                    tdoc = sdoc.copyDocument(targetDocumentReference, context);

                    // We know the target document doesn't exist and we want to save the attachments without
                    // incrementing their versions.
                    // See XWIKI-8157: The "Copy Page" action adds an extra version to the attached file
                    tdoc.setNew(true);

                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                    }
                    if (resetCreationData) {
                        Date now = new Date();
                        tdoc.setCreationDate(now);
                        tdoc.setContentUpdateDate(now);
                        tdoc.setDate(now);
                        tdoc.setCreatorReference(context.getUserReference());
                        tdoc.setAuthorReference(context.getUserReference());
                    }

                    // We don't want to trigger a new version otherwise the version number will be wrong.
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, "Copied from " + sourceStringReference, context);

                    if (!reset) {
                        context.setWikiId(sourceWiki);
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        context.setWikiId(targetWiki);
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        context.setWikiId(targetWiki);
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }

                    // Now we need to copy the translations
                    context.setWikiId(sourceWiki);
                    List<String> tlist = sdoc.getTranslationList(context);
                    for (String clanguage : tlist) {
                        XWikiDocument stdoc = sdoc.getTranslatedDocument(clanguage, context);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Copying document [" + sourceWiki + "], language [" + clanguage + "] to ["
                                + targetDocumentReference + "]");
                        }

                        context.setWikiId(targetWiki);
                        XWikiDocument ttdoc = tdoc.getTranslatedDocument(clanguage, context);

                        // There is already an existing document
                        if (ttdoc != tdoc) {
                            return false;
                        }

                        // Let's switch back again to the original db
                        context.setWikiId(sourceWiki);

                        ttdoc = stdoc.copyDocument(targetDocumentReference, context);

                        // forget past versions
                        if (reset) {
                            ttdoc.setNew(true);
                            ttdoc.setVersion("1.1");
                        }
                        if (resetCreationData) {
                            Date now = new Date();
                            ttdoc.setCreationDate(now);
                            ttdoc.setContentUpdateDate(now);
                            ttdoc.setDate(now);
                            ttdoc.setCreatorReference(context.getUserReference());
                            ttdoc.setAuthorReference(context.getUserReference());
                        }

                        // we don't want to trigger a new version
                        // otherwise the version number will be wrong
                        tdoc.setMetaDataDirty(false);
                        tdoc.setContentDirty(false);

                        saveDocument(ttdoc, "Copied from " + sourceStringReference, context);

                        if (!reset) {
                            context.setWikiId(sourceWiki);
                            XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                            context.setWikiId(targetWiki);
                            txda = txda.clone(tdoc.getId(), context);
                            getVersioningStore().saveXWikiDocArchive(txda, true, context);
                        } else {
                            getVersioningStore().resetRCSArchive(tdoc, true, context);
                        }
                    }
                } else {
                    // We want only one language in the end
                    XWikiDocument stdoc = sdoc.getTranslatedDocument(wikilocale, context);

                    tdoc = stdoc.copyDocument(targetDocumentReference, context);

                    // We know the target document doesn't exist and we want to save the attachments without
                    // incrementing their versions.
                    // See XWIKI-8157: The "Copy Page" action adds an extra version to the attached file
                    tdoc.setNew(true);

                    // forget language
                    tdoc.setDefaultLanguage(wikilocale);
                    tdoc.setLanguage("");
                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                    }
                    if (resetCreationData) {
                        Date now = new Date();
                        tdoc.setCreationDate(now);
                        tdoc.setContentUpdateDate(now);
                        tdoc.setDate(now);
                        tdoc.setCreatorReference(context.getUserReference());
                        tdoc.setAuthorReference(context.getUserReference());
                    }

                    // we don't want to trigger a new version
                    // otherwise the version number will be wrong
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, "Copied from " + sourceStringReference, context);

                    if (!reset) {
                        context.setWikiId(sourceWiki);
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        context.setWikiId(targetWiki);
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }
                }
            }
            return true;
        } finally {
            context.setWikiId(db);
        }
    }

    public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki, String locale,
        XWikiContext context) throws XWikiException
    {
        return copySpaceBetweenWikis(space, sourceWiki, targetWiki, locale, false, context);
    }

    public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki, String locale, boolean clean,
        XWikiContext context) throws XWikiException
    {
        String db = context.getWikiId();
        int nb = 0;
        // Workaround for XWIKI-3915: Do not use XWikiStoreInterface#searchDocumentNames since currently it has the
        // side effect of hidding hidden documents and no other workaround exists than directly using
        // XWikiStoreInterface#search directly
        String sql = "select distinct doc.fullName from XWikiDocument as doc";
        List<String> parameters = new ArrayList<String>();
        if (space != null) {
            sql += " where doc.space = ?";
            parameters.add(space);
        }

        if (clean) {
            try {
                context.setWikiId(targetWiki);
                List<String> list = getStore().search(sql, 0, 0, parameters, context);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Deleting " + list.size() + " documents from wiki " + targetWiki);
                }

                for (String docname : list) {
                    XWikiDocument doc = getDocument(docname, context);
                    deleteDocument(doc, context);
                }
            } finally {
                context.setWikiId(db);
            }
        }

        try {
            context.setWikiId(sourceWiki);
            List<String> list = getStore().search(sql, 0, 0, parameters, context);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Copying " + list.size() + " documents from wiki " + sourceWiki + " to wiki " + targetWiki);
            }

            WikiReference sourceWikiReference = new WikiReference(sourceWiki);
            WikiReference targetWikiReference = new WikiReference(targetWiki);
            for (String docname : list) {
                DocumentReference sourceDocumentReference = getCurrentMixedDocumentReferenceResolver().resolve(docname);
                sourceDocumentReference = sourceDocumentReference
                    .replaceParent(sourceDocumentReference.getWikiReference(), sourceWikiReference);
                DocumentReference targetDocumentReference =
                    sourceDocumentReference.replaceParent(sourceWikiReference, targetWikiReference);
                copyDocument(sourceDocumentReference, targetDocumentReference, locale, context);
                nb++;
            }
            return nb;
        } finally {
            context.setWikiId(db);
        }
    }

    /**
     * Copy an entire wiki to a target wiki.
     * <p>
     * It does not override document already existing in target wiki.
     *
     * @param sourceWiki the source wiki identifier
     * @param targetWiki the target wiki identifier
     * @param locale the locale to copy
     * @param context see {@link XWikiContext}
     * @return the number of copied documents
     * @throws XWikiException failed to copy wiki
     * @deprecated since 5.3, use {@link WikiManager#copy(String, String, String, boolean, boolean, boolean)} instead
     */
    @Deprecated
    public int copyWiki(String sourceWiki, String targetWiki, String locale, XWikiContext context) throws XWikiException
    {
        return copyWiki(sourceWiki, targetWiki, locale, false, context);
    }

    /**
     * Copy an entire wiki to a target wiki.
     *
     * @param sourceWiki the source wiki identifier
     * @param targetWiki the target wiki identifier
     * @param locale the locale to copy
     * @param clean clean the target wiki before copying
     * @param context see {@link XWikiContext}
     * @return the number of copied documents
     * @throws XWikiException failed to copy wiki
     * @deprecated since 5.3, use {@link WikiManager#copy(String, String, String, boolean, boolean, boolean)} instead
     */
    @Deprecated
    public int copyWiki(String sourceWiki, String targetWiki, String locale, boolean clean, XWikiContext context)
        throws XWikiException
    {
        int documents = copySpaceBetweenWikis(null, sourceWiki, targetWiki, locale, clean, context);

        ObservationManager om = getObservationManager();

        if (om != null) {
            om.notify(new WikiCopiedEvent(sourceWiki, targetWiki), sourceWiki, context);
        }

        return documents;
    }

    public String getEncoding()
    {
        return getConfiguration().getProperty("xwiki.encoding", "UTF-8");
    }

    public URL getServerURL(String database, XWikiContext context) throws MalformedURLException
    {
        String serverurl = null;

        // In virtual wiki path mode the server is the standard one
        if ("1".equals(getConfiguration().getProperty("xwiki.virtual.usepath", "1"))) {
            return null;
        }

        if (database != null) {
            String db = context.getWikiId();
            try {
                context.setWikiId(getDatabase());
                XWikiDocument doc = getDocument("XWiki.XWikiServer" + StringUtils.capitalize(database), context);
                BaseObject serverobject = doc.getXObject(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
                if (serverobject != null) {
                    String server = serverobject.getStringValue("server");
                    if (server != null) {
                        String protocol = getConfiguration().getProperty("xwiki.url.protocol", null);
                        if (protocol == null) {
                            int iSecure = serverobject.getIntValue("secure", -1);
                            // Check the request object if the "secure" property is undefined.
                            boolean secure = iSecure == 1 || (iSecure < 0 && context.getRequest().isSecure());
                            protocol = secure ? "https" : "http";
                        }
                        long port = context.getURL().getPort();
                        if (port == 80 || port == 443) {
                            port = -1;
                        }
                        if (port != -1) {
                            serverurl = protocol + "://" + server + ":" + port + "/";
                        } else {
                            serverurl = protocol + "://" + server + "/";
                        }
                    }
                }
            } catch (Exception ex) {
            } finally {
                context.setWikiId(db);
            }
        }

        if (serverurl != null) {
            return new URL(serverurl);
        } else {
            return null;
        }
    }

    public String getServletPath(String wikiName, XWikiContext context)
    {
        // unless we are in virtual wiki path mode we should return null
        if (!context.getMainXWiki().equalsIgnoreCase(wikiName)
            && "1".equals(getConfiguration().getProperty("xwiki.virtual.usepath", "1"))) {
            String database = context.getWikiId();
            try {
                context.setWikiId(context.getMainXWiki());
                XWikiDocument doc = getDocument(getServerWikiPage(wikiName), context);
                BaseObject serverobject = doc.getXObject(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
                if (serverobject != null) {
                    String server = serverobject.getStringValue("server");
                    return "wiki/" + server + "/";
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get URL for provided wiki [" + wikiName + "]", e);
            } finally {
                context.setWikiId(database);
            }
        }

        String servletPath = getConfiguration().getProperty("xwiki.servletpath", "");

        if (context.getRequest() != null) {
            if (StringUtils.isEmpty(servletPath)) {
                String currentServletpath = context.getRequest().getServletPath();
                if (currentServletpath != null && currentServletpath.startsWith("/bin")) {
                    servletPath = "bin/";
                } else {
                    servletPath = getConfiguration().getProperty("xwiki.defaultservletpath", "bin/");
                }
            }
        }

        return servletPath;
    }

    public String getWebAppPath(XWikiContext context)
    {
        String contextPath = getConfiguration().getProperty("xwiki.webapppath");
        if (contextPath == null) {
            // Try getting the context path by asking the request for it (if a request exists!) and if it doesn't
            // work try extracting it from the context URL.
            // TODO: Instead of trying to extract from the URL, save the context path at webapp init (using a
            // ServlettContextListener for example).
            XWikiRequest request = context.getRequest();
            if (request != null) {
                contextPath = request.getContextPath();
            }
            if (contextPath == null) {
                // Extract the context by getting the first path segment
                contextPath = StringUtils.substringBefore(StringUtils.stripStart(context.getURL().getPath(), "/"), "/");
            }
        }

        // Remove any leading or trailing slashes
        contextPath = StringUtils.strip(contextPath, "/");

        // TODO We're using URL parts in a wrong way, since contextPath and servletPath are returned with a leading /,
        // while we need a trailing /. This code ensure we always have CONTEXTNAME + "/".
        return contextPath + "/";
    }

    /**
     * @since 7.2M1
     */
    public String getURL(EntityReference entityReference, String action, String queryString, String anchor,
        XWikiContext context)
    {
        // TODO: replace this API with a clean implementation of EntityResourceReferenceSerializer

        // Handle attachment URL
        if (EntityType.ATTACHMENT.equals(entityReference.getType())) {
            // Get the full attachment reference
            AttachmentReference attachmentReference = getCurrentAttachmentResolver().resolve(entityReference);
            return getAttachmentURL(attachmentReference, action, queryString, context);
        }

        // For all other types, we return the URL of the default corresponding document.
        DocumentReference documentReference = getCurrentGetDocumentResolver().resolve(entityReference);
        return getURL(documentReference, action, queryString, anchor, context);
    }

    /**
     * @since 7.2M1
     */
    public String getURL(EntityReference reference, String action, XWikiContext context)
    {
        return getURL(reference, action, null, null, context);
    }

    /**
     * @since 7.2RC1
     */
    public String getURL(EntityReference reference, XWikiContext context)
    {
        String action = "view";
        if (reference.getType() == EntityType.ATTACHMENT) {
            action = "download";
        }
        return getURL(reference, action, context);
    }

    /**
     * @since 2.2.1
     */
    public String getURL(DocumentReference documentReference, String action, String queryString, String anchor,
        XWikiContext context)
    {
        // We need to serialize the space reference because the old createURL() API doesn't accept a DocumentReference.
        String spaces = getLocalStringEntityReferenceSerializer().serialize(documentReference.getLastSpaceReference());

        // Take into account the specified document locale.
        Locale documentLocale = documentReference.getLocale();
        String actualQueryString = queryString;
        if (documentLocale != null && documentLocale != Locale.ROOT) {
            String localeQueryString = "language=" + documentLocale;
            if (StringUtils.isEmpty(queryString)) {
                actualQueryString = localeQueryString;
            } else {
                // Note: if the locale is already specified on the given query string then it won't be overwriten
                // because the first parameter value is taken into account.
                actualQueryString += '&' + localeQueryString;
            }
        }

        URL url = context.getURLFactory().createURL(spaces, documentReference.getName(), action, actualQueryString,
            anchor, documentReference.getWikiReference().getName(), context);

        return context.getURLFactory().getURL(url, context);
    }

    /**
     * @deprecated since 2.2.1 use {@link #getURL(DocumentReference, String, String, String, XWikiContext)}
     */
    @Deprecated
    public String getURL(String fullname, String action, String queryString, String anchor, XWikiContext context)
    {
        return getURL(getCurrentMixedDocumentReferenceResolver().resolve(fullname), action, queryString, anchor,
            context);
    }

    public String getURL(String fullname, String action, String querystring, XWikiContext context)
    {
        return getURL(fullname, action, querystring, null, context);
    }

    /**
     * @since 2.3M2
     */
    public String getURL(DocumentReference reference, String action, XWikiContext context)
    {
        return getURL(reference, action, null, null, context);
    }

    /**
     * @deprecated since 2.3M2 use {@link #getURL(DocumentReference, String, XWikiContext)}
     */
    @Deprecated
    public String getURL(String fullname, String action, XWikiContext context)
    {
        return getURL(fullname, action, null, null, context);
    }

    public String getExternalURL(String fullname, String action, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(getCurrentMixedDocumentReferenceResolver().resolve(fullname));

        URL url = context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, null, null,
            doc.getDatabase(), context);
        return url.toString();
    }

    public String getExternalURL(String fullname, String action, String querystring, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(getCurrentMixedDocumentReferenceResolver().resolve(fullname));

        URL url = context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, querystring, null,
            doc.getDatabase(), context);
        return url.toString();
    }

    /**
     * Get the full URL of the given {@link DocumentReference}. This also includes the server name of the wiki.
     *
     * @param documentReference the document that should be resolved
     * @param action the action of the URL
     * @param querystring the URL parameters
     * @param anchor the anchor of the document
     * @param context the current XWikiContext
     * @return the full URL of the given reference
     * @since 9.6RC1
     */
    public String getExternalURL(DocumentReference documentReference, String action, String querystring, String anchor,
        XWikiContext context)
    {
        URL url = context.getURLFactory().createExternalURL(
            this.getLocalStringEntityReferenceSerializer().serialize(documentReference.getLastSpaceReference()),
            documentReference.getName(), action, querystring, anchor, documentReference.getWikiReference().getName(),
            context);
        return url.toString();
    }

    /**
     * @since 7.2M1
     */
    public String getAttachmentURL(AttachmentReference attachmentReference, String action, String queryString,
        XWikiContext context)
    {
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        SpaceReference spaceReference = documentReference.getLastSpaceReference();
        WikiReference wikiReference = spaceReference.getWikiReference();

        // We need to serialize the space reference because the old URLFactory has no method to create an Attachment URL
        // from an AttachmentReference...
        String serializedSpace = getLocalStringEntityReferenceSerializer().serialize(spaceReference);

        URL url = context.getURLFactory().createAttachmentURL(attachmentReference.getName(), serializedSpace,
            documentReference.getName(), action, queryString, wikiReference.getName(), context);

        return context.getURLFactory().getURL(url, context);
    }

    /**
     * @since 7.2M1
     */
    public String getAttachmentURL(AttachmentReference attachmentReference, String queryString, XWikiContext context)
    {
        return getAttachmentURL(attachmentReference, "download", queryString, context);
    }

    /**
     * @since 7.2M1
     */
    public String getAttachmentRevisionURL(AttachmentReference attachmentReference, String revision, String queryString,
        XWikiContext context)
    {
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        SpaceReference spaceReference = documentReference.getLastSpaceReference();
        WikiReference wikiReference = spaceReference.getWikiReference();

        // We need to serialize the space reference because the old URLFactory has no method to create an Attachment URL
        // from an AttachmentReference...
        String serializedSpace = getLocalStringEntityReferenceSerializer().serialize(spaceReference);

        URL url = context.getURLFactory().createAttachmentRevisionURL(attachmentReference.getName(), serializedSpace,
            documentReference.getName(), revision, queryString, wikiReference.getName(), context);

        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentURL(String fullname, String filename, XWikiContext context) throws XWikiException
    {
        return getAttachmentURL(fullname, filename, null, context);
    }

    /**
     * @since 2.5RC1
     */
    public String getAttachmentURL(String fullname, String filename, String queryString, XWikiContext context)
        throws XWikiException
    {
        AttachmentReference attachmentReference =
            new AttachmentReference(filename, getCurrentMixedDocumentReferenceResolver().resolve(fullname));

        return getAttachmentURL(attachmentReference, queryString, context);
    }

    // Usefull date functions

    public int getTimeDelta(long time)
    {
        Date ctime = new Date();
        return (int) (ctime.getTime() - time);
    }

    public boolean isMultiLingual(XWikiContext context)
    {
        return "1".equals(getXWikiPreference("multilingual", "0", context));
    }

    public boolean isLDAP()
    {
        return "1".equals(getConfiguration().getProperty("xwiki.authentication.ldap"));
    }

    /**
     * @return true if XWikiAllGroup group should be seen as virtual group containing all users, false to use it as any
     *         other group
     * @since 9.3RC1
     */
    public boolean isAllGroupImplicit()
    {
        return "1".equals(getConfiguration().getProperty("xwiki.authentication.group.allgroupimplicit"));
    }

    public int checkActive(XWikiContext context) throws XWikiException
    {
        return checkActive(context.getUser(), context);
    }

    public int checkActive(String user, XWikiContext context) throws XWikiException
    {
        int active = 1;

        // These users are necessarily active. Note that superadmin might be main-wiki-prefixed when in a subwiki.
        if (user.equals(XWikiRightService.GUEST_USER_FULLNAME)
            || (user.endsWith(XWikiRightService.SUPERADMIN_USER_FULLNAME))) {
            return active;
        }

        String checkactivefield = getXWikiPreference("auth_active_check", context);
        if (checkactivefield.equals("1")) {
            XWikiDocument userdoc = getDocument(user, context);
            active = userdoc.getIntValue("XWiki.XWikiUsers", "active");
        }

        return active;
    }

    /**
     * @since 2.3M1
     */
    public DocumentReference getDocumentReference(XWikiRequest request, XWikiContext context)
    {
        DocumentReference reference;
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            if (request.getParameter("topic") != null) {
                reference = getCurrentMixedDocumentReferenceResolver().resolve(request.getParameter("topic"));
            } else {
                // Point to this wiki's home page
                reference = getDefaultDocumentReference().setWikiReference(new WikiReference(context.getWikiId()));
            }
        } else if (context.getMode() == XWikiContext.MODE_XMLRPC) {
            reference = new DocumentReference(context.getWikiId(),
                context.getDoc().getDocumentReference().getLastSpaceReference().getName(),
                context.getDoc().getDocumentReference().getName());
        } else {
            ResourceReference resourceReference = getResourceReferenceManager().getResourceReference();
            if (resourceReference instanceof EntityResourceReference) {
                EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
                String action = entityResource.getAction().getActionName();
                if ((request.getParameter("topic") != null) && (action.equals("edit") || action.equals("inline"))) {
                    reference = getCurrentMixedDocumentReferenceResolver().resolve(request.getParameter("topic"));
                } else {
                    reference = new DocumentReference(
                        entityResource.getEntityReference().extractReference(EntityType.DOCUMENT));
                }
            } else {
                // TODO: Handle references not pointing to a document...
                // Big problem we don't have an Entity URL!
                throw new RuntimeException(
                    String.format("Resource Reference [%s] isn't an Entity Resource Reference!", resourceReference));
            }
        }

        return reference;
    }

    /**
     * Helper method, removes a predefined path segment (the context path or the servel path) from the start of the
     * requested URI and returns the remainder. This method is needed because special characters in the path can be
     * URL-encoded, depending on whether the request is forwarded through the request dispatcher or not, and also
     * depending on the client (some browsers encode -, while some don't).
     *
     * @param path the path, as taken from the requested URI
     * @param segment the segment to remove, as reported by the container
     * @return the path with the specified segment trimmed from its start
     */
    public static String stripSegmentFromPath(String path, String segment)
    {
        if (!path.startsWith(segment)) {
            // The context path probably contains special characters that are encoded in the URL
            try {
                segment = URIUtil.encodePath(segment);
            } catch (URIException e) {
                LOGGER.warn("Invalid path: [" + segment + "]");
            }
        }
        if (!path.startsWith(segment)) {
            // Some clients also encode -, although it's allowed in the path
            segment = segment.replaceAll("-", "%2D");
        }
        if (!path.startsWith(segment)) {
            // Can't find the context path in the URL (shouldn't happen), just skip to the next path segment
            return path.substring(path.indexOf('/', 1));
        }
        return path.substring(segment.length());
    }

    public boolean prepareDocuments(XWikiRequest request, XWikiContext context, VelocityContext vcontext)
        throws XWikiException
    {
        XWikiDocument doc;
        context.getWiki().prepareResources(context);
        DocumentReference reference = getDocumentReference(request, context);
        if (context.getAction().equals("register")) {
            setPhonyDocument(reference, context, vcontext);
            doc = context.getDoc();
        } else {
            try {
                doc = getDocument(reference, context);
            } catch (XWikiException e) {
                doc = context.getDoc();
                if (context.getAction().equals("delete")) {
                    if (doc == null) {
                        setPhonyDocument(reference, context, vcontext);
                        doc = context.getDoc();
                    }
                    if (!checkAccess("admin", doc, context)) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        // We need to check rights before we look for translations
        // Otherwise we don't have the user language
        if (checkAccess(context.getAction(), doc, context) == false) {
            Object[] args = { doc.getFullName(), context.getUser() };
            setPhonyDocument(reference, context, vcontext);
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access to document {0} has been denied to user {1}", null, args);
        } else if (checkActive(context) == 0) { // if auth_active_check, check if user is inactive
            boolean allow = false;
            String action = context.getAction();
            /*
             * Allow inactive users to see skins, ressources, SSX, JSX and downloads they could have seen as guest. The
             * rational behind this behaviour is that inactive users should be able to access the same UI that guests
             * are used to see, including custom icons, panels, and so on...
             */
            if ((action.equals("skin") && (doc.getSpace().equals("skins") || doc.getSpace().equals("resources")))
                || ((action.equals("skin") || action.equals("download") || action.equals("ssx") || action.equals("jsx"))
                    && getRightService().hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME,
                        doc.getPrefixedFullName(), context))
                || ((action.equals("view") && doc.getFullName().equals("XWiki.AccountValidation")))) {
                allow = true;
            } else {
                String allowed = getConfiguration().getProperty("xwiki.inactiveuser.allowedpages", "");
                if (context.getAction().equals("view") && !allowed.equals("")) {
                    String[] allowedList = StringUtils.split(allowed, " ,");
                    for (String element : allowedList) {
                        if (element.equals(doc.getFullName())) {
                            allow = true;
                            break;
                        }
                    }
                }
            }
            if (!allow) {
                Object[] args = { context.getUser() };
                setPhonyDocument(reference, context, vcontext);
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INACTIVE,
                    "User {0} account is inactive", null, args);
            }
        }

        context.put("doc", doc);
        context.put("cdoc", doc);
        vcontext.put("doc", doc.newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        XWikiDocument tdoc = doc.getTranslatedDocument(context);
        try {
            String rev = (String) context.get("rev");
            if (StringUtils.isNotEmpty(rev)) {
                tdoc = getDocument(tdoc, rev, context);
            }
        } catch (Exception ex) {
            // Invalid version, just use the most recent one
        }
        context.put("tdoc", tdoc);
        vcontext.put("tdoc", tdoc.newDocument(context));

        return true;
    }

    /**
     * @since 8.3M1
     */
    public void setPhonyDocument(DocumentReference reference, XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument(reference);
        doc.setElements(XWikiDocument.HAS_ATTACHMENTS | XWikiDocument.HAS_OBJECTS);
        doc.setStore(getStore());
        context.put("doc", doc);
    }

    /**
     * @since 2.3M1
     * @deprecated since 8.3M1, use {@link #setPhonyDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public void setPhonyDocument(DocumentReference reference, XWikiContext context, VelocityContext vcontext)
    {
        setPhonyDocument(reference, context);

        vcontext.put("doc", context.getDoc().newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        vcontext.put("tdoc", vcontext.get("doc"));
    }

    /**
     * @deprecated since 2.3M1 use {@link #setPhonyDocument(DocumentReference, XWikiContext, VelocityContext)}
     */
    @Deprecated
    public void setPhonyDocument(String docName, XWikiContext context, VelocityContext vcontext)
    {
        setPhonyDocument(getCurrentMixedDocumentReferenceResolver().resolve(docName), context, vcontext);
    }

    public XWikiEngineContext getEngineContext()
    {
        return this.engine_context;
    }

    public void setEngineContext(XWikiEngineContext engine_context)
    {
        this.engine_context = engine_context;
    }

    public void setAuthService(XWikiAuthService authService)
    {
        this.authService = authService;
    }

    public void setRightService(XWikiRightService rightService)
    {
        this.rightService = rightService;
    }

    public XWikiGroupService getGroupService(XWikiContext context) throws XWikiException
    {
        synchronized (this.GROUP_SERVICE_LOCK) {
            if (this.groupService == null) {
                String groupClass = getConfiguration().getProperty("xwiki.authentication.groupclass",
                    "com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl");

                try {
                    this.groupService = (XWikiGroupService) Class.forName(groupClass).newInstance();
                } catch (Exception e) {
                    LOGGER.error("Failed to instantiate custom group service class: " + e.getMessage(), e);
                    this.groupService = new XWikiGroupServiceImpl();
                }
                this.groupService.init(this, context);
            }

            return this.groupService;
        }
    }

    public void setGroupService(XWikiGroupService groupService)
    {
        this.groupService = groupService;
    }

    // added some log statements to make debugging easier - LBlaze 2005.06.02
    public XWikiAuthService getAuthService()
    {
        synchronized (this.AUTH_SERVICE_LOCK) {
            if (this.authService == null) {

                LOGGER.info("Initializing AuthService...");

                String authClass = getConfiguration().getProperty("xwiki.authentication.authclass");
                if (StringUtils.isNotEmpty(authClass)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using custom AuthClass " + authClass + ".");
                    }
                } else {
                    if (isLDAP()) {
                        authClass = "com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl";
                    } else {
                        authClass = "com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl";
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using default AuthClass " + authClass + ".");
                    }
                }

                try {
                    // Get the current ClassLoader
                    @SuppressWarnings("deprecation")
                    ClassLoaderManager clManager = Utils.getComponent(ClassLoaderManager.class);
                    ClassLoader classloader = null;
                    if (clManager != null) {
                        classloader = clManager.getURLClassLoader("wiki:xwiki", false);
                    }

                    // Get the class
                    if (classloader != null) {
                        this.authService = (XWikiAuthService) Class.forName(authClass, true, classloader).newInstance();
                    } else {
                        this.authService = (XWikiAuthService) Class.forName(authClass).newInstance();
                    }

                    LOGGER.debug("Initialized AuthService using Relfection.");
                } catch (Exception e) {
                    LOGGER.warn("Failed to initialize AuthService " + authClass
                        + " using Reflection, trying default implementations using 'new'.", e);

                    this.authService = new XWikiAuthServiceImpl();

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                            "Initialized AuthService " + this.authService.getClass().getName() + " using 'new'.");
                    }
                }
            }

            return this.authService;
        }
    }

    private static final String DEFAULT_RIGHT_SERVICE_CLASS =
        "org.xwiki.security.authorization.internal.XWikiCachingRightService";

    public XWikiRightService getRightService()
    {
        synchronized (this.RIGHT_SERVICE_LOCK) {
            if (this.rightService == null) {
                LOGGER.info("Initializing RightService...");

                String rightsClass = getConfiguration().getProperty("xwiki.authentication.rightsclass");
                if (rightsClass != null && !rightsClass.equals(DEFAULT_RIGHT_SERVICE_CLASS)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.warn("Using custom Right Service [{}].", rightsClass);
                    }
                } else {
                    rightsClass = DEFAULT_RIGHT_SERVICE_CLASS;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using default Right Service [{}].", rightsClass);
                    }
                }

                try {
                    this.rightService = (XWikiRightService) Class.forName(rightsClass).newInstance();
                    LOGGER.debug("Initialized RightService using Reflection.");
                } catch (Exception e) {
                    Exception lastException = e;

                    if (!rightsClass.equals(DEFAULT_RIGHT_SERVICE_CLASS)) {
                        LOGGER.warn(String.format(
                            "Failed to initialize custom RightService [%s]"
                                + " by Reflection, using default implementation [%s].",
                            rightsClass, DEFAULT_RIGHT_SERVICE_CLASS), e);
                        rightsClass = DEFAULT_RIGHT_SERVICE_CLASS;
                        try {
                            this.rightService = (XWikiRightService) Class.forName(rightsClass).newInstance();
                            LOGGER.debug("Initialized default RightService using Reflection.");
                        } catch (Exception e1) {
                            lastException = e1;
                        }
                    }

                    if (this.rightService == null) {
                        LOGGER.warn(String.format(
                            "Failed to initialize RightService [%s]"
                                + " by Reflection, using OLD implementation [%s] with 'new'.",
                            rightsClass, XWikiRightServiceImpl.class.getCanonicalName()), lastException);

                        this.rightService = new XWikiRightServiceImpl();

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Initialized old RightService implementation "
                                + this.rightService.getClass().getName() + " using 'new'.");
                        }
                    }
                }
            }
            return this.rightService;
        }
    }

    public XWikiStatsService getStatsService(XWikiContext context)
    {
        synchronized (this.STATS_SERVICE_LOCK) {
            if (this.statsService == null) {
                if ("1".equals(getConfiguration().getProperty("xwiki.stats", "1"))) {
                    String storeClass = getConfiguration().getProperty("xwiki.stats.class",
                        "com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl");
                    try {
                        this.statsService = (XWikiStatsService) Class.forName(storeClass).newInstance();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);

                        this.statsService = new XWikiStatsServiceImpl();
                    }

                    this.statsService.init(context);
                }
            }

            return this.statsService;
        }
    }

    public XWikiURLFactoryService getURLFactoryService()
    {
        if (this.urlFactoryService == null) {
            synchronized (this.URLFACTORY_SERVICE_LOCK) {
                if (this.urlFactoryService == null) {
                    LOGGER.info("Initializing URLFactory Service...");

                    XWikiURLFactoryService factoryService = null;

                    String urlFactoryServiceClass = getConfiguration().getProperty("xwiki.urlfactory.serviceclass");
                    if (urlFactoryServiceClass != null) {
                        try {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Using custom URLFactory Service Class [" + urlFactoryServiceClass + "]");
                            }
                            factoryService = (XWikiURLFactoryService) Class.forName(urlFactoryServiceClass)
                                .getConstructor(new Class<?>[] { XWiki.class }).newInstance(new Object[] { this });
                        } catch (Exception e) {
                            factoryService = null;
                            LOGGER.warn("Failed to initialize URLFactory Service [" + urlFactoryServiceClass + "]", e);
                        }
                    }
                    if (factoryService == null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Using default URLFactory Service Class [" + urlFactoryServiceClass + "]");
                        }
                        factoryService = new XWikiURLFactoryServiceImpl(this);
                    }

                    // Set the urlFactoryService object in one assignment to prevent threading
                    // issues when checking for
                    // null above.
                    this.urlFactoryService = factoryService;
                }
            }
        }

        return this.urlFactoryService;
    }

    public XWikiCriteriaService getCriteriaService(XWikiContext context)
    {
        return this.criteriaService;
    }

    public ZipOutputStream getZipOutputStream(XWikiContext context) throws IOException
    {
        return new ZipOutputStream(context.getResponse().getOutputStream());
    }

    private Map<String, SearchEngineRule> getSearchEngineRules(XWikiContext context)
    {
        // We currently hardcode the rules
        // We will put them in the preferences soon
        Map<String, SearchEngineRule> map = new HashMap<String, SearchEngineRule>();
        map.put("Google", new SearchEngineRule("google.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("MSN", new SearchEngineRule("search.msn.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("Yahoo", new SearchEngineRule("search.yahoo.", "s/(^|.*&)p=(.*?)(&.*|$)/$2/"));
        map.put("Voila", new SearchEngineRule("voila.fr", "s/(^|.*&)kw=(.*?)(&.*|$)/$2/"));

        return map;
    }

    public String getRefererText(String referer, XWikiContext context)
    {
        try {
            URL url = new URL(referer);
            Map<String, SearchEngineRule> searchengines = getSearchEngineRules(context);
            if (searchengines != null) {
                for (SearchEngineRule senginerule : searchengines.values()) {
                    String host = url.getHost();
                    int i1 = host.indexOf(senginerule.getHost());
                    if (i1 != -1) {
                        String query = context.getUtil().substitute(senginerule.getRegEx(), url.getQuery());
                        if ((query != null) && (!query.equals(""))) {
                            // We return the query text instead of the full referer
                            return host.substring(i1) + ":" + query;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        String result = referer.substring(referer.indexOf("://") + 3);
        if (result.endsWith("/")) {
            return result.substring(0, result.length() - 1);
        } else {
            return result;
        }
    }

    public boolean isMySQL()
    {
        if (getHibernateStore() == null) {
            return false;
        }

        Object dialect = getHibernateStore().getConfiguration().getProperties().get("dialect");
        return "org.hibernate.dialect.MySQLDialect".equals(dialect)
            || "net.sf.hibernate.dialect.MySQLDialect".equals(dialect);
    }

    public String getFullNameSQL()
    {
        return getFullNameSQL(true);
    }

    public String getFullNameSQL(boolean newFullName)
    {
        if (newFullName) {
            return "doc.fullName";
        }

        if (this.fullNameSQL == null) {
            if (isMySQL()) {
                this.fullNameSQL = "CONCAT(doc.space,'.',doc.name)";
            } else {
                this.fullNameSQL = "doc.space||'.'||doc.name";
            }
        }

        return this.fullNameSQL;
    }

    public String getUserName(String user, XWikiContext context)
    {
        return getUserName(user, null, true, context);
    }

    public String getUserName(String user, String format, XWikiContext context)
    {
        return getUserName(user, format, true, context);
    }

    /**
     * @return a formatted and pretty printed user name for displaying
     */
    public String getUserName(String user, String format, boolean link, XWikiContext context)
    {
        if (StringUtils.isBlank(user)) {
            return localizePlainOrKey("core.users.unknownUser");
        }

        DocumentReference userReference = getCurrentMixedDocumentReferenceResolver().resolve(user);

        return getUserName(userReference, format, link, true, context);
    }

    /**
     * Generate a display user name and return it.
     *
     * @param userReference
     * @param format a Velocity scnippet used to format the user name
     * @param link true if a full html link snippet should be returned
     * @param escapeXML true if the returned name should be escaped (forced true if {@code link} is true)
     * @param context see {@link XWikiContext}
     * @return the display user name or a html snippet with the link to the passed user
     * @since 6.4RC1
     */
    public String getUserName(DocumentReference userReference, String format, boolean link, boolean escapeXML,
        XWikiContext context)
    {
        if (userReference == null) {
            return localizePlainOrKey("core.users.unknownUser");
        }

        XWikiDocument userdoc = null;
        try {
            userdoc = getDocument(userReference, context);
            if (userdoc == null) {
                return escapeXML ? XMLUtils.escape(userReference.getName()) : userReference.getName();
            }

            BaseObject userobj = userdoc.getObject("XWiki.XWikiUsers");
            if (userobj == null) {
                return escapeXML ? XMLUtils.escape(userdoc.getDocumentReference().getName())
                    : userdoc.getDocumentReference().getName();
            }

            String text;

            if (format == null) {
                text = userobj.getStringValue("first_name");
                String lastName = userobj.getStringValue("last_name");
                if (!text.isEmpty() && !lastName.isEmpty()) {
                    text += ' ';
                }
                text += userobj.getStringValue("last_name");
                if (StringUtils.isBlank(text)) {
                    text = userdoc.getDocumentReference().getName();
                }
            } else {
                VelocityContext vcontext = new VelocityContext();
                for (String propname : userobj.getPropertyList()) {
                    vcontext.put(propname, userobj.getStringValue(propname));
                }
                text = evaluateVelocity(format,
                    "<username formatting code in " + context.getDoc().getDocumentReference() + ">", vcontext);
            }

            if (escapeXML || link) {
                text = XMLUtils.escape(text.trim());
            }

            if (link) {
                text = "<span class=\"wikilink\"><a href=\"" + userdoc.getURL("view", context) + "\">" + text
                    + "</a></span>";
            }
            return text;
        } catch (Exception e) {
            LOGGER.warn("Failed to display the user name of [{}]. Root cause is [{}]. Falling back on the user id.",
                userReference, ExceptionUtils.getRootCauseMessage(e));

            return escapeXML ? XMLUtils.escape(userReference.getName()) : userReference.getName();
        }
    }

    /**
     * @param content the Velocity content to evaluate
     * @param namespace the namespace under which to evaluate it (used for isolation)
     * @param vcontext the Velocity context to use when evaluating. If {@code null}, then a new context will be created,
     *            initialized and used.
     * @return the evaluated content
     * @since 7.2M1
     */
    public String evaluateVelocity(String content, String namespace, VelocityContext vcontext)
    {
        try {
            return getVelocityEvaluator().evaluateVelocity(content, namespace, vcontext);
        } catch (XWikiException xe) {
            LOGGER.error("Error while parsing velocity template namespace [{}] with content:\n[{}]", namespace, content,
                    xe.getCause());
            return Util.getHTMLExceptionMessage(xe, null);
        }
    }

    /**
     * @param content the Velocity content to evaluate
     * @param name the namespace under which to evaluate it (used for isolation)
     * @return the evaluated content
     * @since 7.2M1
     */
    public String evaluateVelocity(String content, String name)
    {
        try {
            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            VelocityContext velocityContext = velocityManager.getVelocityContext();
            return evaluateVelocity(content, name, velocityContext);
        } catch (Exception e) {
            LOGGER.error("Error while parsing velocity template namespace [{}] with content:\n[{}]", name, content, e);
            Object[] args = { name };
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION, "Error while parsing velocity page {0}", e,
                args);
            return Util.getHTMLExceptionMessage(xe, null);
        }
    }

    /**
     * Generate and return an unescaped user display name.
     *
     * @param userReference the user reference
     * @param context see {@link XWikiContext}
     * @return the unescaped display user name
     * @since 6.4RC1
     */
    public String getPlainUserName(DocumentReference userReference, XWikiContext context)
    {
        return getUserName(userReference, null, false, false, context);
    }

    public boolean hasCentralizedAuthentication(XWikiContext context)
    {
        String bl = getXWikiPreference("authentication_centralized", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.authentication.centralized", "0"));
    }

    public String getLocalUserName(String user, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, null, true, context);
        } else {
            return getUserName(user.substring(user.indexOf(':') + 1), null, true, context);
        }
    }

    public String getLocalUserName(String user, String format, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, true, context);
        } else {
            return getUserName(user.substring(user.indexOf(':') + 1), format, true, context);
        }
    }

    public String getLocalUserName(String user, String format, boolean link, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, link, context);
        } else {
            return getUserName(user.substring(user.indexOf(':') + 1), format, link, context);
        }
    }

    public String formatDate(Date date, String format, XWikiContext context)
    {
        if (date == null) {
            return "";
        }
        String xformat = format;
        String defaultFormat = "yyyy/MM/dd HH:mm";

        if (format == null) {
            xformat = getXWikiPreference("dateformat", defaultFormat, context);
        }

        try {
            DateFormatSymbols formatSymbols = null;
            try {
                String language = getLanguagePreference(context);
                formatSymbols = new DateFormatSymbols(new Locale(language));
            } catch (Exception e2) {
                String language = getXWikiPreference("default_language", context);
                if ((language != null) && (!language.equals(""))) {
                    formatSymbols = new DateFormatSymbols(new Locale(language));
                }
            }

            SimpleDateFormat sdf;
            if (formatSymbols != null) {
                sdf = new SimpleDateFormat(xformat, formatSymbols);
            } else {
                sdf = new SimpleDateFormat(xformat);
            }

            try {
                sdf.setTimeZone(TimeZone.getTimeZone(getUserTimeZone(context)));
            } catch (Exception e) {
            }

            return sdf.format(date);
        } catch (Exception e) {
            LOGGER.info("Failed to format date [" + date + "] with pattern [" + xformat + "]: " + e.getMessage());
            if (format == null) {
                if (xformat.equals(defaultFormat)) {
                    return date.toString();
                } else {
                    return formatDate(date, defaultFormat, context);
                }
            } else {
                return formatDate(date, null, context);
            }
        }
    }

    /*
     * Allow to read user setting providing the user timezone All dates will be expressed with this timezone
     */
    public String getUserTimeZone(XWikiContext context)
    {
        String tz = getUserPreference("timezone", context);
        // We perform this verification ourselves since TimeZone#getTimeZone(String) with an invalid parameter returns
        // GMT and not the system default.
        if (!ArrayUtils.contains(TimeZone.getAvailableIDs(), tz)) {
            String defaultTz = TimeZone.getDefault().getID();
            return getConfiguration().getProperty("xwiki.timezone", defaultTz);
        } else {
            return tz;
        }
    }

    /**
     * @deprecated since 2.2.1 use {@link #exists(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public boolean exists(String fullname, XWikiContext context)
    {
        return exists(getCurrentMixedDocumentReferenceResolver().resolve(fullname), context);
    }

    public boolean exists(DocumentReference documentReference, XWikiContext context)
    {
        String currentWiki = context.getWikiId();

        try {
            XWikiDocument doc = new XWikiDocument(documentReference, documentReference.getLocale());

            context.setWikiId(documentReference.getWikiReference().getName());

            return getStore().exists(doc, context);
        } catch (XWikiException e) {
            return false;
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    public String getAdType(XWikiContext context)
    {
        String adtype = "";
        XWikiDocument wikiServer = context.getWikiServer();
        if (wikiServer != null) {
            adtype = wikiServer.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "adtype");
        }

        if (adtype.equals("")) {
            adtype = getConfiguration().getProperty("xwiki.ad.type", "");
        }

        return adtype;
    }

    public String getAdClientId(XWikiContext context)
    {
        final String defaultadclientid = "pub-2778691407285481";
        String adclientid = "";
        XWikiDocument wikiServer = context.getWikiServer();
        if (wikiServer != null) {
            adclientid = wikiServer.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "adclientid");
        }

        if (adclientid.equals("")) {
            adclientid = getConfiguration().getProperty("xwiki.ad.clientid", "");
        }

        if (adclientid.equals("")) {
            adclientid = defaultadclientid;
        }

        return adclientid;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public XWikiPluginInterface getPlugin(String name, XWikiContext context)
    {
        XWikiPluginManager plugins = getPluginManager();
        Vector<String> pluginlist = plugins.getPlugins();
        for (String pluginname : pluginlist) {
            if (pluginname.equals(name)) {
                return plugins.getPlugin(pluginname);
            }
        }

        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Api getPluginApi(String name, XWikiContext context)
    {
        XWikiPluginInterface plugin = getPlugin(name, context);
        if (plugin != null) {
            return plugin.getPluginApi(plugin, context);
        }

        return null;
    }

    public int getHttpTimeout(XWikiContext context)
    {
        return getConfiguration().getProperty("xwiki.http.timeout", 60000);
    }

    public String getHttpUserAgent(XWikiContext context)
    {
        return getConfiguration().getProperty("xwiki.http.useragent", "XWikiBot/1.0");
    }

    public String getURLContent(String surl, XWikiContext context) throws IOException
    {
        return getURLContent(surl, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public String getURLContent(String surl, int timeout, String userAgent) throws IOException
    {
        String content;
        HttpClient client = getHttpClient(timeout, userAgent);
        GetMethod get = new GetMethod(surl);

        try {
            client.executeMethod(get);
            content = get.getResponseBodyAsString();
        } finally {
            // Release any connection resources used by the method
            get.releaseConnection();
        }

        return content;
    }

    public String getURLContent(String surl, String username, String password, XWikiContext context) throws IOException
    {
        return getURLContent(surl, username, password, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public String getURLContent(String surl, String username, String password, int timeout, String userAgent)
        throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(new AuthScope(null, -1, null),
            new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests. Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBodyAsString();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl, XWikiContext context) throws IOException
    {
        return getURLContentAsBytes(surl, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public byte[] getURLContentAsBytes(String surl, int timeout, String userAgent) throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl, String username, String password, XWikiContext context)
        throws IOException
    {
        return getURLContentAsBytes(surl, username, password, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public byte[] getURLContentAsBytes(String surl, String username, String password, int timeout, String userAgent)
        throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(new AuthScope(null, -1, null),
            new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests. Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    /**
     * API to list all spaces in the current wiki.
     * <p>
     * Hidden spaces are filtered unless current user enabled them.
     *
     * @return a list of string representing all non-hidden spaces (ie spaces that have non-hidden pages) for the
     *         current wiki
     * @throws XWikiException if something went wrong
     * @deprecated use query service instead
     */
    @Deprecated
    public List<String> getSpaces(XWikiContext context) throws XWikiException
    {
        try {
            return getStore().getQueryManager().getNamedQuery("getSpaces")
                .addFilter(Utils.<QueryFilter>getComponent(QueryFilter.class, "hidden")).execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    /**
     * API to list all non-hidden documents in a space.
     *
     * @param spaceReference the local reference of the space for which to return all non-hidden documents
     * @return the list of document names (in the format {@code Space.Page}) for non-hidden documents in the specified
     *         space
     * @throws XWikiException if the loading went wrong
     * @deprecated use query service instead
     */
    @Deprecated
    public List<String> getSpaceDocsName(String spaceReference, XWikiContext context) throws XWikiException
    {
        try {
            return getStore().getQueryManager().getNamedQuery("getSpaceDocsName")
                .addFilter(Utils.<QueryFilter>getComponent(QueryFilter.class, "hidden"))
                .bindValue("space", spaceReference).execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    public List<String> getIncludedMacros(String defaultSpace, String content, XWikiContext context)
    {
        List<String> list;

        try {
            String pattern = "#includeMacros[ ]*\\([ ]*([\"'])(.*?)\\1[ ]*\\)";
            list = context.getUtil().getUniqueMatches(content, pattern, 2);
            for (int i = 0; i < list.size(); i++) {
                String name = list.get(i);
                if (name.indexOf('.') == -1) {
                    list.set(i, defaultSpace + "." + name);
                }
            }
        } catch (Exception e) {
            // This should never happen
            LOGGER.error("Failed to extract #includeMacros targets from provided content [" + content + "]", e);

            list = Collections.emptyList();
        }

        return list;
    }

    /**
     * accessor for the isReadOnly instance var.
     *
     * @see #isReadOnly
     */
    public boolean isReadOnly()
    {
        return this.isReadOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.isReadOnly = readOnly;
    }

    public void deleteAllDocuments(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        deleteAllDocuments(doc, true, context);
    }

    public void deleteAllDocuments(XWikiDocument doc, boolean toTrash, XWikiContext context) throws XWikiException
    {
        // Wrap the work as a batch operation.
        BatchOperationExecutor batchOperationExecutor = Utils.getComponent(BatchOperationExecutor.class);
        batchOperationExecutor.execute(() -> {
            // Delete all translation documents
            for (Locale locale : doc.getTranslationLocales(context)) {
                XWikiDocument tdoc = doc.getTranslatedDocument(locale, context);
                deleteDocument(tdoc, toTrash, context);
            }

            // Delete the default document
            deleteDocument(doc, toTrash, context);
        });
    }

    public void refreshLinks(XWikiContext context) throws XWikiException
    {
        try {
            // refreshes all Links of each doc of the wiki
            @SuppressWarnings("deprecation")
            List<String> docs = getStore().getQueryManager().getNamedQuery("getAllDocuments")
                .addFilter(Utils.<QueryFilter>getComponent(QueryFilter.class, "hidden")).execute();
            for (int i = 0; i < docs.size(); i++) {
                XWikiDocument myDoc = this.getDocument(docs.get(i), context);
                myDoc.getStore().saveLinks(myDoc, context, true);
            }
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    public boolean hasBacklinks(XWikiContext context)
    {
        if (this.hasBacklinks == null) {
            this.hasBacklinks = "1".equals(getXWikiPreference("backlinks", "xwiki.backlinks", "0", context));
        }
        return this.hasBacklinks;
    }

    public boolean hasTags(XWikiContext context)
    {
        return "1".equals(getXWikiPreference("tags", "xwiki.tags", "0", context));
    }

    public boolean hasCustomMappings()
    {
        return "1".equals(getConfiguration().getProperty("xwiki.store.hibernate.custommapping", "1"));
    }

    public boolean hasDynamicCustomMappings()
    {
        return "1".equals(getConfiguration().getProperty("xwiki.store.hibernate.custommapping.dynamic", "0"));
    }

    public String getDefaultSpace(XWikiContext context)
    {
        String defaultSpace = getXWikiPreference("defaultweb", "", context);
        if (StringUtils.isEmpty(defaultSpace)) {
            return getConfiguration().getProperty("xwiki.defaultweb", DEFAULT_HOME_SPACE);
        }
        return defaultSpace;
    }

    public boolean showViewAction(XWikiContext context)
    {
        String bl = getXWikiPreference("showviewaction", "", context);
        if ("1".equals(bl)) {
            return true;
        } else if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.showviewaction", "1"));
    }

    public boolean useDefaultAction(XWikiContext context)
    {
        String bl = getXWikiPreference("usedefaultaction", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.usedefaultaction", "0"));
    }

    public String getDefaultPage(XWikiContext context)
    {
        String defaultPage = getXWikiPreference("defaultpage", "", context);
        if (StringUtils.isEmpty(defaultPage)) {
            return getConfiguration().getProperty("xwiki.defaultpage", DEFAULT_SPACE_HOMEPAGE);
        }
        return defaultPage;
    }

    public boolean hasEditComment(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.editcomment", "0"));
    }

    public boolean isEditCommentFieldHidden(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_hidden", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.editcomment.hidden", "0"));
    }

    public boolean isEditCommentSuggested(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_suggested", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.editcomment.suggested", "0"));
    }

    public boolean isEditCommentMandatory(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_mandatory", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.editcomment.mandatory", "0"));
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#hasMinorEdit()
     */
    public boolean hasMinorEdit(XWikiContext context)
    {
        String bl = getXWikiPreference("minoredit", "", context);
        if ("1".equals(bl)) {
            return true;
        }

        if ("0".equals(bl)) {
            return false;
        }

        return "1".equals(getConfiguration().getProperty("xwiki.minoredit", "1"));
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#hasRecycleBin()
     * @param context see {@link XWikiContext}
     */
    public boolean hasRecycleBin(XWikiContext context)
    {
        return "1".equals(getConfiguration().getProperty("xwiki.recyclebin", "1"));
    }

    /**
     * Indicates whether deleted attachments are stored in a recycle bin or not. This can be configured using the key
     * <var>storage.attachment.recyclebin</var>.
     *
     * @param context see {@link XWikiContext}
     */
    public boolean hasAttachmentRecycleBin(XWikiContext context)
    {
        return "1".equals(getConfiguration().getProperty("storage.attachment.recyclebin", "1"));
    }

    /**
     * @deprecated use {@link XWikiDocument#rename(String, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument renamePage(XWikiDocument doc, String newFullName, XWikiContext context) throws XWikiException
    {
        if (context.getWiki().exists(newFullName, context)) {
            XWikiDocument delDoc = context.getWiki().getDocument(newFullName, context);
            context.getWiki().deleteDocument(delDoc, context);
        }

        XWikiDocument renamedDoc = doc.copyDocument(newFullName, context);
        saveDocument(renamedDoc, context);
        renamedDoc.saveAllAttachments(context);
        deleteDocument(doc, context);

        return renamedDoc;
    }

    /**
     * @deprecated use {@link XWikiDocument#rename(String, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument renamePage(XWikiDocument doc, XWikiContext context, String newFullName) throws XWikiException
    {
        return renamePage(doc, newFullName, context);
    }

    /**
     * @since 2.2M2
     */
    public BaseClass getXClass(DocumentReference documentReference, XWikiContext context) throws XWikiException
    {
        // Used to avoid recursive loading of documents if there are recursives usage of classes
        BaseClass bclass = context.getBaseClass(documentReference);
        if (bclass != null) {
            return bclass;
        }

        return getDocument(documentReference, context).getXClass();
    }

    /**
     * @deprecated since 2.2M2 use {@link #getXClass(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public BaseClass getClass(String fullName, XWikiContext context) throws XWikiException
    {
        DocumentReference reference = null;
        if (StringUtils.isNotEmpty(fullName)) {
            reference = getCurrentMixedDocumentReferenceResolver().resolve(fullName);
        }
        return getXClass(reference, context);
    }

    public String getEditorPreference(XWikiContext context)
    {
        String defaultSyntaxContentEditor = getEditConfiguration().getDefaultEditor(SyntaxContent.class);

        return defaultSyntaxContentEditor == null ? "" : defaultSyntaxContentEditor.toLowerCase();
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String. Note that Groovy scripts
     * compilation is cached.
     *
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromString(String script, XWikiContext xcontext) throws XWikiException
    {
        return getParseGroovyFromString().parseGroovyFromString(script, xcontext);
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String, using a classloader including all
     * JAR files located in the passed page as attachments. Note that Groovy scripts compilation is cached
     *
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromString(String script, String jarWikiPage, XWikiContext xcontext) throws XWikiException
    {
        XWikiPageClassLoader pcl = new XWikiPageClassLoader(jarWikiPage, xcontext);
        Object prevParentClassLoader = xcontext.get("parentclassloader");
        try {
            xcontext.put("parentclassloader", pcl);

            return parseGroovyFromString(script, xcontext);
        } finally {
            if (prevParentClassLoader == null) {
                xcontext.remove("parentclassloader");
            } else {
                xcontext.put("parentclassloader", prevParentClassLoader);
            }
        }
    }

    public Object parseGroovyFromPage(String fullname, XWikiContext context) throws XWikiException
    {
        return parseGroovyFromString(context.getWiki().getDocument(fullname, context).getContent(), context);
    }

    public Object parseGroovyFromPage(String fullName, String jarWikiPage, XWikiContext context) throws XWikiException
    {
        return parseGroovyFromString(context.getWiki().getDocument(fullName, context).getContent(), jarWikiPage,
            context);
    }

    public String getMacroList(XWikiContext context)
    {
        String macrosmapping = "";
        XWiki xwiki = context.getWiki();

        try {
            macrosmapping = getResourceContent(MACROS_FILE);
        } catch (IOException e) {
        }

        macrosmapping += "\r\n" + xwiki.getXWikiPreference("macros_mapping", "", context);

        return macrosmapping;
    }

    // This functions adds an object from an new object creation form
    public BaseObject getObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        Map<String, String[]> map = Util.getObject(context.getRequest(), className);
        BaseClass bclass = context.getWiki().getClass(className, context);
        BaseObject newobject = (BaseObject) bclass.fromMap(map, context);

        return newobject;
    }

    public String getConvertingUserNameType(XWikiContext context)
    {
        if (StringUtils.isNotBlank(context.getWiki().getXWikiPreference("convertmail", context))) {
            return context.getWiki().getXWikiPreference("convertmail", "0", context);
        }

        return getConfiguration().getProperty("xwiki.authentication.convertemail", "0");
    }

    public String convertUsername(String username, XWikiContext context)
    {
        if (username == null) {
            return null;
        }

        if (getConvertingUserNameType(context).equals("1") && (username.indexOf('@') != -1)) {
            String id = "" + username.hashCode();
            id = id.replace("-", "");
            if (username.length() > 1) {
                int i1 = username.indexOf('@');
                id = "" + username.charAt(0) + username.substring(i1 + 1, i1 + 2)
                    + username.charAt(username.length() - 1) + id;
            }

            return id;
        } else if (getConvertingUserNameType(context).equals("2")) {
            return username.replaceAll("[\\.\\@]", "_");
        } else {
            return username;
        }
    }

    public boolean hasSectionEdit(XWikiContext context)
    {
        return getConfiguration().getProperty("xwiki.section.edit", 0) == 1;
    }

    /**
     * @return The maximum section depth for which section editing is available. This can be customized through the
     *         {@code xwiki.section.depth} configuration property. Defaults to 2 when not defined.
     */
    public long getSectionEditingDepth()
    {
        return getConfiguration().getProperty("xwiki.section.depth", 2L);
    }

    public String getWysiwygToolbars(XWikiContext context)
    {
        return getConfiguration().getProperty("xwiki.wysiwyg.toolbars", "");
    }

    public String clearName(String name, XWikiContext context)
    {
        return clearName(name, true, true, context);
    }

    public String clearName(String name, boolean stripDots, boolean ascii, XWikiContext context)
    {
        String temp = name;
        temp = temp.replaceAll(
            "[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]", "A");
        temp = temp.replaceAll(
            "[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]", "a");
        temp = temp.replaceAll("[\u00c6\u01e2\u01fc]", "AE");
        temp = temp.replaceAll("[\u00e6\u01e3\u01fd]", "ae");
        temp = temp.replaceAll("[\u008c\u0152]", "OE");
        temp = temp.replaceAll("[\u009c\u0153]", "oe");
        temp = temp.replaceAll("[\u00c7\u0106\u0108\u010a\u010c]", "C");
        temp = temp.replaceAll("[\u00e7\u0107\u0109\u010b\u010d]", "c");
        temp = temp.replaceAll("[\u00d0\u010e\u0110]", "D");
        temp = temp.replaceAll("[\u00f0\u010f\u0111]", "d");
        temp = temp.replaceAll("[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]", "E");
        temp = temp.replaceAll("[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]", "e");
        temp = temp.replaceAll("[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]", "G");
        temp = temp.replaceAll("[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]", "g");
        temp = temp.replaceAll("[\u0124\u0126\u021e]", "H");
        temp = temp.replaceAll("[\u0125\u0127\u021f]", "h");
        temp = temp.replaceAll("[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]", "I");
        temp = temp.replaceAll("[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]", "i");
        temp = temp.replaceAll("[\u0132]", "IJ");
        temp = temp.replaceAll("[\u0133]", "ij");
        temp = temp.replaceAll("[\u0134]", "J");
        temp = temp.replaceAll("[\u0135]", "j");
        temp = temp.replaceAll("[\u0136\u01e8]", "K");
        temp = temp.replaceAll("[\u0137\u0138\u01e9]", "k");
        temp = temp.replaceAll("[\u0139\u013b\u013d\u013f\u0141]", "L");
        temp = temp.replaceAll("[\u013a\u013c\u013e\u0140\u0142\u0234]", "l");
        temp = temp.replaceAll("[\u00d1\u0143\u0145\u0147\u014a\u01f8]", "N");
        temp = temp.replaceAll("[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]", "n");
        temp = temp.replaceAll(
            "[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c"
                + "\u022e\u0230]",
            "O");
        temp = temp.replaceAll(
            "[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d"
                + "\u022f\u0231]",
            "o");
        temp = temp.replaceAll("[\u0156\u0158\u0210\u0212]", "R");
        temp = temp.replaceAll("[\u0157\u0159\u0211\u0213]", "r");
        temp = temp.replaceAll("[\u015a\u015c\u015e\u0160\u0218]", "S");
        temp = temp.replaceAll("[\u015b\u015d\u015f\u0161\u0219]", "s");
        temp = temp.replaceAll("[\u00de\u0162\u0164\u0166\u021a]", "T");
        temp = temp.replaceAll("[\u00fe\u0163\u0165\u0167\u021b\u0236]", "t");
        temp = temp.replaceAll(
            "[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]",
            "U");
        temp = temp.replaceAll(
            "[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]",
            "u");
        temp = temp.replaceAll("[\u0174]", "W");
        temp = temp.replaceAll("[\u0175]", "w");
        temp = temp.replaceAll("[\u00dd\u0176\u0178\u0232]", "Y");
        temp = temp.replaceAll("[\u00fd\u00ff\u0177\u0233]", "y");
        temp = temp.replaceAll("[\u0179\u017b\u017d]", "Z");
        temp = temp.replaceAll("[\u017a\u017c\u017e]", "z");
        temp = temp.replaceAll("[\u00df]", "SS");
        temp = temp.replaceAll("[_':,;\\\\/]", " ");
        name = temp;
        name = name.replaceAll("\\s+", "");
        name = name.replaceAll("[\\(\\)]", " ");

        if (stripDots) {
            name = name.replaceAll("[\\.]", "");
        }

        if (ascii) {
            name = name.replaceAll("[^a-zA-Z0-9\\-_\\.]", "");
        }

        if (name.length() > 250) {
            name = name.substring(0, 250);
        }

        return name;

    }

    public String getUniquePageName(String space, XWikiContext context)
    {
        String pageName = generateRandomString(16);

        return getUniquePageName(space, pageName, context);
    }

    public String getUniquePageName(String space, String name, XWikiContext context)
    {
        String pageName = clearName(name, context);
        if (exists(space + "." + pageName, context)) {
            int i = 0;
            while (exists(space + "." + pageName + "_" + i, context)) {
                i++;
            }

            return pageName + "_" + i;
        }

        return pageName;
    }

    public PropertyClass getPropertyClassFromName(String propPath, XWikiContext context)
    {
        int i1 = propPath.indexOf('_');
        if (i1 == -1) {
            return null;
        } else {
            String className = propPath.substring(0, i1);
            String propName = propPath.substring(i1 + 1);
            try {
                return (PropertyClass) getDocument(className, context).getXClass().get(propName);
            } catch (XWikiException e) {
                return null;
            }
        }
    }

    public boolean validateDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return doc.validate(context);
    }

    public String addTooltip(String html, String message, String params, XWikiContext context)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<span class=\"tooltip_span\" onmouseover=\"");
        buffer.append(params);
        buffer.append("; return escape('");
        buffer.append(message.replaceAll("'", "\\'"));
        buffer.append("');\">");
        buffer.append(html);
        buffer.append("</span>");

        return buffer.toString();
    }

    public String addTooltipJS(XWikiContext context)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<script type=\"text/javascript\" src=\"");
        buffer.append(getSkinFile("ajax/wzToolTip.js", context));
        buffer.append("\"></script>");
        // buffer.append("<div id=\"dhtmltooltip\"></div>");

        return buffer.toString();
    }

    public String addTooltip(String html, String message, XWikiContext context)
    {
        return addTooltip(html, message, "this.WIDTH='300'", context);
    }

    public void renamePage(String fullName, String newFullName, XWikiContext context) throws XWikiException
    {
        renamePage(context.getWiki().getDocument(fullName, context), newFullName, context);
    }

    public String addMandatory(XWikiContext context)
    {
        String star =
            "<span class=\"mandatoryParenthesis\">&nbsp;(</span><span class=\"mandatoryDot\">&lowast;</span><span class=\"mandatoryParenthesis\">)&nbsp;</span>";
        return context.getWiki().getXWikiPreference("mandatory_display", star, context);
    }

    /**
     * @since 2.3M1
     */
    public boolean hasVersioning(XWikiContext context)
    {
        return ("1".equals(getConfiguration().getProperty("xwiki.store.versioning", "1")));
    }

    public boolean hasAttachmentVersioning(XWikiContext context)
    {
        return ("1".equals(getConfiguration().getProperty("xwiki.store.attachment.versioning", "1")));
    }

    public String getExternalAttachmentURL(String fullName, String filename, XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullName, context);

        return doc.getExternalAttachmentURL(filename, "download", context);
    }

    public int getMaxRecursiveSpaceChecks(XWikiContext context)
    {
        int max = getXWikiPreferenceAsInt("rights_maxrecursivespacechecks", -1, context);
        if (max == -1) {
            return getConfiguration().getProperty("xwiki.rights.maxrecursivespacechecks", 0);
        } else {
            return max;
        }
    }

    /**
     * Restore a document with passed index from recycle bin.
     *
     * @param doc the document to restore
     * @param comment the comment to use when saving the document
     * @param context see {@link XWikiContext}
     * @throws XWikiException when failing to restore document
     * @since 5.4RC1
     */
    public void restoreFromRecycleBin(final XWikiDocument doc, String comment, XWikiContext context)
        throws XWikiException
    {
        XWikiDeletedDocument[] deletedDocuments = getRecycleBinStore().getAllDeletedDocuments(doc, context, true);
        if (deletedDocuments != null && deletedDocuments.length > 0) {
            long index = deletedDocuments[0].getId();
            restoreFromRecycleBin(doc, index, comment, context);
        }
    }

    /**
     * Restore a document with passed index from recycle bin.
     *
     * @param doc the document to restore
     * @param index the index of the document in the recycle bin
     * @param comment the comment to use when saving the document
     * @param context see {@link XWikiContext}
     * @throws XWikiException when failing to restore document
     * @since 5.4RC1
     * @deprecated since 9.4RC1. Use {@link #restoreFromRecycleBin(long, String, XWikiContext)} instead.
     */
    @Deprecated
    public void restoreFromRecycleBin(final XWikiDocument doc, long index, String comment, XWikiContext context)
        throws XWikiException
    {
        restoreFromRecycleBin(index, comment, context);
    }

    /**
     * Restore a document with passed index from recycle bin.
     *
     * @param index the index of the document in the recycle bin
     * @param comment the comment to use when saving the document
     * @param context see {@link XWikiContext}
     * @throws XWikiException when failing to restore document
     * @since 9.4RC1
     */
    public void restoreFromRecycleBin(long index, String comment, XWikiContext context) throws XWikiException
    {
        XWikiDocument newdoc = getRecycleBinStore().restoreFromRecycleBin(index, context, true);
        saveDocument(newdoc, comment, context);
        getRecycleBinStore().deleteFromRecycleBin(index, context, true);
    }

    public XWikiDocument rollback(final XWikiDocument tdoc, String rev, XWikiContext context) throws XWikiException
    {
        LOGGER.debug("Rolling back [{}] to version [{}]", tdoc, rev);

        // Let's clone rolledbackDoc since we might modify it
        XWikiDocument rolledbackDoc = getDocument(tdoc, rev, context).clone();

        if ("1".equals(getConfiguration().getProperty("xwiki.store.rollbackattachmentwithdocuments", "1"))) {
            // Attachment handling strategy:
            // - Two lists: Old Attachments, Current Attachments
            // Goals:
            // 1. Attachments that are only in OA must be restored from the trash
            // 2. Attachments that are only in CA must be sent to the trash
            // 3. Attachments that are in both lists should be reverted to the right version
            // 4. Gotcha: deleted and re-uploaded attachments should be both trashed and restored.
            // Plan:
            // - Construct two lists: to restore, to revert
            // - Iterate over OA.
            // -- If the attachment is not in CA, add it to the restore list
            // -- If it is in CA, but the date of the first version of the current attachment is after the date of the
            // restored document version, add it the restore & move the current attachment to the recycle bin
            // -- Otherwise, add it to the revert list
            // - Iterate over CA
            // -- If the attachment is not in OA, delete it

            List<XWikiAttachment> oldAttachments = rolledbackDoc.getAttachmentList();
            List<XWikiAttachment> currentAttachments = tdoc.getAttachmentList();
            List<XWikiAttachment> toRestore = new ArrayList<>();
            List<XWikiAttachment> toRevert = new ArrayList<>();

            // First step, determine what to do with each attachment
            LOGGER.debug("Checking attachments");

            for (XWikiAttachment oldAttachment : oldAttachments) {
                String filename = oldAttachment.getFilename();
                XWikiAttachment equivalentAttachment = tdoc.getAttachment(filename);
                if (equivalentAttachment == null) {
                    // Deleted attachment
                    LOGGER.debug("Deleted attachment: [{}]", filename);
                    toRestore.add(oldAttachment);
                    continue;
                }
                XWikiAttachment equivalentAttachmentRevision =
                    equivalentAttachment.getAttachmentRevision(oldAttachment.getVersion(), context);
                // We compare the number of milliseconds instead of the date objects directly because Hibernate can
                // return java.sql.Timestamp for date fields and the JavaDoc says that Timestamp.equals(Object) doesn't
                // return true if the passed value is a java.util.Date object with the same number of milliseconds
                // because the nanoseconds component of the passed date is unknown.
                if (equivalentAttachmentRevision == null
                    || equivalentAttachmentRevision.getDate().getTime() != oldAttachment.getDate().getTime()) {
                    // Recreated attachment
                    LOGGER.debug("Recreated attachment: [{}]", filename);
                    // If the attachment trash is not available, don't lose the existing attachment
                    if (getAttachmentRecycleBinStore() != null) {
                        getAttachmentRecycleBinStore().saveToRecycleBin(equivalentAttachment, context.getUser(),
                            new Date(), context, true);
                        toRestore.add(oldAttachment);
                    }
                    continue;
                }
                if (!StringUtils.equals(oldAttachment.getVersion(), equivalentAttachment.getVersion())) {
                    // Updated attachment
                    LOGGER.debug("Updated attachment: [{}]", filename);
                    toRevert.add(equivalentAttachment);
                }
            }
            for (XWikiAttachment attachment : currentAttachments) {
                if (rolledbackDoc.getAttachment(attachment.getFilename()) == null) {
                    LOGGER.debug("New attachment: " + attachment.getFilename());
                    // XWikiDocument#save() is actually the only way to delete an attachment cleanly
                    rolledbackDoc.getAttachmentsToRemove().add(new XWikiAttachmentToRemove(attachment, true));
                }
            }

            // Second step, treat each affected attachment

            // Revert updated attachments to the old version
            for (XWikiAttachment attachmentToRevert : toRevert) {
                String oldAttachmentVersion =
                    rolledbackDoc.getAttachment(attachmentToRevert.getFilename()).getVersion();
                XWikiAttachment oldAttachmentRevision =
                    attachmentToRevert.getAttachmentRevision(oldAttachmentVersion, context);
                if (oldAttachmentRevision == null) {
                    // Previous version is lost, just leave the current version in place
                    rolledbackDoc.setAttachment(attachmentToRevert);

                    continue;
                }
                // We can't just leave the old version in place, since it will break the revision history, given the
                // current implementation, so we set the attachment version to the most recent version, mark the content
                // as dirty, and the storage will automatically bump up the version number.
                // This is a hack, to be fixed once the storage doesn't take care of updating the history and version,
                // and once the current attachment version can point to an existing version from the history.
                oldAttachmentRevision.setVersion(attachmentToRevert.getVersion());
                oldAttachmentRevision.setMetaDataDirty(true);
                oldAttachmentRevision.getAttachment_content().setContentDirty(true);

                rolledbackDoc.setAttachment(oldAttachmentRevision);
            }

            // Restore deleted attachments from the trash
            if (getAttachmentRecycleBinStore() != null) {
                for (XWikiAttachment attachmentToRestore : toRestore) {
                    // There might be multiple versions of the attachment in the trash, search for the right one
                    List<DeletedAttachment> deletedVariants =
                        getAttachmentRecycleBinStore().getAllDeletedAttachments(attachmentToRestore, context, true);
                    DeletedAttachment correctVariant = null;
                    for (DeletedAttachment variant : deletedVariants) { // Reverse chronological order
                        if (variant.getDate().before(rolledbackDoc.getDate())) {
                            break;
                        }
                        correctVariant = variant;
                    }
                    if (correctVariant == null) {
                        // Not found in the trash, nothing left to do
                        continue;
                    }
                    XWikiAttachment restoredAttachment = correctVariant.restoreAttachment();
                    XWikiAttachment restoredAttachmentRevision =
                        restoredAttachment.getAttachmentRevision(attachmentToRestore.getVersion(), context);

                    if (restoredAttachmentRevision != null) {
                        restoredAttachmentRevision.setAttachment_archive(restoredAttachment.getAttachment_archive());
                        restoredAttachmentRevision.getAttachment_archive().setAttachment(restoredAttachmentRevision);
                        restoredAttachmentRevision.setVersion(restoredAttachment.getVersion());
                        restoredAttachmentRevision.setMetaDataDirty(true);
                        restoredAttachmentRevision.getAttachment_content().setContentDirty(true);

                        rolledbackDoc.setAttachment(restoredAttachmentRevision);
                    } else {
                        // This particular version is lost, update to the one available
                        rolledbackDoc.setAttachment(restoredAttachment);
                    }
                }
            } else {
                // No trash, can't restore. Remove the attachment references, so that the document is not broken
                for (XWikiAttachment attachmentToRestore : toRestore) {
                    rolledbackDoc.getAttachmentList().remove(attachmentToRestore);
                }
            }
        }

        // Special treatment for deleted objects
        rolledbackDoc.addXObjectsToRemoveFromVersion(tdoc);

        // now we save the final document..
        rolledbackDoc.setOriginalDocument(tdoc);
        rolledbackDoc.setAuthorReference(context.getUserReference());
        rolledbackDoc.setRCSVersion(tdoc.getRCSVersion());
        rolledbackDoc.setVersion(tdoc.getVersion());
        rolledbackDoc.setContentDirty(true);

        ObservationManager om = getObservationManager();
        if (om != null) {
            // Notify listeners about the document that is going to be rolled back.
            // Note that for the moment the event being send is a bridge event, as we are still passing around
            // an XWikiDocument as source and an XWikiContext as data.
            om.notify(new DocumentRollingBackEvent(rolledbackDoc.getDocumentReference(), rev), rolledbackDoc, context);
        }

        saveDocument(rolledbackDoc, localizePlainOrKey("core.comment.rollback", rev), context);

        // Since the the store resets the original document, we need to temporarily put it back to send notifications.
        XWikiDocument newOriginalDocument = rolledbackDoc.getOriginalDocument();
        rolledbackDoc.setOriginalDocument(tdoc);

        try {
            if (om != null) {
                // Notify listeners about the document that was rolled back.
                // Note that for the moment the event being send is a bridge event, as we are still passing around an
                // XWikiDocument as source and an XWikiContext as data.
                om.notify(new DocumentRolledBackEvent(rolledbackDoc.getDocumentReference(), rev), rolledbackDoc,
                    context);
            }
        } finally {
            rolledbackDoc.setOriginalDocument(newOriginalDocument);
        }

        return rolledbackDoc;
    }

    /**
     * @return the ids of configured syntaxes for this wiki (e.g. {@code xwiki/2.0}, {@code xwiki/2.1},
     *         {@code mediawiki/1.0}, etc)
     * @deprecated since 8.2M1, use the XWiki Rendering Configuration component or the Rendering Script Service one
     *             instead
     */
    @Deprecated
    public List<String> getConfiguredSyntaxes()
    {
        return this.configuredSyntaxes;
    }

    /**
     * @return the syntax id of the syntax to use when creating new documents
     */
    public String getDefaultDocumentSyntax()
    {
        // TODO: Fix this method to return a Syntax object instead of a String
        return getDefaultDocumentSyntaxInternal().toIdString();
    }

    /**
     * @return the syntax to use when creating new documents
     */
    private Syntax getDefaultDocumentSyntaxInternal()
    {
        return Utils.getComponent(CoreConfiguration.class).getDefaultDocumentSyntax();
    }

    /**
     * Get the syntax of the content currently being executed.
     * <p>
     * The document currently being executed is not the same than the actual content syntax since the executed code
     * might come from an included page or some macro that change the context syntax. The same logic used inside
     * rendering macros is used (see {@link org.xwiki.rendering.macro.MacroContentParser}).
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     *
     * @param defaultSyntaxId the default value to return if no document can be found
     * @return the syntax identifier
     */
    public String getCurrentContentSyntaxId(String defaultSyntaxId, XWikiContext context)
    {
        String syntaxId = getCurrentContentSyntaxIdInternal(context);

        if (syntaxId == null) {
            syntaxId = defaultSyntaxId;
        }

        return syntaxId;
    }

    /**
     * Get the syntax of the content currently being executed.
     * <p>
     * The document currently being executed is not the same than the actual content syntax since the executed code
     * might come from an included page or some macro that change the context syntax. The same logic used inside
     * rendering macros is used (see {@link org.xwiki.rendering.macro.MacroContentParser}).
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     *
     * @return the syntax identifier
     */
    public String getCurrentContentSyntaxId(XWikiContext context)
    {
        String syntaxId = getCurrentContentSyntaxIdInternal(context);

        if (syntaxId == null) {
            throw new RuntimeException("Cannot get the current syntax since there's no current document set");
        }

        return syntaxId;
    }

    /**
     * Get the syntax of the content currently being executed.
     * <p>
     * The document currently being executed is not the same than the actual content syntax since the executed code
     * might come from an included page or some macro that change the context syntax. The same logic used inside
     * rendering macros is used (see {@link org.xwiki.rendering.macro.MacroContentParser}).
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     *
     * @return the syntax identifier
     */
    private String getCurrentContentSyntaxIdInternal(XWikiContext context)
    {
        Syntax syntax = getCurrentContentSyntaxInternal(context);

        return syntax != null ? syntax.toIdString() : null;
    }

    /**
     * Get the syntax of the code currently being executed.
     * <p>
     * The document currently being executed is not the same than the actual content syntax since the executed code
     * might come from an included page or some macro that change the context syntax. The same logic used inside
     * rendering macros is used (see {@link org.xwiki.rendering.macro.MacroContentParser}).
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     *
     * @return the syntax
     */
    private Syntax getCurrentContentSyntaxInternal(XWikiContext context)
    {
        Syntax syntax = null;

        // Try to find the current syntax
        if (getRenderingContext() != null) {
            Block curentBlock = getRenderingContext().getCurrentBlock();

            if (curentBlock != null) {
                MetaDataBlock metaDataBlock =
                    curentBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.SYNTAX), Axes.ANCESTOR_OR_SELF);

                if (metaDataBlock != null) {
                    return (Syntax) metaDataBlock.getMetaData().getMetaData(MetaData.SYNTAX);
                }
            }
        }

        // Fallback on secure and current document in the context
        if (context.get("sdoc") != null) {
            // The content document
            syntax = ((XWikiDocument) context.get("sdoc")).getSyntax();
        } else if (context.getDoc() != null) {
            // The context document
            syntax = context.getDoc().getSyntax();
        }

        return syntax;
    }

    /**
     * @return true if title handling should be using the compatibility mode or not. When the compatibility mode is
     *         active, if the document's content first header (level 1 or level 2) matches the document's title the
     *         first header is stripped.
     */
    public boolean isTitleInCompatibilityMode()
    {
        return "1".equals(getConfiguration().getProperty("xwiki.title.compatibility", "0"));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            // A wiki has been deleted
            onWikiDeletedEvent((WikiDeletedEvent) event);
        } else if (event instanceof ComponentDescriptorAddedEvent) {
            // A new mandatory document initializer has been installed
            onMandatoryDocumentInitializerAdded((ComponentDescriptorAddedEvent) event, (ComponentManager) source);
        } else {
            // Document modifications

            XWikiDocument doc = (XWikiDocument) source;

            if (event instanceof XObjectPropertyEvent) {
                EntityReference reference = ((XObjectPropertyEvent) event).getReference();
                String modifiedProperty = reference.getName();
                if ("backlinks".equals(modifiedProperty)) {
                    this.hasBacklinks = doc.getXObject((ObjectReference) reference.getParent()).getIntValue("backlinks",
                        getConfiguration().getProperty("xwiki.backlinks", 0)) == 1;
                }
            }
        }
    }

    private void onWikiDeletedEvent(WikiDeletedEvent event)
    {
        this.initializedWikis.remove(event.getWikiId());
    }

    private void onMandatoryDocumentInitializerAdded(ComponentDescriptorAddedEvent event,
        ComponentManager componentManager)
    {
        String namespace;
        if (componentManager instanceof NamespacedComponentManager) {
            namespace = ((NamespacedComponentManager) componentManager).getNamespace();
        } else {
            namespace = null;
        }

        MandatoryDocumentInitializer initializer;
        try {
            initializer = componentManager.getInstance(MandatoryDocumentInitializer.class, event.getRoleHint());

            XWikiContext context = getXWikiContext();
            if (namespace == null) {
                // Initialize in main wiki
                initializeMandatoryDocument(context.getMainXWiki(), initializer, context);
                // Initialize in already initialized sub wikis (will be initialized in others when they are initialized)
                for (String wiki : this.initializedWikis.keySet()) {
                    initializeMandatoryDocument(wiki, initializer, context);
                }
            } else if (namespace.startsWith("wiki:")) {
                // Initialize in the wiki where the extension is installed
                initializeMandatoryDocument(namespace.substring("wiki:".length()), initializer, context);
            }
        } catch (ComponentLookupException e) {
            LOGGER.error("Failed to lookup mandatory document initializer", e);
        }
    }

    /**
     * The reference to match properties "plugins" and "backlinks" of class XWiki.XWikiPreference on whatever wiki.
     */
    private static final RegexEntityReference XWIKIPREFERENCE_PROPERTY_REFERENCE =
        XWikiPreferencesDocumentInitializer.OBJECT_REFERENCE;

    private static final List<Event> LISTENER_EVENTS =
        Arrays.<Event>asList(new XObjectPropertyAddedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE),
            new XObjectPropertyDeletedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE),
            new XObjectPropertyUpdatedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE), new WikiDeletedEvent(),
            new ComponentDescriptorAddedEvent(MandatoryDocumentInitializer.class));

    @Override
    public List<Event> getEvents()
    {
        return LISTENER_EVENTS;
    }

    @Override
    public String getName()
    {
        return "xwiki-core";
    }

    /**
     * Return the document reference to the wiki preferences.
     *
     * @param context see {@link XWikiContext}
     * @since 4.3M2
     */
    private DocumentReference getPreferencesDocumentReference(XWikiContext context)
    {
        String database = context.getWikiId();
        EntityReference spaceReference;
        if (database != null) {
            spaceReference = new EntityReference(SYSTEM_SPACE, EntityType.SPACE, new WikiReference(database));
        } else {
            spaceReference = getCurrentMixedEntityReferenceResolver().resolve(SYSTEM_SPACE, EntityType.SPACE);
        }
        return new DocumentReference("XWikiPreferences", new SpaceReference(spaceReference));
    }

    /**
     * Search attachments by passing HQL where clause values as parameters. You can specify properties of the "attach"
     * (the attachment) or "doc" (the document it is attached to)
     *
     * @param parametrizedSqlClause The HQL where clause. For example {@code where doc.fullName
     *        <> ? and (attach.author = ? or (attach.filename = ? and doc.space = ?))}
     * @param checkRight if true, only return attachments in documents which the "current user" has permission to view.
     * @param nb The number of rows to return. If 0 then all rows are returned
     * @param start The number of rows to skip at the beginning.
     * @param parameterValues A {@link java.util.List} of the where clause values that replace the question marks (?)
     * @param context see {@link XWikiContext}
     * @return A List of {@link XWikiAttachment} objects.
     * @throws XWikiException in case of error while performing the query
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(String, int, int, java.util.List, XWikiContext)
     * @since 5.0M2
     * @deprecated since 9.7RC1, use the QueryManager instead along with the "attachment" query filter
     */
    @Deprecated
    public List<XWikiAttachment> searchAttachments(String parametrizedSqlClause, boolean checkRight, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        parametrizedSqlClause = parametrizedSqlClause.trim().replaceFirst("^and ", "").replaceFirst("^where ", "");

        // Get the attachment filenames and document fullNames
        List<java.lang.Object[]> results = this.getStore().search(
            "select attach.filename, doc.fullName from XWikiAttachment attach, XWikiDocument doc where doc.id = attach.docId and "
                + parametrizedSqlClause,
            nb, start, parameterValues, context);

        HashMap<String, List<String>> filenamesByDocFullName = new HashMap<String, List<String>>();

        // Put each attachment name with the document name it belongs to
        for (int i = 0; i < results.size(); i++) {
            String filename = (String) results.get(i)[0];
            String docFullName = (String) results.get(i)[1];
            if (!filenamesByDocFullName.containsKey(docFullName)) {
                filenamesByDocFullName.put(docFullName, new ArrayList<String>());
            }
            filenamesByDocFullName.get(docFullName).add(filename);
        }

        List<XWikiAttachment> out = new ArrayList<XWikiAttachment>();

        // Index through the document names, get relivent attachments
        for (Map.Entry<String, List<String>> entry : filenamesByDocFullName.entrySet()) {
            String fullName = entry.getKey();

            XWikiDocument doc = getDocument(fullName, context);
            if (checkRight) {
                if (!context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), doc.getFullName(),
                    context)) {
                    continue;
                }
            }
            List<String> returnedAttachmentNames = entry.getValue();
            for (XWikiAttachment attach : doc.getAttachmentList()) {
                if (returnedAttachmentNames.contains(attach.getFilename())) {
                    out.add(attach);
                }
            }
        }

        return out;
    }

    /**
     * Count attachments returned by a given parameterized query
     *
     * @param parametrizedSqlClause Everything which would follow the "WHERE" in HQL
     * @param parameterValues A {@link java.util.List} of the where clause values that replace the question marks (?)
     * @param context see {@link XWikiContext}
     * @return int number of attachments found.
     * @throws XWikiException in event of an exception querying the database
     * @see #searchAttachments(String, boolean, int, int, java.util.List, XWikiContext)
     * @since 5.0M2
     */
    public int countAttachments(String parametrizedSqlClause, List<?> parameterValues, XWikiContext context)
        throws XWikiException
    {
        parametrizedSqlClause = parametrizedSqlClause.trim().replaceFirst("^and ", "").replaceFirst("^where ", "");

        List l = getStore().search("select count(attach) from XWikiAttachment attach, XWikiDocument doc where "
            + "attach.docId=doc.id and " + parametrizedSqlClause, 0, 0, parameterValues, context);
        return ((Number) l.get(0)).intValue();
    }

    // Deprecated

    /**
     * @deprecated since 6.1M2, use {@link XWikiCfgConfigurationSource#getConfigPath()} instead
     */
    @Deprecated
    public static String getConfigPath() throws NamingException
    {
        return XWikiCfgConfigurationSource.getConfigPath();
    }

    /**
     * @deprecated since 6.1M3, use {@link #XWiki(XWikiContext)} instead
     */
    @Deprecated
    public XWiki(XWikiConfig config, XWikiContext context) throws XWikiException
    {
        this(config, context, null, false);
    }

    /**
     * @deprecated since 6.1M3, use {@link #XWiki(XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public XWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate)
        throws XWikiException
    {
        initXWiki(config, context, engine_context, noupdate);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiContext)} instead
     */
    @Deprecated
    public XWiki(String xwikicfgpath, XWikiContext context) throws XWikiException
    {
        this(xwikicfgpath, context, null, false);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public XWiki(String xwikicfgpath, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate)
        throws XWikiException
    {
        try {
            initXWiki(new XWikiConfig(new FileInputStream(xwikicfgpath)), context, engine_context, noupdate);
        } catch (FileNotFoundException e) {
            Object[] args = { xwikicfgpath };
            throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG, XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND,
                "Configuration file {0} not found", e, args);
        }
    }

    /**
     * @deprecated use {@link #XWiki(XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public XWiki(InputStream is, XWikiContext context, XWikiEngineContext engine_context) throws XWikiException
    {
        initXWiki(new XWikiConfig(is), context, engine_context, true);
    }

    /**
     * @deprecated since 6.1M2, use {@link ConfigurationSource} component with hint <code>xwikicfg</code> instead
     */
    @Deprecated
    public XWikiConfig getConfig()
    {
        return new XWikiConfigDelegate(getConfiguration());
    }

    /**
     * @deprecated since 6.1M2
     */
    @Deprecated
    public void setConfig(XWikiConfig config)
    {
        ConfigurationSource configuration = getConfiguration();

        if (configuration instanceof XWikiCfgConfigurationSource) {
            ((XWikiCfgConfigurationSource) configuration).set(config);
        }
    }

    /**
     * @deprecated since 6.1M2, use {@link ConfigurationSource} component with hint <code>xwikicfg</code> instead
     */
    @Deprecated
    public String Param(String key)
    {
        return Param(key, null);
    }

    /**
     * @deprecated since 6.1M2, use {@link ConfigurationSource} component with hint <code>xwikicfg</code> instead
     */
    @Deprecated
    public String Param(String key, String default_value)
    {
        if (getConfiguration() != null) {
            return getConfiguration().getProperty(key, default_value);
        }

        return default_value;
    }

    /**
     * @deprecated since 6.1M2, use {@link ConfigurationSource} component with hint <code>xwikicfg</code> instead
     */
    @Deprecated
    public long ParamAsLong(String key)
    {
        return getConfiguration().getProperty(key, long.class);
    }

    /**
     * @deprecated since 6.1M2, use {@link ConfigurationSource} component with hint <code>xwikicfg</code> instead
     */
    @Deprecated
    public long ParamAsLong(String key, long default_value)
    {
        return getConfiguration().getProperty(key, default_value);
    }
}
