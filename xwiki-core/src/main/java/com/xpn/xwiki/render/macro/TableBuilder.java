/*
 *      Copyright 2001-2004 Fraunhofer Gesellschaft, Munich, Germany, for its 
 *      Fraunhofer Institute Computer Architecture and Software Technology
 *      (FIRST), Berlin, Germany
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.xpn.xwiki.render.macro;

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.radeox.macro.table.Table;

/**
 * Builds a Radeox {@link Table} by parsing its textual representation, usually the content of a {table} macro.
 * 
 * @version $Id$
 */
public class TableBuilder
{
    public static Table build(String content)
    {
        Table table = new Table();
        StringTokenizer tokenizer = new StringTokenizer(content, "|\n", true);
        String lastToken = null;
        boolean firstCell = true;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("\r")) {
                continue;
            }
            // If a token contains [, then all tokens up to one containing a ] are concatenated. Kind of a block marker.
            if (token.indexOf('[') != -1 && token.indexOf(']') == -1) {
                String linkToken = "";
                while (token.indexOf(']') == -1 && tokenizer.hasMoreTokens()) {
                    linkToken += token;
                    token = tokenizer.nextToken();
                }
                token = linkToken + token;
            }
            if ("\n".equals(token)) {
                // New line: either new row, or a literal newline.
                lastToken = (lastToken == null) ? "" : lastToken;
                if (!StringUtils.endsWith(lastToken, "\\")) {
                    // A new row, not a literal newline.
                    // If the last cell didn't contain any data, then it was skipped. Add a blank cell to compensate.
                    if ((StringUtils.isEmpty(lastToken) || "|".equals(lastToken)) && !firstCell) {
                        table.addCell(" ");
                    }
                    table.newRow();
                } else {
                    // A continued row, with a literal newline.
                    String cell = lastToken;
                    // Keep concatenating while the cell data ends with \\
                    while (cell.endsWith("\\") && tokenizer.hasMoreTokens()) {
                        token = tokenizer.nextToken();
                        if (!"|".equals(token)) {
                            cell = cell + token;
                        } else {
                            break;
                        }
                    }
                    firstCell = false;
                    table.addCell(cell.trim());
                    if (!tokenizer.hasMoreTokens()) {
                        table.newRow();
                    }
                }
            } else if (!"|".equals(token)) {
                // Cell data
                if (!token.endsWith("\\")) {
                    // If the cell data ends with \\, then it will be continued. Current data is stored in lastToken.
                    table.addCell(token.trim());
                    firstCell = false;
                } else if (!tokenizer.hasMoreTokens()) {
                    // Remove backslashes from the end
                    while (token.endsWith("\\")) {
                        token = StringUtils.chop(token);
                    }
                    table.addCell(token.trim());
                }
            } else if ("|".equals(token)) {
                // Cell delimiter
                if ((StringUtils.isEmpty(lastToken) && firstCell) || "|".equals(lastToken)) {
                    // If the last cell didn't contain any data, then it was skipped. Add a blank cell to compensate.
                    table.addCell(" ");
                    firstCell = false;
                } else if (StringUtils.endsWith(lastToken, "\\")) {
                    // The last cell wasn't added because it ended with a continuation mark (\\). Add it now.
                    table.addCell(lastToken.trim());
                    firstCell = false;
                }
            }
            lastToken = token;
        }
        return table;
    }
}
