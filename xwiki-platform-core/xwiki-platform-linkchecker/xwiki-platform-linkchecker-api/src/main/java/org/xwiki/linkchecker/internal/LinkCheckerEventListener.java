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
package org.xwiki.linkchecker.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.transformation.linkchecker.LinkState;
import org.xwiki.rendering.transformation.linkchecker.LinkStateManager;

/**
 * Listen to Document updates and removes all Link States for the Document being modified.
 *
 * @version $Id$
 * @since 3.3M2
 */
@Component
@Named("linkchecker")
@Singleton
public class LinkCheckerEventListener implements EventListener
{
    /**
     * The Reference serializer to serialize the reference of the Document which is being modified in order to
     * compare it with the link state information.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to access the link states.
     */
    @Inject
    private LinkStateManager linkStateManager;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new DocumentUpdatingEvent());
    }

    @Override
    public String getName()
    {
        return "LinkChecker";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentModelBridge doc = (DocumentModelBridge) source;
        final String reference = this.serializer.serialize(doc.getDocumentReference());

        Map<String, Map<String, LinkState>> states = this.linkStateManager.getLinkStates();
        for (Map.Entry<String, Map<String, LinkState>> entry : states.entrySet()) {
            Map<String, LinkState> state = entry.getValue();
            state.remove(reference);
            if (state.size() == 0) {
                states.remove(entry.getKey());
            }
        }
    }
}
