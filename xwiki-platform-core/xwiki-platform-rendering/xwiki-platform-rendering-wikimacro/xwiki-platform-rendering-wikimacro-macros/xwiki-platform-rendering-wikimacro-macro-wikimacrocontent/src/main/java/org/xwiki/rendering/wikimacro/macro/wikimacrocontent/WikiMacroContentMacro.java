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
package org.xwiki.rendering.wikimacro.macro.wikimacrocontent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.AbstractNoParameterMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * A macro that should only be used inside a WikiMacro to show the content of the wikimacro and make it inline editable.
 * @since 11.4RC1
 * @version $Id$
 */
@Component
@Named("wikimacrocontent")
@Singleton
@Unstable
public class WikiMacroContentMacro extends AbstractNoParameterMacro
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display editable content of a wikimacro.";

    private static final String WIKIMACRO_CONTENT = "content";

    private static final String WIKIMACRO_DESCRIPTOR = "descriptor";

    /**
     * The {@link Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    private MacroContentParser contentParser;

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

    /**
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    private String extractMacroContent(Map<String, Object> macroInfo)
    {
        String content = null;
        if (macroInfo != null && macroInfo.containsKey(WIKIMACRO_CONTENT)) {
            content = (String) macroInfo.get(WIKIMACRO_CONTENT);
        }

        return content;
    }

    private MetaData getNonGeneratedContentMetaData(Map<String, Object> macroInfo)
    {
        ContentDescriptor contentDescriptor = getMacroContentDescriptor(macroInfo);
        return AbstractMacro.getNonGeneratedContentMetaData(contentDescriptor);
    }

    private ContentDescriptor getMacroContentDescriptor(Map<String, Object> macroInfo)
    {
        ContentDescriptor result = null;
        if (macroInfo != null && macroInfo.containsKey(WIKIMACRO_DESCRIPTOR)) {
            MacroDescriptor macroDescriptor = (MacroDescriptor) macroInfo.get(WIKIMACRO_DESCRIPTOR);
            result = macroDescriptor.getContentDescriptor();
        }
        return result;
    }

    private XDOM parseContent(String macroContent, Map<String, Object> macroInfo, MacroTransformationContext context)
        throws MacroExecutionException
    {
        ContentDescriptor contentDescriptor = getMacroContentDescriptor(macroInfo);
        Syntax currentSyntax = context.getSyntax();
        if (contentDescriptor == null || !contentDescriptor.getType().equals(Block.LIST_BLOCK_TYPE)) {
            context.setSyntax(Syntax.PLAIN_1_0);
        }
        try {
            return this.contentParser.parse(macroContent, context, true, context.isInline());
        } finally {
            context.setSyntax(currentSyntax);
        }
    }

    @Override
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();
        Map<String, Object> macroInfo = (Map) getContext().get("macro");
        String macroContent = extractMacroContent(macroInfo);
        if (macroContent != null) {
            MetaData nonGeneratedContentMetaData = this.getNonGeneratedContentMetaData(macroInfo);
            nonGeneratedContentMetaData.addMetaData("wikimacrocontent", "true");
            XDOM parse = this.parseContent(macroContent, macroInfo, context);
            result = Collections.singletonList(new MetaDataBlock(parse.getChildren(),
                nonGeneratedContentMetaData));
        }

        return result;
    }
}
