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
package org.xwiki.gwt.dom.client;

/**
 * In-line sub-windows.
 * 
 * @version $Id$
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#edef-IFRAME">W3C HTML
 *      Specification</a>
 */
public class IFrameElement extends com.google.gwt.dom.client.IFrameElement
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected IFrameElement()
    {
    }

    /**
     * @return a reference to the content window
     */
    public final native Window getContentWindow()
    /*-{
        return this.contentWindow;
    }-*/;

    /**
     * This is a utility method for accessing the content document of an in-line frame in a static way. This method is
     * solely useful when called from native code. As we know, only static references to overlay types are allowed from
     * JSNI.
     * 
     * @param iframe an in-line frame element
     * @return the content document of the given in-line element
     */
    public static Document getContentDocument(IFrameElement iframe)
    {
        return (Document) iframe.getContentDocument();
    }
}
