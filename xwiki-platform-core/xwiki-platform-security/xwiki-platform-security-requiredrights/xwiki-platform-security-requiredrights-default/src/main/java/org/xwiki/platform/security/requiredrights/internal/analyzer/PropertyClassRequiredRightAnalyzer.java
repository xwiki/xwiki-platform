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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;

import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Required rights analyzer for a {@link PropertyClass}. Analyzes the custom display script.
 *
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 * @version $Id$
 */
@Component
@Singleton
public class PropertyClassRequiredRightAnalyzer implements RequiredRightAnalyzer<PropertyClass>
{
    @Inject
    private XClassWikiContentAnalyzer wikiContentAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(PropertyClass propertyClass) throws RequiredRightsException
    {
        if (propertyClass != null && StringUtils.isNotBlank(propertyClass.getCustomDisplay())) {
            return this.wikiContentAnalyzer.analyzeWikiContent(propertyClass.getOwnerDocument().getXClass(),
                propertyClass.getCustomDisplay(), propertyClass.getReference());
        }

        return List.of();
    }
}
