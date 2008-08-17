package org.xwiki.rendering.renderer;

public abstract class AbstractPrintRenderer implements PrintRenderer
{
    private WikiPrinter printer;

    public AbstractPrintRenderer(WikiPrinter printer)
    {
        this.printer = printer;
    }

    protected void print(String text)
    {
        getPrinter().print(text);
    }

    protected void println(String text)
    {
        getPrinter().println(text);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.PrintRenderer#getPrinter()
     */
    public WikiPrinter getPrinter()
    {
        return this.printer;
    }

    protected void setPrinter(WikiPrinter printer)
    {
        this.printer = printer;
    }

}
