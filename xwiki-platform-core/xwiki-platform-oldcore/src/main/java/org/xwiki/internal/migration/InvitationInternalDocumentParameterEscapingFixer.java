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
package org.xwiki.internal.migration;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;

import static java.util.regex.Matcher.quoteReplacement;

/**
 * Fix a content by looking for localization of {@code xe.invitation.internalDocument} and escaping its parameter. The
 * translation fixed by this method is initially introduced by the invitation application but the fix is localed in
 * oldcore so that the fix is applied on pages even if the invitation application has been uninstalled.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component(roles = InvitationInternalDocumentParameterEscapingFixer.class)
@Singleton
public class InvitationInternalDocumentParameterEscapingFixer
{
    private static final Pattern PATTERN = Pattern.compile("(\\{\\{info}}"
        + "\\$services\\.localization\\.render\\('xe\\.invitation\\.internalDocument', \\[)(\"[^\"]+\")(]\\)\\"
        + "{\\{/info}})");

    /**
     * @param content the context to fix
     * @param syntax the syntax of the context to fix (xwiki/2.0 or xwiki/2.1)
     * @return the context with the fix applied, or {@link Optional#empty()} if nothing needs to be fixed
     */
    public Optional<String> fix(String content, Syntax syntax)
    {
        String escapedContent = PATTERN.matcher(content).replaceAll(matchResult -> {
            if (matchResult.group(2).contains("services.rendering.escape")) {
                return matchResult.group();
            } else {
                // Concatenate the various groups of the regex while wrapping the localization argument with a
                // call to the escaping script service.
                String format = String.format("%s$services.rendering.escape(%s, '%s')%s", matchResult.group(1),
                    matchResult.group(2), syntax.toIdString(), matchResult.group(3));
                return quoteReplacement(format);
            }
        });

        if (Objects.equals(escapedContent, content)) {
            return Optional.empty();
        } else {
            return Optional.of(escapedContent);
        }
    }
}
