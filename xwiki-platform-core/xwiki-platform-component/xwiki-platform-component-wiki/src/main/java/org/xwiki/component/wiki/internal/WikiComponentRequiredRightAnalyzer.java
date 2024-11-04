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
package org.xwiki.component.wiki.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;

import com.xpn.xwiki.objects.BaseObject;

/**
 * A {@link RequiredRightAnalyzer} for wiki component XObjects.
 *
 * @since 15.10.12
 * @since 16.4.3
 * @since 16.8.0RC1
 * @version $Id$
 */
@Component
@Singleton
@Named("XWiki.ComponentClass")
public class WikiComponentRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        return List.of(new RequiredRightAnalysisResult(object.getReference(),
            this.translationMessageSupplierProvider.get("platform.component.wiki.programmingRightRequiredMessage"),
            this.objectBlockSupplierProvider.get(object),
            List.of(RequiredRight.PROGRAM)));
    }
}
