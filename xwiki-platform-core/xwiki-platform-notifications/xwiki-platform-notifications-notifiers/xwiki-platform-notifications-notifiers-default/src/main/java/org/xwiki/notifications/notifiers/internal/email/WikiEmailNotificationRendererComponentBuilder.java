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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.notifications.notifiers.internal.AbstractWikiNotificationRendererComponentBuilder;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This component allows the definition of a {@link NotificationDisplayer} in wiki pages. It uses
 * {@link org.xwiki.eventstream.UntypedRecordableEvent#getEventType} to be bound to a specific event type.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named(WikiEmailNotificationRendererDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiEmailNotificationRendererComponentBuilder extends AbstractWikiNotificationRendererComponentBuilder
{
    @Inject
    private Provider<WikiEmailNotificationRenderer> rendererProvider;

    @Override
    public EntityReference getClassReference()
    {
        return WikiEmailNotificationRendererDocumentInitializer.CLASS_REFERENCE;
    }

    @Override
    protected WikiComponent instantiateComponent(BaseObject baseObject) throws NotificationException
    {
        WikiEmailNotificationRenderer renderer = this.rendererProvider.get();

        renderer.initialize(baseObject);

        return renderer;
    }
}
