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
package org.xwiki.template;

import java.io.Writer;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.skin.Skin;

/**
 * Internal toolkit to experiment on wiki-based templates.
 *
 * @version $Id$
 * @since 7.0M1
 */
@Role
public interface TemplateManager
{
    /**
     * Parse the template with the provided name and return it as {@link XDOM}.
     * <p>
     * Any failure is "printed" in the returned {@link XDOM}.
     * 
     * @param template the template
     * @return the XDOM source of the template
     * @since 8.3RC1
     */
    XDOM getXDOMNoException(Template template);

    /**
     * Parse the template with the provided name and return it as {@link XDOM}.
     * <p>
     * Any failure is "printed" in the returned {@link XDOM}.
     * 
     * @param templateName the name of the template
     * @return the XDOM source of the template
     */
    XDOM getXDOMNoException(String templateName);

    /**
     * Parse the template with the provided name and return it as {@link XDOM}.
     * 
     * @param template the template
     * @return the XDOM source of the template
     * @throws Exception when failing to parse the template
     * @since 8.3RC1
     */
    XDOM getXDOM(Template template) throws Exception;

    /**
     * Parse the template with the provided name and return it as {@link XDOM}.
     * 
     * @param templateName the name of the template
     * @return the XDOM source of the template
     * @throws Exception when failing to parse the template
     */
    XDOM getXDOM(String templateName) throws Exception;

    /**
     * Execute and return the template as {@link XDOM}.
     * <p>
     * Any failure is "printed" in the returned {@link XDOM}.
     * 
     * @param template the template
     * @return the {@link XDOM} result of the template execution
     * @since 8.3RC1
     */
    XDOM executeNoException(Template template);

    /**
     * Execute and return the template as {@link XDOM}.
     * <p>
     * Any failure is "printed" in the returned {@link XDOM}.
     * 
     * @param templateName the name of the template
     * @return the {@link XDOM} result of the template execution
     */
    XDOM executeNoException(String templateName);

    /**
     * Execute and return the template as {@link XDOM}.
     * 
     * @param template the template
     * @return the {@link XDOM} result of the template execution
     * @throws Exception when failing to parse the template
     * @since 8.3RC1
     */
    XDOM execute(Template template) throws Exception;

    /**
     * Execute and return the template as {@link XDOM}.
     * 
     * @param templateName the name of the template
     * @return the {@link XDOM} result of the template execution
     * @throws Exception when failing to parse the template
     */
    XDOM execute(String templateName) throws Exception;

    /**
     * Execute and render the template in current target syntax.
     * <p>
     * Any failure is "printed" in the returned result.
     * 
     * @param templateName the name of the template
     * @return the result of the execution of the template in the current target syntax
     */
    String renderNoException(String templateName);

    /**
     * Execute and render the template in current target syntax.
     * 
     * @param templateName the name of the template
     * @return the result of the execution of the template in the current target syntax
     * @throws Exception when failing to render the template
     */
    String render(String templateName) throws Exception;

    /**
     * Execute and render the template in current target syntax.
     * 
     * @param template the template
     * @param writer the writer containing the result of the execution and rendering
     * @since 8.3RC1
     */
    void renderNoException(Template template, Writer writer);

    /**
     * Execute and render the template in current target syntax.
     * 
     * @param templateName the name of the template
     * @param writer the writer containing the result of the execution and rendering
     */
    void renderNoException(String templateName, Writer writer);

    /**
     * Execute and render the template in current target syntax.
     * 
     * @param template the name of the template
     * @param writer the writer containing the result of the execution and rendering
     * @throws Exception when failing to render the template
     */
    void render(Template template, Writer writer) throws Exception;

    /**
     * Execute and render the template in current target syntax.
     * 
     * @param templateName the name of the template
     * @param writer the writer containing the result of the execution and rendering
     * @throws Exception when failing to render the template
     */
    void render(String templateName, Writer writer) throws Exception;

    /**
     * Execute and render the template in current target syntax from the passed skin. When the template is not found in
     * the passed skin it fallback on skin parent etc.
     * 
     * @param templateName the name of the template
     * @param skin the skin
     * @return the result of the execution of the template in the current target syntax
     * @throws Exception when failing to render the template
     */
    String renderFromSkin(String templateName, Skin skin) throws Exception;

    /**
     * Execute and render the template in current target syntax from the passed skin. When the template is not found in
     * the passed skin it fallback on skin parent etc.
     * 
     * @param templateName the name of the template
     * @param skin the skin
     * @param writer the writer containing the result of the execution and rendering
     * @throws Exception when failing to render the template
     */
    void renderFromSkin(String templateName, Skin skin, Writer writer) throws Exception;

    /**
     * Search the template with passed name in the passed skin.
     * <p>
     * Does not fallback on parent skin.
     * 
     * @param templateName the name of the template
     * @param skin the skin
     * @return the template
     */
    Template getSkinTemplate(String templateName, Skin skin);

    /**
     * Search the template with passed name in the passed skin.
     * <p>
     * Fallback on parent skin.
     * 
     * @param templateName the name of the template
     * @param skin the skin
     * @return the template
     */
    Template getTemplate(String templateName, Skin skin);

    /**
     * Search everywhere for the template with passed name depending on the current context (current skin, etc).
     * 
     * @param templateName the name of the template
     * @return the template
     */
    Template getTemplate(String templateName);

    /**
     * Create a new template using a given content and a specific author.
     *
     * @param content the template content
     * @param author the template author
     * @return the template
     * @throws Exception if an error occurred during template instanciation
     * @since 9.6RC1
     */
    default Template createStringTemplate(String content, DocumentReference author) throws Exception
    {
        throw new UnsupportedOperationException(
                "org.xwiki.template.TemplateManager#createStringTemplate() "
                        + "has been called without being reimplemented.");
    }
}
