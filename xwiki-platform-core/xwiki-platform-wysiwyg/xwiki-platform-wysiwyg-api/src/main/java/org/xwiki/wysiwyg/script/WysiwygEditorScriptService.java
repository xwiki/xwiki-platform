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
package org.xwiki.wysiwyg.script;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wysiwyg.converter.HTMLConverter;
import org.xwiki.wysiwyg.importer.AttachmentImporter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The WYSIWYG editor API exposed to server-side scripts like Velocity.
 * 
 * @version $Id$
 */
@Component
@Named("wysiwyg")
@Singleton
public class WysiwygEditorScriptService implements ScriptService
{
    /**
     * The context property which indicates if the current code was called from a template (only Velocity execution) or
     * from a wiki page (wiki syntax rendering).
     * 
     * @see #parseAndRender(String, String)
     */
    private static final String IS_IN_RENDERING_ENGINE = "isInRenderingEngine";

    @Inject
    private Logger logger;

    /**
     * The component manager. We need it because we have to access components dynamically.
     */
    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * The component used to convert HTML to wiki syntax.
     */
    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("office")
    private AttachmentImporter officeAttachmentImporter;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Checks if there is a parser and a renderer available for the specified syntax.
     * <p>
     * This method should be called before attempting to load the WYSIWYG editor.
     * 
     * @param syntaxId the syntax identifier, like {@code xwiki/2.0}
     * @return {@code true} if the specified syntax is currently supported by the editor, {@code false} otherwise
     */
    public boolean isSyntaxSupported(String syntaxId)
    {
        // Special handling for XHTML since right the XHTML renderer doesn't produce valid XHTML. Thus if, for example,
        // you the WYSIWYG editor and add 2 paragraphs, it'll generate {@code <p>a</p><p>b</p>} which is invalid XHTML
        // and the page will fail to render.
        if (syntaxId.equals(Syntax.XHTML_1_0.toIdString())) {
            return false;
        }

        try {
            this.contextComponentManager.getInstance(Parser.class, syntaxId);
            this.contextComponentManager.getInstance(PrintRendererFactory.class, syntaxId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     * <p>
     * This method is currently used in {@code wysiwyginput.vm} and its purpose is to refresh the content of the WYSIWYG
     * editor. This method is called for instance when a macro is inserted or edited.
     * 
     * @param html the HTML fragment to be rendered
     * @param syntaxId the storage syntax identifier
     * @return the XHTML result of rendering the given HTML fragment
     */
    public String parseAndRender(String html, String syntaxId)
    {
        XWikiDocument originalSecurityDocument = setSecurityDocument(createSecurityDocument());

        // Save the value of the "is in rendering engine" context property.
        Object isInRenderingEngine = this.xcontextProvider.get().get(IS_IN_RENDERING_ENGINE);

        try {
            // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
            // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
            // templates using only Velocity for example).
            this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, true);

            return this.htmlConverter.parseAndRender(html, syntaxId);
        } catch (Exception e) {
            // Leave the previous HTML in case of an exception.
            return html;
        } finally {
            // Restore the value of the value of the "is in rendering engine" context property.
            if (isInRenderingEngine != null) {
                this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                this.xcontextProvider.get().remove(IS_IN_RENDERING_ENGINE);
            }

            setSecurityDocument(originalSecurityDocument);
        }
    }

    /**
     * Produces the input for the editor by rendering the specified content template as a full HTML page, making sure
     * the skin extension hooks are resolved. The template is rendered in the context of the current document and the
     * Velocity context is not isolated so you can put the data needed by the template in the Velocity context before
     * calling this method. The advantage of using this method to obtain the editor input is that the editor doesn't
     * have to make an additional HTTP request for the content template.
     * 
     * @param templateReference specifies the document that serves as the template for the editor content
     * @return the result of rendering the specified content template
     */
    public String render(DocumentReference templateReference)
    {
        if (!this.authorization.hasAccess(Right.VIEW, templateReference)) {
            return null;
        }

        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument template = xcontext.getWiki().getDocument(templateReference, xcontext);
            String templateSyntax = template.getSyntax().toIdString();
            String output = xcontext.getDoc().getRenderedContent(template.getContent(), templateSyntax, xcontext);
            // Make sure the skin extension hooks are properly replaced with style sheets includes.
            return xcontext.getWiki().getPluginManager().endParsing(output, xcontext);
        } catch (XWikiException e) {
            this.logger.debug("Failed to render [{}].", templateReference, e);
            return null;
        }
    }

    /**
     * Converts the given source text from the specified syntax to annotated XHTML, which can be used as input for the
     * WYSIWYG editor.
     * 
     * @param source the text to be converted
     * @param syntaxId the syntax identifier
     * @return the annotated XHTML result of the conversion
     */
    public String toAnnotatedXHTML(String source, String syntaxId)
    {
        XWikiDocument originalSecurityDocument = setSecurityDocument(createSecurityDocument());

        // Save the value of the "is in rendering engine" context property.
        Object isInRenderingEngine = this.xcontextProvider.get().get(IS_IN_RENDERING_ENGINE);

        try {
            // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
            // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
            // templates using only Velocity for example).
            this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, true);

            return this.htmlConverter.toHTML(source, syntaxId);
        } catch (Exception e) {
            // Return the source text in case of an exception.
            return source;
        } finally {
            // Restore the value of the value of the "is in rendering engine" context property.
            if (isInRenderingEngine != null) {
                this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                this.xcontextProvider.get().remove(IS_IN_RENDERING_ENGINE);
            }

            setSecurityDocument(originalSecurityDocument);
        }
    }

