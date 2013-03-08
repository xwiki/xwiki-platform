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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.internal.ie.BeforePasteEvent;
import org.xwiki.gwt.dom.client.internal.ie.BeforePasteHandler;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Extends {@link PasteManager} in order to perform operations specific to Internet Explorer.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class IEPasteManager extends PasteManager implements BeforePasteHandler
{
    @Override
    protected List<HandlerRegistration> addHandlers()
    {
        List<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
        registrations.add(getTextArea().addCopyHandler(this));
        // On Internet Explorer the text selection cannot be changed by a paste event handler. We need to catch the
        // BeforePaste event instead. Note that we don't use addDomHandler() because the BeforePaste event is unknown by
        // GWT and we want to prevent a "Trying to sink unknown event type beforepaste" exception.
        registrations.add(getTextArea().addHandler(this, BeforePasteEvent.getType()));
        return registrations;
    }

    @Override
    public void onBeforePaste(BeforePasteEvent event)
    {
        onPaste(null);
    }
}
