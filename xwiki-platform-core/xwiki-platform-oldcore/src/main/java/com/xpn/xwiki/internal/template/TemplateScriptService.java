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

import groovy.lang.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;

/**
 * Internal helper to manipulate wiki based templates from scripts.
 * 
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Singleton
public class TemplateScriptService implements ScriptService
{
    private WikiTemplateRenderer renderer;

    /**
     * Execute and render passed template.
     * 
     * @param template the template name
     * @param targetSyntax the syntax in which to render the template
     * @return the result of the template execution and rendering
     */
    public String render(String template, Syntax targetSyntax)
    {
        return this.renderer.renderNoExceptions(template, targetSyntax);
    }

    /**
     * Execute and render passed template.
     * 
     * @param template the template name
     * @param targetSyntax the syntax in which to render the template
     * @param transformationId the identifier of the transformation
     * @return the result of the template execution and rendering
     */
    public String render(String template, Syntax targetSyntax, String transformationId)
    {
        return this.renderer.renderNoExceptions(template, targetSyntax, transformationId);
    }
}
