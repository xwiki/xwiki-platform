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
package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.LinkedHashMap;
import java.util.Map;

public class RadeoxMacroParameters extends LinkedHashMap<String, RadeoxMacroParameter>
{
    private LinkedHashMap<Integer, RadeoxMacroParameter> indexMap = new LinkedHashMap<Integer, RadeoxMacroParameter>();

    public RadeoxMacroParameters()
    {

    }

    public RadeoxMacroParameters(Map<String, RadeoxMacroParameter> paremeters)
    {
        super(paremeters);
    }

    public RadeoxMacroParameter get(int index)
    {
        return this.indexMap.get(index);
    }

    @Override
    public RadeoxMacroParameter remove(Object key)
    {
        RadeoxMacroParameter parameter = super.remove(key);

        if (parameter != null) {
            this.indexMap.remove(parameter.getIndex());
        }

        return parameter;
    }

    public void addParameter(int index, String name, String value)
    {
        RadeoxMacroParameter parameter = new RadeoxMacroParameter(index, name, value);
        this.indexMap.put(index, parameter);
        if (name != null) {
            put(name, parameter);
        }
    }
}
