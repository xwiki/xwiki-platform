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
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Required right analyzer for skin extensions.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component(hints = { "object/XWiki.JavaScriptExtension", "object/XWiki.StyleSheetExtension" })
@Singleton
public class SkinExtensionObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private TranslationMessageSupplierProvider translationMessageSupplierProvider;

    @Inject
    private XObjectDisplayerProvider xObjectDisplayerProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        String use = object.getStringValue("use");
        boolean isAlways = Objects.equals(use, "always");

        Right right;
        String translationKey;
        if (isAlways) {
            right = Right.PROGRAM;
            translationKey = "security.requiredrights.object.skinExtension.always";
        } else {
            right = Right.SCRIPT;
            translationKey = "security.requiredrights.object.skinExtension";
        }

        return List.of(new RequiredRightAnalysisResult(
            object.getReference(),
            this.translationMessageSupplierProvider.get(translationKey),
            this.xObjectDisplayerProvider.get(object),
            List.of(new RequiredRightAnalysisResult.RequiredRight(right, EntityType.DOCUMENT, false))
        ));
    }
}
