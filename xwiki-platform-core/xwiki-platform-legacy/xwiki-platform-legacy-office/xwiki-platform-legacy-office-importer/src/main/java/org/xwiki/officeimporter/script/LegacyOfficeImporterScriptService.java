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
package org.xwiki.officeimporter.script;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;

/**
 * Legacy version of {@link OfficeImporterScriptService}, holding deprecated methods.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
@Component
@Named("officeimporter")
@Singleton
public class LegacyOfficeImporterScriptService extends OfficeImporterScriptService
{
    /**
     * Used for converting string document names to objects.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    /**
     * Imports the given office document into an {@link XHTMLOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document
     * @param officeFileName name of the input office document, this argument is mainly used for determining input
     *            document format where necessary
     * @param referenceDocument reference wiki document w.r.t which import process is carried out; this argument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document
     * @param filterStyles whether to filter styling information associated with the office document's content or not
     * @return {@link XHTMLOfficeDocument} containing xhtml result of the import operation or null if an error occurs
     * @since 2.2M1
     * @deprecated use {@link #officeToXHTML(InputStream, String, DocumentReference, boolean)} instead
     */
    public XHTMLOfficeDocument officeToXHTML(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        return officeToXHTML(officeFileStream, officeFileName,
            this.currentMixedDocumentReferenceResolver.resolve(referenceDocument), filterStyles);
    }

    /**
     * Imports the given office document into an {@link XDOMOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document
     * @param officeFileName name of the input office document, this argument is mainly is used for determining input
     *            document format where necessary
     * @param referenceDocument reference wiki document w.r.t which import process is carried out; this srgument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document
     * @param filterStyles whether to filter styling information associated with the office document's content or not
     * @return {@link XDOMOfficeDocument} containing {@link org.xwiki.rendering.block.XDOM} result of the import
     *         operation or null if an error occurs
     * @deprecated use {@link #officeToXDOM(InputStream, String, DocumentReference, boolean)} instead
     */
    public XDOMOfficeDocument officeToXDOM(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        return officeToXDOM(officeFileStream, officeFileName,
            this.currentMixedDocumentReferenceResolver.resolve(referenceDocument), filterStyles);
    }

    /**
     * Attempts to save the given {@link XDOMOfficeDocument} into the target wiki page specified by arguments.
     * 
     * @param doc {@link XDOMOfficeDocument} to be saved
     * @param target name of the target wiki page
     * @param syntaxId syntax of the target wiki page
     * @param parent name of the parent wiki page or null
     * @param title title of the target wiki page or null
     * @param append whether to append content if the target wiki page exists
     * @return true if the operation completes successfully, false otherwise
     * @deprecated use {@link #save(XDOMOfficeDocument, DocumentReference, String, DocumentReference, String, boolean)}
     *             instead
     */
    public boolean save(XDOMOfficeDocument doc, String target, String syntaxId, String parent, String title,
        boolean append)
    {
        return save(doc, this.currentMixedDocumentReferenceResolver.resolve(target), syntaxId,
            this.currentMixedDocumentReferenceResolver.resolve(parent), title, append);
    }

    /**
     * Splits the given {@link XDOMOfficeDocument} into multiple {@link XDOMOfficeDocument} instances according to the
     * specified criterion. This method is useful when a single office document has to be imported and split into
     * multiple wiki pages. An auto generated TOC structure will be returned associated to <b>rootDocumentName</b>
     * {@link TargetDocumentDescriptor} entry.
     * 
     * @param xdomDocument {@link XDOMOfficeDocument} to be split
     * @param headingLevels heading levels defining the split points on the original document
     * @param namingCriterionHint hint indicating the child pages naming criterion
     * @param rootDocumentName name of the root document w.r.t which splitting will occur; in the results set the entry
     *            corresponding to <b>rootDocumentName</b> {@link TargetDocumentDescriptor} will hold an auto-generated
     *            TOC structure
     * @return a map holding {@link XDOMOfficeDocument} fragments against corresponding {@link TargetDocumentDescriptor}
     *         instances or null if an error occurs
     * @deprecated use {@link #split(XDOMOfficeDocument, String[], String, DocumentReference)} instead
     */
    public Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument xdomDocument,
        String[] headingLevels, String namingCriterionHint, String rootDocumentName)
    {
        return split(xdomDocument, headingLevels, namingCriterionHint,
            this.currentMixedDocumentReferenceResolver.resolve(rootDocumentName));
    }
}
