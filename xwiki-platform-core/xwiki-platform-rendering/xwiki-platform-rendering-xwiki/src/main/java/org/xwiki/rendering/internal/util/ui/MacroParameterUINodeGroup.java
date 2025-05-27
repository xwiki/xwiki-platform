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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MacroParameterUINodeGroup extends AbstractMacroParameterUINode
{
    private List<String> children;
    private String featureName;
    private boolean feature;
    private boolean featureOnly;

    public MacroParameterUINodeGroup(String id)
    {
        super(MacroParameterUINodeType.GROUP, id);
        this.children = new ArrayList<>();
    }

    public List<String> getChildren()
    {
        return children;
    }

    public MacroParameterUINodeGroup setChildren(List<String> children)
    {
        this.children = children;
        return this;
    }

    public String getFeatureName()
    {
        return featureName;
    }

    public MacroParameterUINodeGroup setFeatureName(String featureName)
    {
        this.featureName = featureName;
        return this;
    }

    public boolean isFeature()
    {
        return feature;
    }

    public MacroParameterUINodeGroup setFeature(boolean feature)
    {
        this.feature = feature;
        return this;
    }

    public boolean isFeatureOnly()
    {
        return featureOnly;
    }

    public MacroParameterUINodeGroup setFeatureOnly(boolean featureOnly)
    {
        this.featureOnly = featureOnly;
        return this;
    }

    @Override
    public String getKey()
    {
        if (isFeatureOnly()) {
            return "FEATURE:" + getId();
        } else {
            return super.getKey();
        }
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

        MacroParameterUINodeGroup that = (MacroParameterUINodeGroup) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(feature, that.feature)
            .append(children, that.children)
            .append(featureName, that.featureName)
            .append(featureOnly, that.featureOnly)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63)
            .appendSuper(super.hashCode())
            .append(children)
            .append(featureName)
            .append(feature)
            .append(featureOnly)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("children", children)
            .append("featureName", featureName)
            .append("feature", feature)
            .append("featureOnly", featureOnly)
            .toString();
    }
}
