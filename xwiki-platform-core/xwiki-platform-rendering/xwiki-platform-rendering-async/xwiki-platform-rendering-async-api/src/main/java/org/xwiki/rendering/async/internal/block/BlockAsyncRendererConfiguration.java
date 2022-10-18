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
package org.xwiki.rendering.async.internal.block;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Configuration to pass to {@link BlockAsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class BlockAsyncRendererConfiguration extends AsyncRendererConfiguration
{
    private List<String> id;

    private Block block;

    private XDOM xdom;

    private boolean asyncAllowed;

    private boolean cacheAllowed;

    private Syntax defaultSyntax;

    private String transformationId;

    private Syntax targetSyntax;

    private boolean inline;

    private boolean resricted;

    private BlockAsyncRendererDecorator decorator;

    private Set<EntityReference> references;

    private Set<ComponentRole<?>> roles;

    /**
     * @param idElements the id used as prefix (concatenated with contextual information) for the actual job identifier
     * @param block the block to transform
     */
    public BlockAsyncRendererConfiguration(List<?> idElements, Block block)
    {
        if (idElements != null) {
            this.id = new ArrayList<>(idElements.size());
            addElements(idElements);
        } else {
            this.id = new ArrayList<>();
        }
        this.block = block;

        // Enabled by default
        this.asyncAllowed = true;
    }

    private void addElements(Iterable<?> elements)
    {
        for (Object element : elements) {
            if (element instanceof Iterable) {
                addElements((Iterable<?>) element);
            } else {
                this.id.add(element != null ? element.toString() : null);
            }
        }
    }

    /**
     * @return the id
     */
    public List<String> getId()
    {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public void setId(List<String> id)
    {
        this.id = id;
    }

    /**
     * @return the block
     */
    public Block getBlock()
    {
        return this.block;
    }

    /**
     * @return the XDOM to use in the transformation context
     * @since 14.8RC1
     * @since 14.4.5
     * @since 13.10.10
     */
    public XDOM getXDOM()
    {
        return this.xdom;
    }

    /**
     * @param xdom the XDOM to use in the transformation context
     * @since 14.8RC1
     * @since 14.4.5
     * @since 13.10.10
     */
    public void setXDOM(XDOM xdom)
    {
        this.xdom = xdom;
    }

    /**
     * @return true if the execution should be asynchronous if possible
     */
    public boolean isAsyncAllowed()
    {
        return this.asyncAllowed;
    }

    /**
     * @param asyncAllowed true if the execution should be asynchronous if possible
     */
    public void setAsyncAllowed(boolean asyncAllowed)
    {
        this.asyncAllowed = asyncAllowed;
    }

    /**
     * @return true if the result cache be reused several times
     */
    public boolean isCacheAllowed()
    {
        return cacheAllowed;
    }

    /**
     * @param cacheAllowed true if the result cache be reused several times
     */
    public void setCacheAllowed(boolean cacheAllowed)
    {
        this.cacheAllowed = cacheAllowed;
    }

    /**
     * @return the default syntax
     */
    public Syntax getDefaultSyntax()
    {
        return this.defaultSyntax;
    }

    /**
     * @param defaultSyntax the default syntax
     */
    public void setDefaultSyntax(Syntax defaultSyntax)
    {
        this.defaultSyntax = defaultSyntax;
    }

    /**
     * @return the transformation id to set in the {@link TransformationContext}
     */
    public String getTransformationId()
    {
        return this.transformationId;
    }

    /**
     * @param transformationId the transformation id to set in the {@link TransformationContext}
     */
    public void setTransformationId(String transformationId)
    {
        this.transformationId = transformationId;
    }

    /**
     * @return the target Syntax
     */
    public Syntax getTargetSyntax()
    {
        return this.targetSyntax;
    }

    /**
     * @param targetSyntax the target syntax
     */
    public void setTargetSyntax(Syntax targetSyntax)
    {
        this.targetSyntax = targetSyntax;
    }

    /**
     * @return true if the rendering should be done in an inline context
     */
    public boolean isInline()
    {
        return this.inline;
    }

    /**
     * @param inline true if the rendering should be done in an inline context
     */
    public void setInline(boolean inline)
    {
        this.inline = inline;
    }

    /**
     * @return indicator of whether the transformation context is restricted or not
     * @since 14.9
     * @since 14.4.6
     * @since 13.10.10
     */
    public boolean isResricted()
    {
        return this.resricted;
    }

    /**
     * @param resricted indicator of whether the transformation context is restricted or not
     * @since 14.9
     * @since 14.4.6
     * @since 13.10.10
     */
    public void setResricted(boolean resricted)
    {
        this.resricted = resricted;
    }

    /**
     * @return the decorator
     */
    public BlockAsyncRendererDecorator getDecorator()
    {
        return this.decorator;
    }

    /**
     * @param decorator the decorator
     */
    public void setDecorator(BlockAsyncRendererDecorator decorator)
    {
        this.decorator = decorator;
    }

    /**
     * @return the references involved in the rendering (they will be used to invalidate the cache when one of those
     *         entities is modified). More can be injected by the {@link Block}s trough script macros.
     */
    public Set<EntityReference> getReferences()
    {
        return this.references != null ? this.references : Collections.emptySet();
    }

    /**
     * @param references the references involved in the rendering (they will be used to invalidate the cache when one of
     *            those entities is modified). More can be injected by the {@link Block}s trough script macros.
     */
    public void setReferences(Set<EntityReference> references)
    {
        this.references = new HashSet<>(references);
    }

    /**
     * @param reference the reference to add
     */
    public void useEntity(EntityReference reference)
    {
        if (this.references == null) {
            this.references = new HashSet<>();
        }

        this.references.add(reference);
    }

    /**
     * @return the components involved in the rendering (they will be used to invalidate the cache when one of those
     *         component is modified). More can be injected by the {@link Block}s trough script macros.
     */
    public Set<ComponentRole<?>> getRoles()
    {
        return this.roles != null ? this.roles : Collections.emptySet();
    }

    /**
     * @param roles involved in the rendering (they will be used to invalidate the cache when one of those component is
     *            modified). More can be injected by the {@link Block}s trough script macros.
     */
    public void setRoles(Set<ComponentRole<?>> roles)
    {
        this.roles = roles;
    }

    /**
     * Indicate that the execution manipulate components of the passed type and the result will need to be removed from
     * the cache if any is unregistered or a new one registered.
     * 
     * @param roleType the type of the component role
     */
    public void useComponent(Type roleType)
    {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }

        this.roles.add(new DefaultComponentRole<>(roleType, null));
    }

    /**
     * Indicate that the execution manipulate component wit the passed type and hint and the result will need to be
     * removed from the cache if it's registered or unregistered.
     * 
     * @param roleType the type of the component role
     * @param roleHint the hint of the component
     */
    public void useComponent(Type roleType, String roleHint)
    {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }

        this.roles.add(new DefaultComponentRole<>(roleType, roleHint));
    }
}
