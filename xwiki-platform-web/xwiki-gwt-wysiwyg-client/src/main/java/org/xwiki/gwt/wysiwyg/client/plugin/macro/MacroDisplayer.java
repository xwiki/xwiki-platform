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
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;

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
    private RichTextArea textArea;

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
     * Sets the underlying rich text area where the macros are displayed.<br/>
     * NOTE: We were forced to add this method because instances of this class are created using deferred binding and
     * thus we cannot pass the rich text area as a parameter to the constructor. As a consequence this method can be
     * called only once.
     * 
     * @param textArea the rich text area whose macros will be displayed using this object
     */
    public void setTextArea(RichTextArea textArea)
    {
        if (this.textArea != null) {
            throw new IllegalStateException("Text area has already been set!");
        }
        this.textArea = textArea;

        // Listen to rich text area's inner HTML changes to detect new macros.
        textArea.getDocument().addInnerHTMLListener(this);

        // Display the current macros.
        display(getStartMacroCommentNodes(textArea.getDocument().getBody()));
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
        // Create the read only text box.
        Element container = createReadOnlyBox();
        container.setClassName(MACRO_STYLE_NAME);

        MacroCall call = new MacroCall(start.getNodeValue());
        container.setTitle(call.getName() + " macro");
        // Use a place holder when the macro is collapsed or when it is empty.
        // The place holder is hidden when the macro is expanded.
        container.appendChild(createPlaceHolder(call));

        // Extract the macro output, if there is any.
        if (siblingCount > 0) {
            int startIndex = domUtils.getNodeIndex(start);
            int endIndex = startIndex + siblingCount + 1;
            // We need to put macro output inside a container to be able to hide it when the macro is collapsed.
            Element output = textArea.getDocument().createDivElement().cast();
            output.setClassName("macro-output");
            output.appendChild(domUtils.extractNodeContents(start.getParentNode(), startIndex + 1, endIndex));

            // Let's see if the macro should be displayed in-line or as a block.
            container.addClassName(isInLine(output) ? INLINE_MACRO_STYLE_NAME : BLOCK_MACRO_STYLE_NAME);

            container.appendChild(output);
        }

        // Insert the macro container before the start macro comment node, which will be removed.
        start.getParentNode().insertBefore(container, start);

        return container;
    }

    /**
     * @param output the output of a macro
     * @return {@code false} if the output of the macro contains block-level elements and thus the macro needs to be
     *         displayed as a block, {@code true} otherwise
     */
    private boolean isInLine(Element output)
    {
        Node child = output.getFirstChild();
        while (child != null) {
            if (domUtils.isBlock(child)) {
                return false;
            }
            child = child.getNextSibling();
        }
        return true;
    }

    /**
     * @return an element whose contents cannot be edited inside the rich text area
     */
    protected Element createReadOnlyBox()
    {
        Element container = (Element) textArea.getDocument().createElement(getMacroContainerTagName());
        container.setAttribute("contentEditable", "false");
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

        Element placeHolder = document.createSpanElement().cast();
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
        return getMacroContainerTagName().equalsIgnoreCase(node.getNodeName())
            && Element.as(node).hasClassName(MACRO_STYLE_NAME);
    }

    /**
     * @param root a DOM element
     * @return the list of macro containers in the specified subtree
     */
    public List<Element> getMacroContainers(Element root)
    {
        List<Element> containers = new ArrayList<Element>();
        NodeList<com.google.gwt.dom.client.Element> divs = root.getElementsByTagName(getMacroContainerTagName());
        for (int i = 0; i < divs.getLength(); i++) {
            Element div = divs.getItem(i).cast();
            if (isMacroContainer(div)) {
                containers.add(div);
            }
        }
        return containers;
    }

    /**
     * @return the name of the element used as macro container
     */
    protected String getMacroContainerTagName()
    {
        return "button";
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
        if (output != null) {
            output.getStyle().setDisplay(collapse ? Display.NONE : Display.BLOCK);
        }

        Element placeHolder = getPlaceHolder(container);
        placeHolder.getStyle().setDisplay(collapse ? Display.INLINE : Display.NONE);
    }

    /**
     * @param container a macro container
     * @return {@code true} if the specified macro is collapsed, {@code false} otherwise
     */
    public boolean isCollapsed(Element container)
    {
        return !Display.NONE.getCssName().equals(getPlaceHolder(container).getStyle().getDisplay());
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
