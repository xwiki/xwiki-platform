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
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;

import java.util.List;
import java.util.ArrayList;


public class DOMUtils {

    public static List getElementByTagName(Element el, String tagName){
        return getElementByTagName(el, tagName, new ArrayList());
    }

    private static List getElementByTagName(Element parentEl, String tagName, List elms){
        for(int i = 0; i < DOM.getChildCount(parentEl); i++){
            Element el = DOM.getChild(parentEl, i);
            if (DOM.getElementProperty(el, "tagName").equalsIgnoreCase(tagName)){
                elms.add(el);
            }
            getElementByTagName(el, tagName, elms);
        }
        return elms;
    }

    public static Element getFirstElementByTagName(Element parentEl, String tagName){
        for(int i = 0; i < DOM.getChildCount(parentEl); i++){
            Element el = DOM.getChild(parentEl, i);
            if (DOM.getElementProperty(el, "tagName").equalsIgnoreCase(tagName)){
                return el;
            }
            getFirstElementByTagName(el, tagName);
        }
        return null;
    }

        public static Element getElementByClassName(Element parentEl, String style) {
        for (int i = 0;  i < DOM.getChildCount(parentEl); i++){
            Element el = DOM.getChild(parentEl, i);
            if (isClassName(el, style))
                return el;
            el = getElementByClassName(el, style);
            if (el != null)
                return el;
        }
        return null;
    }

    public static boolean isClassName(Element elem, String style){
        //algorithm from UIObject.setStyleName(...)

        // Get the current style string.
        String elStyle = DOM.getElementProperty(elem, "className");
        if (elStyle == null) {
          return false;
        }

        int idx = elStyle.indexOf(style);

        // Calculate matching index.
        while (idx != -1) {
          if (idx == 0 || elStyle.charAt(idx - 1) == ' ') {
            int last = idx + style.length();
            int lastPos = elStyle.length();
            if ((last == lastPos)
                || ((last < lastPos) && (elStyle.charAt(last) == ' '))) {
              break;
            }
          }
          idx = elStyle.indexOf(style, idx + 1);
        }

        if (idx != -1)
            return true;
        return false;

    }

}
