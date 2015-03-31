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
package com.xpn.xwiki.internal.skin;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.skin.Resource;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.skin.Skin;

/**
 * @version $Id$
 * @since 6.4M1
 */
public abstract class AbstractSkin implements Skin
{
    protected Skin VOID = new Skin()
    {
        @Override
        public Resource<?> getResource(String resource)
        {
            return null;
        }

        @Override
        public Resource<?> getLocalResource(String resource)
        {
            return null;
        }

        @Override
        public Skin getParent()
        {
            return null;
        }

        @Override
        public String getId()
        {
            return null;
        }

        @Override
        public Syntax getOutputSyntax()
        {
            return null;
        }
    };

    protected InternalSkinManager skinManager;

    protected InternalSkinConfiguration configuration;

    protected String id;

    protected Skin parent;

    private Logger logger;
    
    private SyntaxFactory syntaxFactory;
    
    public AbstractSkin(String id, InternalSkinManager skinManager, InternalSkinConfiguration configuration,
        Logger logger, SyntaxFactory syntaxFactory)
    {
        this.id = id;
        this.skinManager = skinManager;
        this.configuration = configuration;
        this.logger = logger;
        this.syntaxFactory = syntaxFactory;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Skin getParent()
    {
        if (this.parent == null) {
            this.parent = createParent();

            if (this.parent == null) {
                this.parent = this.skinManager.getSkin(this.configuration.getDefaultParentSkinId());
            }
        }

        return this.parent;
    }

    @Override
    public Resource<?> getResource(String resourceName)
    {
        Resource<?> resource = getLocalResource(resourceName);

        if (resource == null) {
            // Make sure to not try several times the same skin
            Set<String> skins = new HashSet<String>();
            skins.add(getId());
            for (ResourceRepository parent = getParent(); parent != null && resource == null
                && !skins.contains(parent.getId()); parent = parent.getParent()) {
                resource = parent.getLocalResource(resourceName);
                skins.add(parent.getId());
            }
        }

        return resource;
    }

    protected abstract Skin createParent();

    @Override
    public Syntax getOutputSyntax()
    {
        Syntax targetSyntax = null;
        String targetSyntaxString = getOutputSyntaxString();
        if (StringUtils.isNotEmpty(targetSyntaxString)) {
            targetSyntax = parseSyntax(this, targetSyntaxString);
            if (targetSyntax != null) {
                return targetSyntax;
            }
        }
        
        Skin parent = getParent();
        if (parent != null) {
            targetSyntax = parent.getOutputSyntax();
        }
        
        // Fallback to the XHTML 1.0 syntax for backward compatibility
        return targetSyntax != null ? targetSyntax : Syntax.XHTML_1_0;
    }

    /**
     * @return the id of the syntax to use for this skin
     */
    protected abstract String getOutputSyntaxString();

    private Syntax parseSyntax(Skin skin, String syntax)
    {
        try {
            return syntaxFactory.createSyntaxFromIdString(syntax);
        } catch (ParseException e) {
            logger.warn("Failed to parse the syntax [{}] configured by the skin [{}].",
                    syntax, skin.getId());
        }
        
        // let getOutputSyntax() do the proper fallback
        return null;
    }
}
