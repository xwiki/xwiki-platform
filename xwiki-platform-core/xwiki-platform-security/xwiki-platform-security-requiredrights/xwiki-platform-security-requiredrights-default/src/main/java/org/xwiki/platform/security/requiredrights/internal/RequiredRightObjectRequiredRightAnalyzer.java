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
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.Right;

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
    private TranslationMessageSupplierProvider translationMessageSupplierProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        String requiredRight = object.getStringValue("level");

        if (StringUtils.isNotBlank(requiredRight)) {
            Right right = Right.toRight(requiredRight);

            if (right != Right.ILLEGAL) {
                return List.of(
                    new RequiredRightAnalysisResult(object.getReference(),
                        this.translationMessageSupplierProvider.get("security.requiredrights.requiredrightobject",
                            requiredRight),
                        () -> null,
                        List.of(new RequiredRightAnalysisResult.RequiredRight(right, EntityType.DOCUMENT, false))
                    ),
                    new RequiredRightAnalysisResult(object.getDocumentReference(),
                        this.translationMessageSupplierProvider.get(
                            "security.requiredrights.requiredrightobjectcontent",
                            requiredRight),
                        () -> null,
                        List.of(new RequiredRightAnalysisResult.RequiredRight(right, EntityType.DOCUMENT, false))
                    )
                );
            }
        }

        return List.of();
    }
}
