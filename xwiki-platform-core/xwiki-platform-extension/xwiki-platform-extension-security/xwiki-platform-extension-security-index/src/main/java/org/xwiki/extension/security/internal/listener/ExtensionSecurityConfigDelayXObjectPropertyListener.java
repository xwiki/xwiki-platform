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
package org.xwiki.extension.security.internal.listener;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.internal.ExtensionSecurityScheduler;
import org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration;
import org.xwiki.extension.security.internal.configuration.DocConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

import static com.xpn.xwiki.XWiki.DEFAULT_MAIN_WIKI;
import static org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration.SCAN_DELAY;
import static org.xwiki.extension.security.internal.configuration.DocConfigurationSource.XCLASS_REFERENCE;
import static org.xwiki.extension.security.internal.configuration.DocConfigurationSource.XOBJECT_REFERENCE;

/**
 * Listen for a change in the {@link DefaultExtensionSecurityConfiguration#SCAN_DELAY} property of the
 * {@link DocConfigurationSource#XCLASS_REFERENCE} XClass in the {DocConfigurationSource#XOBJECT_REFERENCE} XObject.
 * Restarts the {@link ExtensionSecurityScheduler} when the delay is changed.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Named(ExtensionSecurityConfigDelayXObjectPropertyListener.NAME)
@Singleton
public class ExtensionSecurityConfigDelayXObjectPropertyListener extends AbstractEventListener

{
    /**
     * The name of the event listener (and its component hint).
     */
    public static final String NAME = "ExtensionSecurityConfigDelayXObjectPropertyListener";

    private static final BaseObjectReference OBJECT_REFERENCE = new BaseObjectReference(XCLASS_REFERENCE, 0,
        new DocumentReference(XOBJECT_REFERENCE, new WikiReference(DEFAULT_MAIN_WIKI)));

    private static final ObjectPropertyReference OBJECT_PROPERTY_REFERENCE =
        new ObjectPropertyReference(SCAN_DELAY, OBJECT_REFERENCE);

    @Inject
    private Provider<ExtensionSecurityScheduler> schedulerProvider;

    /**
     * Default constructor.
     */
    public ExtensionSecurityConfigDelayXObjectPropertyListener()
    {
        super(NAME, List.of(
            new XObjectPropertyAddedEvent(OBJECT_PROPERTY_REFERENCE),
            new XObjectPropertyUpdatedEvent(OBJECT_PROPERTY_REFERENCE),
            new XObjectPropertyDeletedEvent(OBJECT_PROPERTY_REFERENCE)
        ));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.schedulerProvider.get().restart();
    }
}
