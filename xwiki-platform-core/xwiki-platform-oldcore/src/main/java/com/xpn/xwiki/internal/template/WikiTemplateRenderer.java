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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.input.ReaderInputSource;
import org.xwiki.filter.internal.input.DefaultInputStreamInputSource;
import org.xwiki.filter.internal.input.StringInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.MissingParserException;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Internal toolkit to experiment on wiki bases templates.
 * 
 * @version $Id$
 * @since 6.1M1
 */
@Component(roles = WikiTemplateRenderer.class)
@Singleton
public class WikiTemplateRenderer
{
    private static final Pattern FIRSTLINE = Pattern.compile("^##(source|raw)\\.syntax=(.*)$\r?\n?", Pattern.MULTILINE);

    private static final LocalDocumentReference SKINCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiSkins");

    @Inject
    private Environment environment;

    @Inject
    private ContentParser parser;

    @Inject
    private SyntaxFactory syntaxFactory;

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
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private Logger logger;

    private class StringContent
    {
        public String content;

        public Syntax sourceSyntax;

        public Syntax rawSyntax;

        public StringContent(String content, Syntax sourceSyntax, Syntax rawSyntax)
        {
            this.content = content;
            this.sourceSyntax = sourceSyntax;
            this.rawSyntax = rawSyntax;
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

            // Try to get it from user preferences
            skin = getSkinFromUser();
            if (skin != null) {
                return skin;
            }
        }

        // Try to get it from xwiki.cfg
        skin = this.xwikicfg.getProperty("xwiki.defaultskin", "colibri");

        return StringUtils.isNotEmpty(skin) ? skin : null;
    }

    private String getSkinFromUser()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWiki xwiki = xcontext.getWiki();
        if (xwiki != null && xwiki.getStore() != null) {
            String skin = xwiki.getUserPreference("skin", xcontext);
            if (StringUtils.isEmpty(skin)) {
                return null;
            }
        }

        return null;
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
        baseskin = this.xwikicfg.getProperty("xwiki.defaultbaseskin", "colibri");

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

    @SuppressWarnings("resource")
    private InputSource getTemplateStreamFromSkin(String skin, String template)
    {
        InputSource source = null;

        // Try from wiki pages
        source = getTemplateStreamFromDocumentSkin(skin, template);

        // Try from filesystem skins
        if (skin != null) {
            InputStream stream = this.environment.getResourceAsStream("/skins/" + skin + "/" + template);
            if (stream != null) {
                return new DefaultInputStreamInputSource(stream, true);
            }
        }

        return source;
    }

    private InputSource getTemplateStreamFromDocumentSkin(String skin, String template)
    {
        XWikiDocument skinDocument = getSkinDocument(skin);

        if (skinDocument != null) {
            // Try parsing the object property
            BaseObject skinObject = skinDocument.getXObject(SKINCLASS_REFERENCE);
            if (skinObject != null) {
                String escapedTemplateName = template.replaceAll("/", ".");
                String content = skinObject.getStringValue(escapedTemplateName);

                return new StringInputSource(content);
            }

            // Try parsing a document attachment
            XWikiAttachment attachment = skinDocument.getAttachment(template);
            if (attachment != null) {
                // It's impossible to know the real attachment encoding, but let's assume that they respect the
                // standard and use UTF-8 (which is required for the files located on the filesystem)
                try {
                    return new DefaultInputStreamInputSource(attachment.getContentInputStream(this.xcontextProvider
                        .get()), true);
                } catch (XWikiException e) {
                    this.logger.error("Faied to get attachment content [{}]", skinDocument.getDocumentReference(), e);
                }
            }
        }

        return null;
    }

    @SuppressWarnings("resource")
    private InputSource getTemplateStream(String template)
    {
        InputSource source = null;

        // Try from skin
        String skin = getSkin();
        if (skin != null) {
            source = getTemplateStreamFromSkin(skin, template);
        }

        // Try from base skin
        if (source == null) {
            String baseSkin = getBaseSkin();
            if (baseSkin != null) {
                source = getTemplateStreamFromSkin(baseSkin, template);
            }
        }

        // Try from /template/ resources
        if (source == null) {
            String templatePath = "/templates/" + template;

            // Prevent inclusion of templates from other directories
            String normalizedTemplate = URI.create(templatePath).normalize().toString();
            if (!normalizedTemplate.startsWith("/templates/")) {
                this.logger.warn("Direct access to template file [{}] refused. Possible break-in attempt!", normalizedTemplate);

                return null;
            }

            source = new DefaultInputStreamInputSource(this.environment.getResourceAsStream(templatePath), true);
        }

        return source;
    }

