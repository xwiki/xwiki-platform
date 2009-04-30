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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.Document;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Base class for {@link TextRange} and {@link ControlRange}, the two types of range provided by Internet Explorer.
 * 
 * @version $Id$
 */
public class NativeRange extends JavaScriptObject
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected NativeRange()
    {
    }

    /**
     * Makes the selection equal to the current object. When applied to a TextRange object, the select method causes the
     * current object to be highlighted. When applied to a ControlRange object, the select method produces a shaded
     * rectangle around the elements in the control range.
     */
    public final native void select()
    /*-{
        // If the owner document has the focus, and thus the current selection, then apply this range directly.
        // Otherwise, use the bookmark to save this range; later, when the owner document receives the focus, this
        // range will be reconstructed and applied.
        if (typeof(this.ownerDocument.body.__bookmark) == 'undefined') {
            // The owner document holds the current selection.
            this.select();
        // Save this range in the bookmark to be applied later, when the owner document receives the focus.
        } else if (this.getBookmark) {
            // Text range.
            this.ownerDocument.body.__bookmark = this.getBookmark();
        } else if (this.item && this.length > 0) {
            // Control range.
            this.ownerDocument.body.__bookmark = this.item(0);
        } else {
            // No range. Set the bookmark to an unsupported value.
            this.ownerDocument.body.__bookmark = false;
        }
    }-*/;

    /**
     * @return The document used to create this range.
     */
    public final native Document getOwnerDocument()
    /*-{
        return this.ownerDocument;
    }-*/;

    /**
     * This method is needed because <code>instanceof</code> operator returns true all the time when applied on a
     * overlay type. For instance:<br/>
     * 
     * <pre>
     * TextRange textRange = TextRange.newInstance(doc);
     * boolean result = textRange instanceof TextRange; // result is true, which is right.
     * result = textRange instanceof ControlRange // result is also true, which is wrong.
     * </pre>
     * 
     * @return true if this is a text range, and false if it is a control range.
     * @see http://code.google.com/p/google-web-toolkit/wiki/OverlayTypes
     */
    public final native boolean isTextRange()
    /*-{
        return this.item ? false : true;
    }-*/;
}
