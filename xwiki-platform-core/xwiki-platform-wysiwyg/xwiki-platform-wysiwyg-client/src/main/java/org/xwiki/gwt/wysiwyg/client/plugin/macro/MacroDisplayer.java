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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.InnerHTMLListener;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Hides macro meta data and displays macro output in a read only text box.
 * 
 * @version $Id$
 */
public class MacroDisplayer implements InnerHTMLListener
{
    /**
     * The CSS class name used on the text box containing the output of a macro.
     */
    public static final String MACRO_STYLE_NAME = "macro";

    /**
     * The CSS class name used on the text box containing the output of a selected macro.
     */
    public static final String SELECTED_MACRO_STYLE_NAME = MACRO_STYLE_NAME + "-selected";

    /**
     * The CSS class name used on the text box containing the output of a block macro.
     */
    public static final String BLOCK_MACRO_STYLE_NAME = MACRO_STYLE_NAME + "-block";

    /**
     * The CSS class name used on the text box containing the output of an in-line macro.
     */
    public static final String INLINE_MACRO_STYLE_NAME = MACRO_STYLE_NAME + "-inline";

    /**
     * The CSS class name used on the macro container when a place-holder is displayed instead of the macro output.
     */
    public static final String COLLAPSED_MACRO_STYLE_NAME = MACRO_STYLE_NAME + "-collapsed";

    /**
     * The prefix of the start macro comment node.
     */
    public static final String START_MACRO_COMMENT_PREFIX = "startmacro:";

    /**
     * The value of the stop macro comment node.
     */
    public static final String STOP_MACRO_COMMENT_VALUE = "stopmacro";

    /**
     * Collection of DOM utility methods.
     */
    protected final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * The underlying rich text area where the macros are displayed.
     */
    private final RichTextArea textArea;

    /**
     * Creates a new macro displayer for the given rich text area.
     * 
     * @param textArea the rich text area whose macros will be displayed using this object
     */
    public MacroDisplayer(RichTextArea textArea)
    {
        this.textArea = textArea;

        // Listen to rich text area's inner HTML changes to detect new macros.
        textArea.getDocument().addInnerHTMLListener(this);

        // Display the current macros.
        display(getStartMacroCommentNodes(textArea.getDocument().getBody()));
    }

    /**
     * Destroys this displayer.
     */
    public void destroy()
    {
        textArea.getDocument().removeInnerHTMLListener(this);
    }

    /**
     * @return {@link #textArea}
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * Displays the macros from the given list. Each macro is identified by its start comment node which contains meta
     * data regarding the macro call.
     * 
     * @param startMacroComments the list of macros to be displayed
     */
    private void display(List<Node> startMacroComments)
    {
        for (Node start : startMacroComments) {
            display(start);
        }
    }

    /**
     * Displays the macro identified by the given start comment node which contains meta data about the macro call.
     * 
     * @param start the start comment node identifying the macro to be displayed
     */
    private void display(Node start)
    {
        // Look for the stop macro comment.
        Node stop = start.getNextSibling();
        int siblingCount = 0;
        int openedMacrosCount = 0;
        while (stop != null) {
            if (stop.getNodeType() == DOMUtils.COMMENT_NODE) {
                if (stop.getNodeValue().startsWith(START_MACRO_COMMENT_PREFIX)) {
                    // Nested macro. Ignore the next stop macro comment.
                    openedMacrosCount++;
                } else if (STOP_MACRO_COMMENT_VALUE.equals(stop.getNodeValue())) {
                    // Check if there are nested macros opened.
                    if (openedMacrosCount == 0) {
                        break;
                    }
                    openedMacrosCount--;
                }
            }

            stop = stop.getNextSibling();
            siblingCount++;
        }
        if (stop == null) {
            return;
        }

        Element container;
        if (siblingCount == 1 && isMacroContainer(start.getNextSibling())) {
            // Macro container is already there.
            container = (Element) start.getNextSibling();
        } else {
            // Put macro output inside a read only text box.
            container = createMacroContainer(start, stop, siblingCount);
            // Expand the macro by default.
            setCollapsed(container, false);
        }

        // Hide macro meta data.
        container.setMetaData(extractMetaData(start, stop));

        // We have to display the macro as unselected to ensure the selected state is changed only from the
        // MacroSelector.
        setSelected(container, false);
    }

