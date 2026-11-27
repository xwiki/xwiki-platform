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
package org.xwiki.wysiwyg.importer;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the default implementation of {@link AttachmentImporter#importAttachment(AttachmentReference, Map)},
 * which is what an importer written before 19.0.0RC1 inherits.
 *
 * @version $Id$
 */
class AttachmentImporterTest
{
    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("my.doc", new DocumentReference("wiki", "Some", "Page"));

    private static final Syntax BLOCKNOTE = new Syntax(new SyntaxType("blocknote", "BlockNote"), "1.0");

    /**
     * An importer written against the 9.8 interface: it implements {@code toHTML} and nothing else.
     */
    private final AttachmentImporter htmlOnlyImporter =
        (attachmentReference, parameters) -> "<p>" + attachmentReference.getName() + "</p>";

    @Test
    void importAttachmentWithoutOutputSyntaxReturnsHTML() throws Exception
    {
        assertEquals("<p>my.doc</p>", this.htmlOnlyImporter.importAttachment(ATTACHMENT_REFERENCE, Map.of()));
    }

    @Test
    void importAttachmentWithAnnotatedXHTMLReturnsHTML() throws Exception
    {
        Map<String, Object> parameters =
            Map.of(AttachmentImporter.OUTPUT_SYNTAX, Syntax.ANNOTATED_XHTML_1_0);
        assertEquals("<p>my.doc</p>", this.htmlOnlyImporter.importAttachment(ATTACHMENT_REFERENCE, parameters));
    }

    @Test
    void importAttachmentWithAnotherSyntaxFails()
    {
        // Returning HTML here would hand BlockNote annotated XHTML to parse as JSON, failing much later.
        Map<String, Object> parameters = Map.of(AttachmentImporter.OUTPUT_SYNTAX, BLOCKNOTE);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> this.htmlOnlyImporter.importAttachment(ATTACHMENT_REFERENCE, parameters));
        assertEquals("This importer doesn't support the [blocknote/1.0] output syntax.", exception.getMessage());
    }

    @Test
    void importAttachmentWithNonSyntaxOutputSyntaxFails()
    {
        Map<String, Object> parameters = Map.of(AttachmentImporter.OUTPUT_SYNTAX, "blocknote/1.0");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.htmlOnlyImporter.importAttachment(ATTACHMENT_REFERENCE, parameters));
        assertEquals("The [outputSyntax] import parameter must be a [org.xwiki.rendering.syntax.Syntax] but was "
            + "[java.lang.String].", exception.getMessage());
    }
}
