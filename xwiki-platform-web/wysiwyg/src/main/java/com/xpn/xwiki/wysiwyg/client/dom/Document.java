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

import java.util.Iterator;

import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.BaseElement;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.HRElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.dom.client.SpanElement;

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
     * @param data contains the data to be added to the comment.
     * @return the created comment node.
     */
    public final native Node createComment(String data)
    /*-{
        return this.createComment(data);
    }-*/;

    /**
     * Creates a &lt;link&gt; element.
     * 
     * @return the newly created element
     */
    public final LinkElement xCreateLinkElement()
    {
        return xCreateElement("link").cast();
    }

    /**
     * Creates a &lt;script&gt; element.
     * 
     * @return the newly created element
     */
    public final ScriptElement xCreateScriptElement()
    {
        return xCreateElement("script").cast();
    }

    /**
     * Creates a &lt;br&gt; element.
     * 
     * @return the newly created element
     */
    public final BRElement xCreateBRElement()
    {
        return xCreateElement("br").cast();
    }

    /**
     * Creates a &lt;p&gt; element.
     * 
     * @return the newly created element
     */
    public final ParagraphElement xCreatePElement()
    {
        return xCreateElement("p").cast();
    }

    /**
     * Creates a &lt;div&gt; element.
     * 
     * @return the newly created element.
     */
    public final DivElement xCreateDivElement()
    {
        return xCreateElement("div").cast();
    }

    /**
     * Creates a &lt;span&gt; element.
     * 
     * @return the newly created element.
     */
    public final SpanElement xCreateSpanElement()
    {
        return xCreateElement("span").cast();
    }

    /**
     * @return a new base element
     */
    public final BaseElement xCreateBaseElement()
    {
        return xCreateElement("base").cast();
    }

    /**
     * Creates a &lt;hr&gt; element.
     * 
     * @return the newly create element.
     */
    public final HRElement xCreateHRElement()
    {
        return xCreateElement("hr").cast();
    }

    /**
     * Creates a &lt;img&gt; element.
     * 
     * @return the newly create element.
     */
    public final ImageElement xCreateImageElement()
    {
        return xCreateElement("img").cast();
    }

    /**
     * Creates a &lt;li&gt; element.
     * 
     * @return the newly created element.
     */
    public final LIElement xCreateLIElement()
    {
        return xCreateElement("li").cast();
    }

    /**
     * Creates a &lt;button&gt; element.
     * 
     * @return the newly create element.
     */
    public final ButtonElement xCreateButtonElement()
    {
        return xCreateElement("button").cast();
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

    /**
     * Returns an iterator for the depth-first pre-order strategy, starting in <code>startNode</code>.
     * 
     * @param startNode node to start iteration from
     * @return the depth-first pre-order iterator
     * @see {@link DepthFirstPreOrderIterator}
     * @see http://www.w3.org/TR/DOM-Level-2-Traversal-Range/traversal.html#Traversal-Document
     */
    public final Iterator<Node> getIterator(Node startNode)
    {
        return new DepthFirstPreOrderIterator(startNode);
    }

    /**
     * When an HTML document has been switched to designMode, the document object exposes the execCommand method which
     * allows one to run commands to manipulate the contents of the editable region. Most commands affect the document's
     * selection (bold, italics, etc), while others insert new elements (adding a link) or affect an entire line
     * (indenting). When using contentEditable, calling execCommand will affect the currently active editable element.
     * 
     * @param command The name of the command.
     * @param parameter Some commands (such as insertimage) require an extra value argument (the image's url). Pass an
     *            argument of null if no argument is needed.
     * @return true if the specified command has been successfully executed.
     */
    public final native boolean execCommand(String command, String parameter)
    /*-{
        try{
            return this.execCommand(command, false, parameter);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * @param command The name of the command to query.
     * @return The current value of the current range for the given command. If a command value has not been explicitly
     *         set then it returns null.
     */
    public final native String queryCommandValue(String command)
    /*-{
        try{
            return this.queryCommandValue(command);
        } catch(e) {
            return null;
        }
    }-*/;

    /**
     * @param command The name of the command to query.
     * @return true if the given command can be executed on the current range.
     */
    public final native boolean queryCommandEnabled(String command)
    /*-{
        try{
            return this.queryCommandEnabled(command);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * @param command The name of the command to query.
     * @return true if the given command has been executed on the current range.
     */
    public final native boolean queryCommandState(String command)
    /*-{
        try{
            return this.queryCommandState(command);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * @param command The name of the command to query.
     * @return true if the given command is supported by the current browser.
     */
    public final native boolean queryCommandSupported(String command)
    /*-{
        try{
            return this.queryCommandSupported(command);
        } catch(e) {
            return true;
        }
    }-*/;

    /**
     * @return the document element.
     */
    public final native Element getDocumentElement()
    /*-{
        return this.documentElement;
    }-*/;

    /**
     * Creates an empty document fragment.<br/>
     * A DocumentFragment is a minimal document object that has no parent. It supports the following DOM 2 methods:
     * appendChild, cloneNode, hasAttributes, hasChildNodes, insertBefore, normalize, removeChild, replaceChild.<br/>
     * It also supports the following DOM 2 properties: attributes, childNodes, firstChild, lastChild, localName,
     * namespaceURI, nextSibling, nodeName, nodeType, nodeValue, ownerDocument, parentNode, prefix, previousSibling,
     * textContent.<br/>
     * Various other methods can take a document fragment as an argument (e.g. Node interface methods such as
     * appendChild and insertBefore), in which case the children of the fragment are appended or inserted, not the
     * fragment itself.
     * 
     * @return The newly created document fragment.
     */
    public final native DocumentFragment createDocumentFragment()
    /*-{
        return this.createDocumentFragment();
    }-*/;

    /**
     * Opens a document stream for writing.
     */
    public final native void open()
    /*-{
        this.open();
    }-*/;

    /**
     * Closes a document stream for writing.
     */
    public final native void close()
    /*-{
        this.close();
    }-*/;

    /**
     * Writes a string of text to a document stream opened by {@link #open()}.
     * 
     * @param html a string containing the HTML to be written to the document.
     */
    public final native void write(String html)
    /*-{
        this.write(html);
    }-*/;

    /**
     * Registers a new listener for changes to <code>innerHTML</code> property of element within this document.
     * 
     * @param listener The listener to be registered.
     */
    public final native void addInnerHTMLListener(InnerHTMLListener listener)
    /*-{
        if (!this.innerHTMLListeners) {
            this.innerHTMLListeners = [];
        }
        this.innerHTMLListeners.push(listener);
    }-*/;

    /**
     * Stop sending notifications to the given listener when the <code>innerHTML</code> property, of some element
     * included in this document, changes.
     * 
     * @param listener The listener to be unregistered.
     */
    public final native void removeInnerHTMLListener(InnerHTMLListener listener)
    /*-{
        if (this.innerHTMLListeners) {
            for (var i = 0; i < this.innerHTMLListeners.length; i++) {
                if (this.innerHTMLListeners[i] == listener) {
                    this.innerHTMLListeners.splice(i, 1);
                }
            }
        }
    }-*/;

    /**
     * Notify all listeners of the change to the given element's <code>innerHTML</code> property.<br/>
     * NOTE: Keep this method accessible only from within this package because only {@link Element} should call it.
     * 
     * @param element The element whose <code>innerHTML</code> property has changed.
     */
    final native void fireInnerHTMLChange(Element element)
    /*-{
        if (this.innerHTMLListeners) {
            for (var i = 0; i < this.innerHTMLListeners.length; i++) {
                this.innerHTMLListeners[i].
    @com.xpn.xwiki.wysiwyg.client.dom.InnerHTMLListener::onInnerHTMLChange(Lcom/xpn/xwiki/wysiwyg/client/dom/Element;)
    (element);
            }
        }
    }-*/;
}