    /**
     * Puts macro output inside a read only text box that can be collapsed.
     * 
     * @param start start macro comment node
     * @param stop stop macro comment node
     * @param siblingCount the number of siblings between start and stop nodes
     * @return the created container that holds the macro output
     */
    protected Element createMacroContainer(Node start, Node stop, int siblingCount)
    {
        boolean inLine = isInLine(start, stop, siblingCount);

        // Create the read only text box.
        Element container = createReadOnlyBox(inLine);
        container.addClassName(MACRO_STYLE_NAME);
        container.addClassName(inLine ? INLINE_MACRO_STYLE_NAME : BLOCK_MACRO_STYLE_NAME);

        MacroCall call = new MacroCall(start.getNodeValue());
        container.setTitle(call.getName() + " macro");
        // Use a place holder when the macro is collapsed or when it is empty.
        // The place holder is hidden when the macro is expanded.
        container.appendChild(createPlaceHolder(call));

        // Extract the macro output, if there is any.
        if (siblingCount > 0) {
            int startIndex = domUtils.getNodeIndex(start);
            int endIndex = startIndex + siblingCount + 1;
            Document doc = textArea.getDocument();
            // We need to put macro output inside a container to be able to hide it when the macro is collapsed.
            Element output = Element.as(inLine ? doc.createSpanElement() : doc.createDivElement());
            output.appendChild(domUtils.extractNodeContents(start.getParentNode(), startIndex + 1, endIndex));
            output.setClassName("macro-output");
            container.appendChild(output);
        }

        // Insert the macro container before the start macro comment node, which will be removed.
        start.getParentNode().insertBefore(container, start);

        return container;
    }

    /**
     * @param start start macro comment node
     * @param stop stop macro comment node
     * @param siblingCount the number of siblings between start and stop nodes
     * @return {@code false} if the output of the macro contains block-level elements and thus the macro needs to be
     *         displayed as a block, {@code true} otherwise
     */
    private boolean isInLine(Node start, Node stop, int siblingCount)
    {
        if (siblingCount > 0) {
            Node sibling = start.getNextSibling();
            while (sibling != stop) {
                if (domUtils.isBlock(sibling)) {
                    return false;
                }
                sibling = sibling.getNextSibling();
            }
            return true;
        } else {
            return !domUtils.isFlowContainer(start.getParentNode());
        }
    }

    /**
     * @param inLine {@code true} if the read-only box is going to displayed in-line, {@code false} otherwise
     * @return an element whose contents cannot be edited inside the rich text area
     */
    protected Element createReadOnlyBox(boolean inLine)
    {
        Document doc = textArea.getDocument();
        Element container = Element.as(inLine ? doc.createSpanElement() : doc.createDivElement());
        container.setClassName("readOnly");
        return container;
    }

