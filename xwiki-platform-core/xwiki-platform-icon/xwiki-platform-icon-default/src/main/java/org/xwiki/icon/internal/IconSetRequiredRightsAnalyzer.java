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
package org.xwiki.icon.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Required rights analyzer for icon themes.
 *
 * @version $Id$
 * @since 15.10
 */
@Component
@Named("IconThemesCode.IconThemeClass")
@Singleton
public class IconSetRequiredRightsAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object)
    {
        // Icon sets always require script right but scripts might also use programming right.
        // Therefore, report script for the object and script and maybe programming for the content.
        return List.of(
            new RequiredRightAnalysisResult(
                object.getReference(),
                this.translationBlockSupplierProvider.get("icon.requiredrights.object"),
                this.objectBlockSupplierProvider.get(object),
                List.of(RequiredRight.SCRIPT)
            ),
            new RequiredRightAnalysisResult(
                object.getDocumentReference(),
                this.translationBlockSupplierProvider.get("icon.requiredrights.content"),
                this.stringCodeBlockSupplierProvider.get(object.getOwnerDocument().getContent()),
                RequiredRight.SCRIPT_AND_MAYBE_PROGRAM
            )
        );
    }
}
