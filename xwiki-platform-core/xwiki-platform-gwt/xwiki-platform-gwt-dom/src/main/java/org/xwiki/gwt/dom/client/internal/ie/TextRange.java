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
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.RangeCompare;

/**
 * A text range is a DOM fragment that usually starts and ends inside a text node. It can be used to visually select a
 * continuous text fragment.
 * 
 * @version $Id$
 */
public final class TextRange extends NativeRange
{
    /**
     * A unit defining a fragment of a text range. It is used when moving the end points of a text range.
     */
    public enum Unit
    {
        /**
         * A character.
         */
        CHARACTER,
        /**
         * A word is a collection of characters terminated by a space or some other white-space character, such as a
         * tab.
         */
        WORD,
        /**
         * A sentence is a collection of words terminated by a punctuation character, such as a period.
         */
        SENTENCE,
        /**
         * The start or end of the original range.
         */
        TEXTEDIT
    }

    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected TextRange()
    {
    }

    /**
     * Creates a new text range for the given document. This range can be used to select only DOM nodes within this
     * document.
     * 
     * @param doc The owner document of the created range.
     * @return The created text range.
     */
    public static native TextRange newInstance(Document doc)
    /*-{
        var textRange = doc.body.createTextRange();
        textRange.ownerDocument = doc;
        return textRange;
    }-*/;

    /**
     * @return The HTML source as a valid HTML fragment.
     */
    public native String getHTML()
    /*-{
        return this.htmlText;
    }-*/;

    /**
     * Pastes HTML text into this text range, replacing any previous text and HTML elements in the range. This method
     * might alter the HTML text to make it fit the given text range. For example, pasting a table cell into a text
     * range that does not contain a table might cause the method to insert a table element. For predictable results,
     * paste only well-formed HTML text that fits within the given text range.
     * 
     * @param html The HTML text to paste. The string can contain text and any combination of the HTML tags.
     */
    public native void setHTML(String html)
    /*-{
        this.pasteHTML(html);
    }-*/;

    /**
     * @return The text contained within the range.
     */
    public native String getText()
    /*-{
        return this.text;
    }-*/;

    /**
     * @param text The text to be placed inside the range.
     */
    public native void setText(String text)
    /*-{
        this.text = text;
    }-*/;

    /**
     * Moves the insertion point to the beginning or end of the current range.
     * 
     * @param toStart if true moves the insertion point to the beginning of the text range. Otherwise, moves the
     *            insertion point to the end of the text range.
     */
    public native void collapse(boolean toStart)
    /*-{
        this.collapse(toStart);
    }-*/;

    /**
     * Compares an end point of a TextRange object with an end point of another range. A text range has two end points.
     * One end point is located at the beginning of the text range, and the other is located at the end of the text
     * range. An end point also can be characterized as the position between two characters in an HTML document.
     * 
     * @param how Specifies which end points to compare.
     * @param range The range object to compare with this object.
     * @return One of the following possible values:
     *         <ul>
     *         <li>-1 if the end point of this object is further to the left than the end point of the given range.</li>
     *         <li>0 if the end point of this object is at the same location as the end point of the given range.</li>
     *         <li>1 if the end point of this object is further to the right than the end point of the given range.</li>
     *         </ul>
     */
    public short compareEndPoints(RangeCompare how, TextRange range)
    {
        // In Internet Explorer the meaning of RangeCompare is reversed, so we reverse the end points.
        return compareEndPoints(how.reverse().toString(), range);
    }

