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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.slf4j.event.Level.WARN;
import static org.xwiki.model.EntityType.DOCUMENT;
import static org.xwiki.security.authorization.Right.SCRIPT;

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
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Override
    public List<RequiredRightAnalysisResult> analyze(XWikiDocument document) throws RequiredRightsException
    {
        // Analyze the content
        try {
            return this.documentContextExecutor.call(() -> this.authorExecutor.call(() -> {
                List<RequiredRightAnalysisResult> result = new ArrayList<>();
                // Analyze the title
                if (StringUtils.containsAny(document.getTitle(), "#", "$")
                    && !this.contextualAuthorizationManager.hasAccess(SCRIPT))
                {
                    // TODO: introduce a build, with a check that all the mandatory field are 
                    result.add(new RequiredRightAnalysisResult(document.getDocumentReference(), ID,
                        "security.requiredrights.title", List.of(document.getTitle()), SCRIPT, DOCUMENT).setLevel(
                        WARN));
                }

                return result;
            }, document.getContentAuthorReference(), document.getDocumentReference()), document);
        } catch (Exception e) {
            throw new RequiredRightsException("Error...", e);
        }
    }
}
