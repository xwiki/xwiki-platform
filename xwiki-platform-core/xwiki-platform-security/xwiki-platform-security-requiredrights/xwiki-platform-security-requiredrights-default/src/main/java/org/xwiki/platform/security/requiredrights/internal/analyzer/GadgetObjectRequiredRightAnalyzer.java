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

import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Required right analyzer for instances of XWiki.GadgetClass.
 *
 * @version $Id$
 */
@Component(hints = { "XWiki.GadgetClass" })
@Singleton
public class GadgetObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    private ContentParser contentParser;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        try {
            // Push the document into the context such that we, e.g., get the correct context wiki with the correct
            // wiki macros etc.
            return this.documentContextExecutor.call(() ->
            {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();

                // Analyze the title
                String titleString = object.getStringValue("title");
                if (titleString != null && this.velocityDetector.containsVelocityScript(titleString)) {
                    result.add(new RequiredRightAnalysisResult(
                        object.getReference(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.object.gadget.title"),
                        this.translationMessageSupplierProvider.get(
                            "security.requiredrights.object.gadget.title.description", titleString),
                        List.of(RequiredRight.MAYBE_SCRIPT, RequiredRight.MAYBE_PROGRAM)
                    ));
                }

                // Analyze the content
                String contentString = object.getStringValue("content");
                if (contentString != null) {
                    XDOM parsedContent = this.contentParser.parse(contentString,
                        object.getOwnerDocument().getSyntax(), object.getDocumentReference());
                    parsedContent.getMetaData()
                        .addMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA, object.getReference());
                    result.addAll(this.xdomRequiredRightAnalyzer.analyze(parsedContent));
                }

                return result;
            }, object.getOwnerDocument());
        } catch (Exception e) {
            throw new RequiredRightsException("Error...", e);
        }
    }
}
