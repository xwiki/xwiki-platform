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
package com.xpn.xwiki.wysiwyg.client.dom.internal.ie;

import com.google.gwt.core.client.JavaScriptObject;
import com.xpn.xwiki.wysiwyg.client.dom.Document;

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
        this.select();
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