    /**
     * Compares an end point of a TextRange object with an end point of another range. A text range has two end points.
     * One end point is located at the beginning of the text range, and the other is located at the end of the text
     * range. An end point also can be characterized as the position between two characters in an HTML document.
     * 
     * @param type Can be one of the following:
     *            <ul>
     *            <li>StartToEnd: Compare the start of this TextRange object with the end of the range parameter.</li>
     *            <li>StartToStart: Compare the start of this TextRange object with the start of the given range
     *            parameter.</li>
     *            <li>EndToStart: Compare the end of this TextRange object with the start of the range parameter.</li>
     *            <li>EndToEnd: Compare the end of this TextRange object with the end of the range parameter.</li>
     *            </ul>
     * @param range TextRange object that specifies the range to compare with this object.
     * @return One of the following possible values:
     *         <ul>
     *         <li>-1 if the end point of this object is further to the left than the end point of the given range.</li>
     *         <li>0 if the end point of this object is at the same location as the end point of the given range.</li>
     *         <li>1 if the end point of this object is further to the right than the end point of the given range.</li>
     *         </ul>
     */
    private native short compareEndPoints(String type, TextRange range)
    /*-{
        return this.compareEndPoints(type, range);
    }-*/;

    /**
     * @return A duplicate of this TextRange.
     */
    public native TextRange duplicate()
    /*-{
        var clone = this.duplicate();
        clone.ownerDocument = this.ownerDocument;
        return clone;
    }-*/;

    /**
     * Collapses the given text range and moves the empty range by the given number of units.
     * 
     * @param unit Specifies the units to move
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    public int move(Unit unit, int count)
    {
        return move(unit.toString(), count);
    }

    /**
     * Collapses the given text range and moves the empty range by the given number of units.
     * 
     * @param unit Specifies the units to move, using one of the following values:
     *            <ul>
     *            <li>character: Moves one or more characters.</li>
     *            <li>word: Moves one or more words. A word is a collection of characters terminated by a space or some
     *            other white-space character, such as a tab.</li>
     *            <li>sentence: Moves one or more sentences. A sentence is a collection of words terminated by a
     *            punctuation character, such as a period.</li>
     *            <li>textedit: Moves to the start or end of the original range.</li>
     *            </ul>
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    private native int move(String unit, int count)
    /*-{
        return this.move(unit, count);
    }-*/;

    /**
     * Moves the end of this range by the given number of units.
     * 
     * @param unit Specifies the units to move.
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    public int moveEnd(Unit unit, int count)
    {
        return moveEnd(unit.toString(), count);
    }

    /**
     * Moves the end of this range by the given number of units.
     * 
     * @param unit Specifies the units to move, using one of the following values:
     *            <ul>
     *            <li>character: Moves one or more characters.</li>
     *            <li>word: Moves one or more words. A word is a collection of characters terminated by a space or some
     *            other white-space character, such as a tab.</li>
     *            <li>sentence: Moves one or more sentences. A sentence is a collection of words terminated by a
     *            punctuation character, such as a period.</li>
     *            <li>textedit: Moves to the start or end of the original range.</li>
     *            </ul>
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    private native int moveEnd(String unit, int count)
    /*-{
        return this.moveEnd(unit, count);
    }-*/;

    /**
     * Moves the start of this range by the given number of units.
     * 
     * @param unit Specifies the units to move.
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    public int moveStart(Unit unit, int count)
    {
        return moveStart(unit.toString(), count);
    }

    /**
     * Moves the start of this range by the given number of units.
     * 
     * @param unit Specifies the units to move, using one of the following values:
     *            <ul>
     *            <li>character: Moves one or more characters.</li>
     *            <li>word: Moves one or more words. A word is a collection of characters terminated by a space or some
     *            other white-space character, such as a tab.</li>
     *            <li>sentence: Moves one or more sentences. A sentence is a collection of words terminated by a
     *            punctuation character, such as a period.</li>
     *            <li>textedit: Moves to the start or end of the original range.</li>
     *            </ul>
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    private native int moveStart(String unit, int count)
    /*-{
        return this.moveStart(unit, count);
    }-*/;

    /**
     * Moves the text range so that the start and end positions of the range encompass the text in the given element.
     * 
     * @param element The element object to move to.
     */
    public native void moveToElementText(Element element)
    /*-{
        this.moveToElementText(element);
    }-*/;

