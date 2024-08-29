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
package org.xwiki.officeimporter;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.script.LegacyOfficeImporterScriptService;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.script.service.ScriptService;

/**
 * A bridge between velocity and office importer.
 * 
 * @version $Id$
 * @since 1.8M1
 * @deprecated since 4.1M1 use the {@link ScriptService} with hint "officeimporter" instead
 */
@Deprecated
public class OfficeImporterVelocityBridge
{
    /**
     * File extensions corresponding to slide presentations.
     */
    public static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "pptx", "odp");

    /**
     * The key used to place any error messages while importing office documents.
     */
    public static final String OFFICE_IMPORTER_ERROR = "OFFICE_IMPORTER_ERROR";

    /**
     * The underlying script service.
     */
    private final ScriptService scriptService;

    /**
     * Default constructor.
     * 
     * @param componentManager used to lookup for other necessary components.
     * @throws OfficeImporterException if an error occurs while looking up for other required components.
     */
    public OfficeImporterVelocityBridge(ComponentManager componentManager) throws OfficeImporterException
    {
        try {
            // We don't cast to the actual implementation here because it complicates the unit test that verifies the
            // bridge initialization. See VelocityContextInitializerTest#testVelocityBridges().
            scriptService = componentManager.getInstance(ScriptService.class, "officeimporter");
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while initializing office importer velocity bridge.", ex);
        }
    }

    /**
     * Casts the {@link ScriptService} to its actual implementation so that we can call its methods.
     * 
     * @return the actual implementation of the script service
     */
    private LegacyOfficeImporterScriptService getScriptService()
    {
        return (LegacyOfficeImporterScriptService) scriptService;
    }

    /**
     * Imports the given office document into an {@link XHTMLOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document.
     * @param officeFileName name of the input office document, this argument is mainly used for determining input
     *            document format where necessary.
     * @param referenceDocument reference wiki document w.r.t which import process is carried out. This argument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document.
     * @param filterStyles whether to filter styling information associated with the office document's content or not.
     * @return {@link XHTMLOfficeDocument} containing xhtml result of the import operation or null if an error occurs.
     * @since 2.2M1
     */
    public XHTMLOfficeDocument officeToXHTML(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        return getScriptService().officeToXHTML(officeFileStream, officeFileName, referenceDocument, filterStyles);
    }

    /**
     * Imports the given {@link XHTMLOfficeDocument} into an {@link XDOMOfficeDocument}.
     * 
     * @param xhtmlOfficeDocument {@link XHTMLOfficeDocument} to be imported.
     * @return {@link XDOMOfficeDocument} containing {@link org.xwiki.rendering.block.XDOM} result of the import
     *         operation or null if an error occurs.
     * @since 2.2M1
     */
    public XDOMOfficeDocument xhtmlToXDOM(XHTMLOfficeDocument xhtmlOfficeDocument)
    {
        return getScriptService().xhtmlToXDOM(xhtmlOfficeDocument);
    }

    /**
     * Imports the given office document into an {@link XDOMOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document.
     * @param officeFileName name of the input office document, this argument is mainly is used for determining input
     *            document format where necessary.
     * @param referenceDocument reference wiki document w.r.t which import process is carried out. This srgument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document.
     * @param filterStyles whether to filter styling information associated with the office document's content or not.
     * @return {@link XDOMOfficeDocument} containing {@link org.xwiki.rendering.block.XDOM} result of the import
     *         operation or null if an error occurs.
     * @since 2.2M1
     */
    public XDOMOfficeDocument officeToXDOM(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        return getScriptService().officeToXDOM(officeFileStream, officeFileName, referenceDocument, filterStyles);
    }

    /**
     * Splits the given {@link XDOMOfficeDocument} into multiple {@link XDOMOfficeDocument} instances according to the
     * specified criterion. This method is useful when a single office document has to be imported and split into
     * multiple wiki pages. An auto generated TOC structure will be returned associated to <b>rootDocumentName</b>
     * {@link org.xwiki.officeimporter.splitter.TargetDocumentDescriptor} entry.
     * 
     * @param xdomDocument {@link XDOMOfficeDocument} to be split.
     * @param headingLevels heading levels defining the split points on the original document.
     * @param namingCriterionHint hint indicating the child pages naming criterion.
     * @param rootDocumentName name of the root document w.r.t which splitting will occur. In the results set the entry
     *            corresponding to <b>rootDocumentName</b> {@link TargetDocumentDescriptor} will hold an auto-generated
     *            TOC structure.
     * @return a map holding {@link XDOMOfficeDocument} fragments against corresponding {@link TargetDocumentDescriptor}
     *         instances or null if an error occurs.
     * @since 2.2M1
     */
    public Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument xdomDocument,
        String[] headingLevels, String namingCriterionHint, String rootDocumentName)
    {
        return getScriptService().split(xdomDocument, headingLevels, namingCriterionHint, rootDocumentName);
    }

    /**
     * Attempts to save the given {@link XDOMOfficeDocument} into the target wiki page specified by arguments.
     * 
     * @param doc {@link XDOMOfficeDocument} to be saved.
     * @param target name of the target wiki page.
     * @param syntaxId syntax of the target wiki page.
     * @param parent name of the parent wiki page or null.
     * @param title title of the target wiki page or null.
     * @param append whether to append content if the target wiki page exists.
     * @return true if the operation completes successfully, false otherwise.
     */
    public boolean save(XDOMOfficeDocument doc, String target, String syntaxId, String parent, String title,
        boolean append)
    {
        return getScriptService().save(doc, target, syntaxId, parent, title, append);
    }

    /**
     * @return an error message set inside current execution (during import process) or null.
     */
    public String getErrorMessage()
    {
        return getScriptService().getErrorMessage();
    }
}
