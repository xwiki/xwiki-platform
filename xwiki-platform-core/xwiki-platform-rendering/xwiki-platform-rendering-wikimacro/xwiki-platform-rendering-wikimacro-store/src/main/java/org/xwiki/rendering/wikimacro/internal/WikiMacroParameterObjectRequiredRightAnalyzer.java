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
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.ObjectPropertyRequiredRightAnalyzer;
import org.xwiki.rendering.block.Block;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;

import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY;
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
    private ObjectPropertyRequiredRightAnalyzer propertyRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results =
            new ArrayList<>(this.propertyRequiredRightAnalyzer.analyzeAllProperties(object));
        String type = object.getStringValue(PARAMETER_TYPE_PROPERTY);
        try {
            // Only check types that contain "<" to avoid parsing types that cannot be the list block type.
            if (PARAMETER_TYPE_WIKI.equals(type) || (StringUtils.contains(type, "<") && Block.LIST_BLOCK_TYPE.equals(
                ReflectionUtils.unserializeType(type, Thread.currentThread().getContextClassLoader()))))
            {
                String content = object.getStringValue(PARAMETER_DEFAULT_VALUE_PROPERTY);
                PropertyInterface defaultField = object.getField(PARAMETER_DEFAULT_VALUE_PROPERTY);
                results.addAll(this.propertyRequiredRightAnalyzer.analyzeWikiContent(object, content,
                    defaultField.getReference()));
            }
        } catch (ClassNotFoundException e) {
            // Ignore an unknown parameter type as it can't be the wiki parameter type.
        }

        return results;
    }
}
