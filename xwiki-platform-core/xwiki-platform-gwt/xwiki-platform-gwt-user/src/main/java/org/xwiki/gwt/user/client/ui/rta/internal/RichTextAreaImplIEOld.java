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

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.IFrameElement;

/**
 * Specific implementation of rich-text editing for older versions of the Internet Explorer browser (6, 7 and 8).
 * 
 * @version $Id$
 */
public class RichTextAreaImplIEOld extends com.google.gwt.user.client.ui.impl.RichTextAreaImplIE8toIE10
{
    /**
     * Flag indicating if the load event needs to be fired manually when the rich text area is attached to the document.
     */
    private static final String FIRE_LOAD_EVENT_MANUALLY = "__fireLoadEventManually";

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: Remove this method as soon as Issue 3147 is fixed. <br>
     * We also need this method to be able to hook simplification of the DOM tree storing meta data in elements.
     * </p>
     * <ul>
     * <li>http://code.google.com/p/google-web-toolkit/issues/detail?id=3147</li>
     * <li>http://code.google.com/p/google-web-toolkit/issues/detail?id=3156</li>
     * </ul>
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#setHTMLImpl(String)
     */
    @Override
    protected void setHTMLImpl(String html)
    {
        if (elem.getPropertyBoolean(RichTextArea.DIRTY)) {
            elem.setPropertyBoolean(RichTextArea.DIRTY, false);
            ((Element) IFrameElement.as(elem).getContentDocument().getBody().cast()).xSetInnerHTML(html);
        }
    }

    @Override
    public native void initElement()
    /*-{
        var iframe = this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        if (!iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED]
            || iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::INITIALIZING]) {
            // We need to signal that the element is initializing even when the rich text area is not fully loaded for
            // the case when the rich text area widget is quickly attached and detached.
            this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitializing()();
        }
        if (!iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::INITIALIZING]) {
            if (iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED]
                && iframe[@org.xwiki.gwt.user.client.ui.rta.internal.RichTextAreaImplIEOld::FIRE_LOAD_EVENT_MANUALLY]) {
                // See #uninitElement() for the explanation.
                iframe.fireEvent('onload');
            }
            return;
        }
        this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();

        var outer = this;
        iframe.contentWindow.attachEvent('onunload', function() {
            iframe.contentWindow.detachEvent('onunload', arguments.callee);
            iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED] = false;
            outer.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::uninitElement()();
        });
    }-*/;

    @Override
    protected void setEnabledImpl(boolean enabled)
    {
        if (enabled != isEnabledImpl()) {
            ((Document) IFrameElement.as(elem).getContentDocument()).setDesignMode(enabled);
        }
    }

    @Override
    protected boolean isEnabledImpl()
    {
        return ((Document) IFrameElement.as(elem).getContentDocument()).isDesignMode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * IE doesn't unload the in-line frame used by the rich text area when we remove it from the DOM document. Its state
     * is preserved when we re-attach it. Naturally, IE doesn't fire the load and unload events in these cases since the
     * in-line frame's document is kept in memory, thus not reloaded when the in-line frame is re-attached.
     * </p>
     * <p>
     * If the rich text area is detached using GWT code then its element is uninitialized even though the unload event
     * wasn't fired. If the unload event wasn't fired then the load event won't be fired next time we re-attach the rich
     * text area. We have to overwrite this method to prevent the rich text area's element from being uninitialized if
     * the unload even wasn't fired.
     * </p>
     * 
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6#uninitElement()
     */
    @Override
    public void uninitElement()
    {
        if (!elem.getPropertyBoolean(RichTextArea.LOADED)) {
            super.uninitElement();
        } else {
            // Remember to manually fire a load event next time the rich text area is re-attached because IE doesn't do
            // it and the load event listeners must to be notified (e.g. when the rich text area is on a dialog box).
            elem.setPropertyBoolean(FIRE_LOAD_EVENT_MANUALLY, true);
        }
    }

    @Override
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
        body.ondblclick =
        body.onbeforepaste =
        body.onpaste =
        body.oncopy = body.onclick;

        if (body.parentNode.nodeName.toLowerCase() == 'html') {
          // In standards mode the body element doesn't take all the available space so the user can click on the HTML
          // element. When this happens the text selection is lost. To prevent this we listen to mouse up event and
          // focus the body element.
          body.parentNode.onmouseup = body.onclick;
          // Listen to the mouse up event just once.
          body.onmouseup = null;
        }
    }-*/;

    @Override
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
        var body = elem.contentWindow.document.body;
        body.ondblclick =
        body.onbeforepaste =
        body.onpaste =
        body.oncopy =
        body.parentNode.onmouseup = null;
    }-*/;
}
