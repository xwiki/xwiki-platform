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

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link InvitationInternalDocumentParameterEscapingFixer}.
 *
 * @version $Id$
 */
@ComponentTest
class InvitationInternalDocumentParameterEscapingFixerTest
{
    @InjectMockComponents
    private InvitationInternalDocumentParameterEscapingFixer fixer;

    public static Stream<Arguments> fixSource()
    {
        return Stream.of(
            Arguments.of(
                "nothing to change 1\n"
                    + "{{info}}$services.localization.render('xe.invitation.internalDocument', [$noChange]){{/info}}"
                    + "nothing to change 2\n"
                    + "{{info}}$services.localization.render('xe.invitation.internalDocument', [\"$change\"]){{/info}}"
                    + "nothing to change 3",
                Optional.of(
                    "nothing to change 1\n"
                        + "{{info}}$services.localization.render('xe.invitation.internalDocument', [$noChange]){{/info}}nothing to change 2\n"
                        + "{{info}}$services.localization.render('xe.invitation.internalDocument', [$services.rendering.escape(\"$change\", 'xwiki/2.1')]){{/info}}nothing to change 3"
                )
            ),
            Arguments.of(
                "nothing to change 1\n"
                    + "{{info}}$services.localization.render('xe.invitation.internalDocument', [$noChange]){{/info}}"
                    + "nothing to change 2\n"
                    + "nothing to change 3",
                Optional.empty()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("fixSource")
    void fix(String value, Optional<String> expected)
    {
        assertEquals(expected, this.fixer.fix(value, Syntax.XWIKI_2_1));
    }
}
