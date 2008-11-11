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

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.dom.Text;

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
    public static enum Unit
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
     * Creates a new text range from the given control range. Only the first element in the control range is taken into
     * account. If this element has inner text then the return text range starts before the first character of the first
     * text node descendent and ends after the last character of the last text node descendent. Otherwise, the returned
     * text range starts before this element and ends after it.
     * 
     * @param controlRange The source of the new text range.
     * @return The created text range.
     */
    public static TextRange newInstance(ControlRange controlRange)
    {
        TextRange textRange = newInstance(controlRange.getOwnerDocument());
        textRange.moveToElementText(controlRange.get(0));
        return textRange;
    }

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
        // We do this because IE trims the leading comment (if any) in the html, so we add a dummy text before it and 
        // remove it afterwards. We need this when adding wikilinks for example, who's html start with a comment. Even 
        // if we hadn't the links, it is generally a good method to make sure that "what you set is what you get".
        var id = 'org.xwiki.wysiwyg.iesucks';
        this.pasteHTML('<span id="' + id + '">iesucks</span>' + html);
        var marker = this.ownerDocument.getElementById(id);
        marker.parentNode.removeChild(marker);
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
     * Sets the end point of this range based on the given DOM node. A text range has two end points: one at the
     * beginning of the text range and one at the end. An end point can also be the position between two characters in
     * an HTML document.
     * 
     * @param how Specifies the end point to transfer using one of the following values:
     *            <ul>
     *            <li>{@link RangeCompare#START_TO_END}: Move the end of this TextRange object before the refNode.</li>
     *            <li>{@link RangeCompare#START_TO_START}: Move the start of this TextRange object before the refNode.</li>
     *            <li>{@link RangeCompare#END_TO_START}: Move the start of this TextRange object after the refNode.</li>
     *            <li>{@link RangeCompare#END_TO_END}: Move the end of this TextRange object after the refNode.</li>
     *            </ul>
     * @param refNode The reference point.
     */
    public void setEndPoint(RangeCompare how, Node refNode)
    {
        setEndPoint(how, refNode, 0);
    }

    /**
     * Sets the end point of this range based on the given DOM node and the specified offset. A text range has two end
     * points: one at the beginning of the text range and one at the end. An end point can also be the position between
     * two characters in an HTML document.
     * 
     * @param how Specifies the end point to transfer using one of the following values:
     *            <ul>
     *            <li>{@link RangeCompare#START_TO_END}: Move the end of this TextRange object before the refNode.</li>
     *            <li>{@link RangeCompare#START_TO_START}: Move the start of this TextRange object before the refNode.</li>
     *            <li>{@link RangeCompare#END_TO_START}: Move the start of this TextRange object after the refNode.</li>
     *            <li>{@link RangeCompare#END_TO_END}: Move the end of this TextRange object after the refNode.</li>
     *            </ul>
     * @param refNode The reference point.
     * @param offset The number of possible-cursor-positions to move from the reference point.
     */
    public void setEndPoint(RangeCompare how, Node refNode, int offset)
    {
        TextRange refRange = this.duplicate();
        if (refNode.getNodeType() == Node.ELEMENT_NODE) {
            refRange.moveToElementText(Element.as(refNode));
        } else if (refNode.getNodeType() == Node.TEXT_NODE) {
            refRange.moveToTextNode(Text.as(refNode));
        } else {
            throw new IllegalArgumentException("Expecting element or text node!");
        }
        refRange.shift(Unit.CHARACTER, offset);
        this.setEndPoint(how, refRange);
    }

    /**
     * Position the start of this text range before the first character of the given text node and the end after the
     * last character.
     * 
     * @param textNode The text node to be wrapped by this range.
     */
    public void moveToTextNode(Text textNode)
    {
        if (textNode.getLength() == 0) {
            // In Internet Explorer you cannot position the caret inside an empty text node. As a consequence we have to
            // insert a space character. We have to do this in order to ensure that the implementation of W3C Range
            // specification using IE's API is as powerful as the Mozilla one.
            // @see http://forums.microsoft.com/msdn/ShowPost.aspx?postid=4097966
            textNode.setData(" ");
        }

        int startOffset = 0;
        Node refNode = textNode.getPreviousSibling();
        // We look for a reference element. We first search for the closest element sibling to the left of the textNode.
        // At the same time, we count the number of character we jump back.
        while (refNode != null && refNode.getNodeType() != Node.ELEMENT_NODE) {
            if (refNode.getNodeType() == Node.TEXT_NODE) {
                startOffset += refNode.getNodeValue().length();
            }
        }
        if (refNode == null) {
            // Looks like textNode doesn't have any previous element siblings.
            // Thus we use the parent element as the reference point.
            refNode = textNode.getParentNode();
        }

        this.moveToElementText(Element.as(refNode));
        this.shift(Unit.CHARACTER, startOffset);
    }

    /**
     * Moves both end points of this range by the given number of units.
     * 
     * @param unit Specifies the units to move.
     * @param count Specifies the number of units to move. This can be positive or negative.
     * @return The number of units moved.
     */
    public int shift(Unit unit, int count)
    {
        if (count == 0) {
            return 0;
        } else if (count > 0) {
            moveEnd(unit, count);
            return moveStart(unit, count);
        } else {
            moveStart(unit, count);
            return moveEnd(unit, count);
        }
    }
}