    private StringContent getStringContent(String template) throws IOException, ParseException
    {
        InputSource source = getTemplateStream(template);

        if (source == null) {
            return null;
        }

        String content;
        try {
            if (source instanceof StringInputSource) {
                content = source.toString();
            } else if (source instanceof ReaderInputSource) {
                content = IOUtils.toString(((ReaderInputSource) source).getReader());
            } else if (source instanceof InputStreamInputSource) {
                content = IOUtils.toString(((InputStreamInputSource) source).getInputStream(), "UTF-8");
            } else {
                // Unsupported type
                return null;
            }
        } finally {
            source.close();
        }

        Matcher matcher = FIRSTLINE.matcher(content);

        if (matcher.find()) {
            content = content.substring(matcher.end());

            String syntaxString = matcher.group(2);
            Syntax syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxString);

            String mode = matcher.group(1);
            switch (mode) {
                case "source":
                    return new StringContent(content, syntax, null);
                case "raw":
                    return new StringContent(content, null, syntax);
                default:
                    break;
            }
        }

        // The default is xhtml to support old templates
        return new StringContent(content, null, Syntax.XHTML_1_0);
    }

    private String renderError(Throwable throwable)
    {
        XDOM xdom = generateError(throwable);

        WikiPrinter printer = new DefaultWikiPrinter();

        BlockRenderer blockRenderer;
        try {
            blockRenderer =
                this.componentManagerProvider.get().getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
        } catch (ComponentLookupException e) {
            blockRenderer = this.plainRenderer;
        }

        blockRenderer.render(xdom, printer);

        return printer.toString();
    }

    private XDOM generateError(Throwable throwable)
    {
        List<Block> errorBlocks = new ArrayList<Block>();

        // Add short message
        Map<String, String> errorBlockParams = Collections.singletonMap("class", "xwikirenderingerror");
        errorBlocks.add(new GroupBlock(Arrays.<Block> asList(new WordBlock("Failed to render step content")),
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
     * @param template the template to parse
     * @return the result of the template parsing
     */
    public XDOM getXDOMNoException(String template)
    {
        XDOM xdom;

        try {
            xdom = getXDOM(template);
        } catch (Throwable e) {
            xdom = generateError(e);
        }

        return xdom;
    }

    private XDOM getXDOM(StringContent content) throws ParseException, MissingParserException,
        XWikiVelocityException
    {
        XDOM xdom;

        if (content != null) {
            if (content.sourceSyntax != null) {
                xdom = this.parser.parse(content.content, content.sourceSyntax);
            } else {
                String result = evaluateString(content.content);
                xdom = new XDOM(Arrays.asList(new RawBlock(result, content.rawSyntax)));
            }
        } else {
            xdom = new XDOM(Collections.<Block> emptyList());
        }

        return xdom;
    }

    public XDOM getXDOM(String template) throws IOException, ParseException, MissingParserException,
        XWikiVelocityException
    {
        StringContent content = getStringContent(template);

        return getXDOM(content);
    }

    public String renderNoException(String template)
    {
        try {
            return render(template);
        } catch (Exception e) {
            return renderError(e);
        }
    }

    public String render(String template) throws ComponentLookupException, IOException, ParseException,
        MissingParserException, XWikiVelocityException
    {
        StringContent content = getStringContent(template);

        if (content != null) {
            if (content.sourceSyntax != null) {
                XDOM xdom = execute(content);

                WikiPrinter printer = new DefaultWikiPrinter();

                BlockRenderer blockRenderer =
                    this.componentManagerProvider.get()
                        .getInstance(BlockRenderer.class, getTargetSyntax().toIdString());
                blockRenderer.render(xdom, printer);

                return printer.toString();
            } else {
                return evaluateString(content.content);
            }
        } else {
            return "";
        }
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

    private XDOM execute(StringContent content) throws ParseException, MissingParserException,
        XWikiVelocityException
    {
        XDOM xdom = getXDOM(content);

        transform(xdom);

        return xdom;
    }

    public XDOM execute(String template) throws IOException, ParseException, MissingParserException,
        XWikiVelocityException
    {
        StringContent content = getStringContent(template);

        return execute(content);
    }

    private String evaluateString(String content) throws XWikiVelocityException
    {
        VelocityContext velocityContext = this.velocityManager.getVelocityContext();

        StringWriter writer = new StringWriter();

        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String namespace = this.renderingContext.getTransformationId();
        if (namespace == null) {
            namespace = "unknown namespace";
        }

        VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();

        velocityEngine.startedUsingMacroNamespace(namespace);
        try {
            velocityEngine.evaluate(velocityContext, writer, namespace, content);
        } finally {
            velocityEngine.stoppedUsingMacroNamespace(namespace);
        }

        return writer.toString();
    }

    private Syntax getTargetSyntax()
    {
        Syntax targetSyntax = this.renderingContext.getTargetSyntax();

        return targetSyntax != null ? targetSyntax : Syntax.PLAIN_1_0;
    }
}
