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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Interface used to import existing attachments into the content of a WYSIWYG editor.
 *
 * @version $Id$
 * @since 9.8
 */
@Role
public interface AttachmentImporter
{
    /**
     * The import parameter holding the {@link Syntax} the attachment content is converted to.
     *
     * @since 19.0.0RC1
     */
    @Unstable
    String OUTPUT_SYNTAX = "outputSyntax";

    /**
     * Generates the HTML needed to import the specified attachment.
     *
     * @param attachmentReference specifies the attachment to import
     * @param parameters import parameters
     * @return the HTML to insert into the content of the WYSIWIYG editor
     * @throws Exception if importing the specified attachment fails
     * @deprecated use {@link #importAttachment(AttachmentReference, Map)} instead
     */
    @Deprecated(since = "19.0.0RC1")
    String toHTML(AttachmentReference attachmentReference, Map<String, Object> parameters) throws Exception;

    /**
     * Generates the content needed to import the specified attachment, in the syntax asked for by the
     * {@link #OUTPUT_SYNTAX} parameter.
     *
     * @param attachmentReference specifies the attachment to import
     * @param parameters import parameters; {@link #OUTPUT_SYNTAX} holds the {@link Syntax} to convert the attachment
     *     content to, {@link Syntax#ANNOTATED_XHTML_1_0} by default
     * @return the content to insert into the content of the WYSIWYG editor
     * @throws Exception if importing the specified attachment fails
     * @throws UnsupportedOperationException if this importer cannot produce the requested syntax
     * @since 19.0.0RC1
     */
    @Unstable
    default String importAttachment(AttachmentReference attachmentReference, Map<String, Object> parameters)
        throws Exception
    {
        Object outputSyntax = parameters.get(OUTPUT_SYNTAX);
        if (outputSyntax == null || Syntax.ANNOTATED_XHTML_1_0.equals(outputSyntax)) {
            return toHTML(attachmentReference, parameters);
        }
        // Syntax#toString gives the display name rather than the identifier a caller passes.
        String outputSyntaxId =
            outputSyntax instanceof Syntax syntax ? syntax.toIdString() : String.valueOf(outputSyntax);
        throw new UnsupportedOperationException(
            String.format("This importer doesn't support the [%s] output syntax.", outputSyntaxId));
    }
}
