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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;

import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Helper component to analyze a wiki content property of an XClass.
 *
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 * @version $Id$
 */
@Component(roles = XClassWikiContentAnalyzer.class)
@Singleton
public class XClassWikiContentAnalyzer
{
    @Inject
    private ContentParser contentParser;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    /**
     * Analyze the wiki content in a property of an XClass.
     *
     * @param xClass the XClass whose property contains the provided wiki content
     * @param script the wiki content to analyze
     * @param reference the reference of the property containing the wiki content
     * @return the analysis result
     */
    public List<RequiredRightAnalysisResult> analyzeWikiContent(BaseClass xClass, String script,
        ClassPropertyReference reference)
    {
        try {
            XDOM scriptXDOM = this.contentParser.parse(script, xClass.getOwnerDocument().getSyntax(),
                xClass.getDocumentReference());
            scriptXDOM.getMetaData().addMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA, reference);
            return this.xdomRequiredRightAnalyzer.analyze(scriptXDOM);
        } catch (Exception e) {
            return List.of(new RequiredRightAnalysisResult(reference,
                this.translationMessageSupplierProvider.get("security.requiredrights.class.errorAnalyzingWiki",
                    ExceptionUtils.getRootCauseMessage(e)),
                this.stringCodeBlockSupplierProvider.get(script),
                List.of(RequiredRight.MAYBE_PROGRAM)));
        }
    }

}
