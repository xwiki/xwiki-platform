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
package org.xwiki.security.authorization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Formatter;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.model.EntityType;

import static org.xwiki.security.SecurityReference.FARM;
import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.DENY;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Enumeration of the possible rights.
 * @version $Id$
 * @since 4.0M2
 */
public class Right implements RightDescription, Serializable, Comparable<Right>
{
    /** The login access right. */
    public static final Right LOGIN;

    /** The view access right. */
    public static final Right VIEW;

    /** The edit access right. */
    public static final Right EDIT;

    /** The delete access right. */
    public static final Right DELETE;

    /** The Admin access right. */
    public static final Right ADMIN;

    /** The program access right. */
    public static final Right PROGRAM;

    /** The register access right. */
    public static final Right REGISTER;

    /** The comment access right. */
    public static final Right COMMENT;

    /** Illegal value. */
    public static final Right ILLEGAL;

    /** Targeted entity type list to target only the main wiki. */
    public static final Set<EntityType> FARM_ONLY = null;

    /** Targeted entity type list to target only wikis (including main wiki). */
    public static final Set<EntityType> WIKI_ONLY = EnumSet.of(EntityType.WIKI);

    /** Targeted entity type list to target wikis and spaces. */
    public static final Set<EntityType> WIKI_SPACE = EnumSet.of(EntityType.WIKI, EntityType.SPACE);

    /** Targeted entity type list to target wikis, spaces and documents. */
    public static final Set<EntityType> WIKI_SPACE_DOCUMENT
        = EnumSet.of(EntityType.WIKI, EntityType.SPACE, EntityType.DOCUMENT);

    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /** Internal list of existing instances. */
    private static final List<Right> VALUES = new ArrayList<Right>();

    /** Unmodifiable list of existing instance for public dissemination. */
    private static final List<Right> UNMODIFIABLE_VALUES = Collections.unmodifiableList(VALUES);

    /** List of all rights, as strings. */
    private static final List<String> ALL_RIGHTS = new LinkedList<String>();

    /** List of all rights, as strings. */
    private static final List<String> UNMODIFIABLE_ALL_RIGHTS = Collections.unmodifiableList(ALL_RIGHTS);

    /**
     * The enabled rights by entity types.  There is a special case hardcoded : The PROGRAM
     * right should only be enabled for the main wiki, not for wikis in general.
     */
    private static final Map<EntityType, Set<Right>> ENABLED_RIGHTS = new HashMap<EntityType, Set<Right>>();

    /**
     * The enabled rights by entity types, but with unmodifiable list for getters.
     */
    private static final Map<EntityType, Set<Right>> UNMODIFIABLE_ENABLED_RIGHTS
        = new HashMap<EntityType, Set<Right>>();

    static {
        LOGIN    = new Right("login",       ALLOW,  ALLOW, true,  null, WIKI_ONLY          , true);
        VIEW     = new Right("view",        ALLOW,  DENY,  true,  null, WIKI_SPACE_DOCUMENT, true);
        EDIT     = new Right("edit",        ALLOW,  DENY,  true,  null, WIKI_SPACE_DOCUMENT, false);
        DELETE   = new Right("delete",      DENY,   DENY,  true,  null, WIKI_SPACE_DOCUMENT, false);
        REGISTER = new Right("register",    ALLOW,  ALLOW, false, null, WIKI_ONLY          , false);
        COMMENT  = new Right("comment",     ALLOW,  DENY,  true,  null, WIKI_SPACE_DOCUMENT, false);

        PROGRAM  = new Right("programming", DENY,   ALLOW, false,
            new RightSet(LOGIN, VIEW, EDIT, DELETE, REGISTER, COMMENT), FARM_ONLY         , true);

        ADMIN    = new Right("admin",       DENY,   ALLOW, false,
            new RightSet(LOGIN, VIEW, EDIT, DELETE, REGISTER, COMMENT, PROGRAM), WIKI_SPACE, true);

        ILLEGAL  = new Right("illegal",     DENY,   DENY,  false, null, null               , false);
    }

