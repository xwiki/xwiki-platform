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
 *
 */
package com.xpn.xwiki.plugin.charts.params;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class FontChartParam extends AbstractChartParam
{
    private Map styleChoices = new HashMap();

    public FontChartParam(String name)
    {
        super(name);
        init();
    }

    public FontChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    @Override
    public Class getType()
    {
        return Font.class;
    }

    public void init()
    {
        styleChoices.put("plain", new Integer(Font.PLAIN));
        styleChoices.put("bold", new Integer(Font.BOLD));
        styleChoices.put("italic", new Integer(Font.ITALIC));
        styleChoices.put("bold+italic", new Integer(Font.BOLD + Font.ITALIC));
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value, 3);
        return new Font(getStringArg(map, "name"), getStyleParam(map, "style"), getIntArg(map, "size"));
    }

    private int getStyleParam(Map map, String name) throws ParamException
    {
        return ((Integer) getChoiceArg(map, name, styleChoices)).intValue();
    }
}
