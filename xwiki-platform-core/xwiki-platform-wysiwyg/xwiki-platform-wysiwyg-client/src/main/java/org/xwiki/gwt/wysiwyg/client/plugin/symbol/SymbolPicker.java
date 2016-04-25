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
 */
package org.xwiki.gwt.wysiwyg.client.plugin.symbol;

import org.xwiki.gwt.user.client.ui.CompositeDialogBox;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Image;

/**
 * A popup panel which allows you to pick a symbol from a symbol palette by clicking on that symbol.
 * 
 * @version $Id$
 */
public class SymbolPicker extends CompositeDialogBox implements SelectionHandler<String>
{
    /**
     * The default list of symbols.
     */
    private static final Object[][] SYMBOLS = {
        {"&nbsp;", "&#160;", true, "no-break space"},
        {"&amp;", "&#38;", true, "ampersand"},
        {"&quot;", "&#34;", true, "quotation mark"},
        // finance
        {"&cent;", "&#162;", true, "cent sign"},
        {"&euro;", "&#8364;", true, "euro sign"},
        {"&pound;", "&#163;", true, "pound sign"},
        {"&yen;", "&#165;", true, "yen sign"},
        // signs
        {"&copy;", "&#169;", true, "copyright sign"},
        {"&reg;", "&#174;", true, "registered sign"},
        {"&trade;", "&#8482;", true, "trade mark sign"},
        {"&permil;", "&#8240;", true, "per mille sign"},
        {"&micro;", "&#181;", true, "micro sign"},
        {"&middot;", "&#183;", true, "middle dot"},
        {"&bull;", "&#8226;", true, "bullet"},
        {"&hellip;", "&#8230;", true, "three dot leader"},
        {"&prime;", "&#8242;", true, "minutes / feet"},
        {"&Prime;", "&#8243;", true, "seconds / inches"},
        {"&sect;", "&#167;", true, "section sign"},
        {"&para;", "&#182;", true, "paragraph sign"},
        {"&szlig;", "&#223;", true, "sharp s / ess-zed"},
        // quotations
        {"&lsaquo;", "&#8249;", true, "single left-pointing angle quotation mark"},
        {"&rsaquo;", "&#8250;", true, "single right-pointing angle quotation mark"},
        {"&laquo;", "&#171;", true, "left pointing guillemet"},
        {"&raquo;", "&#187;", true, "right pointing guillemet"},
        {"&lsquo;", "&#8216;", true, "left single quotation mark"},
        {"&rsquo;", "&#8217;", true, "right single quotation mark"},
        {"&ldquo;", "&#8220;", true, "left double quotation mark"},
        {"&rdquo;", "&#8221;", true, "right double quotation mark"},
        {"&sbquo;", "&#8218;", true, "single low-9 quotation mark"},
        {"&bdquo;", "&#8222;", true, "double low-9 quotation mark"},
        {"&lt;", "&#60;", true, "less-than sign"},
        {"&gt;", "&#62;", true, "greater-than sign"},
        {"&le;", "&#8804;", true, "less-than or equal to"},
        {"&ge;", "&#8805;", true, "greater-than or equal to"},
        {"&ndash;", "&#8211;", true, "en dash"},
        {"&mdash;", "&#8212;", true, "em dash"},
        {"&macr;", "&#175;", true, "macron"},
        {"&oline;", "&#8254;", true, "overline"},
        {"&curren;", "&#164;", true, "currency sign"},
        {"&brvbar;", "&#166;", true, "broken bar"},
        {"&uml;", "&#168;", true, "diaeresis"},
        {"&iexcl;", "&#161;", true, "inverted exclamation mark"},
        {"&iquest;", "&#191;", true, "turned question mark"},
        {"&circ;", "&#710;", true, "circumflex accent"},
        {"&tilde;", "&#732;", true, "small tilde"},
        {"&deg;", "&#176;", true, "degree sign"},
        {"&minus;", "&#8722;", true, "minus sign"},
        {"&plusmn;", "&#177;", true, "plus-minus sign"},
        {"&divide;", "&#247;", true, "division sign"},
        {"&frasl;", "&#8260;", true, "fraction slash"},
        {"&times;", "&#215;", true, "multiplication sign"},
        {"&sup1;", "&#185;", true, "superscript one"},
        {"&sup2;", "&#178;", true, "superscript two"},
        {"&sup3;", "&#179;", true, "superscript three"},
        {"&frac14;", "&#188;", true, "fraction one quarter"},
        {"&frac12;", "&#189;", true, "fraction one half"},
        {"&frac34;", "&#190;", true, "fraction three quarters"},
        // math / logical
        {"&fnof;", "&#402;", true, "function / florin"},
        {"&int;", "&#8747;", true, "integral"},
        {"&sum;", "&#8721;", true, "n-ary sumation"},
        {"&infin;", "&#8734;", true, "infinity"},
        {"&radic;", "&#8730;", true, "square root"},
        {"&sim;", "&#8764;", false, "similar to"},
        {"&cong;", "&#8773;", false, "approximately equal to"},
        {"&asymp;", "&#8776;", true, "almost equal to"},
        {"&ne;", "&#8800;", true, "not equal to"},
        {"&equiv;", "&#8801;", true, "identical to"},
        {"&isin;", "&#8712;", false, "element of"},
        {"&notin;", "&#8713;", false, "not an element of"},
        {"&ni;", "&#8715;", false, "contains as member"},
        {"&prod;", "&#8719;", true, "n-ary product"},
        {"&and;", "&#8743;", false, "logical and"},
        {"&or;", "&#8744;", false, "logical or"},
        {"&not;", "&#172;", true, "not sign"},
        {"&cap;", "&#8745;", true, "intersection"},
        {"&cup;", "&#8746;", false, "union"},
        {"&part;", "&#8706;", true, "partial differential"},
        {"&forall;", "&#8704;", false, "for all"},
        {"&exist;", "&#8707;", false, "there exists"},
        {"&empty;", "&#8709;", false, "diameter"},
        {"&nabla;", "&#8711;", false, "backward difference"},
        {"&lowast;", "&#8727;", false, "asterisk operator"},
        {"&prop;", "&#8733;", false, "proportional to"},
        {"&ang;", "&#8736;", false, "angle"},
        // undefined
        {"&acute;", "&#180;", true, "acute accent"},
        {"&cedil;", "&#184;", true, "cedilla"},
        {"&ordf;", "&#170;", true, "feminine ordinal indicator"},
        {"&ordm;", "&#186;", true, "masculine ordinal indicator"},
        {"&dagger;", "&#8224;", true, "dagger"},
        {"&Dagger;", "&#8225;", true, "double dagger"},
        // alphabetical special chars
        {"&Agrave;", "&#192;", true, "A - grave"}, 
        {"&Aacute;", "&#193;", true, "A - acute"},
        {"&Acirc;", "&#194;", true, "A - circumflex"}, 
        {"&Atilde;", "&#195;", true, "A - tilde"},
        {"&Auml;", "&#196;", true, "A - diaeresis"}, 
        {"&Aring;", "&#197;", true, "A - ring above"},
        {"&AElig;", "&#198;", true, "ligature AE"}, 
        {"&Ccedil;", "&#199;", true, "C - cedilla"},
        {"&Egrave;", "&#200;", true, "E - grave"}, 
        {"&Eacute;", "&#201;", true, "E - acute"},
        {"&Ecirc;", "&#202;", true, "E - circumflex"}, 
        {"&Euml;", "&#203;", true, "E - diaeresis"},
        {"&Igrave;", "&#204;", true, "I - grave"},         
        {"&Iacute;", "&#205;", true, "I - acute"},
        {"&Icirc;", "&#206;", true, "I - circumflex"}, 
        {"&Iuml;", "&#207;", true, "I - diaeresis"},
        {"&ETH;", "&#208;", true, "ETH"}, 
        {"&Ntilde;", "&#209;", true, "N - tilde"},
        {"&Ograve;", "&#210;", true, "O - grave"}, 
        {"&Oacute;", "&#211;", true, "O - acute"},
        {"&Ocirc;", "&#212;", true, "O - circumflex"}, 
        {"&Otilde;", "&#213;", true, "O - tilde"},
        {"&Ouml;", "&#214;", true, "O - diaeresis"},
        {"&Oslash;", "&#216;", true, "O - slash"},
        {"&OElig;", "&#338;", true, "ligature OE"}, 
        {"&Scaron;", "&#352;", true, "S - caron"},
        {"&Ugrave;", "&#217;", true, "U - grave"}, 
        {"&Uacute;", "&#218;", true, "U - acute"},
        {"&Ucirc;", "&#219;", true, "U - circumflex"}, 
        {"&Uuml;", "&#220;", true, "U - diaeresis"},
        {"&Yacute;", "&#221;", true, "Y - acute"},         
        {"&Yuml;", "&#376;", true, "Y - diaeresis"},
        {"&THORN;", "&#222;", true, "THORN"}, 
        {"&agrave;", "&#224;", true, "a - grave"},
        {"&aacute;", "&#225;", true, "a - acute"}, 
        {"&acirc;", "&#226;", true, "a - circumflex"},
        {"&atilde;", "&#227;", true, "a - tilde"}, 
        {"&auml;", "&#228;", true, "a - diaeresis"},
        {"&aring;", "&#229;", true, "a - ring above"}, 
        {"&aelig;", "&#230;", true, "ligature ae"},
        {"&ccedil;", "&#231;", true, "c - cedilla"}, 
        {"&egrave;", "&#232;", true, "e - grave"},
        {"&eacute;", "&#233;", true, "e - acute"}, 
        {"&ecirc;", "&#234;", true, "e - circumflex"},
        {"&euml;", "&#235;", true, "e - diaeresis"}, 
        {"&igrave;", "&#236;", true, "i - grave"},
        {"&iacute;", "&#237;", true, "i - acute"}, 
        {"&icirc;", "&#238;", true, "i - circumflex"},
        {"&iuml;", "&#239;", true, "i - diaeresis"},         
        {"&eth;", "&#240;", true, "eth"},
        {"&ntilde;", "&#241;", true, "n - tilde"}, 
        {"&ograve;", "&#242;", true, "o - grave"},
        {"&oacute;", "&#243;", true, "o - acute"},
        {"&ocirc;", "&#244;", true, "o - circumflex"},
        {"&otilde;", "&#245;", true, "o - tilde"},
        {"&ouml;", "&#246;", true, "o - diaeresis"},
        {"&oslash;", "&#248;", true, "o slash"},
        {"&oelig;", "&#339;", true, "ligature oe"},
        {"&scaron;", "&#353;", true, "s - caron"},
        {"&ugrave;", "&#249;", true, "u - grave"},
        {"&uacute;", "&#250;", true, "u - acute"},
        {"&ucirc;", "&#251;", true, "u - circumflex"},
        {"&uuml;", "&#252;", true, "u - diaeresis"},
        {"&yacute;", "&#253;", true, "y - acute"},
        {"&thorn;", "&#254;", true, "thorn"},
        {"&yuml;", "&#255;", true, "y - diaeresis"},
        {"&Alpha;", "&#913;", true, "Alpha"},
        {"&Beta;", "&#914;", true, "Beta"},
        {"&Gamma;", "&#915;", true, "Gamma"},
        {"&Delta;", "&#916;", true, "Delta"},
        {"&Epsilon;", "&#917;", true, "Epsilon"},
        {"&Zeta;", "&#918;", true, "Zeta"},
        {"&Eta;", "&#919;", true, "Eta"},
        {"&Theta;", "&#920;", true, "Theta"},
        {"&Iota;", "&#921;", true, "Iota"},
        {"&Kappa;", "&#922;", true, "Kappa"},
        {"&Lambda;", "&#923;", true, "Lambda"},
        {"&Mu;", "&#924;", true, "Mu"},
        {"&Nu;", "&#925;", true, "Nu"},
        {"&Xi;", "&#926;", true, "Xi"},
        {"&Omicron;", "&#927;", true, "Omicron"},
        {"&Pi;", "&#928;", true, "Pi"},
        {"&Rho;", "&#929;", true, "Rho"},
        {"&Sigma;", "&#931;", true, "Sigma"},
        {"&Tau;", "&#932;", true, "Tau"},
        {"&Upsilon;", "&#933;", true, "Upsilon"},
        {"&Phi;", "&#934;", true, "Phi"},
        {"&Chi;", "&#935;", true, "Chi"},
        {"&Psi;", "&#936;", true, "Psi"},
        {"&Omega;", "&#937;", true, "Omega"},
        {"&alpha;", "&#945;", true, "alpha"},
        {"&beta;", "&#946;", true, "beta"},
        {"&gamma;", "&#947;", true, "gamma"},
        {"&delta;", "&#948;", true, "delta"},
        {"&epsilon;", "&#949;", true, "epsilon"},
        {"&zeta;", "&#950;", true, "zeta"},
        {"&eta;", "&#951;", true, "eta"},
        {"&theta;", "&#952;", true, "theta"},
        {"&iota;", "&#953;", true, "iota"},
        {"&kappa;", "&#954;", true, "kappa"},
        {"&lambda;", "&#955;", true, "lambda"},
        {"&mu;", "&#956;", true, "mu"},
        {"&nu;", "&#957;", true, "nu"},
        {"&xi;", "&#958;", true, "xi"},
        {"&omicron;", "&#959;", true, "omicron"},
        {"&pi;", "&#960;", true, "pi"},
        {"&rho;", "&#961;", true, "rho"},
        {"&sigmaf;", "&#962;", true, "final sigma"},
        {"&sigma;", "&#963;", true, "sigma"},
        {"&tau;", "&#964;", true, "tau"},
        {"&upsilon;", "&#965;", true, "upsilon"},
        {"&phi;", "&#966;", true, "phi"},
        {"&chi;", "&#967;", true, "chi"},
        {"&psi;", "&#968;", true, "psi"},
        {"&omega;", "&#969;", true, "omega"},
        // symbols
        {"&alefsym;", "&#8501;", false, "alef symbol"},
        {"&piv;", "&#982;", false, "pi symbol"},
        {"&real;", "&#8476;", false, "real part symbol"},
        {"&thetasym;", "&#977;", false, "theta symbol"},
        {"&upsih;", "&#978;", false, "upsilon - hook symbol"},
        {"&weierp;", "&#8472;", false, "Weierstrass p"},
        {"&image;", "&#8465;", false, "imaginary part"},
        // arrows
        {"&larr;", "&#8592;", true, "leftwards arrow"}, 
        {"&uarr;", "&#8593;", true, "upwards arrow"},
        {"&rarr;", "&#8594;", true, "rightwards arrow"},
        {"&darr;", "&#8595;", true, "downwards arrow"},
        {"&harr;", "&#8596;", true, "left right arrow"}, 
        {"&crarr;", "&#8629;", false, "carriage return"},
        {"&lArr;", "&#8656;", false, "leftwards double arrow"}, 
        {"&uArr;", "&#8657;", false, "upwards double arrow"},
        {"&rArr;", "&#8658;", false, "rightwards double arrow"},
        {"&dArr;", "&#8659;", false, "downwards double arrow"},
        {"&hArr;", "&#8660;", false, "left right double arrow"}, 
        {"&there4;", "&#8756;", false, "therefore"},
        {"&sub;", "&#8834;", false, "subset of"}, 
        {"&sup;", "&#8835;", false, "superset of"},
        {"&nsub;", "&#8836;", false, "not a subset of"}, 
        {"&sube;", "&#8838;", false, "subset of or equal to"},
        {"&supe;", "&#8839;", false, "superset of or equal to"}, 
        {"&oplus;", "&#8853;", false, "circled plus"},
        {"&otimes;", "&#8855;", false, "circled times"}, 
        {"&perp;", "&#8869;", false, "perpendicular"},
        {"&sdot;", "&#8901;", false, "dot operator"}, 
        {"&lceil;", "&#8968;", false, "left ceiling"},
        {"&rceil;", "&#8969;", false, "right ceiling"},
        {"&lfloor;", "&#8970;", false, "left floor"},
        {"&rfloor;", "&#8971;", false, "right floor"}, 
        {"&lang;", "&#9001;", false, "left-pointing angle bracket"},
        {"&rang;", "&#9002;", false, "right-pointing angle bracket"}, 
        {"&loz;", "&#9674;", true, "lozenge"},
        {"&spades;", "&#9824;", false, "black spade suit"}, 
        {"&clubs;", "&#9827;", true, "black club suit"},
        {"&hearts;", "&#9829;", true, "black heart suit"}, 
        {"&diams;", "&#9830;", true, "black diamond suit"},
        {"&ensp;", "&#8194;", false, "en space"}, 
        {"&emsp;", "&#8195;", false, "em space"},
        {"&thinsp;", "&#8201;", false, "thin space"}, 
        {"&zwnj;", "&#8204;", false, "zero width non-joiner"},
        {"&zwj;", "&#8205;", false, "zero width joiner"},
        {"&lrm;", "&#8206;", false, "left-to-right mark"},
        {"&rlm;", "&#8207;", false, "right-to-left mark"}, 
        {"&shy;", "&#173;", false, "soft hyphen"}};

