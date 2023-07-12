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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /** Imply rights provided to creator of a document. */
    public static final Right CREATOR;

    /** The Admin access right. */
    public static final Right ADMIN;

    /** The program access right. */
    public static final Right PROGRAM;

    /** The script access right. */
    public static final Right SCRIPT;

    /** The register access right. */
    public static final Right REGISTER;

    /** The comment access right. */
    public static final Right COMMENT;

    /** The creation of a Wiki right. */
    public static final Right CREATE_WIKI;

    /** Illegal value. */
    public static final Right ILLEGAL;

    /** Illegal right name. */
    public static final String ILLEGAL_RIGHT_NAME = "illegal";

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
    private static final List<Right> VALUES = new CopyOnWriteArrayList<>();

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
    private static final Map<EntityType, Set<Right>> ENABLED_RIGHTS = new HashMap<>();

    /**
     * The enabled rights by entity types, but with unmodifiable list for getters.
     */
    private static final Map<EntityType, Set<Right>> UNMODIFIABLE_ENABLED_RIGHTS = new HashMap<>();

    /**
     * The rights that are statically initialized, to be distinguished with the custom rights that can be registered
     * through the components.
     */
    private static final List<Right> STANDARD_RIGHTS;

    static {
        LOGIN = new Right("login", ALLOW, ALLOW, true, null, WIKI_ONLY, true);
        VIEW = new Right("view", ALLOW, DENY, true, null, WIKI_SPACE_DOCUMENT, true);
        EDIT = new Right("edit", ALLOW, DENY, true, new RightSet(VIEW), WIKI_SPACE_DOCUMENT, false);
        DELETE = new Right("delete", DENY, DENY, true, Collections.singleton(VIEW), WIKI_SPACE_DOCUMENT, false);
        CREATOR =
            new Right("creator", DENY, ALLOW, false, new RightSet(DELETE), EnumSet.of(EntityType.DOCUMENT), false);
        REGISTER = new Right("register", ALLOW, ALLOW, true, null, WIKI_ONLY, false);
        COMMENT = new Right("comment", ALLOW, DENY, true, null, WIKI_SPACE_DOCUMENT, false);
        SCRIPT = new Right("script", DENY, DENY, true, null, WIKI_SPACE_DOCUMENT, true);

        ADMIN = new Right("admin", DENY, ALLOW, false,
            new RightSet(LOGIN, VIEW, SCRIPT, EDIT, DELETE, REGISTER, COMMENT), WIKI_SPACE, true);

        CREATE_WIKI = new Right("createwiki", DENY, DENY, true, null, FARM_ONLY, false);

        PROGRAM = new Right("programming", DENY, ALLOW, false,
            new RightSet(LOGIN, VIEW, SCRIPT, EDIT, DELETE, REGISTER, COMMENT, ADMIN, CREATE_WIKI), FARM_ONLY, true);

        ILLEGAL = new Right(ILLEGAL_RIGHT_NAME, DENY, DENY, false, null, null, false);

        STANDARD_RIGHTS = Collections.unmodifiableList(Arrays.asList(
            LOGIN, VIEW, EDIT, DELETE, CREATOR, REGISTER, COMMENT, SCRIPT, ADMIN, CREATE_WIKI, PROGRAM, ILLEGAL
        ));
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

    /**
     * Immutable instance of the Additional rights implied by this right.
     */
    private transient Set<Right> immutableImpliedRights;

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
     * Construct a new Right from its description.
     * This is a package private constructor, the registration of a new right should be done using
     * the {@link AuthorizationManager}
     *
     * @param description Description of the right to create.
     * @param impliedByRights the already existing rights that imply this new right.
     * @since 12.6
     */
    Right(RightDescription description, Set<Right> impliedByRights)
    {
        this(description.getName(), description.getDefaultState(), description.getTieResolutionPolicy(),
            description.getInheritanceOverridePolicy(),
            description.getImpliedRights(),
            description.getTargetedEntityType(), description.isReadOnly(), impliedByRights);
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
        this(name, defaultState, tieResolutionPolicy, inheritanceOverridePolicy, impliedRights, validEntityTypes,
            isReadOnly, Collections.emptySet());
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
     * @param impliedByRights Rights that imply the new right we are adding.
     * @since 12.6
     */
    private Right(String name, RuleState defaultState, RuleState tieResolutionPolicy,
        boolean inheritanceOverridePolicy, Set<Right> impliedRights, Set<EntityType> validEntityTypes,
        boolean isReadOnly, Set<Right> impliedByRights)
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
            if (!name.equals(ILLEGAL_RIGHT_NAME)) {
                ALL_RIGHTS.add(name);
            }
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

            for (Right impliedByRight : impliedByRights) {
                impliedByRight.impliedRights.add(this);
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
        if (name == null || ALL_RIGHTS.contains(name) || (ILLEGAL != null && name.equals(ILLEGAL_RIGHT_NAME))) {
            throw new IllegalArgumentException(String.format("Duplicate name for right [%s]", name));
        }

        if (defaultState == null || defaultState == UNDETERMINED) {
            throw new IllegalArgumentException(
                String.format("Invalid default state [%s] for right [%s]", defaultState, name));
        }

        if (tieResolutionPolicy == null || tieResolutionPolicy == UNDETERMINED) {
            throw new IllegalArgumentException(
                String.format("Invalid tie resolution policy [%s] for right [%s]", tieResolutionPolicy, name));
        }
    }

    /**
     * Clone implied Rights.
     * @param impliedRights the collection of rights to clone.
     * @return the cloned collection or an empty RightSet.
     */
    private Set<Right> cloneImpliedRights(Set<Right> impliedRights)
    {
        if (impliedRights == null || impliedRights.isEmpty()) {
            // We don't return null here since we want to be able to modify
            // the set of other rights in constructor.
            return new RightSet();
        }

        return new RightSet(impliedRights);
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
            if (right.name.equalsIgnoreCase(string)) {
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
        Set<Right> enabledRights = UNMODIFIABLE_ENABLED_RIGHTS.get(entityType);
        if (enabledRights == null) {
            enabledRights = Collections.emptySet();
        }
        return enabledRights;
    }

    /**
     * Retrieve a right based on its ordinal.
     * @param ordinal the ordinal of the right
     * @return the {@code Right}
     */
    public static Right get(int ordinal)
    {
        return VALUES.get(ordinal);
    }

    /**
     * @return the count of all existing rights
     */
    public static int size()
    {
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
     * @return the list of statically registered rights.
     * @since 13.5RC1
     */
    public static List<Right> getStandardRights()
    {
        return STANDARD_RIGHTS;
    }

    /**
     * Remove all occurrences of the current right.
     * This method removes the right from the list of registered right, but also removes it from the map of rights
     * associated to the entity types, and to the different lists of implied rights.
     *
     * @since 13.5RC1
     */
    void unregister()
    {
        Set<EntityType> entityTypes = this.getTargetedEntityType();
        synchronized (VALUES) {
            VALUES.remove(this);
            ALL_RIGHTS.remove(this.name);
            if (entityTypes != null) {
                for (EntityType type : entityTypes) {
                    if (type == EntityType.WIKI) {
                        // If enabled on a wiki, remove also on main wiki.
                        removeFromEnabledRights(FARM);
                    }
                    removeFromEnabledRights(type);
                }
            } else {
                // If enabled on a wiki, enable also on main wiki.
                removeFromEnabledRights(FARM);
            }

            for (Right impliedByRight : VALUES) {
                impliedByRight.impliedRights.remove(this);
            }
        }
    }

    private void removeFromEnabledRights(EntityType type)
    {
        Set<Right> rights = ENABLED_RIGHTS.get(type);
        rights.remove(this);
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
        // We only assign an immutable set value if the implied right is not empty:
        // if it's empty, we keep returning a null value for backward compatibility and performance reasons.
        if (this.immutableImpliedRights == null && !impliedRights.isEmpty()) {
            this.immutableImpliedRights = Collections.unmodifiableSet(this.impliedRights);
        }
        return this.immutableImpliedRights;
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
        if (levels.contains(null)) {
            if (levels.contains(EntityType.WIKI)) {
                levels.remove(null);
            } else {
                return null;
            }
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
            .isEquals() && this.likeImpliedRightsFrom(description);
    }

    /**
     * Allow to verify that implied rights are equals.
     * This method returns {@code true} even if the current instance returns an empty set and the description null,
     * and vice versa. For other cases we rely on an usual EqualsBuilder check.
     * This is a bulletproof method used in {@link #like(RightDescription)} since there's no guarantee that
     * {@link #getImpliedRights()} returns an empty set or a null value.
     *
     * @param description the description for which to check implied rights.
     * @return {@code true} if both the current instance implied right and the description's one are equals according to
     *          {@link EqualsBuilder}, or if one is null and the other one is empty.
     */
    private boolean likeImpliedRightsFrom(RightDescription description)
    {
        Set<Right> localImpliedRights = getImpliedRights();
        Set<Right> otherImpliedRights = description.getImpliedRights();
        boolean result = new EqualsBuilder().append(localImpliedRights, otherImpliedRights).isEquals();
        // If then result is false then we check it's not because of the special case where
        // one value is null and the other empty.
        // Note: we don't use single boolean operation to avoid checkstyle issue about complexity.
        if (!result) {
            // No risk of NPE on the isEmpty call since if the other variable would have been null, then
            // the result would have been equal on the first place and we wouldn't be there.
            result = (localImpliedRights == null && otherImpliedRights.isEmpty())
                || (otherImpliedRights == null && localImpliedRights.isEmpty());
        }
        return result;
    }
}
