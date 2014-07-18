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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Internal helper to manipulate wiki based templates from scripts.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named("template")
@Singleton
public class TemplateScriptService implements ScriptService
{
    @Inject
    private WikiTemplateRenderer renderer;

    /**
     * Execute and render passed template.
     * <p>
     * The current transformation id is used.
     * 
     * @param template the template name
     * @param targetSyntax the syntax in which to render the template
     * @return the result of the template execution and rendering
     */
    public String render(String template)
    {
        return this.renderer.renderNoException(template);
    }

    /**
     * Execute and passed template.
     * <p>
     * The current transformation id is used.
     * 
     * @param template the template name
     * @param transformationId the identifier of the transformation
     * @return the result of the template execution and rendering
     */
    public void execute(String template) throws Exception
    {
        this.renderer.execute(template);
    }
}
