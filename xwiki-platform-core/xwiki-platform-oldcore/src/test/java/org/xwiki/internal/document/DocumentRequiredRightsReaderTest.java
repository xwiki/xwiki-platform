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
package org.xwiki.internal.document;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DocumentRequiredRightsReader}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentRequiredRightsReaderTest
{
    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "RequiredRightClass");

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("wiki", "Space", "Page");

    @InjectMockComponents
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @ParameterizedTest
    @MethodSource("getArguments")
    void readRequiredRights(boolean enforce, List<String> values, DocumentRequiredRights expected)
    {
        XWikiDocument document = mock();
        String objectReferenceName = "objectReference";
        // Object references need other components to be available to compute their name, to avoid these dependencies,
        // the reference is mocked instead.
        BaseObjectReference objectReference = mock(objectReferenceName);
        when(document.isEnforceRequiredRights()).thenReturn(enforce);
        List<BaseObject> objects = values.stream()
            .map(value -> {
                if (value == null) {
                    return null;
                } else {
                    BaseObject object = mock();
                    when(object.getStringValue("level")).thenReturn(value);
                    when(object.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
                    when(object.getReference()).thenReturn(objectReference);
                    return object;
                }
            })
            .toList();
        when(document.getXObjects(CLASS_REFERENCE)).thenReturn(objects);

        DocumentRequiredRights actual = this.documentRequiredRightsReader.readRequiredRights(document);

        assertEquals(expected, actual);

        List<String> invalidValues = values.stream().filter(v -> StringUtils.startsWith(v, "foo")).toList();
        for (int i = 0; i < invalidValues.size(); i++) {
            assertEquals("Illegal required right value [%s] in object [%s]".formatted(invalidValues.get(i),
                objectReferenceName), this.logCapture.getMessage(i));
        }
    }

    static Stream<Arguments> getArguments()
    {
        return Stream.of(
            Arguments.of(
                false,
                List.of(),
                DocumentRequiredRights.EMPTY
            ),
            Arguments.of(
                true,
                List.of(),
                new DocumentRequiredRights(true, Set.of())
            ),
            Arguments.of(
                true,
                List.of("programming"),
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.PROGRAM, null)))
            ),
            Arguments.of(
                true,
                List.of("edit"),
                new DocumentRequiredRights(true, Set.of())
            ),
            Arguments.of(
                true,
                List.of("wiki_admin"),
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI)))
            ),
            Arguments.of(
                true,
                Arrays.asList(null, "script"),
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)))
            ),
            Arguments.of(
                true,
                Arrays.asList(null, "admin", null),
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.SPACE)))
            ),
            Arguments.of(
                true,
                Arrays.asList("illegal", "foo_bar", Right.SCRIPT.getName()),
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)))
            ),
            Arguments.of(
                true,
                Arrays.asList("block_admin", "wiki_creator", "foo_script", "wiki_illegal"),
                new DocumentRequiredRights(true, Set.of(
                    new DocumentRequiredRight(Right.ADMIN, EntityType.SPACE),
                    new DocumentRequiredRight(Right.CREATOR, EntityType.DOCUMENT)
                ))
            )
        );
    }
}
