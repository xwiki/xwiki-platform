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
package org.xwiki.rendering.internal.util.ui;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MacroParameterUINodeParameter extends AbstractMacroParameterUINode
{
    private boolean advanced;
    private boolean deprecated;
    private String displayType;
    private Object defaultValue;
    private boolean caseInsensitive;
    private String editTemplate;

    public MacroParameterUINodeParameter(String id)
    {
        super(MacroParameterUINodeType.PARAMETER, id);
    }

    public boolean isAdvanced()
    {
        return advanced;
    }

    public MacroParameterUINodeParameter setAdvanced(boolean advanced)
    {
        this.advanced = advanced;
        return this;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public MacroParameterUINodeParameter setDeprecated(boolean deprecated)
    {
        this.deprecated = deprecated;
        return this;
    }

    public String getDisplayType()
    {
        return displayType;
    }

    public MacroParameterUINodeParameter setDisplayType(String displayType)
    {
        this.displayType = displayType;
        return this;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public MacroParameterUINodeParameter setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    public MacroParameterUINodeParameter setCaseInsensitive(boolean caseInsensitive)
    {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    public String getEditTemplate()
    {
        return editTemplate;
    }

    public MacroParameterUINodeParameter setEditTemplate(String editTemplate)
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

        MacroParameterUINodeParameter that = (MacroParameterUINodeParameter) o;

        return new EqualsBuilder().appendSuper(super.equals(o))
            .append(advanced, that.advanced).append(deprecated, that.deprecated)
            .append(caseInsensitive, that.caseInsensitive).append(displayType, that.displayType)
            .append(defaultValue, that.defaultValue).append(editTemplate, that.editTemplate).isEquals();
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
