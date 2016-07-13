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
package org.xwiki.rendering.macro.include;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;

/**
 * Legacy code for {@link IncludeMacro}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public privileged aspect IncludeMacroCompatibilityAspect
{
    @Around("call(* org.xwiki.rendering.internal.macro.include.IncludeMacro.execute(..)) && args(parameters)")
    public Object aroundExecute(ProceedingJoinPoint joinPoint, IncludeMacroParameters parameters) throws Throwable
    {
        // Step 1: Perform legacy checks.
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException(
                "You must specify a 'reference' parameter pointing to the entity to include.");
        }
        return joinPoint.proceed(joinPoint.getArgs());
    }
}
