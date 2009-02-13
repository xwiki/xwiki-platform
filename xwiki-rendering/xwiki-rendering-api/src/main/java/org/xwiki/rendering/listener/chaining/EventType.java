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
package org.xwiki.rendering.listener.chaining;

import java.util.Map;

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * Stores event types and offers a way to call a stored event.
 *  
 * @version $Id$
 * @since 1.8RC1
 */
public enum EventType
{
    BEGIN_DOCUMENT {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginDocument();
        }
    },
    END_DOCUMENT {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endDocument();
        }
    },
    BEGIN_PARAGRAPH {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginParagraph((Map<String, String>) eventParameters[0]);
        }
    },
    END_PARAGRAPH {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endParagraph((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_DEFINITION_LIST {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginDefinitionList();
        }
    },
    END_DEFINITION_LIST {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endDefinitionList();
        }
    },
    BEGIN_DEFINITION_TERM {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginDefinitionTerm();
        }
    },
    END_DEFINITION_TERM {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endDefinitionTerm();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_DEFINITION_DESCRIPTION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginDefinitionDescription();
        }
    },
    END_DEFINITION_DESCRIPTION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endDefinitionDescription();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_ERROR {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginError((String) eventParameters[0], (String) eventParameters[1]);
        }
    },
    END_ERROR {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endError((String) eventParameters[0], (String) eventParameters[1]);
        }
    },
    BEGIN_FORMAT {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginFormat((Format) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    END_FORMAT {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endFormat((Format) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    BEGIN_HEADER {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginHeader((HeaderLevel) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    END_HEADER {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endHeader((HeaderLevel) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    BEGIN_LINK {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginLink((Link) eventParameters[0], (Boolean) eventParameters[1], (Map<String, String>) eventParameters[2]);
        }
    },
    END_LINK {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endLink((Link) eventParameters[0], (Boolean) eventParameters[1], (Map<String, String>) eventParameters[2]);
        }
    },
    BEGIN_LIST {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginList((ListType) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    END_LIST {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endList((ListType) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    BEGIN_LIST_ITEM {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginListItem();
        }
    },
    END_LIST_ITEM {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endListItem();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_MACRO_MARKER {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginMacroMarker((String) eventParameters[0], (Map<String, String>) eventParameters[1], 
                (String) eventParameters[2], (Boolean) eventParameters[3]);
        }
    },
    END_MACRO_MARKER {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endMacroMarker((String) eventParameters[0], (Map<String, String>) eventParameters[1], 
                (String) eventParameters[2], (Boolean) eventParameters[3]);
        }
    },
    BEGIN_QUOTATION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginQuotation((Map<String, String>) eventParameters[0]); 
        }
    },
    END_QUOTATION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endQuotation((Map<String, String>) eventParameters[0]); 
        }
    },
    BEGIN_QUOTATION_LINE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginQuotationLine(); 
        }
    },
    END_QUOTATION_LINE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endQuotationLine(); 
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_SECTION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginSection((Map<String, String>) eventParameters[0]); 
        }
    },
    END_SECTION {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endSection((Map<String, String>) eventParameters[0]); 
        }
    },
    BEGIN_TABLE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginTable((Map<String, String>) eventParameters[0]); 
        }
    },
    END_TABLE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endTable((Map<String, String>) eventParameters[0]); 
        }
    },
    BEGIN_TABLE_CELL {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginTableCell((Map<String, String>) eventParameters[0]); 
        }
    },
    END_TABLE_CELL {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endTableCell((Map<String, String>) eventParameters[0]); 
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_TABLE_HEAD_CELL {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginTableHeadCell((Map<String, String>) eventParameters[0]); 
        }
    },
    END_TABLE_HEAD_CELL {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endTableHeadCell((Map<String, String>) eventParameters[0]); 
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_TABLE_ROW {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginTableRow((Map<String, String>) eventParameters[0]); 
        }
    },
    END_TABLE_ROW {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endTableRow((Map<String, String>) eventParameters[0]); 
        }
    },
    BEGIN_XML_NODE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.beginXMLNode((XMLNode) eventParameters[0]); 
        }
    },
    END_XML_NODE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.endXMLNode((XMLNode) eventParameters[0]); 
        }
    },
    ON_EMPTY_LINES {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onEmptyLines((Integer) eventParameters[0]); 
        }
    },
    ON_HORIZONTAL_LINE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onHorizontalLine((Map<String, String>) eventParameters[0]); 
        }
    },
    ON_ID {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onId((String) eventParameters[0]); 
        }
    },
    ON_IMAGE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onImage((Image) eventParameters[0], (Boolean) eventParameters[1], 
                (Map<String, String>) eventParameters[2]); 
        }
    },
    ON_MACRO {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onMacro((String) eventParameters[0], (Map<String, String>) eventParameters[1], 
                (String) eventParameters[2], (Boolean) eventParameters[3]); 
        }
    },
    ON_NEW_LINE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onNewLine(); 
        }
    },
    ON_SPACE {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onSpace(); 
        }
    },
    ON_SPECIAL_SYMBOL {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onSpecialSymbol((Character) eventParameters[0]); 
        }
    },
    ON_VERBATIM {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onVerbatim((String) eventParameters[0], (Map<String, String>) eventParameters[1], 
               (Boolean) eventParameters[2]); 
        }
    },
    ON_WORD {
        void fireEvent(Listener listener, Object[] eventParameters) {
            listener.onWord((String) eventParameters[0]);
        }
    };
   
    abstract void fireEvent(Listener listener, Object[] eventParameters);
    
    public boolean isInlineEnd()
    {
        return false;
    }
}
