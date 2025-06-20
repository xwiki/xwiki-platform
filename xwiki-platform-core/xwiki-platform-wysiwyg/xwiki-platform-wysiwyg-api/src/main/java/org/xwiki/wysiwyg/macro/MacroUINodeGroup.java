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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Representation of a group of parameters in the macro config UI.
 * Note that this concept is also used to represent a set of parameters bound to the same feature. If a parameter is
 * only bound to a feature but not to a group, then we consider that this is a "featureOnly" group.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
@Unstable
public class MacroUINodeGroup extends AbstractMacroUINode
{
    private List<String> children;
    private String featureName;
    private boolean feature;
    private boolean featureOnly;

    /**
     * Default constructor.
     * @param id identifier of the group defined in the macro descriptor.
     */
    public MacroUINodeGroup(String id)
    {
        super(MacroUINodeType.GROUP, id);
        this.children = new ArrayList<>();
    }

    /**
     * @return the keys of the node children of the group (see {@link AbstractMacroUINode#getKey()}).
     */
    public List<String> getChildren()
    {
        return children;
    }

    /**
     * @param children see {@link #getChildren()}.
     * @return the current instance
     */
    public MacroUINodeGroup setChildren(List<String> children)
    {
        this.children = children;
        return this;
    }

    /**
     * @return the translated name of the feature if this group also represents a feature (see {@link #isFeature()}),
     * {@code null} otherwise.
     */
    public String getFeatureName()
    {
        return featureName;
    }

    /**
     *
     * @param featureName see {@link #getFeatureName()}
     * @return the current instance
     */
    public MacroUINodeGroup setFeatureName(String featureName)
    {
        this.featureName = featureName;
        return this;
    }

    /**
     * @return {@code true} if the parameters of the group are bound to a feature.
     */
    public boolean isFeature()
    {
        return feature;
    }

    /**
     * @param feature see {@link #isFeature()}
     * @return the current instance
     */
    public MacroUINodeGroup setFeature(boolean feature)
    {
        this.feature = feature;
        return this;
    }

    /**
     * @return {@code true} if the parameters of the group are only bound to a feature, and no group name is provided.
     */
    public boolean isFeatureOnly()
    {
        return featureOnly;
    }

    /**
     * @param featureOnly see {@link #isFeatureOnly()}.
     * @return the current instance
     */
    public MacroUINodeGroup setFeatureOnly(boolean featureOnly)
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

        MacroUINodeGroup that = (MacroUINodeGroup) o;

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
