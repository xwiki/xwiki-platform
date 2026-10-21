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

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.ObjectPropertyRequiredRightAnalyzer;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;

import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_VISIBILITY_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.WIKI_MACRO_CLASS;

/**
 * Required rights analyzer for {@code XWiki.WikiMacroClass}.
 * <p>
 * In addition to the rights required by the properties of the macro (like the scripts in its code), the author of the
 * macro document needs the right that {@link DefaultWikiMacroFactory#isAllowed} checks for the visibility of the
 * macro, otherwise the macro isn't registered.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.8
 * @since 17.10.15
 */
@Component
@Singleton
@Named(WIKI_MACRO_CLASS)
public class WikiMacroObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        WikiMacroVisibility visibility =
            WikiMacroVisibility.fromString(object.getStringValue(MACRO_VISIBILITY_PROPERTY));

        return switch (visibility) {
            case GLOBAL -> this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(object,
                RequiredRight.PROGRAM, "rendering.wikimacro.requiredrights.global");
            case WIKI -> this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(object,
                RequiredRight.WIKI_ADMIN, "rendering.wikimacro.requiredrights.wiki");
            case USER -> this.objectPropertyRequiredRightAnalyzer.analyzeAllProperties(object);
        };
    }
}
