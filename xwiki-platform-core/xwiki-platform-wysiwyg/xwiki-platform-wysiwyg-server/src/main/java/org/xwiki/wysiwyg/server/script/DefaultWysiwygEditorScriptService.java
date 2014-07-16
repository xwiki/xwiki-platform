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
package org.xwiki.wysiwyg.server.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.converter.HTMLConverter;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.wysiwyg.server.WysiwygEditorConfiguration;
import org.xwiki.wysiwyg.server.WysiwygEditorScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link WysiwygEditorScriptService}.
 * 
 * @version $Id$
 */
@Component
@Named("wysiwyg")
@Singleton
public class DefaultWysiwygEditorScriptService implements WysiwygEditorScriptService
{
    /**
     * The context property which indicates if the current code was called from a template (only Velocity execution) or
     * from a wiki page (wiki syntax rendering).
     * 
     * @see #parseAndRender(String, String)
     */
    private static final String IS_IN_RENDERING_ENGINE = "isInRenderingEngine";

    /**
     * The component manager. We need it because we have to access components dynamically.
     */
    @Inject
    private ComponentManager componentManager;

    /** Execution context handler, needed for accessing the XWiki context map. */
    @Inject
    private Execution execution;

    /**
     * The component used to convert HTML to wiki syntax.
     */
    @Inject
    private HTMLConverter htmlConverter;

    /**
     * The component used to access the WYSIWYG editor configuration properties.
     */
    @Inject
    private WysiwygEditorConfiguration editorConfiguration;

    @Override
    public boolean isSyntaxSupported(String syntaxId)
    {
        try {
            this.componentManager.getInstance(Parser.class, syntaxId);
            this.componentManager.getInstance(PrintRendererFactory.class, syntaxId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String parseAndRender(String html, String syntax)
    {
        protectAgainstProgrammingRightsExecution();

        // Save the value of the "is in rendering engine" context property.
        Object isInRenderingEngine = getXWikiContext().get(IS_IN_RENDERING_ENGINE);

        try {
            // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
            // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
            // templates using only Velocity for example).
            getXWikiContext().put(IS_IN_RENDERING_ENGINE, true);

            return this.htmlConverter.parseAndRender(html, syntax);
        } catch (Exception e) {
            // Leave the previous HTML in case of an exception.
            return html;
        } finally {
            // Restore the value of the value of the "is in rendering engine" context property.
            if (isInRenderingEngine != null) {
                getXWikiContext().put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                getXWikiContext().remove(IS_IN_RENDERING_ENGINE);
            }
        }
    }

    @Override
    public String toAnnotatedXHTML(String source, String syntaxId)
    {
        protectAgainstProgrammingRightsExecution();

        // Save the value of the "is in rendering engine" context property.
        Object isInRenderingEngine = getXWikiContext().get(IS_IN_RENDERING_ENGINE);

        try {
            // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
            // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
            // templates using only Velocity for example).
            getXWikiContext().put(IS_IN_RENDERING_ENGINE, true);

            return this.htmlConverter.toHTML(source, syntaxId);
        } catch (Exception e) {
            // Return the source text in case of an exception.
            return source;
        } finally {
            // Restore the value of the value of the "is in rendering engine" context property.
            if (isInRenderingEngine != null) {
                getXWikiContext().put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                getXWikiContext().remove(IS_IN_RENDERING_ENGINE);
            }
        }
    }

    @Override
    public WysiwygEditorConfiguration getConfig()
    {
        return this.editorConfiguration;
    }

    /**
     * When the user switches to the Source tab he'll be able to make modifications and when he switches back to the
     * WYSIWYG tab his changes will be rendered. If the document had PR, then we need to be sure that if the user
     * doesn't have PR he won't be able to execute the code. We do this by setting the content author (similar to
     * what happens when a document is saved)
     */
    private void protectAgainstProgrammingRightsExecution()
    {
        XWikiContext xwikiContext = getXWikiContext();
        xwikiContext.getDoc().setContentAuthorReference(xwikiContext.getUserReference());
    }

    @SuppressWarnings("unchecked")
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
