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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Insert a template.
 * 
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("template")
@Singleton
public class TemplateMacro extends AbstractMacro<TemplateMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "[INTERNAL] Insert a template.";

    @Inject
    private WikiTemplateRenderer renderer;

    /**
     * Default constructor.
     */
    public TemplateMacro()
    {
        super("Template", DESCRIPTION, TemplateMacroParameters.class);

        // The template macro must execute first since if it runs with the current context it needs to bring
        // all the macros from the template before the other macros are executed.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_INTERNAL);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(TemplateMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return this.renderer.getXDOM(parameters.getName()).getChildren();
    }
}
