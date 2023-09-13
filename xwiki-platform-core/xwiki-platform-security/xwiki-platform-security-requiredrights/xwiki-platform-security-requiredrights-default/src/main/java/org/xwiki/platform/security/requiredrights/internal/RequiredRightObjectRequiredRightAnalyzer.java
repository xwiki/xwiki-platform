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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Analyzer that checks if the document has the rights indicated in the RequiredRights object.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(RequiredRightObjectRequiredRightAnalyzer.ID)
public class RequiredRightObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    /**
     * The id of this analyzer.
     */
    public static final String ID = "object/XWiki.RequiredRightClass";

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        String requiredRight = object.getStringValue("level");

        if (StringUtils.isNotBlank(requiredRight)) {
            Right right = Right.toRight(requiredRight);
            XWikiDocument document = object.getOwnerDocument();
            DocumentReference documentReference = document.getDocumentReference();

            // Check the right both for the content and the effective metadata author.
            if (right != Right.ILLEGAL && !(
                this.authorizationManager.hasAccess(right, document.getContentAuthorReference(), documentReference)
                    && this.authorizationManager.hasAccess(right, document.getAuthorReference(), documentReference))
            )
            {
                return List.of(new RequiredRightAnalysisResult(object.getDocumentReference(), ID,
                    "security.requiredrights.requiredrightobject", List.of(requiredRight), right, EntityType.DOCUMENT));
            }
        }

        return List.of();
    }
}
