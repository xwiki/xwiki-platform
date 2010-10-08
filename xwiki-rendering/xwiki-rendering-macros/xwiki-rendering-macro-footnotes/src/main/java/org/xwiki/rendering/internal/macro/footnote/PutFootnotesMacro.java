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
package org.xwiki.rendering.internal.macro.footnote;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.footnote.FootnoteMacroParameters;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * List footnotes at the end of the page.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component(PutFootnotesMacro.MACRO_NAME)
public class PutFootnotesMacro extends AbstractMacro<FootnoteMacroParameters>
{
    /** The name of this macro. */
    public static final String MACRO_NAME = "putFootnotes";

    /** The description of the macro. */
    private static final String DESCRIPTION =
        "Displays the footnotes defined so far."
            + " If missing, all footnotes are displayed by default at the end of the page.";

    /** ID attribute name. */
    private static final String ID_ATTRIBUTE_NAME = "id";

    /** CSS Class attribute name. */
    private static final String CLASS_ATTRIBUTE_NAME = "class";

    /** Prefix for the ID of the reference link to the footnote. */
    private static final String FOOTNOTE_ID_PREFIX = "x_footnote_";

    /** Prefix for the ID of the footnote. */
    private static final String FOOTNOTE_REFERENCE_ID_PREFIX = "x_footnote_ref_";

