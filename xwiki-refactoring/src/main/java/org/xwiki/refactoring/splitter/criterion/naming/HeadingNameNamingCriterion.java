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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * A {@link NamingCriterion} based on the opening heading (if present) of the document.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class HeadingNameNamingCriterion implements NamingCriterion
{
    /**
     * Factory to get xwiki 2.0 syntax renderer.
     */
    private PrintRendererFactory rendererFactory;

    /**
     * {@link DocumentAccessBridge} used to lookup for existing wiki pages and avoid name clashes.
     */
    private DocumentAccessBridge docBridge;

    /**
     * In case if we cannot find a heading name present in the document, we will revert back to
     * {@link PageIndexNamingCriterion}.
     */
    private NamingCriterion mainPageNameAndNumberingNamingCriterion;

    /**
     * A map containing all the document names generated so far. This is used to avoid name clashes.
     */
    private Map<String, String> documentNames;

    /**
     * Name of the base page name.
     */
    private String basePageName;
    
    /**
     * Space name to be used with generated page names.
     */
    private String spaceName;

    /**
     * Flag indicating if each generated page name should be prepended with base page name.
     */
    private boolean prependBasePageName;
    
    /**
     * Constructs a new {@link HeadingNameNamingCriterion}.
     * 
     * @param baseDocumentName name of the document that is being split.
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents.
     * @param rendererFactory {@link PrintRendererFactory} used to get xwiki 2.0 syntax renderer.
     * @param prependBasePageName a flag indicating if each generated page name should be prepended with base page name.
     */
    public HeadingNameNamingCriterion(String baseDocumentName, DocumentAccessBridge docBridge,
        PrintRendererFactory rendererFactory, boolean prependBasePageName)
    {
        this.mainPageNameAndNumberingNamingCriterion = new PageIndexNamingCriterion(baseDocumentName, docBridge);
        this.docBridge = docBridge;
        this.rendererFactory = rendererFactory;
        this.documentNames = new HashMap<String, String>();
        int dot = baseDocumentName.lastIndexOf('.');
        this.spaceName = (dot != -1) ? baseDocumentName.substring(0, dot) : "Main";
        this.basePageName = baseDocumentName.substring(dot + 1);
        this.prependBasePageName = prependBasePageName;
    }

    /**
     * {@inheritDoc}
     */
    public String getDocumentName(XDOM newDoc)
    {
        String documentName = null;
        String prefix = spaceName + ".";
        if (newDoc.getChildren().size() > 0) {
            Block firstChild = newDoc.getChildren().get(0);
            if (firstChild instanceof HeaderBlock) {
                // Clone the header block and remove any unwanted stuff
                Block clonedHeaderBlock = firstChild.clone(new BlockFilter()
                {
                    public List<Block> filter(Block block)
                    {
                        List<Block> blocks = new ArrayList<Block>();
                        if (block instanceof WordBlock || block instanceof SpaceBlock) {
                            blocks.add(block);
                        }
                        return blocks;
                    }
                });
                XDOM xdom = new XDOM(clonedHeaderBlock.getChildren());
                WikiPrinter printer = new DefaultWikiPrinter();
                Listener listener = this.rendererFactory.createRenderer(new Syntax(SyntaxType.XWIKI, "2.0"), printer);
                xdom.traverse(listener);
                documentName = cleanPageName(printer.toString());
            }
        }

        // Fall back if necessary.
        if (null == documentName || documentName.equals("")) {
            documentName = mainPageNameAndNumberingNamingCriterion.getDocumentName(newDoc);
        } else if (prependBasePageName) {
            documentName = prefix + basePageName + INDEX_SEPERATOR + documentName;
        } else {
            documentName = prefix + documentName;
        }

        // Resolve any name clashes.
        String newDocumentName = documentName;
        int localIndex = 0;
        while (documentNames.containsKey(newDocumentName) || docBridge.exists(newDocumentName)) {
            // Append a trailing local index if the page already exists
            newDocumentName = documentName + INDEX_SEPERATOR + (++localIndex);
        }

        // Add the newly generated document name into the pool of generated document names.
        documentNames.put(newDocumentName, newDocumentName);

        return newDocumentName;
    }

    /**
     * Utility method for cleaning out invalid / dangerous characters from page names.
     * 
     * @param originalName the page name to be cleaned.
     * @return the cleaned page name.
     */
    private String cleanPageName(String originalName)
    {
        return originalName.trim().replaceAll("[\\.:]", "-");
    }
}
