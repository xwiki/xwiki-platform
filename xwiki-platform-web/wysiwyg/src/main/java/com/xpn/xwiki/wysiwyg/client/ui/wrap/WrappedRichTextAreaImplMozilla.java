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

import com.google.gwt.user.client.ui.impl.RichTextAreaImplMozilla;

public class WrappedRichTextAreaImplMozilla extends RichTextAreaImplMozilla implements HasStyleSheet
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
     * @see RichTextAreaImplMozilla#initElement()
     */
    public native void initElement() /*-{ 
        // Mozilla doesn't allow designMode to be set reliably until the iframe is
        // fully loaded.
        var _this = this;
        var iframe = _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem;
        _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::initializing = true;

        iframe.onload = function() {
            // Some Mozillae have the nasty habit of calling onload again when you set
            // designMode, so let's avoid doing it more than once.
            iframe.onload = null;
            
            // Add stylesheet declaration
            var idoc = iframe.contentWindow.document;
            var head = idoc.getElementsByTagName('head')[0];
            var link = idoc.createElement('link');
            link.setAttribute('rel', 'stylesheet');
            link.setAttribute('href', _this.@com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextAreaImplMozilla::getStyleSheetURL()());
            link.setAttribute('type', 'text/css');
            head.appendChild(link);

            // Send notification that the iframe has finished loading.
            _this.@com.google.gwt.user.client.ui.impl.RichTextAreaImplStandard::onElementInitialized()();

            // Don't set designMode until the RTA actually gets focused. This is
            // necessary because editing won't work on Mozilla if the iframe is
            // *hidden, but attached*. Waiting for focus gets around this issue.
            //
            // Note: This onfocus will not conflict with the addEventListener('focus',
            // ...) // in RichTextAreaImplStandard.
            iframe.contentWindow.onfocus = function() {
                iframe.contentWindow.onfocus = null;
                iframe.contentWindow.document.designMode = 'On';
            };
        };
    }-*/;
}
