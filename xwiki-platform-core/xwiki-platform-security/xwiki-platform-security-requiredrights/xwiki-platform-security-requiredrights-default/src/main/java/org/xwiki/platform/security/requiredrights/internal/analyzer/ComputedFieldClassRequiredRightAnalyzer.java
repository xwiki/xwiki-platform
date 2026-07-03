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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;

import com.xpn.xwiki.objects.classes.ComputedFieldClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Required rights analyzer for {@link ComputedFieldClass}.
 *
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 * @version $Id$
 */
@Component
@Singleton
public class ComputedFieldClassRequiredRightAnalyzer implements RequiredRightAnalyzer<ComputedFieldClass>
{
    @Inject
    private XClassWikiContentAnalyzer wikiContentAnalyzer;

    @Inject
    private RequiredRightAnalyzer<PropertyClass> propertyClassRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(ComputedFieldClass computedFieldClass)
        throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results =
            new ArrayList<>(this.propertyClassRequiredRightAnalyzer.analyze(computedFieldClass));

        if (computedFieldClass != null && StringUtils.isNotBlank(computedFieldClass.getScript())) {
            results.addAll(
                this.wikiContentAnalyzer.analyzeWikiContent(computedFieldClass.getOwnerDocument().getXClass(),
                    computedFieldClass.getScript(), computedFieldClass.getReference()));
        }

        return results;
    }
}
