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
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Stores event types and offers a way to call a stored event.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public enum EventType
{
    BEGIN_DOCUMENT {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginDocument((Map<String, String>) eventParameters[0]);
        }
    },
    END_DOCUMENT {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endDocument((Map<String, String>) eventParameters[0]);
        }
    },
    BEGIN_GROUP {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginGroup((Map<String, String>) eventParameters[0]);
        }
    },
    END_GROUP {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endGroup((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_PARAGRAPH {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginParagraph((Map<String, String>) eventParameters[0]);
        }
    },
    END_PARAGRAPH {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endParagraph((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_DEFINITION_LIST {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginDefinitionList((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            // This is because for nested definition lists, the event after a definition list item content is a new
            // definition list
            return true;
        }
    },
    END_DEFINITION_LIST {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endDefinitionList((Map<String, String>) eventParameters[0]);
        }
    },
    BEGIN_DEFINITION_TERM {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginDefinitionTerm();
        }
    },
    END_DEFINITION_TERM {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endDefinitionTerm();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_DEFINITION_DESCRIPTION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginDefinitionDescription();
        }
    },
    END_DEFINITION_DESCRIPTION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endDefinitionDescription();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_FORMAT {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginFormat((Format) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    END_FORMAT {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endFormat((Format) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    BEGIN_HEADER {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginHeader((HeaderLevel) eventParameters[0], (String) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    END_HEADER {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endHeader((HeaderLevel) eventParameters[0], (String) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    BEGIN_LINK {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginLink((ResourceReference) eventParameters[0], (Boolean) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    END_LINK {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endLink((ResourceReference) eventParameters[0], (Boolean) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    BEGIN_LIST {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginList((ListType) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }

        @Override
        public boolean isInlineEnd()
        {
            // This is because for nested lists, the event after list item content is a new list
            return true;
        }
    },
    END_LIST {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endList((ListType) eventParameters[0], (Map<String, String>) eventParameters[1]);
        }
    },
    BEGIN_LIST_ITEM {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginListItem();
        }
    },
    END_LIST_ITEM {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endListItem();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_MACRO_MARKER {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginMacroMarker((String) eventParameters[0], (Map<String, String>) eventParameters[1],
                (String) eventParameters[2], (Boolean) eventParameters[3]);
        }
    },
    END_MACRO_MARKER {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endMacroMarker((String) eventParameters[0], (Map<String, String>) eventParameters[1],
                (String) eventParameters[2], (Boolean) eventParameters[3]);
        }
    },
    BEGIN_QUOTATION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginQuotation((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            // This is because for nested quotations, the event after a quotation line is a new quotation
            return true;
        }
    },
    END_QUOTATION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endQuotation((Map<String, String>) eventParameters[0]);
        }
    },
    BEGIN_QUOTATION_LINE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginQuotationLine();
        }
    },
    END_QUOTATION_LINE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endQuotationLine();
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_SECTION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginSection((Map<String, String>) eventParameters[0]);
        }
    },
    END_SECTION {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endSection((Map<String, String>) eventParameters[0]);
        }
    },
    BEGIN_TABLE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginTable((Map<String, String>) eventParameters[0]);
        }
    },
    END_TABLE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endTable((Map<String, String>) eventParameters[0]);
        }
    },
    BEGIN_TABLE_CELL {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginTableCell((Map<String, String>) eventParameters[0]);
        }
    },
    END_TABLE_CELL {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endTableCell((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_TABLE_HEAD_CELL {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginTableHeadCell((Map<String, String>) eventParameters[0]);
        }
    },
    END_TABLE_HEAD_CELL {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endTableHeadCell((Map<String, String>) eventParameters[0]);
        }

        @Override
        public boolean isInlineEnd()
        {
            return true;
        }
    },
    BEGIN_TABLE_ROW {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.beginTableRow((Map<String, String>) eventParameters[0]);
        }
    },
    END_TABLE_ROW {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.endTableRow((Map<String, String>) eventParameters[0]);
        }
    },
    ON_RAW_TEXT {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onRawText((String) eventParameters[0], (Syntax) eventParameters[1]);
        }
    },
    ON_EMPTY_LINES {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onEmptyLines((Integer) eventParameters[0]);
        }
    },
    ON_HORIZONTAL_LINE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onHorizontalLine((Map<String, String>) eventParameters[0]);
        }
    },
    ON_ID {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onId((String) eventParameters[0]);
        }
    },
    ON_IMAGE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onImage((Image) eventParameters[0], (Boolean) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    ON_MACRO {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onMacro((String) eventParameters[0], (Map<String, String>) eventParameters[1],
                (String) eventParameters[2], (Boolean) eventParameters[3]);
        }
    },
    ON_NEW_LINE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onNewLine();
        }
    },
    ON_SPACE {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onSpace();
        }
    },
    ON_SPECIAL_SYMBOL {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onSpecialSymbol((Character) eventParameters[0]);
        }
    },
    ON_VERBATIM {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onVerbatim((String) eventParameters[0], (Boolean) eventParameters[1],
                (Map<String, String>) eventParameters[2]);
        }
    },
    ON_WORD {
        public void fireEvent(Listener listener, Object[] eventParameters)
        {
            listener.onWord((String) eventParameters[0]);
        }
    };

    public abstract void fireEvent(Listener listener, Object[] eventParameters);

    public boolean isInlineEnd()
    {
        return false;
    }
}
