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
package com.xpn.xwiki.wysiwyg.client.widget.rta.internal;

import java.util.Map;

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;

/**
 * Custom {com.xpn.xwiki.wysiwyg.client.widget.rta.Reloader} implementation for Firefox 2 and prior.
 * 
 * @version $Id$
 */
public class ReloaderImplMozillaOld extends ReloaderImpl
{
    /**
     * {@inheritDoc}
     * <p>
     * Setting the {@code designMode} document property to {@code false} in Firefox 2 doesn't fully restore the
     * read-only state and one of the side effects is that HTML forms are no longer submitted thus the standard reloader
     * implementation won't work. We need to reload the document twice, first to reset the {@code desingMode} flag and
     * second to submit the parameters.
     * 
     * @see ReloaderImpl#reload(Map, LoadHandler)
     */
    public void reload(final Map<String, String> params, final LoadHandler handler)
    {
        // Save the inner HTML listeners.
        swapInnerHTMLListeners();
        // Reload the rich text area without submitting any parameters, just to reset the design mode.
        reload(new LoadHandler()
        {
            public void onLoad(LoadEvent event)
            {
                // Restore the inner HTML listeners.
                swapInnerHTMLListeners();
                // Now we can submit the parameters since the document is not anymore in design mode.
                ReloaderImplMozillaOld.super.reload(params, handler);
            }
        });
    }

    /**
     * Reloads the underlying rich text area, taking care that only the given load listener is notified.
     * 
     * @param handler the object to be notified when the rich text area is reloaded
     */
    private native void reload(LoadHandler handler)
    /*-{
        var rta = this.@com.xpn.xwiki.wysiwyg.client.widget.rta.internal.ReloaderImpl::getTextArea()();
        var iframe = rta.@com.google.gwt.user.client.ui.UIObject::getElement()();

        // Save the current load handler.
        var loadHandler = iframe.onload;
        // Hook a different load handler that doesn't set the design mode on.
        iframe.onload = function() {
            // Restore the previous load handler.
            iframe.onload = loadHandler;
            // Notify the given load handler.
            handler.@com.google.gwt.event.dom.client.LoadHandler::onLoad(Lcom/google/gwt/event/dom/client/LoadEvent;)(null);
        }

        // Reload the in-line frame.
        // NOTE: contentWindow.location.reload() doesn't work. We have to detach and re-attach the in-line frame!
        var nextSibling = iframe.nextSibling;
        var parentNode = iframe.parentNode;
        parentNode.removeChild(iframe);
        if (nextSibling) {
            parentNode.insertBefore(iframe, nextSibling);
        } else {
            parentNode.appendChild(iframe);
        }
    }-*/;
}
