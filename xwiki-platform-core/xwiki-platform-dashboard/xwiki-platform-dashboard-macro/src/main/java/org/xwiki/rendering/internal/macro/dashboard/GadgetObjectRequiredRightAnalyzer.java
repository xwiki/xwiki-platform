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
package org.xwiki.rendering.internal.macro.dashboard;

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
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Required right analyzer for instances of XWiki.GadgetClass.
 *
 * @version $Id$
 * @since 15.10
 */
@Component
@Singleton
@Named("XWiki.GadgetClass")
public class GadgetObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    private ContentParser contentParser;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result = new ArrayList<>();

        // Analyze the title
        String titleString = object.getStringValue("title");
        if (titleString != null && this.velocityDetector.containsVelocityScript(titleString)) {
            result.add(new RequiredRightAnalysisResult(
                object.getReference(),
                this.translationMessageSupplierProvider.get("dashboard.requiredrights.gadget.title"),
                this.translationMessageSupplierProvider.get(
                    "dashboard.requiredrights.gadget.title.description", titleString),
                List.of(RequiredRight.MAYBE_SCRIPT, RequiredRight.MAYBE_PROGRAM)
            ));
        }

        // Analyze the content
        String contentString = object.getStringValue("content");
        if (contentString != null) {
            try {
                XDOM parsedContent = this.contentParser.parse(contentString,
                    object.getOwnerDocument().getSyntax(), object.getDocumentReference());
                parsedContent.getMetaData().addMetaData("entityReference", object.getReference());
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(parsedContent));
            } catch (MissingParserException | ParseException e) {
                throw new RequiredRightsException("Failed to parse value of 'content' property.", e);
            }
        }

        return result;
    }
}
