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
package org.xwiki.rest.script;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Generate REST URLs for different types of resources.
 *  
 * @version $Id$
 * @since 7.2M1 
 */
@Component
@Named("rest")
@Singleton
public class RESTScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> contextProvider;
    
    private List<String> getSpaceList(List<SpaceReference> spaceReferences)
    {
        List<String> spaces = new ArrayList<>(spaceReferences.size());
        for (SpaceReference spaceReference : spaceReferences) {
            spaces.add(spaceReference.getName());
        }
        return spaces;
    }
    
    private URI getBaseURI()
    {
        // TODO: improve this method (too much use of oldcore)
        try {
            XWikiContext context = contextProvider.get();
            String baseURL = context.getURLFactory().getServerURL(context).toString();
            baseURL += context.getRequest().getContextPath();
            baseURL += "/rest";
            return new URI(baseURL);
        } catch (MalformedURLException | URISyntaxException e) {
            // should never happen
            return null;
        }
    }

    /**
     * @param documentReference a document reference
     * @return the REST URL corresponding to the referenced document
     */
    public URL url(DocumentReference documentReference)
    {
        try {
            return Utils.createURI(getBaseURI(), PageResource.class,
                documentReference.getWikiReference().getName(), getSpaceList(documentReference.getSpaceReferences()),
                    documentReference.getName()).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
