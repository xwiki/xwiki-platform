package org.xwiki.security;

import java.util.List;

import org.xwiki.model.EntityType;

/**
 * Describe a {@link Right}, allow adding new Rights, also implemented by the {@link Right} class.
 *
 * @version $Id$
 */
public interface RightDescription {
    /**
     * @return The string representation of this right.
     */
    String getName();

    /**
     * @return The default state, in case no matching right is found
     * at any level.
     */
    RightState getDefaultState();

    /**
     * @return Whether this right should be allowed or denied in case
     * of a tie.
     */
    RightState getTieResolutionPolicy();

    /**
     * @return Policy on how this right should be overridden by
     * lower levels in the entity reference hierarchy. When true,
     * this right on a document override this right on a wiki.
     */
    boolean getInheritanceOverridePolicy();

    /**
     * @return Additional rights implied by this right.
     */
    List<Right> getImpliedRights();

    /**
     * @return The entity type for which this right should be enabled.
     */
    List<EntityType> getDocumentLevels();
}

