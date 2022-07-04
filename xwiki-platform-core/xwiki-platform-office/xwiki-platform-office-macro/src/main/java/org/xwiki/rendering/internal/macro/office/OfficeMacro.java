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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeResourceViewer;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
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
    private OfficeResourceViewer officeViewer;

    @Inject
    private OfficeServer officeServer;

    /**
     * Used to transform the passed attachment reference macro parameter into a typed {@link AttachmentReference}
     * object.
     */
    @Inject
    @Named("macro")
    private AttachmentReferenceResolver<String> macroAttachmentReferenceResolver;

    @Inject
    @Named("macro")
    private DocumentReferenceResolver<String> macroDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Default constructor.
     */
    public OfficeMacro()
    {
        super("Office Document Viewer", "View office attachments (doc, ppt, xls, odt, odp, ods etc.) inside "
            + "wiki pages without downloading or importing them.", OfficeMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(OfficeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Check if the office server is started and if not, generate an error.
        if (!this.officeServer.getState().equals(OfficeServer.ServerState.CONNECTED)) {
            throw new MacroExecutionException("The wiki needs to be connected to an office server in order to view "
                + "office files. Ask your administrator to configure such a server.");
        }

        ResourceReference resourceReference = getResourceReference(context.getCurrentMacroBlock(), parameters);

        Map<String, Object> viewParameters = new HashMap<String, Object>();
        viewParameters.put("filterStyles", parameters.isFilterStyles());
        viewParameters.put("ownerDocument", getOwnerDocument(context.getCurrentMacroBlock()));

        try {
            return this.officeViewer.createView(resourceReference, viewParameters).getChildren();
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to view office attachment.", e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    private DocumentReference getOwnerDocument(MacroBlock block)
    {
        return this.macroDocumentReferenceResolver.resolve("", block);
    }

    private ResourceReference getResourceReference(MacroBlock block, OfficeMacroParameters parameters)
        throws MacroExecutionException
    {
        ResourceReference resourceReference = parameters.getReference();

        // Default reference is attachment
        // Make sure to provide full reference for attachment
        if (resourceReference == null || resourceReference.getType().equals(ResourceType.ATTACHMENT)
            || !resourceReference.isTyped()) {
            AttachmentReference attachmentReference;

            if (resourceReference == null) {
                // Support former way of indicating the file to view
                String reference = parameters.getAttachment();

                if (StringUtils.isEmpty(reference)) {
                    throw new MacroExecutionException(
                        "You must specify the 'reference' parameter pointing to the office file to display.");
                }

                attachmentReference = this.macroAttachmentReferenceResolver.resolve(reference, block);
            } else {
                attachmentReference =
                    this.macroAttachmentReferenceResolver.resolve(resourceReference.getReference(), block);
            }

            resourceReference = new AttachmentResourceReference(this.serializer.serialize(attachmentReference));
        }

        return resourceReference;
    }
}
