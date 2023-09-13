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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(XWikiDocumentRequiredRightAnalyzer.ID)
public class XWikiDocumentRequiredRightAnalyzer implements RequiredRightAnalyzer<XWikiDocument>
{
    /**
     * The id of this component.
     */
    public static final String ID = "document";

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    @Named(DocumentTitleRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<String> documentTitleRequiredRightAnalyzer;

    @Inject
    @Named(XDOMRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    @Named(DefaultObjectRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<BaseObject> objectRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(XWikiDocument document) throws RequiredRightsException
    {
        // Analyze the content
        try {
            return this.documentContextExecutor.call(() ->
            {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();

                this.authorExecutor.call(() -> {
                    // Analyze the title
                    result.addAll(this.documentTitleRequiredRightAnalyzer.analyze(document.getTitle()));

                    // Analyze the content
                    result.addAll(this.xdomRequiredRightAnalyzer.analyze(document.getXDOM()));

                    result.forEach(r -> r.setEntityReference(document.getDocumentReference()));

                    return null;
                }, document.getContentAuthorReference(), document.getDocumentReference());

                // Analyze the objects. Make sure the context author is the object's author.
                this.authorExecutor.call(() -> {
                    for (List<BaseObject> baseObjects : document.getXObjects().values()) {
                        for (BaseObject object : baseObjects) {
                            result.addAll(this.objectRequiredRightAnalyzer.analyze(object));
                        }
                    }

                    return null;
                }, document.getAuthorReference(), document.getDocumentReference());

                return result;
            }, document);
        } catch (Exception e) {
            throw new RequiredRightsException("Error...", e);
        }
    }
}
