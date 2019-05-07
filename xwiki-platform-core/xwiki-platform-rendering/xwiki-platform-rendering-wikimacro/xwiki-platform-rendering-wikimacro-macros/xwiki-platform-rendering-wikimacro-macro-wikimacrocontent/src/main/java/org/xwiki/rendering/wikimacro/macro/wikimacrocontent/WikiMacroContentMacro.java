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
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * A macro that should only be used inside a WikiMacro to show the content of the wikimacro and make it inline editable.
 * @since 11.4RC1
 * @version $Id$
 */
@Component
@Named(WikiMacroContentMacro.WIKIMACRO_CONTENT_MACRO)
@Singleton
@Unstable
public class WikiMacroContentMacro extends AbstractNoParameterMacro
{
    /**
     * The name of the macro.
     */
    public static final String WIKIMACRO_CONTENT_MACRO = "wikimacrocontent";

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
        super(WIKIMACRO_CONTENT_MACRO);
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
        MetaData result;

        if (macroInfo != null && macroInfo.containsKey(WIKIMACRO_DESCRIPTOR)) {
            MacroDescriptor macroDescriptor = (MacroDescriptor) macroInfo.get(WIKIMACRO_DESCRIPTOR);
            result = AbstractMacro.getNonGeneratedContentMetaData(macroDescriptor.getContentDescriptor());
        } else {
            result = AbstractMacro.getNonGeneratedContentMetaData(null);
        }

        return result;
    }

    @Override
    public List<Block> execute(Object parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();
        Map<String, Object> macroInfo = (Map) getContext().get("macro");
        String macroContent = extractMacroContent(macroInfo);
        if (macroContent != null) {
            XDOM parse = this.contentParser.parse(macroContent, context, true, context.isInline());
            result = Collections.singletonList(new MetaDataBlock(parse.getChildren(),
                this.getNonGeneratedContentMetaData(macroInfo)));
        }

        return result;
    }
}
