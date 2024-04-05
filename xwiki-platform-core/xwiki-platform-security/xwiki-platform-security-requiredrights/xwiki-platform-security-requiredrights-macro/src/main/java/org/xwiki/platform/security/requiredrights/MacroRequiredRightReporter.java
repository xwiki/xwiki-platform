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

import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Interface for reporting required rights and analyzing macro contents recursively.
 * <p>
 * The aim of this interface is to allow macro implementations to report required rights to the caller to avoid
 * explicit dependencies on the required rights API.
 * </p>
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Unstable
public interface MacroRequiredRightReporter
{
    /**
     * Reports the given required rights to the caller.
     *
     * @param macroBlock the macro block, used to determine metadata like the entity reference but also to show in
     * the details of the required rights analysis result
     * @param requiredRights the required rights
     * @param summaryTranslationKey the translation key for the summary
     * @param translationParameters the translation parameters for the summary
     */
    void report(MacroBlock macroBlock, List<MacroRequiredRight> requiredRights, String summaryTranslationKey,
        Object... translationParameters);

    /**
     * Analyzes the given content recursively.
     *
     * @param macroBlock the macro block, used to determine metadata like the entity reference
     * @param content the content to analyze
     */
    void analyzeContent(MacroBlock macroBlock, String content);

    /**
     * Analyzes the given content recursively.
     *
     * @param macroBlock the macro block, used to determine metadata like the entity reference
     * @param content the parsed content to analyze
     * @param syntax the syntax of the content
     */
    void analyzeContent(MacroBlock macroBlock, String content, Syntax syntax);
}
