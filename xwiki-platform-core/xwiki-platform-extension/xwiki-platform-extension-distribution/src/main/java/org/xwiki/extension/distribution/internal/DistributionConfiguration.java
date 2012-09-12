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
package org.xwiki.extension.distribution.internal;

import org.xwiki.component.annotation.Role;

/**
 * Configuration properties for the distribution module.
 * 
 * @version $Id$
 */
@Role
public interface DistributionConfiguration
{
    /**
     * When this property is {@code false} any page view should be redirected to the distribution wizard if the
     * distribution job is running. If {@code true} the distribution wizard is accessible only by using the distribution
     * action.
     * <p>
     * This property is needed mostly by functional tests that don't want to handle the distribution wizard.
     * 
     * @return {@code false} if the distribution wizard is displayed whenever the distribution job is running and an
     *         administrator views a wiki page, {@code true} otherwise
     */
    boolean isWizardSkipped();
}
