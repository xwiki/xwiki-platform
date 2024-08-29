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
package org.xwiki.model.validation;

import org.xwiki.component.annotation.Role;

/**
 * Define the configuration of the name strategy to be used.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Role
public interface EntityNameValidationConfiguration
{
    /**
     * @return the name of the current {@link EntityNameValidation} to be used.
     */
    String getEntityNameValidation();

    /**
     * @return {@code true} if the name validation should be used.
     */
    boolean useValidation();

    /**
     * @return {@code true} if the name transformation should be used.
     */
    boolean useTransformation();
}
