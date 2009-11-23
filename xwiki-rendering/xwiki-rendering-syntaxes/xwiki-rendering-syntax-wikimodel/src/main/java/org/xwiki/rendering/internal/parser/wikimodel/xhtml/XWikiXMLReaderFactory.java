package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import javax.xml.parsers.ParserConfigurationException;

import org.wikimodel.wem.xhtml.filter.AccumulationXMLFilter;
import org.wikimodel.wem.xhtml.filter.DTDXMLFilter;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.xml.EntityResolver;
import org.xwiki.xml.XMLReaderFactory;

/**
 * Creates XML Readers that have the following characteristics:
 * <ul>
 * <li>Use DTD caching when the underlying XML parser is Xerces</li>
 * <li>Ignore SAX callbacks when the parser parses the DTD</li>
 * <li>Accumulate onCharacters() calls since SAX parser may normally call this event several times.</li>
 * <li>Remove non-semantic white spaces where needed</li>
 * <li>Resolve DTDs locally to speed DTD loading/validation</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.1RC1
 */
@Component("xwiki")
public class XWikiXMLReaderFactory implements XMLReaderFactory
{
    /**
     * Used to create an optimized SAX XML Reader. In general SAX parsers don't cache DTD grammars and as a consequence
     * parsing a document with a grammar such as the XHTML DTD takes a lot more time than required.
     */
    @Requirement
    private XMLReaderFactory xmlReaderFactory;

    /**
     * In order to speed up DTD loading/validation we use an entity resolver that can resolve DTDs locally.
     */
    @Requirement
    protected EntityResolver entityResolver;

    public XMLReader createXMLReader() throws SAXException, ParserConfigurationException
    {
        XMLReader xmlReader;

        try {
            // Use a performant XML Reader (which does DTD caching for Xerces)
            XMLReader xr = this.xmlReaderFactory.createXMLReader();

            // Ignore SAX callbacks when the parser parses the DTD
            DTDXMLFilter dtdFilter = new DTDXMLFilter(xr);

            // Add a XML Filter to accumulate onCharacters() calls since SAX
            // parser may call it several times.
            AccumulationXMLFilter accumulationFilter = new AccumulationXMLFilter(dtdFilter);

            // Add a XML Filter to remove non-semantic white spaces. We need to do that since all WikiModel
            // events contain only semantic information.
            XWikiXHTMLWhitespaceXMLFilter whitespaceFilter = new XWikiXHTMLWhitespaceXMLFilter(accumulationFilter);

            whitespaceFilter.setEntityResolver(this.entityResolver);

            xmlReader = whitespaceFilter;
        } catch (Exception e) {
            throw new SAXException("Failed to create XML reader", e);
        }

        return xmlReader;
    }
}
