package org.xwiki.url.internal;

import java.util.List;

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceType;

/**
 * The reference of a {@link ResourceReferenceHandler} leading to other sub-{@link ResourceReferenceHandler}s.
 * 
 * @version $Id$
 * @since 10.2RC1
 */
public class ParentResourceReference extends AbstractResourceReference
{
    private String rootPath;

    private String child;

    private List<String> childSegments;

    /**
     * Default constructor.
     * 
     * @param rootPath the path starting with the child
     * @param child the child handler hint
     * @param pathSegments the rest of the path
     */
    public ParentResourceReference(ResourceType type, String rootPath, String child, List<String> pathSegments)
    {
        setType(type);

        this.rootPath = rootPath;
        this.child = child;
        this.childSegments = pathSegments;
    }

    /**
     * @return the path starting with the child
     */
    public String getRootPath()
    {
        return this.rootPath;
    }

    /**
     * @return the child hint
     */
    public String getChild()
    {
        return this.child;
    }

    /**
     * @return the child path (elements after the child)
     */
    public List<String> getPathSegments()
    {
        return this.childSegments;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("path = ");
        builder.append(getRootPath());
        builder.append(", child = ");
        builder.append(getChild());
        builder.append(", pathSegments = ");
        builder.append(getPathSegments());

        return builder.toString();
    }
}
