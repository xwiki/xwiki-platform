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
package org.xwiki.rendering.listener;

public enum SpecialSymbol
{
    LESSTHAN("<"), GREATERTHAN(">"), EQUAL("="), QUOTE("\""), SLASH("/"), DOT("."), DOLLAR("$");
    
    private String symbol;
    
    private SpecialSymbol(String symbol)
    {
        this.symbol = symbol;
    }
    
    @Override
    public String toString()
    {
        return this.symbol;
    }

    public static SpecialSymbol parseString(String symbol)
    {
        SpecialSymbol result;
        if (symbol.equals("<")) {
            result = SpecialSymbol.LESSTHAN;
        } else if (symbol.equals(">")) {
            result = SpecialSymbol.GREATERTHAN;
        } else if (symbol.equals("=")) {
            result = SpecialSymbol.EQUAL;
        } else if (symbol.equals("\"")){
            result = SpecialSymbol.QUOTE;
        } else if (symbol.equals("/")) {
            result = SpecialSymbol.SLASH;
        } else if (symbol.equals(".")) {
            result = SpecialSymbol.DOT;
        } else if (symbol.equals("$")) {
            result = SpecialSymbol.DOLLAR;
        } else {
            throw new IllegalArgumentException("Unrecognized symbol [" + symbol + "]");
        }
        return result;
    }
}
