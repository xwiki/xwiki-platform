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
package org.xwiki.icon.macro.internal;

import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.macro.DisplayIconMacroParameters;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import java.util.List;
import java.util.Map;

class IconParser
{
    public XDOM parseIcon(DisplayIconMacroParameters parameters, MacroTransformationContext context,
                          IconSet iconSet, MacroContentParser parser, 
                          EntityReferenceSerializer<String> defaultEntityReferenceSerializer,
                          IconRenderer iconRenderer)
            throws IconException, MacroExecutionException
    {
        String iconContent = iconRenderer.render(parameters.getName(), iconSet);
        MetaData metaData = null;

        if (iconSet.getSourceDocumentReference() != null) {
            String stringReference = defaultEntityReferenceSerializer.serialize(iconSet.getSourceDocumentReference());
            metaData = new MetaData(Map.of(MetaData.SOURCE, stringReference));
        }

        XDOM iconXDOM = parser.parse(iconContent, Syntax.XWIKI_2_1, context, false, metaData, true);
        if (!context.isInline()) {
            // Wrap the children of the XDOM in a paragraph. We don't ask the parser to produce block content as
            // icons should always be inline, and some icons are defined as raw inline HTML.
            Block wrapper = new ParagraphBlock(iconXDOM.getChildren());
            iconXDOM.setChildren(List.of(wrapper));
        }
        return iconXDOM;
    }
}
