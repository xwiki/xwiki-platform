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
