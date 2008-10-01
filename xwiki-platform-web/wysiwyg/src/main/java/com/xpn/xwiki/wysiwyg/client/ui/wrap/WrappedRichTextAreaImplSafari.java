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

import com.google.gwt.user.client.ui.impl.RichTextAreaImplOpera;
import com.google.gwt.user.client.ui.impl.RichTextAreaImplSafari;

public class WrappedRichTextAreaImplSafari extends RichTextAreaImplSafari implements HasStyleSheet
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
     * @see RichTextAreaImplOpera#initElement()
     */
    public native void initElement()  /*-{
        // Most browsers don't like setting designMode until slightly _after_
        // the iframe becomes attached to the DOM. Any non-zero timeout will do
        // just fine.
        var _this = this;
        _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;
        setTimeout(function() {
            // Turn on design mode.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document.designMode = 'On';

            var idoc = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.contentWindow.document;
            var head = idoc.getElementsByTagName('head')[0];

            // Add stylesheet declaration
            var link = idoc.createElement('link');
            link.setAttribute('rel', 'stylesheet');
            link.setAttribute('href', _this.@com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextAreaImplSafari::getStyleSheetURL()());
            link.setAttribute('type', 'text/css');
            head.appendChild(link);

            // Add global stylesheet declaration
            link = link.cloneNode(false);
            link.setAttribute('href', @com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextArea::STYLESHEET);
            head.appendChild(link); 

            // Send notification that the iframe has reached design mode.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();
        }, 1);
    }-*/;
}
