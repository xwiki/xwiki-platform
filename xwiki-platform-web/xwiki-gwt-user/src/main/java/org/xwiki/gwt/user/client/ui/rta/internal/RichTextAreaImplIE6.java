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
package org.xwiki.gwt.user.client.ui.rta.internal;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.IFrameElement;

/**
 * IE6-specific implementation of rich-text editing.
 * 
 * @version $Id$
 */
public class RichTextAreaImplIE6 extends com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6
{
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
        if (elem.getPropertyBoolean(RichTextArea.DIRTY)) {
            elem.setPropertyBoolean(RichTextArea.DIRTY, false);
            ((Element) IFrameElement.as(elem).getContentDocument().getBody().cast()).xSetInnerHTML(html);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#initElement()
     */
    public native void initElement()
    /*-{
        var iframe = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        if (!iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED]
            || iframe.contentWindow.document.body.isContentEditable) return;

        iframe.contentWindow.document.body.contentEditable = true;

        var outer = this;
        iframe.contentWindow.attachEvent('onunload', function() {
            iframe.contentWindow.detachEvent('onunload', arguments.callee);
            iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED] = false;
            outer.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::uninitElement()()
        });

        this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;
        this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
    }-*/;

    /**
     * {@inheritDoc}
     * <p>
     * IE doesn't unload the in-line frame used by the rich text area when we remove it from the DOM document. Its state
     * is preserved when we re-attach it. Naturally, IE doesn't fire the load and unload events in these cases since the
     * in-line frame's document is kept in memory, thus not reloaded when the in-line frame is re-attached.
     * <p>
     * If the rich text area is detached using GWT code then its element is uninitialized even though the unload event
     * wasn't fired. If the unload event wasn't fired then the load event won't be fired next time we re-attach the rich
     * text area. We have to overwrite this method to prevent the rich text area's element from being uninitialized if
     * the unload even wasn't fired.
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#uninitElement()
     */
    public void uninitElement()
    {
        if (!elem.getPropertyBoolean(RichTextArea.LOADED)) {
            super.uninitElement();
        }
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
