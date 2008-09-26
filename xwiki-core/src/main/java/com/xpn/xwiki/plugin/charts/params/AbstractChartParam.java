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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public abstract class AbstractChartParam implements ChartParam
{
    protected String name;

    protected boolean optional;

    public static final String MAP_SEPARATOR = ";";

    public static final String MAP_ASSIGNMENT = ":";

    public static final String LIST_SEPARATOR = ",";

    public AbstractChartParam(String name)
    {
        this(name, true);
    }

    public AbstractChartParam(String name, boolean optional)
    {
        this.name = name;
        this.optional = optional;
    }

    public String getName()
    {
        return name;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public abstract Class getType();

    public abstract Object convert(String value) throws ParamException;

    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof ChartParam) {
            return getName().equals(((ChartParam) obj).getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    protected String getStringArg(Map map, String name) throws MissingArgumentException
    {
        String value = (String) map.get(name);
        if (value != null) {
            return value;
        } else {
            throw new MissingArgumentException("Invalid value for the parameter " + getName() + ": Argument " + name
                + " is mandatory.");
        }
    }

    protected String getStringOptionalArg(Map map, String name)
    {
        return (String) map.get(name);
    }

    protected int getIntArg(Map map, String name) throws MissingArgumentException, InvalidArgumentException
    {
        String value = getStringArg(map, name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                + ": Non-integer value for the " + name + " argument.");
        }
    }

    protected float getFloatArg(Map map, String name) throws MissingArgumentException, InvalidArgumentException
    {
        String value = getStringArg(map, name);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                + ": Non-float value for the " + name + " argument.");
        }
    }

    protected double getDoubleArg(Map map, String name) throws MissingArgumentException, InvalidArgumentException
    {
        String value = getStringArg(map, name);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                + ": Non-double value for the " + name + " argument.");
        }
    }

    protected Object getChoiceArg(Map map, String name, Map choices) throws MissingArgumentException,
        InvalidArgumentException
    {
        String value = getStringArg(map, name);
        Object obj = choices.get(value);
        if (obj != null) {
            return obj;
        } else {
            throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                + ": The accepted values for the " + name + "argument are " + choices.keySet() + "; encountered: "
                + value);
        }
    }

    protected List getListArg(Map map, String name) throws MissingArgumentException
    {
        return parseList(getStringArg(map, name));
    }

    protected Map parseMap(String value) throws InvalidArgumentException
    {
        String[] args = value.split(MAP_SEPARATOR);
        if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
            return new HashMap(0);
        }
        Map result = new HashMap(args.length);
        for (int i = 0; i < args.length; i++) {
            String[] split = args[i].split(MAP_ASSIGNMENT);
            if (split.length != 2) {
                throw new InvalidArgumentException("Invalid value for the parameter " + getName() + ": " + "name"
                    + MAP_ASSIGNMENT + "value \"" + MAP_SEPARATOR + "\"-separated list expected");
            }
            result.put(split[0].trim(), split[1].trim());
        }
        return result;
    }

    protected Map parseMap(String value, int expectedTokenCount) throws InvalidArgumentException
    {
        Map result = parseMap(value);
        if (result.size() != expectedTokenCount) {
            throw new InvalidArgumentException("Invalid number of arguments given to the " + getName()
                + " parameter; expected:" + expectedTokenCount);
        }
        return result;
    }

    protected List parseList(String value)
    {
        String[] args = value.split(LIST_SEPARATOR);
        if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
            return new ArrayList(0);
        }
        List result = new ArrayList(args.length);
        for (int i = 0; i < args.length; i++) {
            result.add(args[i].trim());
        }
        return result;
    }

    protected List toFloatList(List list) throws InvalidArgumentException
    {
        List result = new ArrayList(list.size());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            try {
                result.add(new Float(value));
            } catch (NumberFormatException e) {
                throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                    + ": Non-float value for " + name);
            }
        }
        return result;
    }

    protected float[] toFloatArray(List list) throws InvalidArgumentException
    {
        float[] result = new float[list.size()];
        Iterator it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            String value = (String) it.next();
            try {
                result[i] = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                throw new InvalidArgumentException("Invalid value for the parameter " + getName()
                    + ": Non-float value for " + name);
            }
            i++;
        }
        return result;
    }
}
