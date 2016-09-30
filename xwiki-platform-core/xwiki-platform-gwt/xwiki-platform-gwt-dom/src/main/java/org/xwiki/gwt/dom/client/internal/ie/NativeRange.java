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
        var doc = this.ownerDocument;
        var wnd = doc.parentWindow;
        // Test if the owner document holds the current selection.
        if (typeof(wnd.__xwe_savedRange) == 'undefined') {
            // Try to apply this range.
            try {
                this.select();
                // This range was successfully applied. Reset the cache.
                wnd.__xwe_cachedRange = undefined;
                wnd.__xwe_cachedRangeWitness = undefined;
            } catch (e) {
                if (e.number == -2146827682) {
                    // "Could not complete the operation due to error 800a025e"
                    // This range probably starts or ends inside a hidden element. In order to make the selection work
                    // for hidden elements (without visual representation, of course) we have to cache this range and
                    // return it next time if the selection doesn't change in the mean time.
                    wnd.__xwe_cachedRange = @org.xwiki.gwt.dom.client.internal.ie.NativeRange::duplicate(Lorg/xwiki/gwt/dom/client/internal/ie/NativeRange;)(this);
                    // The cache expires when the witness range is not anymore selected.
                    wnd.__xwe_cachedRangeWitness = doc.selection.createRange();
                } else {
                    throw e;
                }
            }
        } else {
            // Save this range till the owner document gains the focus.
            // NOTE: This range might not be valid! We can't test its validity without selecting it first. If the saved
            // range is not valid an exception will be thrown when it will be restored.
            wnd.__xwe_savedRange = this;
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
     * See http://code.google.com/p/google-web-toolkit/wiki/OverlayTypes.
     * 
     * @return true if this is a text range, and false if it is a control range.
     */
    public final native boolean isTextRange()
    /*-{
        return this.item ? false : true;
    }-*/;

    /**
     * NOTE: We had to add this method because overlay types don't support method overriding and both {@link TextRange}
     * and {@link ControlRange} have a {@code duplicate} method but with a different implementation. Using an abstract
     * {@code duplicate} method is not an option because overlay types can't implement interfaces and this method can't
     * return an abstract type. We had to make this method static because only static references to overlay types are
     * allowed from JSNI.
     * 
     * @param range the native range to be duplicated
     * @return a duplicate of the given native range
     */
    public static NativeRange duplicate(NativeRange range)
    {
        return range.isTextRange() ? ((TextRange) range).duplicate() : ((ControlRange) range).duplicate();
    }

    /**
     * NOTE: We added this static method for the same reasons we added the {@link #duplicate(NativeRange)} method.
     * 
     * @param alice a native range
     * @param bob a native range
     * @return {@code true} if the given native ranges are equal, {@code false} otherwise
     * @see #duplicate(NativeRange)
     */
    public static boolean areEqual(NativeRange alice, NativeRange bob)
    {
        if (alice == bob) {
            return true;
        }
        if (alice == null || bob == null || alice.isTextRange() != bob.isTextRange()) {
            return false;
        }
        return alice.isTextRange() ? ((TextRange) alice).isEqual((TextRange) bob) : ((ControlRange) alice)
            .isEqual((ControlRange) bob);
    }
}
