package org.xwiki.rendering.listener;

import java.util.Map;

import org.xwiki.rendering.listener.xml.XMLNode;

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
     * 
     * @see org.xwiki.rendering.listener.Listener#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        this.listener.beginFormat(format, parameters);
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.listener.beginList(listType, parameters);
    }

    public void beginListItem()
    {
        this.listener.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.listener.beginMacroMarker(name, parameters, content);
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.listener.beginParagraph(parameters);
    }

    public void beginSection(Map<String, String> parameters)
    {
        this.listener.beginSection(parameters);
    }

    public void beginHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.listener.beginHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginXMLNode(XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        this.listener.beginXMLNode(node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        this.listener.endFormat(format, parameters);
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.listener.endList(listType, parameters);
    }

    public void endListItem()
    {
        this.listener.endListItem();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        this.listener.endMacroMarker(name, parameters, content);
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.listener.endParagraph(parameters);
    }

    public void endSection(Map<String, String> parameters)
    {
        this.listener.endSection(parameters);
    }

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.listener.endHeader(level, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endXMLNode(XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        this.listener.endXMLNode(node);
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.listener.beginLink(link, isFreeStandingURI, parameters);
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.listener.endLink(link, isFreeStandingURI, parameters);
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        this.listener.onStandaloneMacro(name, parameters, content);
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        this.listener.onInlineMacro(name, parameters, content);
    }

    public void onNewLine()
    {
        this.listener.onNewLine();
    }

    public void onSpace()
    {
        this.listener.onSpace();
    }

    public void onSpecialSymbol(char symbol)
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
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.listener.onHorizontalLine(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        this.listener.onEmptyLines(count);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        this.listener.onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(String, Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.listener.onVerbatimStandalone(protectedString, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        this.listener.beginDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        this.listener.endDefinitionList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        this.listener.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        this.listener.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.listener.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.listener.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        this.listener.beginQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.listener.endQuotation(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        this.listener.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        this.listener.endQuotationLine();
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.listener.beginTable(parameters);
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.listener.beginTableCell(parameters);
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.listener.beginTableHeadCell(parameters);
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.listener.beginTableRow(parameters);
    }

    public void endTable(Map<String, String> parameters)
    {
        this.listener.endTable(parameters);
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.listener.endTableCell(parameters);
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.listener.endTableHeadCell(parameters);
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.listener.endTableRow(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(Image, boolean, Map)
     * @since 1.7M2
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.listener.onImage(image, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    public void beginError(String message, String description)
    {
        this.listener.beginError(message, description);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     * @since 1.7M3
     */
    public void endError(String message, String description)
    {
        this.listener.endError(message, description);
    }
}
