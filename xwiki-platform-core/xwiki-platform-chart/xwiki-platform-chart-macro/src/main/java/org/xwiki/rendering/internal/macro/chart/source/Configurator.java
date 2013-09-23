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
package org.xwiki.rendering.internal.macro.chart.source;

import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Configurator interface.
 *
 * Currently, the parameter handling in the Chart macro is a mess, whith most things thrown into the generic "param"
 * parameter.  The configurator interface is used by classes that use the macro parameters for configuration.
 *
 * All parameters will be set by calling setParameter in all configurators before the validation is performed.
 *
 * @version $Id$
 * @since 4.2M1
 */
public interface Configurator
{
    /**
     * Let an implementation set a parameter.
     *
     * @param key The key of the parameter.
     * @param value The value of the parameter.
     * @return {@code true} if the parameter was claimed.
     * @throws MacroExecutionException if the parameter is not supported by the data source.
     */
    boolean setParameter(String key, String value) throws MacroExecutionException;

    /**
     * Let an implementation validate the value of the previously set parameters, and set default values.
     *
     * @throws MacroExecutionException if the previously set value is invalid.
     */
    void validateParameters() throws MacroExecutionException;
}