    /** Used to get the current syntax parser. */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public PutFootnotesMacro()
    {
        super("Put Footnote", DESCRIPTION, FootnoteMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(FootnoteMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        List<Block> result = Collections.emptyList();

        // Get the list of footnotes in the document
        Block root = context.getXDOM();
        List<MacroMarkerBlock> footnotes = root.getChildrenByType(MacroMarkerBlock.class, true);
        for (ListIterator<MacroMarkerBlock> it = footnotes.listIterator(); it.hasNext();) {
            MacroMarkerBlock macro = it.next();
            if (FootnoteMacro.MACRO_NAME.equals(macro.getName())) {
                continue;
            } else if (PutFootnotesMacro.MACRO_NAME.equals(macro.getName())) {
                macro.getParent().replaceChild(Collections.<Block> emptyList(), macro);
            }
            it.remove();
        }
        if (footnotes.size() <= 0) {
            return result;
        }

        NumberedListBlock container = new NumberedListBlock(Collections.<Block> emptyList());
        container.setParameter(CLASS_ATTRIBUTE_NAME, "footnotes");
        Block footnoteResult;

        int counter = 1;
        for (MacroMarkerBlock footnote : footnotes) {
            footnoteResult = processFootnote(footnote, counter, context);
            if (footnoteResult != null) {
                container.addChild(footnoteResult);
                counter++;
            }
        }

        return Collections.<Block> singletonList(container);
    }

    /**
     * Processes a {{footnote}} macro, by generating a footnote element to insert in the footnote list and a reference
     * to it, which is placed instead of the macro call.
     * 
     * @param footnoteMacro the {{footnote}} macro element
     * @param counter the current footnote counter
     * @param context the execution context of the macro
     * @return the footnote element which should be inserted in the footnote list
     * @throws MacroExecutionException if the footnote content cannot be further processed
     */
    private ListItemBlock processFootnote(MacroMarkerBlock footnoteMacro, int counter,
        MacroTransformationContext context) throws MacroExecutionException
    {
        String content = footnoteMacro.getContent();
        if (StringUtils.isBlank(content)) {
            content = " ";
        }
        // Construct the footnote and reference blocks
        Block referenceBlock = createFootnoteReferenceBlock(counter);
        ListItemBlock footnoteBlock = createFootnoteBlock(content, counter, context);
        // Insert the footnote and the reference in the document.
        if (referenceBlock != null && footnoteBlock != null) {
            addFootnoteRef(footnoteMacro, referenceBlock);
            return footnoteBlock;
        }
        return null;
    }

    /**
     * Add a footnote to the list of document footnotes. If such a list doesn't exist yet, create it and append it to
     * the end of the document.
     * 
     * @param footnoteMacro the {{footnote}} macro being processed
     * @param footnoteRef the generated block corresponding to the footnote to be inserted
     */
    private void addFootnoteRef(MacroMarkerBlock footnoteMacro, Block footnoteRef)
    {
        for (ListIterator<Block> it = footnoteMacro.getChildren().listIterator(); it.hasNext();) {
            Block b = it.next();
            it.remove();
            // b.replace(Collections.<Block> emptyList());
        }
        footnoteMacro.addChild(footnoteRef);
    }

    /**
     * Generate the footnote reference (link) that should be inserted at the location of the macro, and should point to
     * the actual footnote at the end of the document.
     * 
     * @param counter the current footnote counter
     * @return the generated reference element, displayed as {@code (superscript(link(footnote index)))}
     */
    private Block createFootnoteReferenceBlock(int counter)
    {
        Block result = new WordBlock(counter + "");
        DocumentResourceReference reference = new DocumentResourceReference(null);
        reference.setAnchor(FOOTNOTE_ID_PREFIX + counter);
        result = new LinkBlock(Collections.singletonList(result), reference, false);
        result = new FormatBlock(Collections.singletonList(result), Format.SUPERSCRIPT);
        ((FormatBlock) result).setParameter(ID_ATTRIBUTE_NAME, FOOTNOTE_REFERENCE_ID_PREFIX + counter);
        ((FormatBlock) result).setParameter(CLASS_ATTRIBUTE_NAME, "footnoteRef");
        return result;
    }

    /**
     * Generate the footnote block, a numbered list item containing a backlink to the footnote's reference, and the
     * actual footnote text, parsed into XDOM.
     * 
     * @param content the string representation of the actual footnote text; the content of the macro
     * @param counter the current footnote counter
     * @param context the macro transformation context, used for obtaining the correct parser for parsing the content
     * @return the generated footnote block
     * @throws MacroExecutionException if parsing the content fails
     */
    private ListItemBlock createFootnoteBlock(String content, int counter, MacroTransformationContext context)
        throws MacroExecutionException
    {
        Parser parser = getSyntaxParser(context);
        List<Block> parsedContent = Collections.emptyList();
        try {
            parsedContent = parser.parse(new StringReader(content)).getChildren();
            if (parsedContent.size() > 0 && parsedContent.get(0) instanceof ParagraphBlock) {
                Block firstBlock = parsedContent.remove(0);
                parsedContent.addAll(0, firstBlock.getChildren());
            }
        } catch (ParseException e) {
            parsedContent = Collections.<Block> singletonList(new WordBlock(content));
        }
        Block result = new WordBlock("^");
        DocumentResourceReference reference = new DocumentResourceReference(null);
        reference.setAnchor(FOOTNOTE_REFERENCE_ID_PREFIX + counter);
        result = new LinkBlock(Collections.singletonList(result), reference, false);
        ((LinkBlock) result).setParameter(ID_ATTRIBUTE_NAME, FOOTNOTE_ID_PREFIX + counter);
        ((LinkBlock) result).setParameter(CLASS_ATTRIBUTE_NAME, "footnoteBackRef");
        result = new ListItemBlock(Collections.singletonList(result));
        result.addChild(SpaceBlock.SPACE_BLOCK);
        result.addChildren(parsedContent);
        ((ListItemBlock) result).setParameter(CLASS_ATTRIBUTE_NAME, FootnoteMacro.MACRO_NAME);
        return (ListItemBlock) result;
    }

    /**
     * Get the parser of the current wiki syntax.
     * 
     * @param context the context of the macro transformation.
     * @return the parser of the current wiki syntax.
     * @throws MacroExecutionException Failed to find source parser.
     */
    protected Parser getSyntaxParser(MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            return this.componentManager.lookup(Parser.class, context.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }
}
