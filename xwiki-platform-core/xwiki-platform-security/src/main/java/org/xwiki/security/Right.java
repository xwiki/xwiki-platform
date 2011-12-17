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
 *
 */
package org.xwiki.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;

import static org.xwiki.security.RightState.ALLOW;
import static org.xwiki.security.RightState.DENY;
import static org.xwiki.security.RightState.UNDETERMINED;

/**
 * Enumeration of the possible rights.
 * @version $Id$
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
     * Specialized map with a chainable put action to avoid exceeding code complexity during initialization.
     */
    private static class ActionMap extends HashMap<String, Right>
    {
        /** Serialization identifier for conformance to Serializable. */
        private static final long serialVersionUID = 1;

        /** Allow filling the map in the initializer without exceeding code complexity.
         * @param action the action name
         * @param right the corresponding right required
         * @return this action map to allow code chaining
         */
        public ActionMap putAction(String action, Right right)
        {
            put(action, right);
            return this;
        }
    }

    /** Map containing all known actions. */
    private static final ActionMap ACTION_MAP = new ActionMap();

    /**
     * The enabled rights by entity types.  There is a special case hardcoded : The PROGRAM
     * right should only be enabled for the main wiki, not for wikis in general.
     */
    private static final Map<EntityType, List<Right>> ENABLED_RIGHTS = new HashMap<EntityType, List<Right>>();

    /**
     * The enabled rights by entity types, but with unmodifiable list for getters.
     */
    private static final Map<EntityType, List<Right>> UNMODIFIABLE_ENABLED_RIGHTS = new HashMap<EntityType, List<Right>>();

    static {
        EntityType[] AllEntities = {EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI};
        EntityType[] SpaceEntities = {EntityType.SPACE, EntityType.WIKI};

        LOGIN    = new Right("login",       ALLOW,  ALLOW, true,  null, Arrays.asList(EntityType.WIKI));
        VIEW     = new Right("view",        ALLOW,  DENY,  true,  null, Arrays.asList(AllEntities)    );
        EDIT     = new Right("edit",        ALLOW,  DENY,  true,  null, Arrays.asList(AllEntities)    );
        DELETE   = new Right("delete",      DENY,   DENY,  true,  null, Arrays.asList(AllEntities)    );
        REGISTER = new Right("register",    ALLOW,  ALLOW, false, null, Arrays.asList(EntityType.WIKI));
        COMMENT  = new Right("comment",     ALLOW,  DENY,  true,  null, Arrays.asList(AllEntities)    );
        
        Right[] adminImpliedRight = {LOGIN, VIEW,   EDIT, DELETE, REGISTER, COMMENT};
        ADMIN    = new Right("admin",       DENY,   ALLOW, false, Arrays.asList(adminImpliedRight),
            Arrays.asList(SpaceEntities));
        
        Right[] programImpliedRight = {LOGIN, VIEW, EDIT, DELETE, ADMIN, REGISTER, COMMENT};
        PROGRAM  = new Right("programming", DENY,   ALLOW, false, Arrays.asList(programImpliedRight),
            Arrays.asList(EntityType.WIKI));
        
        ILLEGAL  = new Right("illegal",     DENY,   DENY,  false, null, null                          );

        ACTION_MAP
            .putAction(LOGIN.getName(), LOGIN)
            .putAction(VIEW.getName(), VIEW)
            .putAction(DELETE.getName(), DELETE)
            .putAction(ADMIN.getName(), ADMIN)
            .putAction(PROGRAM.getName(), PROGRAM)
            .putAction(EDIT.getName(), EDIT)
            .putAction(REGISTER.getName(), REGISTER)
            .putAction("logout", LOGIN)
            .putAction("loginerror", LOGIN)
            .putAction("loginsubmit", LOGIN)
            .putAction("viewrev", VIEW)
            .putAction("get", VIEW)
            // .putAction("downloadrev", "download"); Huh??
            .putAction("downloadrev", VIEW)
            .putAction("plain", VIEW)
            .putAction("raw", VIEW)
            .putAction("attach", VIEW)
            .putAction("charting", VIEW)
            .putAction("skin", VIEW)
            .putAction("download", VIEW)
            .putAction("dot", VIEW)
            .putAction("svg", VIEW)
            .putAction("pdf", VIEW)
            .putAction("deleteversions", ADMIN)
            // .putAction("undelete", "undelete"); Huh??
            .putAction("undelete", EDIT)
            .putAction("reset", DELETE)
            .putAction("commentadd", COMMENT)
            .putAction("redirect", VIEW)
            .putAction("export", VIEW)
            .putAction("import", ADMIN)
            .putAction("jsx", VIEW)
            .putAction("ssx", VIEW)
            .putAction("tex", VIEW)
            .putAction("unknown", VIEW)
            .putAction("save", EDIT)
            .putAction("preview", EDIT)
            .putAction("lock", EDIT)
            .putAction("cancel", EDIT)
            .putAction("delattachment", EDIT)
            .putAction("inline", EDIT)
            .putAction("propadd", EDIT)
            .putAction("propupdate", EDIT)
            .putAction("propdelete", EDIT)
            .putAction("objectadd", EDIT)
            .putAction("objectremove", EDIT)
            .putAction("objectsync", EDIT)
            .putAction("rollback", EDIT)
            .putAction("upload", EDIT)
            .putAction("create", EDIT);
    }

    /** The numeric value of this access right. */
    private final int value;

    /** The string representation. */
    private final String name;

    /** The string representation. */
    private final RightState defaultState;

    /** Whether this right should be allowed or denied in case of a tie. */
    private final RightState tieResolutionPolicy;

    /** Policy on how this right should be overridden by lower levels. */
    private final boolean inheritanceOverridePolicy;

    /** Additional rights implied by this right. */
    private final List<Right> impliedRights;

    /**
     * Construct a new Right from its description
     * @param description Description of the right to create.
     */
    public Right(RightDescription description)
    {
        this(description.getName(), description.getDefaultState(), description.getTieResolutionPolicy(),
            description.getInheritanceOverridePolicy(), description.getImpliedRights(), 
            description.getDocumentLevels());
    }

    /**
     * Construct a new Right.
     * @param name The string representation of this right.
     * @param defaultState The default state, in case no matching right is found at any level.
     * @param tieResolutionPolicy Whether this right should be allowed or denied in case of a tie.
     * @param inheritanceOverridePolicy Policy on how this right should be overridden by lower levels.
     * @param impliedRights Additional rights implied by this right.
     * @param validEntityTypes The type of entity where this right should be enabled.
     */
    private Right(String name, RightState defaultState, RightState tieResolutionPolicy, 
        boolean inheritanceOverridePolicy, List<Right> impliedRights, List<EntityType> validEntityTypes)
    {
        if (name == null || ALL_RIGHTS.contains(name)) {
            throw new IllegalArgumentException("Duplicate name for right [" + name + "].");
        }

        if (defaultState == null || defaultState == UNDETERMINED) {
            throw new IllegalArgumentException("Invalid default state [" + defaultState 
                + "] for right [" + name + "].");
        }
        
        if (tieResolutionPolicy == null || tieResolutionPolicy == UNDETERMINED) {
            throw new IllegalArgumentException("Invalid tie resolution policy [" + tieResolutionPolicy
                + "] for right [" + name + "].");
        }

        this.name = name;
        this.defaultState = defaultState;
        this.tieResolutionPolicy = tieResolutionPolicy;
        this.inheritanceOverridePolicy = inheritanceOverridePolicy;
        if (impliedRights != null && !impliedRights.isEmpty() && impliedRights.get(0) != null) {
            List<Right> implied = new ArrayList<Right>(impliedRights.size());
            for (Right right : impliedRights) {
                if (right != null) {
                    implied.add(right);
                }
            }
            this.impliedRights = Collections.unmodifiableList(implied);
        } else {
            this.impliedRights = null;
        }
        synchronized (VALUES) {
            this.value = VALUES.size();
            if (this.value >= 64) {
                throw new IndexOutOfBoundsException();
            }
            VALUES.add(this);
            ALL_RIGHTS.add(name);
            if (validEntityTypes != null) {
                for (EntityType type : validEntityTypes) {
                    if (type != null) {
                        List<Right> rights = ENABLED_RIGHTS.get(type);
                        if (rights == null) {
                            rights = new ArrayList<Right>();
                            ENABLED_RIGHTS.put(type, rights);
                            UNMODIFIABLE_ENABLED_RIGHTS.put(type, Collections.unmodifiableList(rights));
                        }
                        rights.add(this);
                    }
                }
            }
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
     * Map an action represented by a string to a right.
     * @param action String representation of action.
     * @return right The corresponding Right instance, or
     * {@code ILLEGAL}.
     */
    public static Right actionToRight(String action)
    {
        Right right = ACTION_MAP.get(action);
        if (right == null) {
            return ILLEGAL;
        }
        return right;
    }

    /**
     * Returns the list of rights available for a given entity type
     * @param entityType the entity type
     * @return a list of {@code Right} enabled of this entity type
     */
    public static List<Right> getEnabledRights(EntityType entityType)
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
    public List<Right> getImpliedRights()
    {
        return impliedRights;
    }

    @Override
    public List<EntityType> getDocumentLevels()
    {
        List<EntityType> levels = new ArrayList<EntityType>();
        for (Map.Entry<EntityType,List<Right>> entry : ENABLED_RIGHTS.entrySet()) {
            if (entry.getValue().contains(this)) {
                levels.add(entry.getKey());
            }
        }
        return levels;
    }

    @Override
    public boolean getInheritanceOverridePolicy()
    {
        return inheritanceOverridePolicy;
    }

    @Override
    public RightState getTieResolutionPolicy()
    {
        return tieResolutionPolicy;
    }

    @Override
    public RightState getDefaultState()
    {
        return this.defaultState;
    }

    @Override
    public int compareTo(Right other)
    {
        return this.ordinal() - other.ordinal();
    }
}
