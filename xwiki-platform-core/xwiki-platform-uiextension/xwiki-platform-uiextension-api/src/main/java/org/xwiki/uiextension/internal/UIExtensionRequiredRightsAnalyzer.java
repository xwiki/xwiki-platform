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
package org.xwiki.uiextension.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Required rights analyzer for UI extensions.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Named(WikiUIExtensionConstants.CLASS_REFERENCE_STRING)
@Singleton
public class UIExtensionRequiredRightsAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    /**
     * The list of extension points that only consider extensions whose author has wiki admin right.
     */
    private static final Set<String> ADMIN_ONLY_EXTENSION_POINTS = Set.of(
        "org.xwiki.plaftorm.drawer.header",
        "org.xwiki.plaftorm.drawer",
        "org.xwiki.platform.notifications",
        "org.xwiki.plaftorm.menu.content",
        "org.xwiki.plaftorm.editactions",
        "org.xwiki.plaftorm.adminactions",
        "org.xwiki.plaftorm.moreoptions",
        "org.xwiki.platform.template.menu.viewers",
        "org.xwiki.platform.topmenu.left",
        "org.xwiki.platform.topmenu.right"
    );

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringBlockSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @Inject
    private ContentParser parser;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private VelocityDetector velocityDetector;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        WikiComponentScope scope =
            WikiComponentScope.fromString(object.getStringValue(WikiUIExtensionConstants.SCOPE_PROPERTY));

        List<RequiredRightAnalysisResult> result = new ArrayList<>();

        BaseObjectReference reference = object.getReference();

        // Analyze the scope and rights required for the scope.
        if (scope == WikiComponentScope.GLOBAL) {
            result.add(new RequiredRightAnalysisResult(
                reference,
                this.translationBlockSupplierProvider.get("uiextension.requiredrights.global"),
                this.objectBlockSupplierProvider.get(object),
                List.of(RequiredRight.PROGRAM)
            ));
        } else if (scope == WikiComponentScope.WIKI) {
            result.add(new RequiredRightAnalysisResult(
                reference,
                this.translationBlockSupplierProvider.get("uiextension.requiredrights.wiki"),
                this.objectBlockSupplierProvider.get(object),
                List.of(RequiredRight.WIKI_ADMIN)
            ));
        } else {
            // Some UI extension points only consider extensions whose authors have wiki admin right.
            // TODO: Make it possible to extend the list of admin-only extension points.
            String extensionPoint = object.getStringValue(WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY);
            if (StringUtils.isNotBlank(extensionPoint) && ADMIN_ONLY_EXTENSION_POINTS.contains(extensionPoint)) {
                result.add(new RequiredRightAnalysisResult(
                    reference,
                    this.translationBlockSupplierProvider.get("uiextension.requiredrights.adminOnly",
                        extensionPoint),
                    this.objectBlockSupplierProvider.get(object),
                    List.of(RequiredRight.WIKI_ADMIN)
                ));
            }
        }

        // Analyze the rights required for the UI extension parameters.
        String parameters = object.getStringValue(WikiUIExtensionConstants.PARAMETERS_PROPERTY);
        if (this.velocityDetector.containsVelocityScript(parameters)) {
            result.add(new RequiredRightAnalysisResult(
                reference,
                this.translationBlockSupplierProvider.get("uiextension.requiredrights.parameters"),
                this.stringBlockSupplierProvider.get(parameters),
                RequiredRight.SCRIPT_AND_MAYBE_PROGRAM
            ));
        }

        // Analyze the rights required for the UI extension content.
        String content = object.getStringValue(WikiUIExtensionConstants.CONTENT_PROPERTY);
        try {
            if (StringUtils.isNotBlank(content)) {
                XWikiDocument ownerDocument = object.getOwnerDocument();
                XDOM xdom = this.parser.parse(content, ownerDocument.getSyntax(), object.getDocumentReference());
                xdom.getMetaData().addMetaData("entityReference", reference);
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(xdom));
            }
        } catch (Exception e) {
            result.add(new RequiredRightAnalysisResult(
                reference,
                this.translationBlockSupplierProvider.get("uiextension.requiredrights.contentError",
                    ExceptionUtils.getRootCauseMessage(e)),
                this.stringBlockSupplierProvider.get(content),
                List.of(RequiredRight.MAYBE_PROGRAM)
            ));
        }

        return result;
    }
}
