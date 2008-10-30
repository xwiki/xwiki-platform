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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.ScriptElement;

/**
 * Extends the document implementation provided by GWT to add support for multi-window, selection and range.
 * 
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2772
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3006
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3053
 * @version $Id$
 */
public class Document extends com.google.gwt.dom.client.Document
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Document()
    {
        super();
    }

    /**
     * Creates a new element.<br/>
     * We've added this method because at the time of writing {@link com.google.gwt.dom.client.Document} doesn't offer
     * support for multi-window. This means that currently, using GWT's API we can create elements only within the
     * document of the host page. Since {@link com.google.gwt.user.client.ui.RichTextArea} is based on an in-line frame
     * which has its own window and document we have to be able to create elements within the edited document.
     * 
     * @param tagName the tag name of the element to be created
     * @return the newly created element
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2772
     */
    public final native Element xCreateElement(String tagName)
    /*-{
        return this.createElement(tagName);
    }-*/;

    /**
     * Creates a &lt;link&gt; element.
     * 
     * @return the newly created element
     */
    public final LinkElement xCreateLinkElement()
    {
        return (LinkElement) xCreateElement("link").cast();
    }

    /**
     * Creates a &lt;script&gt; element.
     * 
     * @return the newly created element
     */
    public final ScriptElement xCreateScriptElement()
    {
        return (ScriptElement) xCreateElement("script").cast();
    }

    /**
     * Creates a &lt;br&gt; element.
     * 
     * @return the newly created element
     */
    public final BRElement xCreateBRElement()
    {
        return (BRElement) xCreateElement("br").cast();
    }

    /**
     * Creates a &lt;p&gt; element.
     * 
     * @return the newly created element
     */
    public final ParagraphElement xCreatePElement()
    {
        return (ParagraphElement) xCreateElement("p").cast();
    }

    /**
     * We've added this method because at the time of writing {@link com.google.gwt.dom.client.Document} doesn't offer
     * support for retrieving the current selection.
     * 
     * @return The selection object associated with this document.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3053
     */
    public final Selection getSelection()
    {
        return SelectionManager.INSTANCE.getSelection(this);
    }

    /**
     * We've added this method because at the time of writing {@link com.google.gwt.dom.client.Document} doesn't offer
     * support for creating a range.
     * 
     * @return A new range for this document.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3053
     */
    public final Range createRange()
    {
        return RangeFactory.INSTANCE.createRange(this);
    }

    /**
     * Creates a copy of a node from an external document that can be inserted into this document.<br/>
     * We've added this method because at time of writing
     * {@link com.google.gwt.dom.client.Document#importNode(Node, boolean)} is not well implemented.
     * 
     * @param externalNode The node from another document to be imported.
     * @param deep Indicates whether the children of the given node need to be imported.
     * @return a copy of the given node that can be inserted into this document.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3006
     */
    public final Node xImportNode(Node externalNode, boolean deep)
    {
        return DOMUtils.getInstance().importNode(this, externalNode, deep);
    }
}
