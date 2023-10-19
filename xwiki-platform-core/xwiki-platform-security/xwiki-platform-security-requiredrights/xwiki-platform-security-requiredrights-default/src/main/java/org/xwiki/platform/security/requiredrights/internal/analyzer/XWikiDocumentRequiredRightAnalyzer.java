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
import org.xwiki.model.EntityType;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.platform.security.requiredrights.internal.VelocityUtil;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
public class XWikiDocumentRequiredRightAnalyzer implements RequiredRightAnalyzer<XWikiDocument>
{
    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private RequiredRightAnalyzer<BaseObject> objectRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(XWikiDocument document) throws RequiredRightsException
    {
        // Analyze the content
        try {
            // Push the document into the context such that we, e.g., get the correct context wiki with the correct
            // wiki macros etc.
            return this.documentContextExecutor.call(() ->
            {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();

                // Analyze the title
                if (VelocityUtil.containsVelocityScript(document.getTitle())) {
                    result.add(new RequiredRightAnalysisResult(
                        document.getDocumentReferenceWithLocale(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title"),
                        this.translationMessageSupplierProvider.get("security.requiredrights.title.description",
                            document.getTitle()),
                        List.of(
                            new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, true),
                            new RequiredRight(Right.PROGRAM, EntityType.DOCUMENT, true)
                        )
                    ));
                }

                // Analyze the content
                result.addAll(this.xdomRequiredRightAnalyzer.analyze(document.getXDOM()));

                for (List<BaseObject> baseObjects : document.getXObjects().values()) {
                    for (BaseObject object : baseObjects) {
                        result.addAll(this.objectRequiredRightAnalyzer.analyze(object));
                    }
                }

                return result;
            }, document);
        } catch (Exception e) {
            throw new RequiredRightsException("Error...", e);
        }
    }
}
