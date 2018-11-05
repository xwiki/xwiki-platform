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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Configuration to pass to {@link BlockAsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class BlockAsyncRendererConfiguration
{
    private List<String> id;

    private Block block;

    private boolean authorReferenceSet;

    private DocumentReference authorReference;

    private boolean async;

    private boolean cached;

    private Syntax defaultSyntax;

    private String transformationId;

    private Syntax targetSyntax;

    private BlockAsyncRendererDecorator decorator;

    private Set<EntityReference> references;

    private Set<ComponentRole<?>> roles;

    /**
     * @param idElements the id used as prefix (concatenated with contextual information) for the actual job identifier
     * @param block the block to transform
     */
    public BlockAsyncRendererConfiguration(List<?> idElements, Block block)
    {
        this.id = new ArrayList<>(idElements.size());
        addElements(idElements);
        this.block = block;

        // Enabled by default
        this.async = true;
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
     * @return true if the reference of the author have been set
     */
    public boolean isAuthorReferenceSet()
    {
        return this.authorReferenceSet;
    }

    /**
     * @return the reference of the author of the code
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @param authorReference the reference of the author of the code
     */
    public void setAuthorReference(DocumentReference authorReference)
    {
        this.authorReference = authorReference;
        this.authorReferenceSet = true;
    }

    /**
     * @return true if the execution should be asynchronous
     */
    public boolean isAsync()
    {
        return this.async;
    }

    /**
     * @param async true if the execution should be asynchronous
     */
    public void setAsync(boolean async)
    {
        this.async = async;
    }

    /**
     * @return true if the result cache be reused several times
     */
    public boolean isCached()
    {
        return cached;
    }

    /**
     * @param cached true if the result cache be reused several times
     */
    public void setCached(boolean cached)
    {
        this.cached = cached;
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
