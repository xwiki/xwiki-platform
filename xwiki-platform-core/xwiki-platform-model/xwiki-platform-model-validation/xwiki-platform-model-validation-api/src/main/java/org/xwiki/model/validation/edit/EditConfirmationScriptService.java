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
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * This class provides the script services for handling document edit confirmation.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named("editConfirmation")
@Unstable
public class EditConfirmationScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Performs a check by invoking the check method of all available {@link EditConfirmationChecker} components and
     * aggregating their results.
     *
     * @param editForced a boolean value indicating whether the check is forced
     * @return a {@link CheckResults} object containing the results of the check
     */
    public CheckResults check(Boolean editForced)
    {
        try {
            CheckResults result = new CheckResults();
            boolean unboxedEditForced = Optional.ofNullable(editForced).orElse(false);
            this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class)
                .stream()
                .flatMap(checker -> checker.check(unboxedEditForced).stream())
                .forEach(result::append);
            return result;
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to resolve the list of [{}]. Cause: [{}]", EditConfirmationChecker.class,
                getRootCauseMessage(e));
            return new CheckResults();
        }
    }

    /**
     * Represents the aggregated result of a check operation.
     */
    public static class CheckResults
    {
        private boolean isError;

        private final List<Block> messages = new ArrayList<>();

        /**
         * Appends the check result to the existing list of messages.
         *
         * @param check The EditConfirmationCheckerResult that needs to be appended.
         */
        public void append(EditConfirmationCheckerResult check)
        {
            if (check.isError()) {
                this.isError = true;
            }

            this.messages.add(
                new GroupBlock(List.of(check.getMessage()), Map.of("class",
                    String.format("box %smessage", check.isError() ? "error" : "warning"))));
        }

        /**
         * Checks if an error has occurred.
         *
         * @return {@code true} if any of the {@link EditConfirmationCheckerResult} passed to
         *     {@link #append(EditConfirmationCheckerResult)} was an error
         */
        public boolean isError()
        {
            return this.isError;
        }

        /**
         * Retrieves the list of messages.
         *
         * @return the list of aggregated messages of the {@link EditConfirmationCheckerResult} passed to
         *     {@link #append(EditConfirmationCheckerResult)}
         */
        public List<Block> getMessages()
        {
            return this.messages;
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

            CheckResults that = (CheckResults) o;

            return new EqualsBuilder()
                .append(this.isError, that.isError)
                .append(this.messages, that.messages)
                .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37)
                .append(this.isError)
                .append(this.messages)
                .toHashCode();
        }

        @Override
        public String toString()
        {
            return new XWikiToStringBuilder(this)
                .append("isError", isError())
                .append("messages", getMessages())
                .toString();
        }
    }
}
