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
package org.xwiki.model.validation.edit;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents the aggregated result of a check operation.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Unstable
public class EditConfirmationCheckerResults
{
    private final List<Block> errorMessages = new ArrayList<>();

    private final List<Block> warningMessages = new ArrayList<>();

    /**
     * Appends the check result to the existing list of messages.
     *
     * @param check The EditConfirmationCheckerResult that needs to be appended.
     */
    public void append(EditConfirmationCheckerResult check)
    {
        if (check.isError()) {
            this.errorMessages.add(check.getMessage());
        } else {
            this.warningMessages.add(check.getMessage());
        }
    }

    /**
     * Checks if an error has occurred.
     *
     * @return {@code true} if any of the {@link EditConfirmationCheckerResult} passed to
     *     {@link #append(EditConfirmationCheckerResult)} was an error
     */
    public boolean isError()
    {
        return !this.errorMessages.isEmpty();
    }

    /**
     * @return a {@link List} of {@link Block} objects containing the error messages
     */
    public List<Block> getErrorMessages()
    {
        return this.errorMessages;
    }

    /**
     * @return a {@link List} of {@link Block} objects containing the warning messages
     */
    public List<Block> getWarningMessages()
    {
        return this.warningMessages;
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

        EditConfirmationCheckerResults that = (EditConfirmationCheckerResults) o;

        return new EqualsBuilder()
            .append(this.errorMessages, that.errorMessages)
            .append(this.warningMessages, that.warningMessages)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.errorMessages)
            .append(this.warningMessages)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("isError", isError())
            .append("errorMessages", getErrorMessages())
            .append("warningMessage", getWarningMessages())
            .toString();
    }
}
