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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Analyze an object for required rights and returns a list of the missing required right.
 * <p>To check rights on a whole document, use the {@code RequiredRightAnalyzer<Document>}.
 * <p>To implement an analyzer for a macro or an XObject, implement {@code RequiredRightAnalyzer<MacroBlock>} or
 * {@code RequiredRightAnalyzer<BaseObject>}, respectively, with a hint that corresponds to the macro id or class
 * name. Each analyzer is responsible for recursively analyzing the contents of the object, like nested macros in the
 * content of a macro or in a property of an XObject. The {@code RequiredRightAnalyzer<XDOM>} can be used for this
 * purpose.
 *
 * @param <T> the type of the analyzed object
 * @version $Id$
 * @since 15.9RC1
 */
@Unstable
@Role
public interface RequiredRightAnalyzer<T>
{
    /**
     * @param object the object to analyze
     * @return a list of analysis results
     * @throws RequiredRightsException in case of error during the analysis
     */
    List<RequiredRightAnalysisResult> analyze(T object) throws RequiredRightsException;
}
