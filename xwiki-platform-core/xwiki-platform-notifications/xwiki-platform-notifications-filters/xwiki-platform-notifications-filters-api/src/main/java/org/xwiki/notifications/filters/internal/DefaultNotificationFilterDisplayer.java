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
package org.xwiki.notifications.filters.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * This is the default implementation of a {@link NotificationFilterDisplayer}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultNotificationFilterDisplayer extends AbstractNotificationFilterDisplayer
{
    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block display(NotificationFilter filter, NotificationFilterPreference preference)
        throws NotificationException
    {
        Map<String, Object> backup = setUpContext(this.scriptContextManager, filter, preference);

        try {
            // Try to get a template using the filter name; if no template is found, fallback on the default one.
            String templateName = String.format("notification/filters/%s.vm", filter.getName().replaceAll("\\/", "."));
            Template template = templateManager.getTemplate(templateName);

            return (template != null) ? templateManager.execute(template)
                : templateManager.execute("notification/filters/default.vm");
        } catch (Exception e) {
            throw new NotificationException(
                String.format("Failed to display the notification filter [%s] with the filter preference [%s].", filter,
                    preference),
                e);
        } finally {
            cleanUpContext(this.scriptContextManager, backup);
        }
    }

    @Override
    public Set<String> getSupportedFilters()
    {
        return Collections.emptySet();
    }
}
