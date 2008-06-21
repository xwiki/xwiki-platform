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
 *
 */
package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.XmlSerializable;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link Position}. It uses a [row, column] position mark, with an optional context (text
 * before and after the position).
 * 
 * @see org.xwiki.platform.patchservice.api.Position
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class PositionImpl implements Position, XmlSerializable
{
    /** The name of the XML element corresponding to position objects. */
    public static final String NODE_NAME = "position";

    /** The name of the XML attribute holding the row number. */
    public static final String ROW_ATTRIBUTE_NAME = "row";

    /** The name of the XML attribute holding the column number. */
    public static final String COLUMN_ATTRIBUTE_NAME = "column";

    /** The name of the XML attribute holding the position's length. Currently not used. */
    public static final String SPAN_ATTRIBUTE_NAME = "span";

    /** The name of the XML attribute holding the context before the position. */
    public static final String BEFORE_ATTRIBUTE_NAME = "before";

    /** The name of the XML attribute holding the context after the position. */
    public static final String AFTER_ATTRIBUTE_NAME = "after";

    /** The newline character used. */
    private static final String SEPARATOR = "\n";

    private String before;

    private String after;

    private int row = -1;

    private int column = -1;

    private int span = -1;

    public PositionImpl()
    {
        this(0, 0, -1, null, null);
    }

    public PositionImpl(int row, int column)
    {
        this(row, column, -1);
    }

    public PositionImpl(int row, int column, int span)
    {
        this(row, column, span, null, null);
    }

    public PositionImpl(int row, int column, String before, String after)
    {
        this(row, column, -1, before, after);
    }

    public PositionImpl(int row, int column, int span, String before, String after)
    {
        this.row = row;
        this.column = column;
        this.span = span;
        this.before = before;
        this.after = after;
    }

    /**
     * {@inheritDoc}
     */
    public boolean checkPosition(String text)
    {
        String[] rows = StringUtils.splitPreserveAllTokens(text, SEPARATOR);
        if (rows != null
            && ((rows.length > this.row && rows[this.row].length() >= this.column) || (rows.length == this.row && this.column == 0)))
        {
            return (StringUtils.isEmpty(this.before) || getTextBeforePosition(text).endsWith(this.before))
                && (StringUtils.isEmpty(this.after) || getTextAfterPosition(text).startsWith(this.after));
        }
        return (this.row == 0 || this.row == 1) && this.column == 0 && StringUtils.isEmpty(this.before)
            && StringUtils.isEmpty(this.after);
    }

    /**
     * {@inheritDoc}
     */
    public String getTextBeforePosition(String text)
    {
        String[] rows = StringUtils.splitPreserveAllTokens(text, SEPARATOR);
        if (ArrayUtils.getLength(rows) <= this.row) {
            return StringUtils.defaultString(StringUtils.join(rows, SEPARATOR)) + (this.row == 0 ? "" : "\n");
        }
        return StringUtils.join(ArrayUtils.subarray(rows, 0, this.row), SEPARATOR) + ((this.row > 0) ? SEPARATOR : "")
            + StringUtils.substring(rows[this.row], 0, this.column);
    }

    /**
     * {@inheritDoc}
     */
    public String getTextAfterPosition(String text)
    {
        String[] rows = StringUtils.splitPreserveAllTokens(text, SEPARATOR);
        if (ArrayUtils.getLength(rows) <= this.row) {
            return "";
        }
        String textAfter =
            StringUtils.substring(rows[this.row], this.column) + ((this.row + 1 < rows.length) ? SEPARATOR : "")
                + StringUtils.join(ArrayUtils.subarray(rows, this.row + 1, rows.length), SEPARATOR);
        return (this.span <= 0) ? textAfter : StringUtils.substring(textAfter, this.span);
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.row = Integer.parseInt(e.getAttribute(ROW_ATTRIBUTE_NAME));
        this.column = Integer.parseInt(e.getAttribute(COLUMN_ATTRIBUTE_NAME));
        if (e.hasAttribute(BEFORE_ATTRIBUTE_NAME)) {
            this.before = e.getAttribute(BEFORE_ATTRIBUTE_NAME);
        }
        if (e.hasAttribute(AFTER_ATTRIBUTE_NAME)) {
            this.after = e.getAttribute(AFTER_ATTRIBUTE_NAME);
        }
        if (e.hasAttribute(SPAN_ATTRIBUTE_NAME)) {
            this.span = Integer.parseInt(e.getAttribute(SPAN_ATTRIBUTE_NAME));
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(ROW_ATTRIBUTE_NAME, this.row + "");
        xmlNode.setAttribute(COLUMN_ATTRIBUTE_NAME, this.column + "");
        if (!StringUtils.isEmpty(this.before)) {
            xmlNode.setAttribute(BEFORE_ATTRIBUTE_NAME, this.before);
        }
        if (!StringUtils.isEmpty(this.after)) {
            xmlNode.setAttribute(AFTER_ATTRIBUTE_NAME, this.after);
        }
        if (this.span >= 0) {
            xmlNode.setAttribute(SPAN_ATTRIBUTE_NAME, this.span + "");
        }
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            PositionImpl that = (PositionImpl) other;
            return (that.row == this.row) && that.column == this.column
                && StringUtils.defaultString(that.before).equals(StringUtils.defaultString(this.before))
                && StringUtils.defaultString(that.after).equals(StringUtils.defaultString(this.after))
                && (that.span == this.span);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(5, 47).append(this.row).append(this.column).append(
            StringUtils.defaultString(this.before)).append(StringUtils.defaultString(this.after)).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "@" + this.row + "," + this.column + ": << [" + this.before + "] >> [" + this.after + "]";
    }
}
