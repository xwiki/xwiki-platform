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
package org.xwiki.security.authorization.internal;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * Check if the required rights have been deactivated through the execution context.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@Component(roles = RequiredRightsSkipContext.class)
@Singleton
public class RequiredRightsSkipContext
{
    /**
     * Identifier of the context key for the required rights skipping value. When {@code "true"}, the required rights
     * are not taken into account when computing the rights.
     */
    public static final String SKIP_REQUIRED_RIGHT = "skipRequiredRight";

    @Inject
    private Execution execution;

    /**
     * @return {@code true} when the required rights are skipped, {@code false} otherwise
     */
    public boolean isRequiredRightsSkipped()
    {
        ExecutionContext context = this.execution.getContext();
        return context != null
            && context.hasProperty(SKIP_REQUIRED_RIGHT)
            && Objects.equals(context.getProperty(SKIP_REQUIRED_RIGHT), String.valueOf(Boolean.TRUE));
    }
}
