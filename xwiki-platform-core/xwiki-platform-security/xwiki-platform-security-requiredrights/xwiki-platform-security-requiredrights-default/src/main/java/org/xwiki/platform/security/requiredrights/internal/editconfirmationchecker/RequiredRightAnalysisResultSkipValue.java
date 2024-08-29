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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsEditConfirmationChecker;

/**
 * Holds sub-values, corresponding to a serializable version of {@link RequiredRightAnalysisResult}. They are contained
 * by {@link RequiredRightsChangedResultSkipValue}, and used for the {@link RequiredRightsEditConfirmationChecker}
 * results when previously forced.
 *
 * @version $Id$
 * @since 15.10RC1
 */
public class RequiredRightAnalysisResultSkipValue implements Serializable
{
    private final EntityReference entityReference;

    private final List<RequiredRight> requiredRights;

    private final String summary;

    private final String detailed;

    /**
     * Constructs a new object, for the value of a {@link RequiredRightAnalysisResult}.
     *
     * @param entityReference the entity reference
     * @param requiredRights the list of required rights
     * @param summary the summary message as a string
     * @param detailed the detailed message as a string
     */
    public RequiredRightAnalysisResultSkipValue(EntityReference entityReference, List<RequiredRight> requiredRights,
        String summary, String detailed)
    {

        this.entityReference = entityReference;
        this.requiredRights = requiredRights;
        this.summary = summary;
        this.detailed = detailed;
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

        RequiredRightAnalysisResultSkipValue that = (RequiredRightAnalysisResultSkipValue) object;

        return new EqualsBuilder()
            .append(this.entityReference, that.entityReference)
            .append(this.requiredRights, that.requiredRights)
            .append(this.summary, that.summary)
            .append(this.detailed, that.detailed)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.entityReference)
            .append(this.requiredRights)
            .append(this.summary)
            .append(this.detailed)
            .toHashCode();
    }
}