    /**
     * The parent element is the element that completely encloses the text in the range. If the text range spans text in
     * more than one element, this method returns the smallest element that encloses all the elements. When you insert
     * text into a range that spans multiple elements, the text is placed in the parent element rather than in any of
     * the contained elements.
     * 
     * @return The parent element for this text range.
     */
    public native Element getParentElement()
    /*-{
        return this.parentElement();
    }-*/;

    /**
     * Sets the end point of this range based on the end point of another range. A text range has two end points: one at
     * the beginning of the text range and one at the end. An end point can also be the position between two characters
     * in an HTML document.
     * 
     * @param how Specifies which end point of this text range should be moved and which of the given text range's end
     *            points is the reference.
     * @param range The text range used as the reference.
     */
    public void setEndPoint(RangeCompare how, TextRange range)
    {
        // In Internet Explorer the meaning of RangeCompare is reversed, so we reverse the end points.
        switch (how) {
            case END_TO_START:
                // Move the start of this range to the end of the given range.
                if (compareEndPoints(RangeCompare.END_TO_END, range) < 0) {
                    // The end point needs to be updated also because other wise the start point will go beyond the end
                    // point.
                    setEndPoint(RangeCompare.END_TO_END.toString(), range);
                }
                break;
            case START_TO_START:
                // Move the start of this range to the start of the given range.
                if (compareEndPoints(RangeCompare.START_TO_END, range) < 0) {
                    // The end point needs to be updated also because other wise the start point will go beyond the end
                    // point.
                    setEndPoint(RangeCompare.END_TO_START.toString(), range);
                }
                break;
            case END_TO_END:
                // Move the end of this range to the end of the given range.
                if (compareEndPoints(RangeCompare.END_TO_START, range) > 0) {
                    // The start point needs to be updated also because other wise the end point will go before the
                    // start point.
                    setEndPoint(RangeCompare.START_TO_END.toString(), range);
                }
                break;
            case START_TO_END:
                // Move the end of this range to the start of the given range.
                if (compareEndPoints(RangeCompare.START_TO_START, range) > 0) {
                    // The start point needs to be updated also because other wise the end point will go before the
                    // start point.
                    setEndPoint(RangeCompare.START_TO_START.toString(), range);
                }
                break;
            default:
                // We shouldn't get here.
        }
        setEndPoint(how.reverse().toString(), range);
    }

    /**
     * Sets the end point of this range based on the end point of another range. A text range has two end points: one at
     * the beginning of the text range and one at the end. An end point can also be the position between two characters
     * in an HTML document.
     * 
     * @param type Specifies the end point to transfer using one of the following values:
     *            <ul>
     *            <li>StartToEnd: Move the start of this TextRange object to the end of the range parameter.</li>
     *            <li>StartToStart: Move the start of this TextRange object to the start of the range parameter.</li>
     *            <li>EndToStart: Move the end of this TextRange object to the start of the range parameter.</li>
     *            <li>EndToEnd: Move the end of this TextRange object to the end of the range parameter.</li>
     *            </ul>
     * @param range TextRange object from which the source end point is to be taken.
     */
    private native void setEndPoint(String type, TextRange range)
    /*-{
        this.setEndPoint(type, range);
    }-*/;

