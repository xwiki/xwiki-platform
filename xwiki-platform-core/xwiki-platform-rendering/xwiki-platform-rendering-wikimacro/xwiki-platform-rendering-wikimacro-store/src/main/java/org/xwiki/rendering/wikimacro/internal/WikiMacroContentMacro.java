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
package org.xwiki.rendering.wikimacro.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractNoParameterMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

/**
 * Used as a placeholder for a the content passed to the wiki macro. It's resolved after the execution of the wiki
 * macro.
 * 
 * @since 11.4RC1
 * @version $Id$
 */
@Component
@Named(WikiMacroContentMacro.ID)
@Singleton
@Unstable
public class WikiMacroContentMacro extends AbstractNoParameterMacro
{
    /**
     * The name of the macro.
     * 
     * @since 12.8RC1
     * @since 12.6.2
     */
    public static final String ID = "wikimacrocontent";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display editable content of a wikimacro.";

    /**
     * Default constructor.
     */
    public WikiMacroContentMacro()
    {
        super("WikiMacro Content", DESCRIPTION);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return Collections.emptyList();
    }
}
