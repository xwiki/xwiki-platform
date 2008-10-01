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
package com.xpn.xwiki.wysiwyg.client.ui.wrap;

import com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6;

public class WrappedRichTextAreaImplIE6 extends RichTextAreaImplIE6 implements HasStyleSheet
{
    private String styleSheetURL;

    /**
     * {@inheritDoc}
     * 
     * @see HasStyleSheet#getStyleSheetURL()
     */
    public String getStyleSheetURL()
    {
        return styleSheetURL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasStyleSheet#setStyleSheetURL(String)
     */
    public void setStyleSheetURL(String styleSheetURL)
    {
        this.styleSheetURL = styleSheetURL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaImplIE6#initElement()
     */
    public native void initElement() /*-{        
        var _this = this;
        _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;
        window.setTimeout(function() {
            if(_this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing == false) {
                return;
            } 
            var elem = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
            var doc = elem.contentWindow.document;
            var cssURL = _this.@com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextAreaImplIE6::getStyleSheetURL()();
            var globalCssURL = @com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextArea::STYLESHEET;
            doc.write('<html><head><link rel="stylesheet" type="text/css" href="' + cssURL + '" /><link rel="stylesheet" type="text/css" href="' + globalCssURL + '" /></head><body CONTENTEDITABLE="true"></body></html>');

            // Send notification that the iframe has reached design mode.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
        }, 1);
    }-*/;
}
