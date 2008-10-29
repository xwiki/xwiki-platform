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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.ScriptElement;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.RangeFactory;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;
import com.xpn.xwiki.wysiwyg.client.selection.SelectionManager;

/**
 * Extends the Document implementation provided by GwT to add multi-window, selection and range support.
 * 
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=2772
 * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3006
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
     * Creates a new element.
     * 
     * @param tagName the tag name of the element to be created
     * @return the newly created element
     */
    public final native Element xCreateElement(String tagName) /*-{
        return this.createElement(tagName);
    }-*/;

    /**
     * Creates a &lt;link&gt; element.
     * 
     * @return the newly created element
     */
    public final LinkElement xCreateLinkElement()
    {
        return (LinkElement) xCreateElement("link");
    }

    /**
     * Creates a &lt;script&gt; element.
     * 
     * @return the newly created element
     */
    public final ScriptElement xCreateScriptElement()
    {
        return (ScriptElement) xCreateElement("script");
    }

    /**
     * Creates a &lt;br&gt; element.
     * 
     * @return the newly created element
     */
    public final BRElement xCreateBRElement()
    {
        return (BRElement) xCreateElement("br");
    }

    /**
     * Creates a &lt;p&gt; element.
     * 
     * @return the newly created element
     */
    public final ParagraphElement xCreatePElement()
    {
        return (ParagraphElement) xCreateElement("p");
    }

    /**
     * @return The selection object associated with this document.
     */
    public final Selection getSelection()
    {
        return SelectionManager.INSTANCE.getSelection(this);
    }

    /**
     * @return A new range for this document.
     */
    public final Range createRange()
    {
        return RangeFactory.INSTANCE.createRange(this);
    }

    /**
     * Creates a copy of a node from an external document that can be inserted into this document.
     * 
     * @param externalNode The node from another document to be imported.
     * @param deep Indicates whether the children of the given node need to be imported.
     * @return a copy of the given node that can be inserted into this document.
     */
    public final Node xImportNode(Node externalNode, boolean deep)
    {
        return DOMUtils.getInstance().importNode(this, externalNode, deep);
    }
}
