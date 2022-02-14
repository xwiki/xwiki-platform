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
package org.xwiki.rendering.internal.macro.chart;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.chart.ChartMacroParameters;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link ImageId}.
 *
 * @version $Id$
 * @since 4.2M1
 */
class ImageIdTest
{
    /**
     * Verify id are different even when using same parameters (but different instances of parameters).
     */
    @Test
    void getIdWithSameParametersButDifferentInstances()
    {
        ChartMacroParameters parameters1 = new ChartMacroParameters();
        ChartMacroParameters parameters2 = new ChartMacroParameters();

        assertFalse(new ImageId(parameters1).getId().equals(new ImageId(parameters2).getId()));
    }
}
