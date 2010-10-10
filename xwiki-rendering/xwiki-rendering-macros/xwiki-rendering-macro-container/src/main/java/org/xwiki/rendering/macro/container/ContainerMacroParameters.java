package org.xwiki.rendering.macro.container;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters of the container macro, specifying the layout of the container, for the moment. To be completed with other
 * properties for the container.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class ContainerMacroParameters
{
    /**
     * Flag specifying whether the groups inside this macro are displayed as justified or not.
     */
    private boolean justify;

    /**
     * The style of the layout of this container.
     */
    private String layoutStyle;

    /**
     * @return the string identifying the layout style for this container.
     */
    public String getLayoutStyle()
    {
        return layoutStyle;
    }

    /**
     * Sets the layout style of this container.
     * 
     * @param layoutStyle the style to set, e.g. {@code columns}
     */
    @PropertyDescription("The style of the container layout (e.g. columns)")
    public void setLayoutStyle(String layoutStyle)
    {
        this.layoutStyle = layoutStyle;
    }

    /**
     * @return {@code true} whether the content in this container is justified, {@code false} otherwise
     */
    public boolean isJustify()
    {
        return justify;
    }

    /**
     * Set if the content in this container is justified.
     * 
     * @param justify {@code true} whether the content in this container is aligned "justify", {@code false} otherwise
     */
    @PropertyDescription("Flag specifying whether the content in this container is justified or not.")
    public void setJustify(boolean justify)
    {
        this.justify = justify;
    }
}
