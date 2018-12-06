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

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.WriterWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.skin.Resource;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.skin.Skin;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.internal.skin.AbstractEnvironmentResource;
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
public class InternalTemplateManager
{
    private static final Pattern PROPERTY_LINE = Pattern.compile("^##!(.+)=(.*)$\r?\n?", Pattern.MULTILINE);

    /**
     * The reference of the superadmin user.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE =
        new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);

    @Inject
    private Environment environment;

    @Inject
    private ContentParser parser;

    @Inject
    private VelocityManager velocityManager;

    /**
     * Used to execute transformations.
     */
    @Inject
    private TransformationManager transformationManager;

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
    private AuthorExecutor authorExecutor;

    @Inject
    private InternalSkinManager skins;

    @Inject
    private JobProgressManager progress;

    @Inject
    private Logger logger;

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

    private class EnvironmentTemplate extends AbtractTemplate<FilesystemTemplateContent, AbstractEnvironmentResource>
    {
        EnvironmentTemplate(AbstractEnvironmentResource resource)
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
        StringTemplate(String content, DocumentReference authorReference) throws Exception
        {
            super(new StringResource(content));
            // Initialize the template content
            // As StringTemplate extends DefaultTemplate, the TemplateContent is DefaultTemplateContent
            ((DefaultTemplateContent) this.getContent()).setAuthorReference(authorReference);
        }
    }

    private class DefaultTemplateContent implements RawProperties, TemplateContent
    {
        // TODO: work with streams instead
        protected String content;

        protected boolean authorProvided;

        protected DocumentReference authorReference;

        protected DocumentReference sourceReference;

        @PropertyId("source.syntax")
        public Syntax sourceSyntax;

        @PropertyId("raw.syntax")
        public Syntax rawSyntax;

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

            setSourceReference(sourceReference);
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

        @Override
        public DocumentReference getDocumentReference()
        {
            return this.sourceReference;
        }

        protected void setAuthorReference(DocumentReference authorReference)
        {
            this.authorReference = authorReference;
            this.authorProvided = true;
        }

        protected void setSourceReference(DocumentReference sourceReference)
        {
            this.sourceReference = sourceReference;
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

    private String getResourcePath(String suffixPath, String templateName, boolean testExist)
    {
        String templatePath = suffixPath + templateName;

        // Prevent inclusion of templates from other directories
        String normalizedTemplate = URI.create(templatePath).normalize().toString();
        if (!normalizedTemplate.startsWith(suffixPath)) {
            this.logger.warn("Direct access to template file [{}] refused. Possible break-in attempt!",
                normalizedTemplate);

            return null;
        }

        if (testExist) {
            // Check if the resource exist
            if (this.environment.getResource(templatePath) == null) {
                return null;
            }
        }

        return templatePath;
    }

    private void renderError(Throwable throwable, Writer writer)
    {
        XDOM xdom = generateError(throwable);

        render(xdom, writer);
    }

    private XDOM generateError(Throwable throwable)
    {
        List<Block> errorBlocks = new ArrayList<Block>();

        // Add short message
        Map<String, String> errorBlockParams = Collections.singletonMap("class", "xwikirenderingerror");
        errorBlocks.add(
            new GroupBlock(Arrays.<Block>asList(new WordBlock("Failed to render step content")), errorBlockParams));

        // Add complete error
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        Block descriptionBlock = new VerbatimBlock(writer.toString(), false);
        Map<String, String> errorDescriptionBlockParams =
            Collections.singletonMap("class", "xwikirenderingerrordescription hidden");
        errorBlocks.add(new GroupBlock(Arrays.asList(descriptionBlock), errorDescriptionBlockParams));

        return new XDOM(errorBlocks);
    }

    private void transform(Block block)
    {
        TransformationContext txContext =
            new TransformationContext(block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block)),
                this.renderingContext.getDefaultSyntax(), this.renderingContext.isRestricted());

        txContext.setId(this.renderingContext.getTransformationId());
        txContext.setTargetSyntax(getTargetSyntax());

        try {
            this.transformationManager.performTransformations(block, txContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

            xdom = generateError(e);
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

            xdom = generateError(e);
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
                xdom = new XDOM(Arrays.asList(new RawBlock(result,
                    content.getRawSyntax() != null ? content.getRawSyntax() : renderingContext.getTargetSyntax())));
            }
        }

        return xdom;
    }

    public XDOM getXDOM(String templateName) throws Exception
    {
        Template template = getTemplate(templateName);

        return getXDOM(template);
    }

    public String renderNoException(String template)
    {
        Writer writer = new StringWriter();

        renderNoException(template, writer);

        return writer.toString();
    }

    public void renderNoException(String templateName, Writer writer)
    {
        try {
            render(templateName, writer);
        } catch (Exception e) {
            this.logger.error("Error while rendering template [{}]", templateName, e);

            renderError(e, writer);
        }
    }

    /**
     * @since 8.3RC1
     */
    public void renderNoException(Template template, Writer writer)
    {
        try {
            render(template, writer);
        } catch (Exception e) {
            this.logger.error("Error while rendering template [{}]", template, e);

            renderError(e, writer);
        }
    }

    public String render(String templateName) throws Exception
    {
        return renderFromSkin(templateName, (Skin) null);
    }

    public String renderFromSkin(String templateName, String skinId) throws Exception
    {
        Skin skin = this.skins.getSkin(skinId);

        return skin != null ? renderFromSkin(templateName, skin) : null;
    }

