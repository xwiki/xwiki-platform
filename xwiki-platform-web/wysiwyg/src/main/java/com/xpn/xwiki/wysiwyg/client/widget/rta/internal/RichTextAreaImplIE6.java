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
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.LoadListenerCollection;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * IE6-specific implementation of rich-text editing.
 * 
 * @version $Id$
 */
public class RichTextAreaImplIE6 extends com.google.gwt.user.client.ui.impl.RichTextAreaImplIE6 implements
    SourcesLoadEvents
{
    /**
     * The collection of load listeners.<br/>
     * NOTE: Stop firing load events as soon as GWT provides a way to detect that a rich text area has finished loading.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3059
     */
    private final LoadListenerCollection loadListeners = new LoadListenerCollection();

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
        // This is a workaround to be able to detect when the rich text area has finished loading. The sender is
        // <code>null</code> because we don't have access to a widget.
        loadListeners.fireLoad(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesLoadEvents#addLoadListener(LoadListener)
     */
    public void addLoadListener(LoadListener listener)
    {
        loadListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesLoadEvents#removeLoadListener(LoadListener)
     */
    public void removeLoadListener(LoadListener listener)
    {
        loadListeners.remove(listener);
    }
}
