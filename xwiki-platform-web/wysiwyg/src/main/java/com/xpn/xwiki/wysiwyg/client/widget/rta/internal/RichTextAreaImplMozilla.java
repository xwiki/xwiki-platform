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
 * Mozilla-specific implementation of rich-text editing.
 * 
 * @version $Id$
 */
public class RichTextAreaImplMozilla extends com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla implements
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
     * NOTE: Remove this method as soon as Issue 3156 is fixed. <br />
     * We also need this method to be able to hook simplification of the DOM tree storing meta data in elements.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla#setHTMLImpl(String)
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
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla#getHTMLImpl()
     */
    protected String getHTMLImpl()
    {
        return ((Element) IFrameElement.as(elem).getContentDocument().getBody().cast()).xGetInnerHTML();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla#initElement()
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3176
     */
    public native void initElement()
    /*-{
        // Mozilla doesn't allow designMode to be set reliably until the iframe is fully loaded.
        var _this = this;
        var iframe = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;

        var loaded = focused = false;

        var enterDesignMode = function() {
            iframe.contentWindow.onfocus = null;
            iframe.contentWindow.onblur = null;
            iframe.contentWindow.document.designMode = 'on';
            // It seems that the following line of code fixes the Midas bug which prevents the user 
            // to delete any HTML inserted through DOM API before any printable key has been pressed.
            iframe.contentWindow.document.execCommand('undo', false, null);
        };

        iframe.onload = function() {
            // Some Mozillae have the nasty habit of calling onload again when you set
            // designMode, so let's avoid doing it more than once.
            iframe.onload = null;

            if (focused) {
                enterDesignMode();
            } else {
                loaded = true;
            }

            // Send notification that the iframe has finished loading.
            // NOTE: The iframe didn't reached design mode unless it was focused.
            // If your code relies on design mode you have to focus the iframe before.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
        };

        // Don't set designMode until the RTA actually gets focused. This is
        // necessary because editing won't work on Mozilla if the iframe is
        // *hidden, but attached*. Waiting for focus gets around this issue.
        //
        // Note: This onfocus will not conflict with the addEventListener('focus', ...) in RichTextAreaImplStandard.
        // Note: The iframe should be attached when the following code is executed!
        iframe.contentWindow.onfocus = function() {
            if (loaded) {
                enterDesignMode();
            } else {
                focused = true;
            }
        };

        iframe.contentWindow.onblur = function() {
            focused = false;
        };
    }-*/;

    /**
     * {@inheritDoc}<br/>
     * NOTE: Remove this method as soon as GWT provides a way to detect that a rich text area has finished loading.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla#onElementInitialized()
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
}
