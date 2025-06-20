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
package org.xwiki.wysiwyg.macro;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Represents a single parameter of the macro descriptor.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
@Unstable
public class MacroUINodeParameter extends AbstractMacroUINode
{
    private boolean advanced;

    private boolean deprecated;

    private String displayType;

    private Object defaultValue;

    private boolean caseInsensitive;

    private String editTemplate;

    /**
     * Default constructor.
     *
     * @param id the identifier of the parameter as indicated in the descriptor.
     */
    public MacroUINodeParameter(String id)
    {
        super(MacroUINodeType.PARAMETER, id);
    }

    /**
     * @return {@code true} if the parameter is advanced.
     */
    public boolean isAdvanced()
    {
        return advanced;
    }

    /**
     * @param advanced see {@link #isAdvanced()}.
     * @return the current instance
     */
    public MacroUINodeParameter setAdvanced(boolean advanced)
    {
        this.advanced = advanced;
        return this;
    }

    /**
     * @return {@code true} if the parameter is deprecated.
     */
    public boolean isDeprecated()
    {
        return deprecated;
    }

    /**
     * @param deprecated see {@link #isDeprecated()}.
     * @return the current instance
     */
    public MacroUINodeParameter setDeprecated(boolean deprecated)
    {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * @return a string serialization of the type used for displaying the parameter.
     */
    public String getDisplayType()
    {
        return displayType;
    }

    /**
     * @param displayType see {@link #getDisplayType()}.
     * @return the current instance
     */
    public MacroUINodeParameter setDisplayType(String displayType)
    {
        this.displayType = displayType;
        return this;
    }

    /**
     * @return the default value of the parameter.
     */
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * @param defaultValue see {@link #getDefaultValue()}.
     * @return the current instance
     */
    public MacroUINodeParameter setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * @return {@code true} if the value is case-insensitive (used for enum values).
     */
    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    /**
     * @param caseInsensitive see {@link #isCaseInsensitive()}
     * @return the current instance
     */
    public MacroUINodeParameter setCaseInsensitive(boolean caseInsensitive)
    {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    /**
     * @return the HTML code of the edit template for that parameter.
     */
    public String getEditTemplate()
    {
        return editTemplate;
    }

    /**
     * @param editTemplate see {@link #getEditTemplate()}
     * @return the current instance
     */
    public MacroUINodeParameter setEditTemplate(String editTemplate)
    {
        this.editTemplate = editTemplate;
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MacroUINodeParameter that = (MacroUINodeParameter) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(advanced, that.advanced)
            .append(deprecated, that.deprecated)
            .append(caseInsensitive, that.caseInsensitive)
            .append(displayType, that.displayType)
            .append(defaultValue, that.defaultValue)
            .append(editTemplate, that.editTemplate)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 69)
            .appendSuper(super.hashCode())
            .append(advanced)
            .append(deprecated)
            .append(displayType)
            .append(defaultValue)
            .append(caseInsensitive)
            .append(editTemplate).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("advanced", advanced)
            .append("deprecated", deprecated)
            .append("displayType", displayType)
            .append("defaultValue", defaultValue)
            .append("caseInsensitive", caseInsensitive)
            .append("editTemplate", editTemplate)
            .toString();
    }
}
