package org.xwiki.rendering.parser.xwiki10.macro;

public class RadeoxMacroParameter
{
    private int index;

    private String name;

    private String value;

    public RadeoxMacroParameter(int index, String name, String value)
    {
        this.setIndex(index);
        this.setName(name);
        this.setValue(value);
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public int getIndex()
    {
        return this.index;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.value;
    }
}