    /**
     * The default number of rows in the grid that makes up the symbol palette.
     */
    private static final int SYMBOLS_PER_ROW = 20;

    /**
     * The default number of columns in the grid that makes up the symbol palette.
     */
    private static final int SYMBOLS_PER_COL = 10;

    /**
     * The symbol palette used for picking the symbol.
     */
    private final SymbolPalette symbolPalette;

    /**
     * Creates a new symbol picker using the default list of symbols.
     */
    public SymbolPicker()
    {
        super(false, true);

        getDialog().setIcon(new Image(Images.INSTANCE.charmap()));
        getDialog().setCaption(Strings.INSTANCE.charmap());
        getDialog().addStyleName("xSymbolPicker");

        symbolPalette = new SymbolPalette(SYMBOLS, SYMBOLS_PER_ROW, SYMBOLS_PER_COL);
        symbolPalette.addSelectionHandler(this);

        initWidget(symbolPalette);
    }

    @Override
    public void onSelection(SelectionEvent<String> event)
    {
        if (event.getSource() == symbolPalette) {
            hide();
        }
    }

    /**
     * @return the selected symbol
     */
    public String getSymbol()
    {
        return symbolPalette.getSelectedSymbol();
    }

    @Override
    public void center()
    {
        // Reset the selected symbol each time the symbol picker is shown.
        symbolPalette.setSelectedSymbol(null);

        super.center();
    }
}
