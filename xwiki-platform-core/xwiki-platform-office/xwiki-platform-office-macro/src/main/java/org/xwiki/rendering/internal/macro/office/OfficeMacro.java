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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.rendering.block.Block;
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
     * The component used to resolve the attachment string reference relative to the current document reference.
     */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitStringEntityReferenceResolver;

    /**
     * The component used to get the current document reference.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to view the office attachments.
     */
    @Inject
    private OfficeViewer officeViewer;

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
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
        AttachmentReference attachmentReference =
            new AttachmentReference(explicitStringEntityReferenceResolver.resolve(parameters.getAttachment(),
                EntityType.ATTACHMENT, currentDocumentReference));
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
}
