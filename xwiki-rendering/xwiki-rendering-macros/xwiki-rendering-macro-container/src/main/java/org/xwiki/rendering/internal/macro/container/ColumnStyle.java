package org.xwiki.rendering.internal.macro.container;

import org.apache.commons.lang.StringUtils;

/**
 * Abstraction that encapsulate the different styles that can be given to a column by the section/column macros and can
 * generate the proper style code to be given to the div enclosing the targeted column.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class ColumnStyle
{
    /**
     * The rule to set the floating of a column (to the left).
     */
    private static final String FLOAT_RULE = "float: left;";

    /**
     * The percent sign, to set various box sizes in percents.
     */
    private static final String PERCENT = "%";

    /**
     * The width of the column styled by this object.
     */
    private String width;

    /**
     * The padding right of the column styled by this object (i.e. columns separator).
     */
    private String paddingRight;

    /**
     * @return the style rules in this object, as an inline CSS string.
     */
    public String getStyleAsString()
    {
        String ruleSeparator = ";";
        String style = FLOAT_RULE;
        if (!StringUtils.isBlank(this.width)) {
            style += "width:" + this.width + ruleSeparator;
        }
        if (!StringUtils.isBlank(this.paddingRight)) {
            style += "padding-right:" + this.paddingRight + ruleSeparator;
        }
        return style;
    }

    /**
     * @return the width (as a css string value) of the column styled by this object
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * Sets the width of the column styled by this object, as a string (e.g. 23%, 300px, ).
     * 
     * @param width the width string to set
     */
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * Sets the width of the column styled by this object, as a percent.
     * 
     * @param width the width (in percents) to set
     */
    public void setWidthPercent(double width)
    {
        this.width = width + PERCENT;
    }

    /**
     * @return the padding right (as a css string value) of the column styled by this object
     */
    public String getPaddingRight()
    {
        return paddingRight;
    }

    /**
     * Sets the padding right of the column styled by this object, as a string (e.g. 23%, 300px, ).
     * 
     * @param paddingRight the padding right string to set
     */
    public void setPaddingRight(String paddingRight)
    {
        this.paddingRight = paddingRight;
    }

    /**
     * Sets the padding right of the column styled by this object, as a percent.
     * 
     * @param paddingRight the padding right (in percents) to set
     */    
    public void setPaddingRightPercent(double paddingRight)
    {
        this.paddingRight = paddingRight + PERCENT;
    }
}
