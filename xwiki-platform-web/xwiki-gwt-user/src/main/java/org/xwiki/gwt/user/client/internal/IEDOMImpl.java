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
package org.xwiki.gwt.user.client.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.impl.DOMImplTrident;

/**
 * Fixes some bugs and adds some improvements to the {@link com.google.gwt.user.client.DOM} implementation for IE
 * browsers.
 * <p>
 * NOTE: In the current GWT version (1.7.0) there are two empty classes, DOMImplIE6 and DOMImplIE8, which extend
 * {@link DOMImplTrident}. We tried to extend them but unfortunately DOMImplIE6 has package-only access level..
 * 
 * @version $Id$
 */
public class IEDOMImpl extends DOMImplTrident
{
    /**
     * The JavaScript function used to dispatch DOM events to GWT widgets.
     */
    @SuppressWarnings("unused")
    private static JavaScriptObject dispatchEvent;

    /**
     * {@inheritDoc}
     * 
     * @see DOMImplTrident#maybeInitializeEventSystem()
     */
    public void maybeInitializeEventSystem()
    {
        boolean wasInitialized = eventSystemIsInitialized;
        super.maybeInitializeEventSystem();
        if (!wasInitialized && eventSystemIsInitialized) {
            initCustomEventSystem();
        }
    }

    /**
     * Customizes the initialization of the event dispatch system.
     */
    private native void initCustomEventSystem()
    /*-{
        @org.xwiki.gwt.user.client.internal.IEDOMImpl::dispatchEvent = function() {
            // callDispatchEvent from base class expects "this" to be the object that fired the event. It is like this
            // when callDispatchEvent is registered using the corresponding event property but we use attachEvent
            // instead.
            @com.google.gwt.user.client.impl.DOMImplTrident::callDispatchEvent.call($wnd.event.srcElement);
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see DOMImplTrident#sinkEvents(Element, int)
     */
    public void sinkEvents(Element elem, int bits)
    {
        int changedBits = elem.getPropertyInt("__eventBits") ^ bits;
        super.sinkEvents(elem, bits);
        sinkCustomEvents(elem, bits, changedBits);
    }

    /**
     * Sinks custom events that are not covered by {@link DOMImplTrident#sinkEvents(Element, int)} or events that are
     * badly hooked in the base class.
     * 
     * @param elem the element whose events are to be retrieved
     * @param bits a bit field describing the events this element should listen to
     * @param chMask a bit field describing the events that change their "sink" state relative to this element
     */
    private native void sinkCustomEvents(Element elem, int bits, int chMask)
    /*-{
        if (!chMask) return;

        // The only way to catch the load event from a in-line frame that has been inserted through JavaScript into the
        // page is by using the attachEvent method.
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=1720
        // If the load bit has changed and the element is an in-line frame..
        if ((chMask & 0x08000) && elem.tagName.toLowerCase() == 'iframe') {
            // If the load bit is set..
            if (bits & 0x08000) {
                elem.attachEvent('onload', @org.xwiki.gwt.user.client.internal.IEDOMImpl::dispatchEvent);
            } else {
                elem.detachEvent('onload', @org.xwiki.gwt.user.client.internal.IEDOMImpl::dispatchEvent);
            }
            // Make sure the handler is not called twice (just in case IE developers fix this issue).
            elem.onload = null;
        }
    }-*/;
}
