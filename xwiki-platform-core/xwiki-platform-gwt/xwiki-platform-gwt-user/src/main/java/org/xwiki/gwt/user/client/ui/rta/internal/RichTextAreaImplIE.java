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
import com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard;

/**
 * Specific implementation of rich-text editing for newer versions of the Internet Explorer browser (9 and above).
 * 
 * @version $Id$
 */
public class RichTextAreaImplIE extends RichTextAreaImplStandard
{
    /**
     * {@inheritDoc}
     * <p>
     * NOTE: Remove this method as soon as Issue 3156 is fixed. <br>
     * We also need this method to be able to hook simplification of the DOM tree storing meta data in elements.
     * </p>
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3156.
     * </p>
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
            // The window object is the event target.
            event.target.removeEventListener('unload', arguments.callee, false);
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
        elem.contentWindow.addEventListener('dblclick', elem.__gwt_handler, true);
        elem.contentWindow.addEventListener('beforepaste', elem.__gwt_handler, true);
        elem.contentWindow.addEventListener('paste', elem.__gwt_handler, true);
        elem.contentWindow.addEventListener('copy', elem.__gwt_handler, true);
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
        elem.contentWindow.removeEventListener('beforepaste', elem.__gwt_handler, true);
        elem.contentWindow.removeEventListener('paste', elem.__gwt_handler, true);
        elem.contentWindow.removeEventListener('copy', elem.__gwt_handler, true);
    }-*/;
}
