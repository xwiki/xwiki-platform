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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;

import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_TYPE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_TYPE_WIKI;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS;

/**
 * Required rights analyzer for {@code XWiki.WikiMacroParameterClass}.
 *
 * @version $Id$
 * @since 17.0.0
 * @since 16.10.3
 * @since 16.4.7
 */
@Component
@Singleton
@Named(WIKI_MACRO_PARAMETER_CLASS)
public class WikiMacroParameterObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private ContentParser contentParser;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results =
            new ArrayList<>(analyzeWikiContent(object, PARAMETER_DESCRIPTION_PROPERTY));
        String type = object.getStringValue(PARAMETER_TYPE_PROPERTY);
        try {
            // Only check types that contain "<" to avoid parsing types that cannot be the list block type.
            if (PARAMETER_TYPE_WIKI.equals(type) || (StringUtils.contains(type, "<") && Block.LIST_BLOCK_TYPE.equals(
                ReflectionUtils.unserializeType(type, Thread.currentThread().getContextClassLoader()))))
            {
                results.addAll(analyzeWikiContent(object, PARAMETER_DEFAULT_VALUE_PROPERTY));
            }
        } catch (ClassNotFoundException e) {
            // Ignore an unknown parameter type as it can't be the wiki parameter type.
        }

        return results;
    }

    private List<RequiredRightAnalysisResult> analyzeWikiContent(BaseObject object, String propertyName)
        throws RequiredRightsException
    {
        String value = object.getStringValue(propertyName);
        if (StringUtils.isNotBlank(value)) {
            EntityReference reference = object.getField(propertyName).getReference();
            try {
                XDOM parsedContent = this.contentParser.parse(value, object.getOwnerDocument().getSyntax(),
                    object.getDocumentReference());
                parsedContent.getMetaData().addMetaData("entityReference", reference);
                return this.xdomRequiredRightAnalyzer.analyze(parsedContent);
            } catch (ParseException | MissingParserException e) {
                throw new RequiredRightsException("Failed to parse content of object property", e);
            }
        }

        return List.of();
    }
}
