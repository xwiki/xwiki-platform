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
package com.xpn.xwiki.internal.template;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.ReaderInputSource;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.properties.RawProperties;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyId;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.WriterWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.skin.Resource;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.skin.Skin;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.internal.skin.AbstractSkinResource;
import com.xpn.xwiki.internal.skin.InternalSkinManager;
import com.xpn.xwiki.internal.skin.WikiResource;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Internal toolkit to experiment on templates.
 *
 * @version $Id$
 * @since 7.0M1
 */
@Component(roles = InternalTemplateManager.class)
@Singleton
public class InternalTemplateManager implements Initializable
{
    /**
     * The reference of the superadmin user.
     */
    public static final DocumentReference SUPERADMIN_REFERENCE =
        new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);

    private static final Pattern PROPERTY_LINE = Pattern.compile("^##!(.+)=(.*)$\r?\n?", Pattern.MULTILINE);

    private static final String TEMPLATE_RESOURCE_SUFFIX = "/templates/";

    @Inject
    private Environment environment;

    @Inject
    private ContentParser parser;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainRenderer;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Inject
    @Named("all")
    private ConfigurationSource allConfiguration;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private BeanManager beanManager;

    @Inject
    private ConverterManager converter;

    @Inject
    private InternalSkinManager skins;

    @Inject
    private JobProgressManager progress;

    @Inject
    private Provider<TemplateAsyncRenderer> rendererProvider;

    @Inject
    private BlockAsyncRendererExecutor asyncExecutor;

    @Inject
    private TemplateContext templateContext;

    @Inject
    private VelocityTemplateEvaluator evaluator;

    @Inject
    private Provider<ErrorBlockGenerator> errorBlockGeneratorProvider;

    @Inject
    private Logger logger;

    private String templateRootURL;

    private static abstract class AbtractTemplate<T extends TemplateContent, R extends Resource<?>> implements Template
    {
        protected R resource;

        protected T content;

        public AbtractTemplate(R resource)
        {
            this.resource = resource;
        }

        @Override
        public String getId()
        {
            return this.resource.getId();
        }

        @Override
        public String getPath()
        {
            return this.resource.getPath();
        }

        @Override
        public TemplateContent getContent() throws Exception
        {
            if (this.content == null) {
                // TODO: work with streams instead of forcing String
                String strinContent;

                try (InputSource source = this.resource.getInputSource()) {
                    if (source instanceof StringInputSource) {
                        strinContent = source.toString();
                    } else if (source instanceof ReaderInputSource) {
                        strinContent = IOUtils.toString(((ReaderInputSource) source).getReader());
                    } else if (source instanceof InputStreamInputSource) {
                        // It's impossible to know the real attachment encoding, but let's assume that they respect the
                        // standard and use UTF-8 (which is required for the files located on the filesystem)
                        strinContent = IOUtils.toString(((InputStreamInputSource) source).getInputStream(),
                            StandardCharsets.UTF_8);
                    } else {
                        return null;
                    }
                }

                this.content = getContentInternal(strinContent);
            }

            return this.content;
        }

        protected abstract T getContentInternal(String content) throws Exception;

        @Override
        public String toString()
        {
            return this.resource.getId();
        }
    }

    private class EnvironmentTemplate extends AbtractTemplate<FilesystemTemplateContent, AbstractSkinResource>
    {
        EnvironmentTemplate(AbstractSkinResource resource)
        {
            super(resource);
        }

        @Override
        protected FilesystemTemplateContent getContentInternal(String content)
        {
            return new FilesystemTemplateContent(content);
        }
    }

    private class ClassloaderTemplate extends AbtractTemplate<FilesystemTemplateContent, ClassloaderResource>
    {
        ClassloaderTemplate(ClassloaderResource resource)
        {
            super(resource);
        }

        @Override
        protected FilesystemTemplateContent getContentInternal(String content)
        {
            return new FilesystemTemplateContent(content);
        }
    }

    private class DefaultTemplate extends AbtractTemplate<DefaultTemplateContent, Resource<?>>
    {
        DefaultTemplate(Resource<?> resource)
        {
            super(resource);
        }

        @Override
        protected DefaultTemplateContent getContentInternal(String content)
        {
            if (this.resource instanceof WikiResource) {
                WikiResource<?> wikiResource = ((WikiResource<?>) this.resource);
                return new DefaultTemplateContent(content, wikiResource.getAuthorReference(),
                    wikiResource.getDocumentReference());
            } else {
                return new DefaultTemplateContent(content);
            }
        }
    }

    private class StringTemplate extends DefaultTemplate
    {
        StringTemplate(String content, DocumentReference authorReference, DocumentReference documentReference)
            throws Exception
        {
            super(new StringResource(content));

            // As StringTemplate extends DefaultTemplate, the TemplateContent is DefaultTemplateContent
            ((DefaultTemplateContent) this.getContent()).setAuthorReference(authorReference);
            ((DefaultTemplateContent) this.getContent()).setDocumentReference(documentReference);
        }
    }

    private class DefaultTemplateContent implements RawProperties, TemplateContent
    {
        // TODO: work with streams instead
        protected String content;

        protected boolean authorProvided;

        protected DocumentReference authorReference;

        protected DocumentReference documentReference;

        @PropertyId("source.syntax")
        public Syntax sourceSyntax;

        @PropertyId("raw.syntax")
        public Syntax rawSyntax;

        public boolean cacheAllowed;

        public boolean asyncAllowed;

        public Set<String> contextEntries;

        public UniqueContext unique;

        protected Map<String, Object> properties = new HashMap<>();

        DefaultTemplateContent(String content)
        {
            this.content = content;

            init();
        }

        DefaultTemplateContent(String content, DocumentReference authorReference)
        {
            this(content);

            setAuthorReference(authorReference);
        }

        DefaultTemplateContent(String content, DocumentReference authorReference, DocumentReference sourceReference)
        {
            this(content, authorReference);

            setDocumentReference(sourceReference);
        }

        @Override
        public Syntax getSourceSyntax()
        {
            return this.sourceSyntax;
        }

        @Override
        public Syntax getRawSyntax()
        {
            return this.rawSyntax;
        }

        @Override
        public boolean isAsyncAllowed()
        {
            return this.asyncAllowed;
        }

        @Override
        public boolean isCacheAllowed()
        {
            return this.cacheAllowed;
        }

        @Override
        public UniqueContext getUnique()
        {
            return this.unique;
        }

        @Override
        public Set<String> getContextEntries()
        {
            if (this.contextEntries == null) {
                return Collections.emptySet();
            }

            if (this.contextEntries instanceof AbstractSet) {
                this.contextEntries = Collections.unmodifiableSet(this.contextEntries);
            }

            return this.contextEntries;
        }

        @Override
        public <T> T getProperty(String name, T def)
        {
            if (!this.properties.containsKey(name)) {
                return def;
            }

            if (def != null) {
                return getProperty(name, def.getClass());
            }

            return (T) this.properties.get(name);
        }

        @Override
        public <T> T getProperty(String name, Type type)
        {
            return converter.convert(type, this.properties.get(name));
        }

        protected void init()
        {
            Matcher matcher = PROPERTY_LINE.matcher(this.content);

            Map<String, String> map = new HashMap<>();
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                map.put(key, value);

                // Remove the line from the content
                this.content = this.content.substring(matcher.end());
            }

            try {
                InternalTemplateManager.this.beanManager.populate(this, map);
            } catch (PropertyException e) {
                // Should never happen
                InternalTemplateManager.this.logger.error("Failed to populate properties of template", e);
            }
        }

        @Override
        public String getContent()
        {
            return this.content;
        }

        @PropertyHidden
        @Override
        public boolean isAuthorProvided()
        {
            return this.authorProvided;
        }

        @PropertyId("author")
        @Override
        public DocumentReference getAuthorReference()
        {
            return this.authorReference;
        }

        protected void setAuthorReference(DocumentReference authorReference)
        {
            this.authorReference = authorReference;
            this.authorProvided = true;
        }

        @Override
        public DocumentReference getDocumentReference()
        {
            return this.documentReference;
        }

        protected void setDocumentReference(DocumentReference documentReference)
        {
            this.documentReference = documentReference;
        }

        // RawProperties

        @Override
        public void set(String propertyName, Object value)
        {
            this.properties.put(propertyName, value);
        }
    }

    private class FilesystemTemplateContent extends DefaultTemplateContent
    {
        public FilesystemTemplateContent(String content)
        {
            super(content);

            // Give programming right to filesystem templates by default
            setPrivileged(true);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Allow filesystem template to indicate the user to executed them with.
         * </p>
         *
         * @see #setAuthorReference(DocumentReference)
         */
        @Override
        public void setAuthorReference(DocumentReference authorReference)
        {
            super.setAuthorReference(authorReference);
        }

        /**
         * Made public to be seen as bean property.
         *
         * @since 6.3.1
         * @since 6.4M1
         */
        @SuppressWarnings("unused")
        public boolean isPrivileged()
        {
            return SUPERADMIN_REFERENCE.equals(getAuthorReference());
        }

        /**
         * Made public to be seen as bean property.
         *
         * @since 6.3.1
         * @since 6.4M1
         */
        public void setPrivileged(boolean privileged)
        {
            if (privileged) {
                setAuthorReference(SUPERADMIN_REFERENCE);
            } else {
                // Reset author
                this.authorReference = null;
                this.authorProvided = false;
            }
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        getTemplateRootPath();
    }

    private String getTemplateRootPath()
    {
        if (this.templateRootURL == null) {
            URL url = this.environment.getResource(TEMPLATE_RESOURCE_SUFFIX);

            if (url != null) {
                this.templateRootURL = url.toString();
            }
        }

        return this.templateRootURL;
    }

    private String getTemplateResourcePath(String templateName)
    {
        String templatePath = TEMPLATE_RESOURCE_SUFFIX + templateName;

        URL templateURL = this.environment.getResource(templatePath);

        // Check if the resource exist
        if (templateURL == null) {
            return null;
        }

        // Prevent inclusion of templates from other directories
        String rootTemplate = getTemplateRootPath();
        if (rootTemplate != null) {
            String templateURLString = templateURL.toString();
            if (!templateURLString.startsWith(getTemplateRootPath())) {
                this.logger.warn("Direct access to template file [{}] refused. Possible break-in attempt!",
                    templateURLString);

                return null;
            }
        }

        return templatePath;
    }

    private void renderError(Throwable throwable, boolean inline, Writer writer)
    {
        Block block = generateError(throwable, inline);

        render(block, writer);
    }

    private Block generateError(Throwable throwable, boolean inline)
    {
        List<Block> errorBlocks = this.errorBlockGeneratorProvider.get().generateErrorBlocks(inline, null,
            "Failed to execute template", null, throwable);

        if (inline) {
            if (errorBlocks.size() == 1) {
                return errorBlocks.get(0);
            } else {
                return new CompositeBlock(errorBlocks);
            }
        } else {
            return new XDOM(errorBlocks);
        }
    }

    /**
     * @param templateName the template to parse
     * @return the result of the template parsing
     */
    public XDOM getXDOMNoException(String templateName)
    {
        XDOM xdom;

        try {
            xdom = getXDOM(templateName);
        } catch (Throwable e) {
            this.logger.error("Error while getting template [{}] XDOM", templateName, e);

            xdom = (XDOM) generateError(e, false);
        }

        return xdom;
    }

    /**
     * @param template the template to parse
     * @return the result of the template parsing
     * @since 8.3RC1
     */
    public XDOM getXDOMNoException(Template template)
    {
        XDOM xdom;

        try {
            xdom = getXDOM(template);
        } catch (Throwable e) {
            this.logger.error("Error while getting template [{}] XDOM", template.getId(), e);

            xdom = (XDOM) generateError(e, false);
        }

        return xdom;
    }

    public XDOM getXDOM(Template template) throws Exception
    {
        XDOM xdom;

        if (template != null) {
            xdom = getXDOM(template, template.getContent());
        } else {
            xdom = new XDOM(Collections.<Block>emptyList());
        }

        return xdom;
    }

    private XDOM getXDOM(Template template, TemplateContent content) throws Exception
    {
        XDOM xdom;

        if (content.getSourceSyntax() != null) {
            xdom = this.parser.parse(content.getContent(), content.getSourceSyntax());
        } else {
            String result = evaluateContent(template, content);
            if (StringUtils.isEmpty(result)) {
                xdom = new XDOM(Collections.emptyList());
            } else {
                xdom = new XDOM(Arrays.asList(new RawBlock(result, content.getRawSyntax() != null
                    ? content.getRawSyntax() : this.renderingContext.getTargetSyntax())));
            }
        }

        return xdom;
    }

    public XDOM getXDOM(String templateName) throws Exception
    {
        Template template = getTemplate(templateName);

        return getXDOM(template);
    }

    public String renderNoException(String template, boolean inline)
    {
        Writer writer = new StringWriter();

        renderNoException(template, inline, writer);

        return writer.toString();
    }

    public void renderNoException(String templateName, boolean inline, Writer writer)
    {
        try {
            render(templateName, inline, writer);
        } catch (Exception e) {
            this.logger.error("Error while rendering template [{}]", templateName, e);

            renderError(e, inline, writer);
        }
    }

    /**
     * @since 8.3RC1
     */
    public void renderNoException(Template template, boolean inline, Writer writer)
    {
        try {
            render(template, inline, writer);
        } catch (Exception e) {
            this.logger.error("Error while rendering template [{}]", template, e);

            renderError(e, inline, writer);
        }
    }

    public String render(String templateName, boolean inline) throws Exception
    {
        return renderFromSkin(templateName, (Skin) null, inline);
    }

    public String renderFromSkin(String templateName, String skinId, boolean inline) throws Exception
    {
        Skin skin = this.skins.getSkin(skinId);

        return skin != null ? renderFromSkin(templateName, skin, inline) : null;
    }

    public String renderFromSkin(String templateName, Skin skin, boolean inline) throws Exception
    {
        Writer writer = new StringWriter();

        renderFromSkin(templateName, skin, inline, writer);

        return writer.toString();
    }

    public void render(String templateName, boolean inline, Writer writer) throws Exception
    {
        renderFromSkin(templateName, null, inline, writer);
    }

    public void renderFromSkin(final String templateName, ResourceRepository repository, boolean inline,
        final Writer writer) throws Exception
    {
        this.progress.startStep(templateName, "template.render.message", "Render template [{}]", templateName);

        try {
            final Template template =
                repository != null ? getTemplate(templateName, repository) : getTemplate(templateName);

            if (template != null) {
                render(template, inline, writer);
            }
        } finally {
            this.progress.endStep(templateName);
        }
    }

    public void render(Template template, boolean inline, Writer writer) throws Exception
    {
        if (!shouldExecute(template)) {
            return;
        }

        TemplateAsyncRenderer renderer = this.rendererProvider.get();

        Set<String> contextEntries = renderer.initialize(template, inline, false);

        AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();

        configuration.setContextEntries(contextEntries);

        TemplateContent templateContent = template.getContent();
        if (templateContent.isAuthorProvided()) {
            configuration.setSecureReference(templateContent.getDocumentReference(),
                templateContent.getAuthorReference());
        }

        String result = this.asyncExecutor.render(renderer, configuration);

        writer.append(result);
    }

    private boolean shouldExecute(Template template) throws Exception
    {
        return template != null
            && (template.getContent().getUnique() == null || !this.templateContext.isExecuted(template));
    }

    private void render(Block block, Writer writer)
    {
        WikiPrinter printer = new WriterWikiPrinter(writer);

        BlockRenderer blockRenderer;
        try {
            blockRenderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
        } catch (ComponentLookupException e) {
            blockRenderer = this.plainRenderer;
        }

        blockRenderer.render(block, printer);
    }

    public Block executeNoException(String templateName, boolean inline)
    {
        Block block;

        try {
            block = execute(templateName, inline);
        } catch (Throwable e) {
            this.logger.error("Error while executing template [{}]", templateName, e);

            block = generateError(e, inline);
        }

        return block;
    }

    /**
     * @since 14.0RC1
     */
    public Block executeNoException(Template template, boolean inline)
    {
        Block block;

        try {
            block = execute(template, inline);
        } catch (Throwable e) {
            this.logger.error("Error while executing template [{}]", template.getId(), e);

            block = generateError(e, inline);
        }

        return block;
    }

    /**
     * @since 14.0RC1
     */
    public Block execute(String templateName, boolean inline) throws Exception
    {
        final Template template = getTemplate(templateName);

        return execute(template, inline);
    }

    /**
     * @since 14.0RC1
     */
    public Block execute(Template template, boolean inline) throws Exception
    {
        if (!shouldExecute(template)) {
            return new XDOM(Collections.emptyList());
        }

        TemplateAsyncRenderer renderer = this.rendererProvider.get();

        Set<String> contextEntries = renderer.initialize(template, inline, true);

        AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();

        configuration.setContextEntries(contextEntries);

        if (template.getContent().isAuthorProvided()) {
            configuration.setSecureReference(template.getContent().getDocumentReference(),
                template.getContent().getAuthorReference());
        }

        Block block = this.asyncExecutor.execute(renderer, configuration);

        if (inline) {
            return block;
        }

        if (block instanceof XDOM) {
            return (XDOM) block;
        }

        return new XDOM(Collections.singletonList(block));
    }

    private String evaluateContent(Template template, TemplateContent content) throws Exception
    {
        Writer writer = new StringWriter();

        this.evaluator.evaluateContent(template, content, writer);

        return writer.toString();
    }

    private Syntax getTargetSyntax()
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        return targetSyntax != null ? targetSyntax : Syntax.PLAIN_1_0;
    }

    private EnvironmentTemplate getFileSystemTemplate(String templateName)
    {
        String path = getTemplateResourcePath(templateName);

        return path != null ? new EnvironmentTemplate(new TemplateSkinResource(path, templateName, this.environment))
            : null;
    }

    private Template getClassloaderTemplate(String suffixPath, String templateName)
    {
        return getClassloaderTemplate(Thread.currentThread().getContextClassLoader(), suffixPath, templateName);
    }

    private Template getClassloaderTemplate(ClassLoader classloader, String suffixPath, String templateName)
    {
        String templatePath = suffixPath + templateName;

        URL url = classloader.getResource(templatePath);

        return url != null ? new ClassloaderTemplate(new ClassloaderResource(url, templateName)) : null;
    }

    private Template createTemplate(Resource<?> resource)
    {
        Template template;

        if (resource instanceof AbstractSkinResource) {
            template = new EnvironmentTemplate((AbstractSkinResource) resource);
        } else {
            template = new DefaultTemplate(resource);
        }

        return template;
    }

    public Template getResourceTemplate(String templateName, ResourceRepository repository)
    {
        Resource<?> resource = repository.getLocalResource(templateName);
        if (resource != null) {
            return createTemplate(resource);
        }

        return null;
    }

    public Template getTemplate(String templateName, ResourceRepository repository)
    {
        Resource<?> resource = repository.getResource(templateName);
        if (resource != null) {
            return createTemplate(resource);
        }

        return null;
    }

    public Template getTemplate(String templateName)
    {
        Template template = null;

        // Try from skin
        Skin skin = this.skins.getCurrentSkin(false);
        if (skin != null) {
            template = getTemplate(templateName, skin);
        }

        // Try from base skin if no skin is set
        if (skin == null) {
            Skin baseSkin = this.skins.getCurrentParentSkin(false);
            if (baseSkin != null) {
                template = getTemplate(templateName, baseSkin);
            }
        }

        // Try from /templates/ environment resources
        if (template == null) {
            template = getFileSystemTemplate(templateName);
        }

        // Try from current Thread classloader
        if (template == null) {
            template = getClassloaderTemplate("templates/", templateName);
        }

        return template;
    }

    /**
     * Create a new template using a given content and a specific author and source document.
     *
     * @param content the template content
     * @param author the template author
     * @param sourceReference the reference of the document associated with the {@link Callable} (which will be used to
     *            test the author right)
     * @return the template
     * @throws Exception if an error occurred during template instantiation
     * @since 14.0RC1
     */
    public Template createStringTemplate(String content, DocumentReference author, DocumentReference sourceReference)
        throws Exception
    {
        return new StringTemplate(content, author, sourceReference);
    }
}