    public String renderFromSkin(String templateName, Skin skin) throws Exception
    {
        Writer writer = new StringWriter();

        renderFromSkin(templateName, skin, writer);

        return writer.toString();
    }

    public void render(String templateName, Writer writer) throws Exception
    {
        renderFromSkin(templateName, null, writer);
    }

    public void renderFromSkin(final String templateName, ResourceRepository repository, final Writer writer)
        throws Exception
    {
        this.progress.startStep(templateName, "template.render.message", "Render template [{}]", templateName);

        try {
            final Template template =
                repository != null ? getTemplate(templateName, repository) : getTemplate(templateName);

            if (template != null) {
                if (template.getContent().isAuthorProvided()) {
                    this.authorExecutor.call(() -> {
                        render(template, template.getContent(), writer);

                        return null;
                    }, template.getContent().getAuthorReference(), template.getContent().getDocumentReference());
                } else {
                    render(template, template.getContent(), writer);
                }
            }
        } finally {
            this.progress.endStep(templateName);
        }
    }

    public void render(Template template, Writer writer) throws Exception
    {
        render(template, template.getContent(), writer);
    }

    private void render(Template template, TemplateContent content, Writer writer) throws Exception
    {
        if (content.getSourceSyntax() != null) {
            XDOM xdom = execute(template, content);

            render(xdom, writer);
        } else {
            evaluateContent(template, content, writer);
        }
    }

    private void render(XDOM xdom, Writer writer)
    {
        WikiPrinter printer = new WriterWikiPrinter(writer);

        BlockRenderer blockRenderer;
        try {
            blockRenderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
        } catch (ComponentLookupException e) {
            blockRenderer = this.plainRenderer;
        }

        blockRenderer.render(xdom, printer);
    }

    public XDOM executeNoException(String templateName)
    {
        XDOM xdom;

        try {
            xdom = execute(templateName);
        } catch (Throwable e) {
            this.logger.error("Error while executing template [{}]", templateName, e);

            xdom = generateError(e);
        }

        return xdom;
    }

    /**
     * @since 8.3RC1
     */
    public XDOM executeNoException(Template template)
    {
        XDOM xdom;

        try {
            xdom = execute(template);
        } catch (Throwable e) {
            this.logger.error("Error while executing template [{}]", template.getId(), e);

            xdom = generateError(e);
        }

        return xdom;
    }

    private XDOM execute(Template template, TemplateContent content) throws Exception
    {
        XDOM xdom = getXDOM(template, content);

        transform(xdom);

        return xdom;
    }

    public XDOM execute(String templateName) throws Exception
    {
        final Template template = getTemplate(templateName);

        if (template != null) {
            if (template.getContent().isAuthorProvided()) {
                return this.authorExecutor.call(() -> execute(template, template.getContent()),
                    template.getContent().getAuthorReference(), template.getContent().getDocumentReference());
            } else {
                return execute(template, template.getContent());
            }
        }

        return null;
    }

    public XDOM execute(Template template) throws Exception
    {
        if (template != null) {
            if (template.getContent().isAuthorProvided()) {
                return this.authorExecutor.call(() -> execute(template, template.getContent()),
                    template.getContent().getAuthorReference(), template.getContent().getDocumentReference());
            } else {
                return execute(template, template.getContent());
            }
        }

        return null;
    }

    private String evaluateContent(Template template, TemplateContent content) throws Exception
    {
        Writer writer = new StringWriter();

        evaluateContent(template, content, writer);

        return writer.toString();
    }

    private void evaluateContent(Template template, TemplateContent content, Writer writer) throws Exception
    {
        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String namespace = this.renderingContext.getTransformationId();

        boolean renderingContextPushed = false;
        if (namespace == null) {
            namespace = template.getId() != null ? template.getId() : "unknown namespace";

            if (this.renderingContext instanceof MutableRenderingContext) {
                // Make the current velocity template id available
                ((MutableRenderingContext) this.renderingContext).push(this.renderingContext.getTransformation(),
                    this.renderingContext.getXDOM(), this.renderingContext.getDefaultSyntax(), namespace,
                    this.renderingContext.isRestricted(), this.renderingContext.getTargetSyntax());

                renderingContextPushed = true;
            }
        }

        this.progress.startStep(template, "template.evaluateContent.message",
            "Evaluate content of template with id [{}]", template.getId());

        try {
            this.velocityManager.evaluate(writer, namespace, new StringReader(content.getContent()));
        } finally {
            // Get rid of temporary rendering context
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }

            this.progress.endStep(template);
        }
    }

    private Syntax getTargetSyntax()
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        return targetSyntax != null ? targetSyntax : Syntax.PLAIN_1_0;
    }

    private EnvironmentTemplate getFileSystemTemplate(String suffixPath, String templateName)
    {
        String path = getResourcePath(suffixPath, templateName, true);

        return path != null
            ? new EnvironmentTemplate(new TemplateEnvironmentResource(path, templateName, this.environment)) : null;
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

        if (resource instanceof AbstractEnvironmentResource) {
            template = new EnvironmentTemplate((AbstractEnvironmentResource) resource);
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
            template = getFileSystemTemplate("/templates/", templateName);
        }

        // Try from current Thread classloader
        if (template == null) {
            template = getClassloaderTemplate("templates/", templateName);
        }

        return template;
    }

    /**
     * Create a DefaultTemplate with the given content and the given author.
     *
     * @since 9.6RC1
     * @param content the template content
     * @param author the template author
     * @return the template
     */
    public Template createStringTemplate(String content, DocumentReference author) throws Exception
    {
        return new StringTemplate(content, author);
    }
}
