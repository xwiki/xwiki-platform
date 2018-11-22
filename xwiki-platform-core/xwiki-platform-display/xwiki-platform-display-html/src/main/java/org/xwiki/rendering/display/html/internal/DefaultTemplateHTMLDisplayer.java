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
package org.xwiki.rendering.display.html.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.displayer.HTMLDisplayer;
import org.xwiki.displayer.HTMLDisplayerException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

/**
 * Default implementation of {@code HTMLDisplayer} using templates.
 *
 * @param <T> the type of the {@code HTMLDisplayer}
 * @version $Id$
 * @since 10.11RC1
 */
@Component
public class DefaultTemplateHTMLDisplayer<T> implements HTMLDisplayer<T>
{
    /**
     * Folder containing the HTML Displayers velocity templates.
     */
    public static final String TEMPLATE_FOLDER = "html_displayer";

    /**
     * Template extension (velocity).
     */
    public static final String TEMPLATE_EXTENSION = ".vm";

    @Inject
    protected TemplateManager templateManager;

    @Inject
    protected ScriptContextManager scriptContextManager;

    /**
     * {@inheritDoc}
     *
     * Displays the value with the 'view' mode.
     */
    @Override
    public String display(T value) throws HTMLDisplayerException
    {
        return display(value, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     *
     * Displays the value with the 'view' mode.
     */
    @Override
    public String display(T value, Map<String, String> parameters) throws HTMLDisplayerException
    {
        return display(value, parameters, "view");
    }

    @Override
    public String display(T value, Map<String, String> parameters, String mode) throws HTMLDisplayerException
    {
        try {
            ScriptContext scriptContext = scriptContextManager.getCurrentScriptContext();
            scriptContext.setAttribute("value", value, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("parameters", parameters, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("mode", mode, ScriptContext.ENGINE_SCOPE);

            return templateManager.render(getTemplateName(value, mode));
        } catch (Exception e) {
            throw new HTMLDisplayerException("Couldn't render the template", e);
        }
    }

    /**
     * Computes the template name.
     * The following names will be use in this priority order to find an existing template:
     * <ul>
     *   <li>html_displayer/[type]/[mode].vm
     *   <li>html_displayer/[type].vm
     *   <li>html_displayer/[mode].vm
     *   <li>html_displayer/default.vm
     * </ul>
     * @return the template name used to make the rendering
     */
    protected String getTemplateName(T value, String mode) {
        String type = value.getClass().getSimpleName().toLowerCase();
        String templateName = TEMPLATE_FOLDER + '/' + type + "/" + mode + TEMPLATE_EXTENSION;
        if (templateManager.getTemplate(templateName) == null) {
            templateName = TEMPLATE_FOLDER + '/' + type + TEMPLATE_EXTENSION;
        }
        if (templateManager.getTemplate(templateName) == null) {
            templateName = TEMPLATE_FOLDER + '/' + mode + TEMPLATE_EXTENSION;
        }
        if (templateManager.getTemplate(templateName) == null) {
            templateName = TEMPLATE_FOLDER + '/' + "default" + TEMPLATE_EXTENSION;
        }

        return templateName;
    }
}
