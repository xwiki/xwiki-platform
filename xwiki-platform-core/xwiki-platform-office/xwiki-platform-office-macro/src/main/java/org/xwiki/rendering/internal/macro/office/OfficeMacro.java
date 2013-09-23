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
package org.xwiki.rendering.internal.macro.office;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.office.OfficeMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * View office attachments (doc, ppt, xls, odt, odp, ods etc.) inside wiki pages without downloading or importing them.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Named("office")
@Singleton
public class OfficeMacro extends AbstractMacro<OfficeMacroParameters>
{
    /**
     * The component used to view the office attachments.
     */
    @Inject
    private OfficeViewer officeViewer;

    /**
     * Used to transform the passed attachment reference macro parameter into a typed {@link AttachmentReference}
     * object.
     */
    @Inject
    @Named("macro")
    private AttachmentReferenceResolver<String> macroAttachmentReferenceResolver;

    /**
     * Default constructor.
     */
    public OfficeMacro()
    {
        super("Office Document Viewer", "View office attachments (doc, ppt, xls, odt, odp, ods etc.) inside "
            + "wiki pages without downloading or importing them.", OfficeMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public List<Block> execute(OfficeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        AttachmentReference attachmentReference = resolve(context.getCurrentMacroBlock(), parameters);
        Map<String, String> viewParameters =
            Collections.singletonMap("filterStyles", String.valueOf(parameters.isFilterStyles()));
        try {
            return this.officeViewer.createView(attachmentReference, viewParameters).getChildren();
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to view office attachment.", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    /**
     * Transform the value of the 'attachment' macro parameter into a typed {@link AttachmentReference} object.
     * 
     * @param block an XDOM block that can provide meta data to be used to resolve the attachment reference
     * @param parameters the macro parameters, including the 'attachment' parameter
     * @return the typed {@link AttachmentReference} object that points to the office file to display
     * @throws MacroExecutionException if the attachment parameter is not specified or empty
     */
    private AttachmentReference resolve(MacroBlock block, OfficeMacroParameters parameters)
        throws MacroExecutionException
    {
        String reference = parameters.getAttachment();

        if (StringUtils.isEmpty(reference)) {
            throw new MacroExecutionException(
                "You must specify the 'attachment' parameter pointing to the office file to display.");
        }

        return this.macroAttachmentReferenceResolver.resolve(reference, block);
    }
}
