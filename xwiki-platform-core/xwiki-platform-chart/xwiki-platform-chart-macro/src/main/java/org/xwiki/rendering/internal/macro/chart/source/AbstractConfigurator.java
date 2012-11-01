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
 * Abstract superclass for configurators that provides a common method handling for handling invalid parameter values.
 *
 * @version $Id$
 * @since 4.2M1
 */
public abstract class AbstractConfigurator implements Configurator
{
    @Override
    public void validateParameters() throws MacroExecutionException
    {
    }

    /**
     * Indicate that an invalid parameter value was found.
     *
     * @param parameterName The name of the parameter.
     * @param value The value.
     * @throws MacroExecutionException always.
     */
    protected void invalidParameterValue(String parameterName, String value) throws MacroExecutionException
    {
        throw new MacroExecutionException(String.format("Invalid value for parameter [%s]: [%s]",
            parameterName, value));
    }
}
