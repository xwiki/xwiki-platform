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
package org.xwiki.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.Request;
import org.restlet.resource.Resource;
import org.restlet.util.Template;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rest.model.XStreamFactory;

import com.thoughtworks.xstream.XStream;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

public class Utils
{
    public static void cleanupResource(Request request, ComponentManager componentManager, Logger logger)
    {
        Resource resource = (Resource) request.getAttributes().get(Constants.RESOURCE_COMPONENT);
        if (resource != null) {
            try {
                logger.log(Level.FINE, String.format("Releasing %s", resource));
                componentManager.release(resource);
            } catch (ComponentLifecycleException e) {
                e.printStackTrace();
            }
        }
    }

    public static String toXml(Object object)
    {
        XStream xstream = XStreamFactory.getXStream();

        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(xstream.toXML(object));

        return sb.toString();
    }

    public static String formatUriTemplate(String uriTemplateString, Map<String, String> values)
    {
        Template uriTemplate = new Template(uriTemplateString);
        return uriTemplate.format(values);
    }

    public static int parseInt(String string, int defaultValue)
    {
        if (string == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getResourceAsString(String name)
    {
        try {
            InputStream is = Utils.class.getClassLoader().getResourceAsStream(name);

            Formatter f = new Formatter();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                f.format("%s\n", line);
            }

            is.close();

            return f.toString();
        } catch (IOException e) {

        }

        return null;
    }

    public static String getPrefixedPageName(String database, String space, String name)
    {
        XWikiDocument xwikiDocument = new XWikiDocument();
        xwikiDocument.setDatabase(database);
        xwikiDocument.setName(name);
        xwikiDocument.setSpace(space);

        Document document = new Document(xwikiDocument, null);

        return document.getPrefixedFullName();
    }

}
