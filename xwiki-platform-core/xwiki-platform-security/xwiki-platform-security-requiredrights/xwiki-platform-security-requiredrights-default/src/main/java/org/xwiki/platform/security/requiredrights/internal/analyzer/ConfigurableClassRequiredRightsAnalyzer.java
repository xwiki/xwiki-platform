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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;

/**
 * Required rights analyzer for {@code XWiki.ConfigurableClass}.
 * <p>
 * This required rights analyzer is in the required rights module to avoid introducing a new module just for it.
 *
 * @version $Id$
 * @since 15.10
 */
@Component
@Singleton
@Named("XWiki.ConfigurableClass")
public class ConfigurableClassRequiredRightsAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result =
            new ArrayList<>(this.objectPropertyRequiredRightAnalyzer.analyzeAllProperties(object));
        String headingFieldName = "heading";
        String heading = object.getStringValue(headingFieldName);
        PropertyInterface field = object.getField(headingFieldName);
        if (StringUtils.isNotBlank(heading)) {
            result.addAll(this.objectPropertyRequiredRightAnalyzer.analyzeVelocityScriptValue(heading,
                field.getReference(), "security.requiredrights.object.configurableClassHeading"));
        }

        return result;
    }
}
