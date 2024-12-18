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
package org.xwiki.notifications.notifiers.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link RequiredRightAnalyzer} for wiki notification displayers.
 *
 * @version $Id$
 */
@Component
@Named(WikiNotificationDisplayerDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiNotificationDisplayerRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> baseObjectBlockSupplierProvider;

    @Inject
    private VelocityDetector velocityDetector;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object)
    {
        List<RequiredRightAnalysisResult> result = new ArrayList<>();

        if (object != null) {
            String template =
                object.getStringValue(WikiNotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE);
            if (this.velocityDetector.containsVelocityScript(template)) {
                result.add(new RequiredRightAnalysisResult(object.getReference(),
                    this.translationMessageSupplierProvider.get(
                        "notifications.notifiers.wikiNotificationDisplayerRequiredRightWithScript"),
                    this.baseObjectBlockSupplierProvider.get(object),
                    List.of(RequiredRight.WIKI_ADMIN, RequiredRight.MAYBE_PROGRAM)));
            } else {
                result.add(new RequiredRightAnalysisResult(object.getReference(),
                    this.translationMessageSupplierProvider.get(
                        "notifications.notifiers.wikiNotificationDisplayerRequiredRight"),
                    this.baseObjectBlockSupplierProvider.get(object),
                    List.of(RequiredRight.WIKI_ADMIN)));
            }
        }

        return result;
    }
}
