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
package org.xwiki.security.authservice.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.EntityEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Invalidate the configuration cache when the configuration is modified.
 * 
 * @version $Id$
 * @since 15.3RC1
 */
@Component
@Named(AuthServiceConfigurationInvalidator.NAME)
@Singleton
public class AuthServiceConfigurationInvalidator extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "AuthenticationServiceConfigurationInvalidator";

    private static final EntityReference PROPERTY_REFERENCE_MATCHER = new EntityReference(
        AuthServiceConfigurationClassInitializer.FIELD_SERVICE, EntityType.OBJECT_PROPERTY,
        BaseObjectReference.any(AuthServiceConfigurationClassInitializer.CLASS_REFERENCE_STRING));

    @Inject
    private AuthServiceConfiguration configuration;

    /**
     * Default constructor.
     */
    public AuthServiceConfigurationInvalidator()
    {
        super(NAME, new WikiDeletedEvent(), new XObjectPropertyAddedEvent(PROPERTY_REFERENCE_MATCHER),
            new XObjectPropertyUpdatedEvent(PROPERTY_REFERENCE_MATCHER),
            new XObjectPropertyDeletedEvent(PROPERTY_REFERENCE_MATCHER));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            this.configuration.invalidate(((WikiDeletedEvent) event).getWikiId());
        } else {
            this.configuration
                .invalidate(((EntityEvent) event).getReference().extractReference(EntityType.WIKI).getName());
        }
    }
}
