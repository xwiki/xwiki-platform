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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Used as a placeholder for a the value of a parameter passed to the wiki macro. It's resolved after the execution of
 * the wiki macro.
 * 
 * @since 11.5RC1
 * @version $Id$
 */
@Component
@Named(WikiMacroParameterMacro.ID)
@Singleton
public class WikiMacroParameterMacro extends AbstractMacro<WikiMacroParameterMacroParameters>
{
    /**
     * The name of the macro.
     * 
     * @since 12.8RC1
     * @since 12.6.2
     */
    public static final String ID = "wikimacroparameter";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display editable parameter of a wikimacro.";

    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * Default constructor.
     */
    public WikiMacroParameterMacro()
    {
        super("WikiMacro Parameter", DESCRIPTION, new DefaultContentDescriptor(false),
            WikiMacroParameterMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(WikiMacroParameterMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        Map<String, String> placeholderParameters = new LinkedHashMap<>();
        placeholderParameters.put("data-wikimacro-id", ID);
        placeholderParameters.put("name", parameters.getName());

        if (context.isInline()) {
            return Collections
                .singletonList(new FormatBlock(Collections.emptyList(), Format.NONE, placeholderParameters));
        } else {
            return Collections.singletonList(new GroupBlock(placeholderParameters));
        }
    }
}
