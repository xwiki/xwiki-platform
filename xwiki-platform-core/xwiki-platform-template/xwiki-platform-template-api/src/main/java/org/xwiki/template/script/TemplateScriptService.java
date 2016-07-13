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
package org.xwiki.template.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;

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
    private TemplateManager templates;

    /**
     * Execute and render passed template.
     * <p>
     * The current transformation id is used.
     *
     * @param template the template name
     * @return the result of the template execution and rendering
     */
    public String render(String template)
    {
        return this.templates.renderNoException(template);
    }

    /**
     * Execute the passed template.
     * <p>
     * The current transformation id is used.
     *
     * @param template the template name
     * @throws Exception when failing to execute the template
     */
    public void execute(String template) throws Exception
    {
        this.templates.execute(template);
    }
}
