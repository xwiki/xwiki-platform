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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Required right analyzer that specifically only analyzes the content and not XObjects or XClass of the document.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Singleton
@Named("content")
public class XWikiDocumentContentRequiredRightAnalyzer implements RequiredRightAnalyzer<XWikiDocument>
{
    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private VelocityDetector velocityDetector;

    @Override
    public List<RequiredRightAnalysisResult> analyze(XWikiDocument document) throws RequiredRightsException
    {
        try {
            // Push the document into the context such that we, e.g., get the correct context wiki with the correct
            // wiki macros etc.
            return this.documentContextExecutor.call(() ->
            {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();

                // Analyze the title
                if (this.velocityDetector.containsVelocityScript(document.getTitle())) {
                    result.add(new RequiredRightAnalysisResult(
                        document.getDocumentReferenceWithLocale(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title"),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title.description",
                            document.getTitle()),
                        List.of(RequiredRight.MAYBE_SCRIPT, RequiredRight.MAYBE_PROGRAM)
                    ));
                }

                // Analyze the content
                XDOM xdom = document.getXDOM();
                // Store the document reference with locale so it can correctly be reported by the macro analyzers.
                if (xdom != null && xdom.getMetaData() != null) {
                    xdom.getMetaData().addMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA,
                        document.getDocumentReferenceWithLocale());
                }
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(xdom));

                return result;
            }, document);
        } catch (Exception e) {
            throw new RequiredRightsException("Error analyzing document title and content.", e);
        }
    }
}
