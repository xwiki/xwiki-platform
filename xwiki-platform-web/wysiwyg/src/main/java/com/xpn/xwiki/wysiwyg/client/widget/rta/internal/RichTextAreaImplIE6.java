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

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * IE6-specific implementation of rich-text editing.
 * 
 * @version $Id$
 */
public class RichTextAreaImplIE6 extends com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6 implements
    HasLoadHandlers
{
    /**
     * The handler manager used to fire load events. We need it because this class is not a widget and we don't have
     * access to the rich text area from its implementation.<br/>
     * NOTE: Stop firing load events as soon as GWT provides a way to detect that a rich text area has finished loading.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3059
     */
    private final HandlerManager handlerManager = new HandlerManager(this);

    /**
     * {@inheritDoc}<br/>
     * NOTE: Remove this method as soon as Issue 3147 is fixed. <br />
     * We also need this method to be able to hook simplification of the DOM tree storing meta data in elements.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#setHTMLImpl(String)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3156
     */
    protected void setHTMLImpl(String html)
    {
        if (String.valueOf(true).equals(elem.getAttribute(RichTextArea.DIRTY))) {
            elem.removeAttribute(RichTextArea.DIRTY);
            ((Element) IFrameElement.as(elem).getContentDocument().getBody().cast()).xSetInnerHTML(html);
        }
    }

    /**
     * {@inheritDoc} <br />
     * NOTE: We need this method to be able to hook simplification of the DOM tree storing meta data in elements.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#getHTMLImpl()
     */
    protected String getHTMLImpl()
    {
        return ((Element) IFrameElement.as(elem).getContentDocument().getBody().cast()).xGetInnerHTML();
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Remove this method as soon as GWT provides a way to detect that a rich text area has finished loading.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#onElementInitialized()
     */
    protected void onElementInitialized()
    {
        super.onElementInitialized();
        // We fire a fake load event to notify that the rich text area has finished loading.
        DomEvent.fireNativeEvent(IFrameElement.as(elem).getContentDocument().createLoadEvent(), this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLoadHandlers#addLoadHandler(LoadHandler)
     */
    public HandlerRegistration addLoadHandler(LoadHandler handler)
    {
        return handlerManager.addHandler(LoadEvent.getType(), handler);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLoadHandlers#fireEvent(GwtEvent)
     */
    public void fireEvent(GwtEvent< ? > event)
    {
        handlerManager.fireEvent(event);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#hookEvents()
     */
    protected void hookEvents()
    {
        // JSNI doesn't support super.*
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=3507
        super.hookEvents();
        // Double click event is not caught by default.
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=3944
        hookCustomEvents();
    }

    /**
     * Hooks custom events.
     */
    protected native void hookCustomEvents()
    /*-{
        var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        var body = elem.contentWindow.document.body;
        body.ondblclick = body.onclick;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#unhookEvents()
     */
    protected void unhookEvents()
    {
        // Double click event is not caught by default.
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=3944
        unhookCustomEvents();
        // JSNI doesn't support super.*
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=3507
        super.unhookEvents();
    }

    /**
     * Unhooks custom events.
     */
    protected native void unhookCustomEvents()
    /*-{
        var elem = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        elem.contentWindow.document.body.ondblclick = null;
    }-*/;
}
