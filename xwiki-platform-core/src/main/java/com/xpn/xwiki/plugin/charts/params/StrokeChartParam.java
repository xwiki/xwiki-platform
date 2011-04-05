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

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class StrokeChartParam extends AbstractChartParam
{
    private Map capChoices = new HashMap();

    private Map joinChoices = new HashMap();

    public StrokeChartParam(String name)
    {
        super(name);
        init();
    }

    public StrokeChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    @Override
    public Class getType()
    {
        return Stroke.class;
    }

    public void init()
    {
        capChoices.put("butt", new Integer(BasicStroke.CAP_BUTT));
        capChoices.put("round", new Integer(BasicStroke.CAP_ROUND));
        capChoices.put("square", new Integer(BasicStroke.CAP_SQUARE));

        joinChoices.put("miter", new Integer(BasicStroke.JOIN_MITER));
        joinChoices.put("round", new Integer(BasicStroke.JOIN_ROUND));
        joinChoices.put("bevel", new Integer(BasicStroke.JOIN_BEVEL));
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);
        switch (map.size()) {
            case 0:
                return new BasicStroke();
            case 1:
                return new BasicStroke(getFloatArg(map, "width"));
            case 3:
                return new BasicStroke(getFloatArg(map, "width"), getCapParam(map, "cap"), getJoinParam(map, "join"));
            case 4:
                return new BasicStroke(getFloatArg(map, "width"), getCapParam(map, "cap"), getJoinParam(map, "join"),
                    getFloatArg(map, "miterlimit"));
            case 6:
                return new BasicStroke(getFloatArg(map, "width"), getCapParam(map, "cap"), getJoinParam(map, "join"),
                    getFloatArg(map, "miterlimit"), toFloatArray(getListArg(map, "dash")), getFloatArg(map,
                        "dash_phase"));
            default:
                throw new InvalidParamException("Invalid value for the parameter " + getName()
                    + ": Invalid number of arguments: " + map.size());
        }
    }

    private int getCapParam(Map map, String name) throws ParamException
    {
        return ((Integer) getChoiceArg(map, name, capChoices)).intValue();
    }

    private int getJoinParam(Map map, String name) throws ParamException
    {
        return ((Integer) getChoiceArg(map, name, joinChoices)).intValue();
    }
}
