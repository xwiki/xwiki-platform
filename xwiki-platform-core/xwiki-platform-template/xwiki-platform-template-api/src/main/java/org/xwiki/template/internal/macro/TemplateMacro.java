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
package org.xwiki.template.internal.macro;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.macro.TemplateMacroParameters;

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
    private static final String DESCRIPTION = "Insert a template.";

    @Inject
    private TemplateManager templates;

    /**
     * Default constructor.
     */
    public TemplateMacro()
    {
        super("Template", DESCRIPTION, TemplateMacroParameters.class);

        // The template macro must execute first since if it runs with the current context it needs to bring
        // all the macros from the template before the other macros are executed.
        setPriority(10);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
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
        Block result;
        try {
            result = this.templates.execute(parameters.getName(), context.isInline());
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to execute template [" + parameters.getName() + "]", e);
        }

        if (result != null && parameters.isOutput()) {
            return result instanceof XDOM || result instanceof CompositeBlock ? result.getChildren()
                : Collections.singletonList(result);
        } else {
            return Collections.emptyList();
        }
    }
}