    /**
     * @param root the root of a DOM subtree
     * @return the list of start macro comment nodes for the top level macros under the given subtree (nested macros are
     *         ignored)
     */
    private List<Node> getStartMacroCommentNodes(Node root)
    {
        Document document = (Document) root.getOwnerDocument();
        Iterator<Node> iterator = document.getIterator(root);
        List<Node> startMacroComments = new ArrayList<Node>();
        int openedMacrosCount = 0;
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (node.getNodeType() == DOMUtils.COMMENT_NODE) {
                if (node.getNodeValue().startsWith(START_MACRO_COMMENT_PREFIX)) {
                    // Include only the top level macros.
                    if (openedMacrosCount == 0) {
                        startMacroComments.add(node);
                    }
                    openedMacrosCount++;
                } else if (STOP_MACRO_COMMENT_VALUE.equals(node.getNodeValue())) {
                    openedMacrosCount--;
                }
            }
        }
        return startMacroComments;
    }

    /**
     * Creates a place holder for an empty macro.
     * 
     * @param call the macro call
     * @return a document fragment to be used as a place holder for an empty macro
     */
    private DocumentFragment createPlaceHolder(MacroCall call)
    {
        Document document = textArea.getDocument();

        // We use a separate element to display the macro icon to be sure that the line height is not less than the
        // image height (we cannot set the height for in-line macros). We use a span instead of an image element because
        // we couldn't find a way to hide the image selection when a collapsed macro is selected.
        Element macroIcon = Element.as(document.createSpanElement());
        macroIcon.setClassName("macro-icon");
        // HACK: Insert a Non-Breaking Space in the element used to display the macro icon in order to overcome the
        // following problem: Firefox leaves the caret inside the hidden macro place-holder if you delete the text
        // before an expanded macro; obviously the caret disappears. Another solution would be to put the macro
        // place-holder in the DOM only when the macro is collapsed but this complicates the code that toggles the
        // collapsed state (and we'd need to recreate the macro place-holder after undo/redo operations).
        macroIcon.appendChild(document.createTextNode("\u00A0"));

        Element placeHolder = document.createSpanElement().cast();
        placeHolder.appendChild(macroIcon);
        placeHolder.appendChild(document.createTextNode(call.getName()));
        placeHolder.setClassName("macro-placeholder");

        DocumentFragment output = document.createDocumentFragment();
        output.appendChild(placeHolder);
        return output;
    }

    /**
     * Extracts the meta data nodes from the DOM tree and places them in a document fragment.
     * 
     * @param start start macro comment node
     * @param stop stop macro comment node
     * @return the meta data document fragment
     */
    private DocumentFragment extractMetaData(Node start, Node stop)
    {
        DocumentFragment metaData = textArea.getDocument().createDocumentFragment();
        metaData.appendChild(start);
        metaData.appendChild(stop);
        return metaData;
    }

    /**
     * Changes the appearance of the specified macro based on its selected state.
     * 
     * @param container a macro container
     * @param selected {@code true} to select the specified macro, {@code false} otherwise
     */
    public void setSelected(Element container, boolean selected)
    {
        if (selected) {
            container.addClassName(SELECTED_MACRO_STYLE_NAME);
        } else {
            container.removeClassName(SELECTED_MACRO_STYLE_NAME);
        }
    }

    /**
     * @param container a macro container
     * @return {@code true} if the specified macro is selected, {@code false} otherwise
     */
    public boolean isSelected(Element container)
    {
        return container.hasClassName(SELECTED_MACRO_STYLE_NAME);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is a macro container, {@code false} otherwise
     */
    public boolean isMacroContainer(Node node)
    {
        if (!Element.is(node)) {
            return false;
        }
        Element element = Element.as(node);
        return element.hasClassName(MACRO_STYLE_NAME) && element.hasAttribute(Element.META_DATA_ATTR)
            && element.getAttribute(Element.META_DATA_ATTR).startsWith(START_MACRO_COMMENT_PREFIX, 4);
    }

    /**
     * @param root a DOM element
     * @return the list of macro containers in the specified subtree
     */
    public List<Element> getMacroContainers(Element root)
    {
        Node node = root;
        List<Element> containers = new ArrayList<Element>();
        while (true) {
            boolean isMacroContainer = isMacroContainer(node);
            if (!node.hasChildNodes() || isMacroContainer) {
                // Add the node to the list of containers and skip its descendants.
                if (isMacroContainer) {
                    containers.add(Element.as(node));
                }
                // Look for the next node.
                while (node != root && node.getNextSibling() == null) {
                    node = node.getParentNode();
                }
                if (node == root) {
                    break;
                } else {
                    node = node.getNextSibling();
                }
            } else {
                node = node.getFirstChild();
            }
        }
        return containers;
    }

    /**
     * Collapses or expands the specified macro.
     * 
     * @param container a macro container
     * @param collapsed {@code true} to collapse the specified macro, {@code false} to expand it
     */
    public void setCollapsed(Element container, boolean collapsed)
    {
        Element output = getOutput(container);
        boolean collapse = collapsed || output == null;
        if (collapse) {
            container.addClassName(COLLAPSED_MACRO_STYLE_NAME);
        } else {
            container.removeClassName(COLLAPSED_MACRO_STYLE_NAME);
        }
    }

    /**
     * @param container a macro container
     * @return {@code true} if the specified macro is collapsed, {@code false} otherwise
     */
    public boolean isCollapsed(Element container)
    {
        return container.hasClassName(COLLAPSED_MACRO_STYLE_NAME);
    }

    /**
     * @param container a macro container
     * @return the macro output wrapper from the given macro container
     */
    protected Element getOutput(Element container)
    {
        return (Element) getPlaceHolder(container).getNextSibling();
    }

    /**
     * This method is useful to determine if a macro can be expanded or not. Macros that don't generate any output are
     * always displayed as collapsed because otherwise they would be invisible to the user.
     * 
     * @param container a macro container
     * @return {@code true} if the specified macro has any output, {@code false} otherwise
     */
    public boolean hasOutput(Element container)
    {
        return getOutput(container) != null;
    }

    /**
     * @param container a macro container
     * @return the macro place holder from the given macro container
     */
    protected Element getPlaceHolder(Element container)
    {
        return (Element) container.getFirstChild();
    }

    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(Element)
     */
    public void onInnerHTMLChange(Element element)
    {
        if (element.getOwnerDocument() == textArea.getDocument()) {
            display(getStartMacroCommentNodes(element));
        }
    }

    /**
     * @param container a macro container
     * @return the serialized macro call (e.g. the value of the start macro comment node) associated with the given
     *         macro container
     */
    public String getSerializedMacroCall(Element container)
    {
        return container.getMetaData().getFirstChild().getNodeValue();
    }
}
