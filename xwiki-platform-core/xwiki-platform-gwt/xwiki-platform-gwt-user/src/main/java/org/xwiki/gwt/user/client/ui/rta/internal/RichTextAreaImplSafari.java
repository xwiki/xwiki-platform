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
 * Safari-specific implementation of rich-text editing.
 * 
 * @version $Id$
 */
public class RichTextAreaImplSafari extends com.google.gwt.user.client.ui.impl.RichTextAreaImplSafari
{
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
     * @see com.google.gwt.user.client.ui.impl.RichTextAreaImplSafari#setHTMLImpl(String)
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
        if (!iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::INITIALIZING]) return;
        this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();

        var outer = this;
        iframe.contentWindow.addEventListener('unload', function(event) {
            event.target.defaultView.removeEventListener('unload', arguments.callee, false);
            iframe[@org.xwiki.gwt.user.client.ui.rta.RichTextArea::LOADED] = false;
            // Uninitialize the iframe element only if the event listeners are still attached.
            if (iframe.__gwt_handler) {
                outer.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::uninitElement()();
            }
        }, false);
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
        var wnd = elem.contentWindow;

        // @Override: Check if the selection has been saved before restoring it!
        // NOTE: The save/restore selection methods have been removed in GWT r5639 so we should drop this also after we
        // upgrade to the first stable release that includes the commit.
        // See http://code.google.com/p/google-web-toolkit/source/detail?r=5639
        elem.__gwt_restoreSelection = function() {
            var sel = elem.__gwt_selection;

            // sel.baseNode is null if the selection hasn't been saved.
            // wnd.getSelection is not defined if the iframe isn't attached.
            if (sel.baseNode && wnd.getSelection) {
                wnd.getSelection().setBaseAndExtent(sel.baseNode, sel.baseOffset, sel.extentNode, sel.extentOffset);
            }
        };

        wnd.addEventListener('dblclick', elem.__gwt_handler, true);
        wnd.addEventListener('paste', elem.__gwt_handler, true);
        wnd.addEventListener('copy', elem.__gwt_handler, true);
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
        elem.contentWindow.removeEventListener('dblclick', elem.__gwt_handler, true);
        elem.contentWindow.removeEventListener('paste', elem.__gwt_handler, true);
        elem.contentWindow.removeEventListener('copy', elem.__gwt_handler, true);
    }-*/;

    @Override
    public void setFocus(boolean focused)
    {
        if (focused) {
            // We need to focus the body element (especially if we set contentEditable=true) to initialize the caret
            // (otherwise the caret is hidden before the user clicks on the rich text area).
            IFrameElement.as(getElement()).getContentDocument().getBody().focus();
        }
        super.setFocus(focused);
    }
}
