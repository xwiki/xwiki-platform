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
package org.xwiki.rendering.internal.macro.code.source;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.internal.parser.pygments.PygmentsUtils;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Component(hints = {"ATTACHMENT", "PAGE_ATTACHMENT"})
@Singleton
public class DocumentAttachmentCodeMacroSourceLoader implements EntityCodeMacroSourceLoader
{
    @Override
    public CodeMacroSource load(XWikiDocument document, EntityReference entityReference,
        CodeMacroSourceReference reference, XWikiContext xcontext) throws MacroExecutionException
    {
        XWikiAttachment attachment = document.getAttachment(entityReference.getName());

        if (attachment == null) {
            throw new MacroExecutionException("Unknown attachment [" + entityReference + "]");
        }

        String content;
        try (InputStream stream = attachment.getContentInputStream(xcontext)) {
            // TODO: we would ideally need to store the encoding of the file along with the mime type
            content = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to read content of attachment [" + entityReference + "]", e);
        }

        return new CodeMacroSource(reference, content, PygmentsUtils.mimetypeToLanguage(attachment.getMimeType()));
    }
}