    /**
     * Converts the given annotated HTML produced by the WYSIWYG editor to the specified target syntax.
     * 
     * @param html the annotated HTML to be converted
     * @param targetSyntaxId the target syntax
     * @return the result of converting the given annotated HTML to the specified target syntax
     * @since 10.10RC1
     */
    public String fromAnnotatedXHTML(String html, String targetSyntaxId)
    {
        XWikiDocument originalSecurityDocument = setSecurityDocument(createSecurityDocument());

        // Save the value of the "is in rendering engine" context property.
        Object isInRenderingEngine = this.xcontextProvider.get().get(IS_IN_RENDERING_ENGINE);

        try {
            // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
            // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
            // templates using only Velocity for example).
            this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, true);

            return this.htmlConverter.fromHTML(html, targetSyntaxId);
        } catch (Exception e) {
            // Return the HTML input in case of an exception.
            return html;
        } finally {
            // Restore the value of the value of the "is in rendering engine" context property.
            if (isInRenderingEngine != null) {
                this.xcontextProvider.get().put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                this.xcontextProvider.get().remove(IS_IN_RENDERING_ENGINE);
            }

            setSecurityDocument(originalSecurityDocument);
        }
    }

    /**
     * When the user switches to the Source tab he'll be able to make modifications and when he switches back to the
     * WYSIWYG tab his changes will be rendered. If the document had PR, then we need to be sure that if the user
     * doesn't have PR he won't be able to execute the code. We do this by setting as security document a clone of the
     * current document that has the current user as content author (because the content author is used to check PR).
     */
    private XWikiDocument createSecurityDocument()
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();
        // We clone the document in order to not impact the environment (the document cache for example).
        XWikiDocument clonedDocument = xwikiContext.getDoc().clone();
        clonedDocument.setContentAuthorReference(xwikiContext.getUserReference());
        return clonedDocument;
    }

    /**
     * Sets the document that is going to be used to check for programming rights.
     * 
     * @param document the document that is going to be used to check for programming rights
     * @return the previous security document
     */
    private XWikiDocument setSecurityDocument(XWikiDocument document)
    {
        return (XWikiDocument) this.xcontextProvider.get().put(XWikiDocument.CKEY_SDOC, document);
    }

    /**
     * Builds the annotated XHTML needed to import the specified office attachment in the WYSIWYG editor.
     * 
     * @param attachmentReference the office attachment to import
     * @param parameters the import parameters; {@code filterStyles} controls whether styles are filtered when importing
     *            office text documents; {@code useOfficeViewer} controls whether the office viewer macro is used
     *            instead of converting the content of the office file to wiki syntax
     * @return the annotated XHTML needed to import the specified attachment into the content of the WYSIWYG editor
     */
    public String importOfficeAttachment(AttachmentReference attachmentReference, Map<String, Object> parameters)
    {
        try {
            return this.officeAttachmentImporter.toHTML(attachmentReference, parameters);
        } catch (Exception e) {
            this.logger.warn("Failed to import office attachment [{}]. Root cause is: {}",
                this.entityReferenceSerializer.serialize(attachmentReference), ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
