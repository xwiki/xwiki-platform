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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import com.google.gwt.dom.client.BaseElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Command;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;

/**
 * A {@link Command} that applies a {@link DocumentTemplate} to a {@link Document}.
 * 
 * @version $Id$
 */
class DocumentTemplateEnforcer implements Command
{
    /**
     * The rich text area that provides the template and the document.
     */
    private final RichTextArea rta;

    /**
     * Creates a new enforcer that will ensure the document template of the given rich text area will be applied on its
     * inner document.
     * 
     * @param rta A rich text area
     */
    public DocumentTemplateEnforcer(RichTextArea rta)
    {
        this.rta = rta;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Command#execute()
     */
    public void execute()
    {
        Document document = rta.getDocument();
        DocumentTemplate template = rta.getDocumentTemplate();

        Node head = document.getElementsByTagName("head").getItem(0);

        // Add style sheet declarations
        LinkElement linkPrototype = document.xCreateLinkElement();
        linkPrototype.setRel("stylesheet");
        linkPrototype.setType("text/css");
        for (String styleSheetURL : template.getStyleSheetURLs()) {
            LinkElement link = (LinkElement) linkPrototype.cloneNode(false);
            link.setHref(styleSheetURL);
            head.appendChild(link);
        }

        // Add script declarations
        ScriptElement scriptPrototype = document.xCreateScriptElement();
        scriptPrototype.setType("text/javascript");
        for (String scriptURL : template.getScriptURLs()) {
            ScriptElement script = (ScriptElement) scriptPrototype.cloneNode(false);
            script.setSrc(scriptURL);
            head.appendChild(script);
        }

        // Get existing base element
        BaseElement baseElement;
        NodeList<Element> baseElements = document.getElementsByTagName("base");
        if (baseElements.getLength() > 0) {
            // There already is a base element
            baseElement = baseElements.getItem(0).cast();
        } else {
            baseElement = document.xCreateBaseElement();
        }
        // Set the document base to the URL of the template document
        baseElement.setHref(template.getBaseURL());
        if (baseElement.getParentNode() == null) {
            DOMUtils.getInstance().insertAt(head, baseElement, 0);
        }

        // Set the class and id attributes on body
        document.getBody().setId(template.getBodyId());
        document.getBody().setClassName(template.getBodyClassName());

        // Reload the content of the document, to re-resolve the URLs after the baseURL change
        String documentContent = document.getDocumentElement().getString();
        document.open();
        document.write(documentContent);
        document.close();
    }
}
