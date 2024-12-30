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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.ObjectPropertyRequiredRightAnalyzer;

import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link RequiredRightAnalyzer} for {@code XWiki.Notifications.Code.NotificationEmailRendererClass} objects.
 *
 * @version $Id$
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 */
@Component
@Named(WikiEmailNotificationRendererDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiEmailNotificationRendererRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        return this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(object,
            RequiredRight.WIKI_ADMIN, "notifications.notifiers.emailNotificationRendererRequiredRights");
    }
}
