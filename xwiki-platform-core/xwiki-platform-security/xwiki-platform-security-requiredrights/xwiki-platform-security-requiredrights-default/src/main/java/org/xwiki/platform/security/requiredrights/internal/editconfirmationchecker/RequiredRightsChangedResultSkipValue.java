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
package org.xwiki.platform.security.requiredrights.internal.editconfirmationchecker;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsChangedResult;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsEditConfirmationChecker;
import org.xwiki.security.authorization.Right;

/**
 * Holds a serializable copy of {@link RequiredRightsChangedResult}, used for
 * {@link RequiredRightsEditConfirmationChecker} results comparison, when previously forced.
 *
 * @version $Id$
 * @since 15.10RC1
 */
public class RequiredRightsChangedResultSkipValue implements Serializable
{
    private final Map<Right, Boolean> addedRights;

    private final Map<Right, Boolean> removedRights;

    private final List<RequiredRightAnalysisResultSkipValue> added;

    private final List<RequiredRightAnalysisResultSkipValue> removed;

    /**
     * Creates a new instance of the RequiredRightsChangedSkipValue class.
     *
     * @param addedRights a map of rights that have been added
     * @param removedRights a map of rights that have been removed
     * @param added a list of serialized {@link RequiredRightAnalysisResult} for the added results
     * @param removed a list of serialized {@link RequiredRightAnalysisResult} for the removed results
     */
    public RequiredRightsChangedResultSkipValue(Map<Right, Boolean> addedRights, Map<Right, Boolean> removedRights,
        List<RequiredRightAnalysisResultSkipValue> added, List<RequiredRightAnalysisResultSkipValue> removed)
    {

        this.addedRights = addedRights;
        this.removedRights = removedRights;
        this.added = added;
        this.removed = removed;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        RequiredRightsChangedResultSkipValue that = (RequiredRightsChangedResultSkipValue) object;

        return new EqualsBuilder()
            .append(this.addedRights, that.addedRights)
            .append(this.removedRights, that.removedRights)
            .append(this.added, that.added)
            .append(this.removed, that.removed)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.addedRights)
            .append(this.removedRights)
            .append(this.added)
            .append(this.removed)
            .toHashCode();
    }
}
