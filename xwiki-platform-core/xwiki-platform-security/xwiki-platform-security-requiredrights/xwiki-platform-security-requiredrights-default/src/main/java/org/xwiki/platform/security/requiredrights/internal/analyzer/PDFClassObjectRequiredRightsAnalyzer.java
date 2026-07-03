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

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Analyzer for the {@code XWiki.PDFClass} objects.
 *
 * @version $Id$
 * @since 15.10.5
 * @since 16.0.0RC1
 */
@Component
@Singleton
@Named("XWiki.PDFClass")
public class PDFClassObjectRequiredRightsAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results = new ArrayList<>();

        for (String propertyName : object.getPropertyList()) {
            try {
                // Analyze every property as all properties that are used in PDFClass are evaluated as Velocity.
                BaseProperty<?> field = (BaseProperty<?>) object.get(propertyName);
                String value = (String) field.getValue();

                if (this.velocityDetector.containsVelocityScript(value)) {
                    results.add(new RequiredRightAnalysisResult(field.getReference(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.object.pdfClass",
                            propertyName),
                        this.stringCodeBlockSupplierProvider.get(value),
                        RequiredRight.SCRIPT_AND_MAYBE_PROGRAM));
                }
            } catch (XWikiException e) {
                throw new RequiredRightsException(String.format("Failed to get the property [%s] to analyze",
                    propertyName), e);
            }
        }

        return results;
    }
}