    /**
     * Searches for text in the document and positions the start and end points of the range to encompass the search
     * string.<br/>
     * The value passed for the searchScope parameter controls the part of the document, relative to the range, that is
     * searched. The behavior of the findText method depends on whether the range is collapsed or not:
     * <ul>
     * <li>If the range is collapsed, passing a large positive number causes the text to the right of the range to be
     * searched. Passing a large negative number causes the text to the left of the range to be searched.</li>
     * <li>If the range is not collapsed, passing a large positive number causes the text to the right of the start of
     * the range to be searched. Passing a large negative number causes the text to the left of the end of the range to
     * be searched. Passing 0 causes only the text selected by the range to be searched.</li>
     * </ul>
     * A text range is not modified if the text specified for the findText method is not found.
     * 
     * @param text The text to find.
     * @param searchScope The number of characters to search from the starting point of the range. A positive integer
     *            indicates a forward search; a negative integer indicates a backward search.
     * @param flags One or more of the following flags to indicate the type of search:
     *            <table>
     *            <tr>
     *            <td>0</td>
     *            <td>Default. Match partial words.</td>
     *            </tr>
     *            <tr>
     *            <td>1</td>
     *            <td>Match backwards.</td>
     *            </tr>
     *            <tr>
     *            <td>2</td>
     *            <td>Match whole words only.</td>
     *            </tr>
     *            <tr>
     *            <td>4</td>
     *            <td>Match case.</td>
     *            </tr>
     *            <tr>
     *            <td>131072</td>
     *            <td>Match bytes.</td>
     *            </tr>
     *            <tr>
     *            <td>536870912</td>
     *            <td>Match diacritical marks.</td>
     *            </tr>
     *            <tr>
     *            <td>1073741824</td>
     *            <td>Match Kashida character.</td>
     *            </tr>
     *            <tr>
     *            <td>2147483648</td>
     *            <td>Match AlefHamza character.</td>
     *            </tr>
     *            </table>
     * @return true if the given text was found.
     */
    public native boolean findText(String text, int searchScope, int flags)
    /*-{
        return this.findText(text, searchScope, flags);
    }-*/;

    /**
     * Tests if this text range equals the given text range.
     * 
     * @param other the text range to compare with this text range.
     * @return true if the given text range is equal to this text rage.
     */
    public native boolean isEqual(TextRange other)
    /*-{
        return this.isEqual(other);
    }-*/;

    /**
     * Tests whether one range is contained within another.
     * 
     * @param other The text range that might be contained in this text range.
     * @return true if the given text range is contained within or is equal to this text range.
     */
    public native boolean inRange(TextRange other)
    /*-{
        return this.inRange(other);
    }-*/;

    /**
     * Expands the range so that partial units are completely contained.
     * 
     * @param unit specifies the units that have to be completely included in the range
     * @return {@code true} if the range was successfully expanded, {@code false} otherwise
     */
    public boolean expand(Unit unit)
    {
        return expand(unit.toString());
    }

    /**
     * Expands the range so that partial units are completely contained.
     * 
     * @param unit specifies the units that have to be completely included in the range, using one of the following
     *            values:
     *            <ul>
     *            <li>character: Moves one or more characters.</li>
     *            <li>word: Moves one or more words. A word is a collection of characters terminated by a space or some
     *            other white-space character, such as a tab.</li>
     *            <li>sentence: Moves one or more sentences. A sentence is a collection of words terminated by a
     *            punctuation character, such as a period.</li>
     *            <li>textedit: Moves to the start or end of the original range.</li>
     *            </ul>
     * @return {@code true} if the range was successfully expanded, {@code false} otherwise
     */
    private native boolean expand(String unit)
    /*-{
        return this.expand(unit);
    }-*/;

    /**
     * Moves the start and end positions of this text range to the given point.
     * <p>
     * The coordinates of the point must be in pixels and be relative to the upper-left corner of the window. The
     * resulting text range is empty, but you can expand and move the range using methods such as {@link #expand(Unit)}
     * and {@link #moveEnd(Unit, int)}.
     * 
     * @param x integer that specifies the horizontal offset relative to the upper-left corner of the window, in pixels
     * @param y integer that specifies the vertical offset relative to the upper-left corner of the window, in pixels
     */
    public native void moveToPoint(int x, int y)
    /*-{
        this.moveToPoint(x, y);
    }-*/;

    /**
     * Retrieves the calculated left position of this range's start point relative to the layout or coordinate parent,
     * as specified by the offsetParent property.
     * 
     * @return an integer that specifies the left position, in pixels
     */
    public native int getOffsetLeft()
    /*-{
        return this.offsetLeft;
    }-*/;

    /**
     * Retrieves the calculated top position of this range's start point relative to the layout or coordinate parent, as
     * specified by the offsetParent property.
     * 
     * @return an integer that specifies the top position, in pixels
     */
    public native int getOffsetTop()
    /*-{
        return this.offsetTop;
    }-*/;
}
