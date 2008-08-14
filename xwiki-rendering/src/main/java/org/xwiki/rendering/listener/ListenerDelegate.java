package org.xwiki.rendering.listener;

import java.util.Map;

public class ListenerDelegate implements Listener
{
    private Listener listener;

    public void setWrappedListener(Listener listener)
    {
        this.listener = listener;
    }

    public Listener getWrappedListener()
    {
        return this.listener;
    }

    public void beginDocument()
    {
        this.listener.beginDocument();
    }

    public void endDocument()
    {
        this.listener.endDocument();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginFormat(Format)
     */
    public void beginFormat(Format format)
    {
        this.listener.beginFormat(format);
    }

    public void beginList(ListType listType)
    {
        this.listener.beginList(listType);
    }

    public void beginListItem()
    {
        this.listener.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.listener.beginMacroMarker(name, parameters, content);
    }

    public void beginParagraph()
    {
        this.listener.beginParagraph();
    }

    public void beginSection(SectionLevel level)
    {
        this.listener.beginSection(level);
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        this.listener.beginXMLElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endFormat(Format) 
     */
    public void endFormat(Format format)
    {
        this.listener.endFormat(format);
    }

    public void endList(ListType listType)
    {
        this.listener.endList(listType);
    }

    public void endListItem()
    {
        this.listener.endListItem();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.listener.endMacroMarker(name, parameters, content);
    }

    public void endParagraph()
    {
        this.listener.endParagraph();
    }

    public void endSection(SectionLevel level)
    {
        this.listener.endSection(level);
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        this.listener.endXMLElement(name, attributes);
    }

    public void onEscape(String escapedString)
    {
        this.listener.onEscape(escapedString);
    }

    public void onLineBreak()
    {
        this.listener.onLineBreak();
    }

    public void onLink(Link link)
    {
        this.listener.onLink(link);
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        this.listener.onMacro(name, parameters, content);
    }

    public void onNewLine()
    {
        this.listener.onNewLine();
    }

    public void onSpace()
    {
        this.listener.onSpace();
    }

    public void onSpecialSymbol(String symbol)
    {
        this.listener.onSpecialSymbol(symbol);
    }

    public void onWord(String word)
    {
        this.listener.onWord(word);
    }

    public void onId(String name)
    {
        this.listener.onId(name);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine()
     */
    public void onHorizontalLine()
    {
        this.listener.onHorizontalLine();
    }
}
