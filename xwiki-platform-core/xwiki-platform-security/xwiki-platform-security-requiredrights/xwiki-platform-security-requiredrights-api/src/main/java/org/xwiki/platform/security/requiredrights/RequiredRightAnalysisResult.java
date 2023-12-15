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
package org.xwiki.platform.security.requiredrights;

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents the result of a required right analysis. This class provides getters and setters for various properties
 * related to the analysis result.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Unstable
public class RequiredRightAnalysisResult
{
    private final EntityReference entityReference;

    private final Supplier<Block> summaryMessageProvider;

    private final Supplier<Block> detailedMessageProvider;

    private final List<RequiredRight> requiredRights;

    /**
     * @param entityReference the location of the analyzed entity (e.g., a document content, or a field in an
     *     XObject)
     * @param summaryMessageProvider the summary message to display to the user for this required right analysis
     *     result
     * @param detailedMessageProvider the detailed message to display to the user for this required right analysis
     *     result
     * @param requiredRights the rights that are required for the analyzed entity
     */
    public RequiredRightAnalysisResult(EntityReference entityReference, Supplier<Block> summaryMessageProvider,
        Supplier<Block> detailedMessageProvider, List<RequiredRight> requiredRights)
    {
        this.entityReference = entityReference;
        this.summaryMessageProvider = summaryMessageProvider;
        this.detailedMessageProvider = detailedMessageProvider;
        this.requiredRights = requiredRights;
    }

    /**
     * @return the location of the analyzed entity (e.g., a document content, or a field in an XObject)
     */
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    /**
     * @return the summary message to display to the user for this required right analysis result
     */
    public Block getSummaryMessage()
    {
        return this.summaryMessageProvider.get();
    }

    /**
     * @return the detailed message to display to the user for this required right analysis result
     */
    public Block getDetailedMessage()
    {
        return this.detailedMessageProvider.get();
    }

    /**
     * @return the rights that are required for the analyzed entity
     */
    public List<RequiredRight> getRequiredRights()
    {
        return this.requiredRights;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("entityReference", this.getEntityReference())
            .append("summaryMessageProvider", this.getSummaryMessage())
            .append("detailedMessageProvider", this.getDetailedMessage())
            .append("requiredRights", this.getRequiredRights())
            .toString();
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

        RequiredRightAnalysisResult that = (RequiredRightAnalysisResult) object;

        return new EqualsBuilder()
            .append(this.entityReference, that.entityReference)
            .append(this.requiredRights, that.requiredRights)
            .append(this.summaryMessageProvider, that.summaryMessageProvider)
            .append(this.detailedMessageProvider, that.detailedMessageProvider)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.entityReference)
            .append(this.requiredRights)
            .append(this.summaryMessageProvider)
            .append(this.detailedMessageProvider)
            .toHashCode();
    }
}