    /** The numeric value of this access right. */
    private final int value;

    /** The string representation. */
    private final String name;

    /** The string representation. */
    private final RuleState defaultState;

    /** Whether this right should be allowed or denied in case of a tie. */
    private final RuleState tieResolutionPolicy;

    /** Policy on how this right should be overridden by lower levels. */
    private final boolean inheritanceOverridePolicy;

    /** Additional rights implied by this right. */
    private final Set<Right> impliedRights;

    /** Additional rights implied by this right. */
    private final boolean isReadOnly;

    /**
     * Construct a new Right from its description.
     * This is a package private constructor, the registration of a new right should be done using
     * the {@link AuthorizationManager}
     *
     * @param description Description of the right to create.
     */
    Right(RightDescription description)
    {
        this(description.getName(), description.getDefaultState(), description.getTieResolutionPolicy(),
            description.getInheritanceOverridePolicy(),
            description.getImpliedRights(),
            description.getTargetedEntityType(), description.isReadOnly());
    }

    /**
     * Construct a new Right.
     * @param name The string representation of this right.
     * @param defaultState The default state, in case no matching right is found at any level.
     * @param tieResolutionPolicy Whether this right should be allowed or denied in case of a tie.
     * @param inheritanceOverridePolicy Policy on how this right should be overridden by lower levels.
     * @param impliedRights Additional rights implied by this right.
     * @param validEntityTypes The type of entity where this right should be enabled.
     * @param isReadOnly If true, this right could be allowed when the wiki is in read-only mode.
     */
    private Right(String name, RuleState defaultState, RuleState tieResolutionPolicy,
        boolean inheritanceOverridePolicy, Set<Right> impliedRights, Set<EntityType> validEntityTypes,
        boolean isReadOnly)
    {
        checkIllegalArguments(name, defaultState, tieResolutionPolicy);

        this.name = name;
        this.defaultState = defaultState;
        this.tieResolutionPolicy = tieResolutionPolicy;
        this.inheritanceOverridePolicy = inheritanceOverridePolicy;
        this.impliedRights = cloneImpliedRights(impliedRights);
        this.isReadOnly = isReadOnly;

        synchronized (VALUES) {
            this.value = VALUES.size();
            if (this.value >= 64) {
                throw new IndexOutOfBoundsException();
            }
            VALUES.add(this);
            ALL_RIGHTS.add(name);
            if (validEntityTypes != null) {
                for (EntityType type : validEntityTypes) {
                    if (type == EntityType.WIKI) {
                        // If enabled on a wiki, enable also on main wiki.
                        enableFor(FARM);
                    }
                    enableFor(type);
                }
            } else {
                // If enabled on a wiki, enable also on main wiki.
                enableFor(FARM);
            }
        }
    }

    /**
     * Enable this right for the given entity type.
     * @param type the entity type, null for the the main wiki.
     */
    private void enableFor(EntityType type)
    {
        Set<Right> rights = ENABLED_RIGHTS.get(type);
        if (rights == null) {
            rights = new RightSet();
            ENABLED_RIGHTS.put(type, rights);
            UNMODIFIABLE_ENABLED_RIGHTS.put(type, Collections.unmodifiableSet(rights));
        }
        rights.add(this);
    }

    /**
     * Check for illegal arguments.
     *
     * @param name The string representation of this right.
     * @param defaultState The default state, in case no matching right is found at any level.
     * @param tieResolutionPolicy Whether this right should be allowed or denied in case of a tie.
     */
    private void checkIllegalArguments(String name, RuleState defaultState, RuleState tieResolutionPolicy)
    {
        if (name == null || ALL_RIGHTS.contains(name)) {
            throw new IllegalArgumentException(new Formatter()
                                               .format("Duplicate name for right [%s]", name).toString());
        }

        if (defaultState == null || defaultState == UNDETERMINED) {
            throw new IllegalArgumentException(new Formatter()
                                               .format("Invalid default state [%s] for right [%s]", defaultState, name)
                                               .toString());
        }

        if (tieResolutionPolicy == null || tieResolutionPolicy == UNDETERMINED) {
            throw new IllegalArgumentException(new Formatter()
                                               .format("Invalid tie resolution policy [%s] for right [%s]",
                                                       tieResolutionPolicy,
                                                       name)
                                               .toString());
        }
    }

