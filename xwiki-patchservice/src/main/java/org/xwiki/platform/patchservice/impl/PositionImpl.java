package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Position;

import com.xpn.xwiki.XWikiException;

public class PositionImpl implements Position
{
    public static final String NODE_NAME = "position";

    public static final String ROW_ATTRIBUTE_NAME = "row";

    public static final String COLUMN_ATTRIBUTE_NAME = "column";

    public static final String SPAN_ATTRIBUTE_NAME = "span";

    public static final String BEFORE_ATTRIBUTE_NAME = "before";

    public static final String AFTER_ATTRIBUTE_NAME = "after";
    
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
        if (rows != null && rows.length > row && rows[row].length() >= column) {
            return (StringUtils.isEmpty(before) || getTextBeforePosition(text).endsWith(before))
                && (StringUtils.isEmpty(after) || getTextAfterPosition(text).startsWith(after));
        }
        return row == 0 && column == 0 && StringUtils.isEmpty(before)
            && StringUtils.isEmpty(after);
    }

    /**
     * {@inheritDoc}
     */
    public String getTextBeforePosition(String text)
    {
        String[] rows = StringUtils.splitPreserveAllTokens(text, SEPARATOR);
        if (ArrayUtils.getLength(rows) <= row) {
            return StringUtils.defaultString(StringUtils.join(rows, SEPARATOR));
        }
        return StringUtils.join(ArrayUtils.subarray(rows, 0, row), SEPARATOR)
            + ((row > 0) ? SEPARATOR : "") + StringUtils.substring(rows[row], 0, column);
    }

    /**
     * {@inheritDoc}
     */
    public String getTextAfterPosition(String text)
    {
        String[] rows = StringUtils.splitPreserveAllTokens(text, SEPARATOR);
        if (ArrayUtils.getLength(rows) <= row) {
            return "";
        }
        String textAfter =
            StringUtils.substring(rows[row], column) + ((row + 1 < rows.length) ? SEPARATOR : "")
                + StringUtils.join(ArrayUtils.subarray(rows, row + 1, rows.length), SEPARATOR);
        return (span <= 0) ? textAfter : StringUtils.substring(textAfter, span);
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
        xmlNode.setAttribute(ROW_ATTRIBUTE_NAME, row + "");
        xmlNode.setAttribute(COLUMN_ATTRIBUTE_NAME, column + "");
        if (!StringUtils.isEmpty(before)) {
            xmlNode.setAttribute(BEFORE_ATTRIBUTE_NAME, before);
        }
        if (!StringUtils.isEmpty(after)) {
            xmlNode.setAttribute(AFTER_ATTRIBUTE_NAME, after);
        }
        if (span >= 0) {
            xmlNode.setAttribute(SPAN_ATTRIBUTE_NAME, span + "");
        }
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            PositionImpl that = (PositionImpl) other;
            return (that.row == this.row)
                && that.column == this.column
                && StringUtils.defaultString(that.before).equals(
                    StringUtils.defaultString(before))
                && StringUtils.defaultString(that.after).equals(StringUtils.defaultString(after))
                && (that.span == this.span);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(5, 47).append(this.row).append(this.column).append(
            StringUtils.defaultString(before)).append(StringUtils.defaultString(after))
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "@" + this.row + "," + this.column + ": << [" + this.before + "] >> ["
            + this.after + "]";
    }
}
