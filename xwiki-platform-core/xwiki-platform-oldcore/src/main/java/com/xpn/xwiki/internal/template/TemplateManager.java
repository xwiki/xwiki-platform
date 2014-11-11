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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.properties.RawProperties;
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
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Internal toolkit to experiment on wiki bases templates.
 * 
 * @version $Id$
 * @since 6.3M2
 */
@Component(roles = TemplateManager.class)
@Singleton
public class TemplateManager
{
    static final LocalDocumentReference SKINCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiSkins");

    private static final Pattern PROPERTY_LINE = Pattern.compile("^##!(.+)=(.*)$\r?\n?", Pattern.MULTILINE);

    /**
     * The reference of the superadmin user.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE = new DocumentReference("xwiki", XWiki.SYSTEM_SPACE,
        XWikiRightService.SUPERADMIN_USER);

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
    private Provider<XWikiContext> xcontextProvider;

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
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private BeanManager beanManager;

    @Inject
    private SUExecutor suExecutor;

    @Inject
    private Logger logger;

    private static abstract class AbtractTemplate<T extends TemplateContent> implements Template
    {
        protected String path;

        protected T content;

        public AbtractTemplate(String path)
        {
            this.path = path;
        }

        public AbtractTemplate(String path, T content)
        {
            this(path);

            this.content = content;
        }

        @Override
        public String getId()
        {
            return getPath();
        }

        @Override
        public String getPath()
        {
            return this.path;
        }

        @Override
        public TemplateContent getContent() throws Exception
        {
            if (this.content == null) {
                this.content = getContentInternal();
            }

            return this.content;
        }

        protected abstract T getContentInternal() throws Exception;
    }

    private class FileSystemTemplate extends AbtractTemplate<FilesystemTemplateContent>
    {
        public FileSystemTemplate(String path)
        {
            super(path);
        }

        public FileSystemTemplate(String path, FilesystemTemplateContent content)
        {
            super(path, content);
        }

        @Override
        protected FilesystemTemplateContent getContentInternal()
        {
            InputStream inputStream = environment.getResourceAsStream(getPath());
            if (inputStream != null) {
                try {
                    return new FilesystemTemplateContent(IOUtils.toString(inputStream, "UTF-8"));
                } catch (IOException e) {
                    logger.error("Faied to get content of resource [{}]", getPath(), e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }

            return null;
        }
    }

    private abstract class AbstractWikiTemplate<R extends EntityReference> extends
        AbtractTemplate<DefaultTemplateContent>
    {
        protected R reference;

        public AbstractWikiTemplate(String path, R reference)
        {
            this(path, reference, null);
        }

        public AbstractWikiTemplate(String path, R reference, DefaultTemplateContent content)
        {
            super(path, content);

            this.reference = reference;
        }

        @Override
        protected DefaultTemplateContent getContentInternal() throws Exception
        {
            EntityReference documentReference = this.reference.extractReference(EntityType.DOCUMENT);

            XWikiContext xcontext = xcontextProvider.get();

            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

            return new DefaultTemplateContent(getContentAsString(document), document.getAuthorReference());
        }

        protected abstract String getContentAsString(XWikiDocument document) throws Exception;
    }

    private class ObjectPropertyWikiTemplate extends AbstractWikiTemplate<ObjectPropertyReference>
    {
        public ObjectPropertyWikiTemplate(String path, ObjectPropertyReference reference)
        {
            super(path, reference);
        }

        public ObjectPropertyWikiTemplate(String path, ObjectPropertyReference reference, DefaultTemplateContent content)
        {
            super(path, reference, content);
        }

        @Override
        protected String getContentAsString(XWikiDocument document)
        {
            BaseProperty<ObjectPropertyReference> property = document.getXObjectProperty(this.reference);

            return (String) property.getValue();
        }
    }

    private class AttachmentWikiTemplate extends AbstractWikiTemplate<AttachmentReference>
    {
        public AttachmentWikiTemplate(String path, AttachmentReference reference)
        {
            super(path, reference);
        }

        public AttachmentWikiTemplate(String path, AttachmentReference reference, DefaultTemplateContent content)
        {
            super(path, reference, content);
        }

        @Override
        protected String getContentAsString(XWikiDocument document) throws IOException, XWikiException
        {
            XWikiAttachment attachment = document.getAttachment(this.reference.getName());

            // It's impossible to know the real attachment encoding, but let's assume that they respect the
            // standard and use UTF-8 (which is required for the files located on the filesystem)
            return IOUtils.toString(attachment.getContentInputStream(xcontextProvider.get()), "UTF-8");
        }
    }

    private class DefaultTemplateContent implements RawProperties, TemplateContent
    {
        // TODO: work with streams instead
        protected String content;

        protected boolean authorProvided;

        protected DocumentReference authorReference;

        @PropertyId("source.syntax")
        public Syntax sourceSyntax;

        @PropertyId("raw.syntax")
        public Syntax rawSyntax;

        public Map<String, Object> properties = new HashMap<String, Object>();

        public DefaultTemplateContent(String content)
        {
            this.content = content;

            init();
        }

        protected void init()
        {
            Matcher matcher = PROPERTY_LINE.matcher(this.content);

            Map<String, String> properties = new HashMap<String, String>();
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                properties.put(key, value);

                // Remove the line from the content
                this.content = this.content.substring(matcher.end());
            }

            try {
                beanManager.populate(content, properties);
            } catch (PropertyException e) {
                // Should never happen
                logger.error("Failed to populate properties of template", e);
            }

            // The default is xhtml to support old templates
            if (this.rawSyntax == null && this.sourceSyntax == null) {
                this.rawSyntax = Syntax.XHTML_1_0;
            }
        }

        public DefaultTemplateContent(String content, DocumentReference authorReference)
        {
            this(content);

            setAuthorReference(authorReference);
        }

        @Override
        public String getContent()
        {
            return this.content;
        }

        @PropertyId("author")
        public DocumentReference getAuthorReference()
        {
            return this.authorReference;
        }

        protected void setAuthorReference(DocumentReference authorReference)
        {
            this.authorReference = authorReference;
            this.authorProvided = true;
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
        }

        /**
         * {@inheritDoc}
         * <p>
         * Allow filesystem template to indicate the user to executed them with.
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
         */
        @SuppressWarnings("unused")
        public boolean isPriviledged()
        {
            return SUPERADMIN_REFERENCE.equals(getAuthorReference());
        }