    /**
     * Clone implied Rights.
     * @param impliedRights the collection of rights to clone.
     * @return the cloned collection or null if no valid implied right has been provided.
     */
    private Set<Right> cloneImpliedRights(Set<Right> impliedRights)
    {
        if (impliedRights == null || impliedRights.size() == 0) {
            return null;
        }

        Set<Right> implied = new RightSet(impliedRights);

        if (implied.size() > 0) {
            return Collections.unmodifiableSet(implied);
        } else {
            return null;
        }
    }

    /**
     * @return an unmodifiable list of available Right
     */
    public static List<Right> values()
    {
        return UNMODIFIABLE_VALUES;
    }

    /**
     * Convert a string to a right.
     * @param string String representation of right.
     * @return The corresponding Right instance, or {@code ILLEGAL}.
     */
    public static Right toRight(String string)
    {
        for (Right right : VALUES) {
            if (right.name.equals(string)) {
                return right;
            }
        }
        return ILLEGAL;
    }

    /**
     * Returns the list of rights available for a given entity type.
     *
     * @param entityType the entity type, or null for main wiki.
     * @return a list of {@code Right} enabled of this entity type
     */
    public static Set<Right> getEnabledRights(EntityType entityType)
    {
        return UNMODIFIABLE_ENABLED_RIGHTS.get(entityType);
    }

    /**
     * Retrieve a right based on its ordinal.
     * @param ordinal the ordinal of the right
     * @return the {@code Right}
     */
    public static Right get(int ordinal) {
        return VALUES.get(ordinal);
    }

    /**
     * @return the count of all existing rights
     */
    public static int size() {
        return values().size();
    }

    /**
     * @return a list of the string representation of all valid rights.
     */
    public static List<String> getAllRightsAsString()
    {
        return UNMODIFIABLE_ALL_RIGHTS;
    }

    /**
     * @return The numeric value of this access right.
     */
    public int ordinal()
    {
        return value;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public Set<Right> getImpliedRights()
    {
        return impliedRights;
    }

    @Override
    public Set<EntityType> getTargetedEntityType()
    {
        List<EntityType> levels = new ArrayList<EntityType>();
        for (Map.Entry<EntityType, Set<Right>> entry : ENABLED_RIGHTS.entrySet()) {
            if (entry.getValue().contains(this)) {
                levels.add(entry.getKey());
            }
        }
        if (levels.contains(null) && levels.contains(EntityType.WIKI)) {
            levels.remove(null);
        } else {
            return null;
        }
        return EnumSet.copyOf(levels);
    }

    @Override
    public boolean getInheritanceOverridePolicy()
    {
        return inheritanceOverridePolicy;
    }

    @Override
    public RuleState getTieResolutionPolicy()
    {
        return tieResolutionPolicy;
    }

    @Override
    public RuleState getDefaultState()
    {
        return this.defaultState;
    }

    @Override
    public boolean isReadOnly()
    {
        return this.isReadOnly;
    }

    @Override
    public int compareTo(Right other)
    {
        return this.ordinal() - other.ordinal();
    }

    /**
     * @param description a right description to compare this right to.
     * @return true if the right is equivalent to the provided description.
     */
    boolean like(RightDescription description)
    {
        return new EqualsBuilder()
            .append(this.isReadOnly(), description.isReadOnly())
            .append(this.getDefaultState(), description.getDefaultState())
            .append(this.getTieResolutionPolicy(), description.getTieResolutionPolicy())
            .append(this.getInheritanceOverridePolicy(), description.getInheritanceOverridePolicy())
            .append(this.getTargetedEntityType(), description.getTargetedEntityType())
            .append(this.getImpliedRights(), description.getImpliedRights())
            .isEquals();
    }
}
