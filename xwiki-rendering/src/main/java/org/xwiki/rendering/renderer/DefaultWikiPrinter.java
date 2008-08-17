package org.xwiki.rendering.renderer;

public class DefaultWikiPrinter implements WikiPrinter
{
    private StringBuffer buffer;

    public DefaultWikiPrinter()
    {
        this(new StringBuffer());
    }

    public DefaultWikiPrinter(StringBuffer buffer)
    {
        this.buffer = buffer;
    }

    public StringBuffer getBuffer()
    {
        return this.buffer;
    }

    /**
     * @return a new line symbols
     */
    protected String getEOL()
    {
        return "\n";
    }

    public void print(String text)
    {
        getBuffer().append(text);
    }

    public void println(String text)
    {
        getBuffer().append(text).append(getEOL());
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getBuffer().toString();
    }
}