        /**
         * Made public to be seen as bean property.
         */
        @SuppressWarnings("unused")
        public void setPriviledged(boolean priviledged)
        {
            if (priviledged) {
                setAuthorReference(SUPERADMIN_REFERENCE);
            }
        }
    }

    // TODO: put that in some SkinContext component
    private String getSkin()
    {
        String skin;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Try to get it from context
            skin = (String) xcontext.get("skin");
            if (StringUtils.isNotEmpty(skin)) {
                return skin;
            } else {
                skin = null;
            }

            // Try to get it from URL
            if (xcontext.getRequest() != null) {
                skin = xcontext.getRequest().getParameter("skin");
                if (StringUtils.isNotEmpty(skin)) {
                    return skin;
                } else {
                    skin = null;
                }
            }

            // Try to get it from preferences (user -> space -> wiki -> xwiki.properties)
            skin = this.allConfiguration.getProperty("skin");
            if (skin != null) {
                return skin;
            }
        }

        // Try to get it from xwiki.cfg
        skin = this.xwikicfg.getProperty("xwiki.defaultskin", XWiki.DEFAULT_SKIN);

        return StringUtils.isNotEmpty(skin) ? skin : null;
    }

    // TODO: put that in some SkinContext component
    private String getBaseSkin()
    {
        String baseskin;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Try to get it from context
            baseskin = (String) xcontext.get("baseskin");
            if (StringUtils.isNotEmpty(baseskin)) {
                return baseskin;
            } else {
                baseskin = null;
            }

            // Try to get it from the skin
            String skin = getSkin();
            if (skin != null) {
                BaseObject skinObject = getSkinObject(skin);
                if (skinObject != null) {
                    baseskin = skinObject.getStringValue("baseskin");
                    if (StringUtils.isNotEmpty(baseskin)) {
                        return baseskin;
                    }
                }
            }
        }

        // Try to get it from xwiki.cfg
        baseskin = this.xwikicfg.getProperty("xwiki.defaultbaseskin");

        return StringUtils.isNotEmpty(baseskin) ? baseskin : null;
    }

    private XWikiDocument getSkinDocument(String skin)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            DocumentReference skinReference = this.currentMixedDocumentReferenceResolver.resolve(skin);
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null && xwiki.getStore() != null) {
                XWikiDocument doc;
                try {
                    doc = xwiki.getDocument(skinReference, xcontext);
                } catch (XWikiException e) {
                    this.logger.error("Faied to get document [{}]", skinReference, e);

                    return null;
                }
                if (!doc.isNew()) {
                    return doc;
                }
            }
        }

        return null;
    }

    private BaseObject getSkinObject(String skin)
    {
        XWikiDocument skinDocument = getSkinDocument(skin);

        return skinDocument != null ? skinDocument.getXObject(SKINCLASS_REFERENCE) : null;
    }

    private Template getTemplateContentFromSkin(String templateName, String skin)
    {
        Template template;

        // Try from wiki pages
        XWikiDocument skinDocument = getSkinDocument(skin);
        if (skinDocument != null) {
            template = getTemplateContentFromDocumentSkin(templateName, skinDocument);
        } else {
            // If not a wiki based skin try from filesystem skins
            template = getResourceAsStringContent("/skins/" + skin + '/', templateName);
        }

        return template;
    }

    private BaseProperty<ObjectPropertyReference> getTemplatePropertyValue(String template, XWikiDocument skinDocument)
    {
        // Try parsing the object property
        BaseObject skinObject = skinDocument.getXObject(SKINCLASS_REFERENCE);
        if (skinObject != null) {
            BaseProperty<ObjectPropertyReference> templateProperty =
                (BaseProperty<ObjectPropertyReference>) skinObject.safeget(template);

            // If not found try by replacing '/' with '.'
            if (templateProperty == null) {
                String escapedTemplateName = StringUtils.replaceChars(template, '/', '.');
                templateProperty = (BaseProperty<ObjectPropertyReference>) skinObject.safeget(escapedTemplateName);
            }

            if (templateProperty != null) {
                Object value = templateProperty.getValue();
                if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
                    return templateProperty;
                }
            }
        }

        return null;
    }

    private Template getTemplateContentFromDocumentSkin(String templateName, XWikiDocument skinDocument)
    {
        if (skinDocument != null) {
            // Try parsing the object property
            BaseProperty<ObjectPropertyReference> templateProperty =
                getTemplatePropertyValue(templateName, skinDocument);
            if (templateProperty != null) {
                ObjectPropertyReference reference = templateProperty.getReference();
                return new ObjectPropertyWikiTemplate(getPath(reference), reference,
                    new DefaultTemplateContent((String) templateProperty.getValue(), skinDocument.getAuthorReference()));
            }

            // Try parsing a document attachment
            XWikiAttachment attachment = skinDocument.getAttachment(templateName);
            if (attachment != null) {
                // It's impossible to know the real attachment encoding, but let's assume that they respect the
                // standard and use UTF-8 (which is required for the files located on the filesystem)
                try {
                    return new AttachmentWikiTemplate(getPath(attachment.getReference()),
                        attachment.getReference(), new DefaultTemplateContent(IOUtils.toString(
                            attachment.getContentInputStream(this.xcontextProvider.get()), "UTF-8"),
                            skinDocument.getAuthorReference()));
                } catch (Exception e) {
                    this.logger.error("Faied to get attachment content [{}]", skinDocument.getDocumentReference(), e);
                }
            }
        }

        return null;
    }

    private Template getTemplateContent(String templateName)
    {
        Template template = null;

        // Try from skin
        String skin = getSkin();
        if (skin != null) {
            template = getTemplateContentFromSkin(templateName, skin);
        }

        // Try from base skin
        if (template == null) {
            String baseSkin = getBaseSkin();
            if (baseSkin != null) {
                template = getTemplateContentFromSkin(templateName, baseSkin);
            }
        }

        // Try from /template/ resources
        if (template == null) {
            template = getResourceAsStringContent("/templates/", templateName);
        }

        return template;
    }

    private Template getResourceAsStringContent(String suffixPath, String templateName)
    {
        String templatePath = getResourcePath(suffixPath, templateName, false);

        InputStream inputStream = this.environment.getResourceAsStream(templatePath);
        if (inputStream != null) {
            try {
                return new FileSystemTemplate(templatePath, new FilesystemTemplateContent(IOUtils.toString(inputStream,
                    "UTF-8")));
            } catch (IOException e) {
                this.logger.error("Faied to get content of resource [{}]", templatePath, e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return null;
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

    private Template getTemplateContent(String templateName, String skin)
    {
        return skin != null ? getTemplateContentFromSkin(templateName, skin) : getTemplateContent(templateName);
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
        errorBlocks.add(new GroupBlock(Arrays.<Block>asList(new WordBlock("Failed to render step content")),
            errorBlockParams));

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
            xdom = generateError(e);
        }

        return xdom;
    }

    private XDOM getXDOM(Template template) throws Exception
    {
        XDOM xdom;

        if (template != null) {
            DefaultTemplateContent content = (DefaultTemplateContent) template.getContent();

            xdom = getXDOM(template, content);
        } else {
            xdom = new XDOM(Collections.<Block>emptyList());
        }

        return xdom;
    }

    private XDOM getXDOM(Template template, DefaultTemplateContent content) throws Exception
    {
        XDOM xdom;

        if (content.sourceSyntax != null) {
            xdom = this.parser.parse(content.content, content.sourceSyntax);
        } else {
            String result = evaluateContent(template, content);
            xdom = new XDOM(Arrays.asList(new RawBlock(result, content.rawSyntax)));
        }

        return xdom;
    }

    public XDOM getXDOM(String templateName) throws Exception
    {
        Template template = getTemplateContent(templateName, null);

        return getXDOM(template);
    }

    public String renderNoException(String template)
    {
        Writer writer = new StringWriter();

        renderNoException(template, writer);

        return writer.toString();
    }

    public void renderNoException(String template, Writer writer)
    {
        try {
            render(template, writer);
        } catch (Exception e) {
            renderError(e, writer);
        }
    }

    public String render(String template) throws Exception
    {
        return renderFromSkin(template, null);
    }

    public String renderFromSkin(String template, String skin) throws Exception
    {
        Writer writer = new StringWriter();

        renderFromSkin(template, skin, writer);

        return writer.toString();
    }

    public void render(String template, Writer writer) throws Exception
    {
        renderFromSkin(template, null, writer);
    }

    public void renderFromSkin(final String templateName, final String skin, final Writer writer) throws Exception
    {
        final Template template = getTemplateContent(templateName, skin);

        if (template != null) {
            final DefaultTemplateContent content = (DefaultTemplateContent) template.getContent();

            if (content.authorProvided) {
                this.suExecutor.call(new Callable<Void>()
                {
                    @Override
                    public Void call() throws Exception
                    {
                        render(template, content, writer);

                        return null;
                    }
                }, content.getAuthorReference());
            } else {
                render(template, content, writer);
            }
        }
    }

    public void render(Template template, Writer writer) throws Exception
    {
        DefaultTemplateContent content = (DefaultTemplateContent) template.getContent();

        render(template, content, writer);
    }

    private void render(Template template, DefaultTemplateContent content, Writer writer) throws Exception
    {
        if (content.sourceSyntax != null) {
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

    public XDOM executeNoException(String template)
    {
        XDOM xdom;

        try {
            xdom = execute(template);
        } catch (Throwable e) {
            xdom = generateError(e);
        }

        return xdom;
    }

    private XDOM execute(Template template, DefaultTemplateContent content) throws Exception
    {
        XDOM xdom = getXDOM(template, content);

        transform(xdom);

        return xdom;
    }

    public XDOM execute(String templateName) throws Exception
    {
        final Template template = getTemplateContent(templateName, null);

        if (template != null) {
            final DefaultTemplateContent content = (DefaultTemplateContent) template.getContent();

            if (content.authorProvided) {
                return this.suExecutor.call(new Callable<XDOM>()
                {
                    @Override
                    public XDOM call() throws Exception
                    {
                        return execute(template, content);
                    }
                }, content.getAuthorReference());
            } else {
                return execute(template, content);
            }
        }

        return null;
    }

    private String evaluateContent(Template template, DefaultTemplateContent content) throws Exception
    {
        Writer writer = new StringWriter();

        evaluateContent(template, content, writer);

        return writer.toString();
    }

    private void evaluateContent(Template template, DefaultTemplateContent content, Writer writer) throws Exception
    {
        VelocityContext velocityContext = this.velocityManager.getVelocityContext();

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

        VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();

        velocityEngine.startedUsingMacroNamespace(namespace);
        try {
            velocityEngine.evaluate(velocityContext, writer, namespace, content.content);
        } finally {
            velocityEngine.stoppedUsingMacroNamespace(namespace);

            // Get rid of temporary rendering context
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
    }

    private Syntax getTargetSyntax()
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        return targetSyntax != null ? targetSyntax : Syntax.PLAIN_1_0;
    }

    private Template getTemplateFromDocumentSkin(String template, XWikiDocument skinDocument)
    {
        if (skinDocument != null) {
            // Try parsing the object property
            BaseProperty<ObjectPropertyReference> templateProperty = getTemplatePropertyValue(template, skinDocument);
            if (templateProperty != null) {
                ObjectPropertyReference reference = templateProperty.getReference();
                return new ObjectPropertyWikiTemplate(getPath(reference), reference);
            }

            // Try parsing a document attachment
            XWikiAttachment attachment = skinDocument.getAttachment(template);
            if (attachment != null) {
                AttachmentReference reference = attachment.getReference();
                return new AttachmentWikiTemplate(getPath(reference), reference);
            }
        }

        return null;
    }

    private Template getTemplateFromSkin(String templateName, String skin)
    {
        Template template;

        // Try from wiki pages
        XWikiDocument skinDocument = getSkinDocument(skin);
        if (skinDocument != null) {
            template = getTemplateFromDocumentSkin(templateName, skinDocument);
        } else {
            // If not a wiki based skin try from filesystem skins
            template = getFileSystemTemplate("/skins/" + skin + '/', templateName);
        }

        return template;
    }

    private FileSystemTemplate getFileSystemTemplate(String suffixPath, String templateName)
    {
        String path = getResourcePath(suffixPath, templateName, true);

        return path != null ? new FileSystemTemplate(path) : null;
    }

    public Template getTemplate(String templateName)
    {
        Template template = null;

        // Try from skin
        String skin = getSkin();
        if (skin != null) {
            template = getTemplateFromSkin(templateName, skin);
        }

        // Try from base skin
        if (template == null) {
            String baseSkin = getBaseSkin();
            if (baseSkin != null) {
                template = getTemplateFromSkin(templateName, baseSkin);
            }
        }

        // Try from /template/ resources
        if (template == null) {
            template = getFileSystemTemplate("/templates/", templateName);
        }

        return template;
    }

    private String getPath(EntityReference reference)
    {
        return this.referenceSerializer.serialize(reference);
    }
}
