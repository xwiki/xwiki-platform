/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 17 mars 2004
 * Time: 15:10:09
 */

/**
 * Crude extension to org.apache.commons.digester.rss.RSSDigester
 * that digests RSS 1.0 documents (the kind with &lt;rdf:RDF&gt; as their
 * root element).  This would break if someone used a non-standard
 * prefix for the 'rdf' namespace, because Digester's namespace support
 * is not very flexible.
 * @since 0.2d
 * @author Joe Germuska
 * @version 0.2d
 */

package com.xpn.xwiki.render.macro.rss;

import org.apache.commons.digester.rss.*;

public class DeluxeRSSDigester extends RSSDigester {
    protected void configure() {
        if (this.configured) return;
        super.configure();
        // add rules for RSS 1.0 (and 0.91?)
        this.setNamespaceAware(false);

        this.addObjectCreate("rdf:RDF", Channel.class);
        this.addCallMethod("rdf:RDF/channel/title","setTitle", 1);
        this.addCallParam("rdf:RDF/channel/title",0);
        this.addCallMethod("rdf:RDF/channel/link","setLink", 1);
        this.addCallParam("rdf:RDF/channel/link",0);
        this.addCallMethod("rdf:RDF/channel/description","setDescription", 1);
        this.addCallParam("rdf:RDF/channel/description",0);

        this.addObjectCreate("rdf:RDF/image", Image.class);
        this.addCallMethod("rdf:RDF/image/title","setTitle", 1);
        this.addCallParam("rdf:RDF/image/title",0);
        this.addCallMethod("rdf:RDF/image/link","setLink", 1);
        this.addCallParam("rdf:RDF/image/link",0);
        this.addCallMethod("rdf:RDF/image/description","setDescription", 1);
        this.addCallParam("rdf:RDF/image/description",0);
        this.addCallMethod("rdf:RDF/image/url","setURL", 1);
        this.addCallParam("rdf:RDF/image/url",0);
        this.addSetRoot("rdf:RDF/image", "setImage");

        this.addObjectCreate("rdf:RDF/textinput", TextInput.class);
        this.addCallMethod("rdf:RDF/textinput/title","setTitle", 1);
        this.addCallParam("rdf:RDF/textinput/title",0);
        this.addCallMethod("rdf:RDF/textinput/link","setLink", 1);
        this.addCallParam("rdf:RDF/textinput/link",0);
        this.addCallMethod("rdf:RDF/textinput/description","setDescription", 1);
        this.addCallParam("rdf:RDF/textinput/description",0);
        this.addCallMethod("rdf:RDF/textinput/name","setName", 1);
        this.addCallParam("rdf:RDF/textinput/name",0);
        this.addSetRoot("rdf:RDF/textinput", "setTextInput");

        this.addObjectCreate("rdf:RDF/item", Item.class);
        this.addCallMethod("rdf:RDF/item/title","setTitle", 1);
        this.addCallParam("rdf:RDF/item/title",0);
        this.addCallMethod("rdf:RDF/item/link","setLink", 1);
        this.addCallParam("rdf:RDF/item/link",0);
        this.addCallMethod("rdf:RDF/item/description","setDescription", 1);
        this.addCallParam("rdf:RDF/item/description",0);
        this.addSetRoot("rdf:RDF/item", "addItem");

        this.configured = true;
    }

}

