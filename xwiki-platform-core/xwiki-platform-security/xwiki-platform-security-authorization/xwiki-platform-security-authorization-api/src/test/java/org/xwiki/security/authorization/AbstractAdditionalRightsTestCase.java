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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.xwiki.model.EntityType;
import org.xwiki.security.AbstractSecurityTestCase;

import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.DENY;

/**
 * A base class for authorization tests, defining some additional Rights.
 *
 * @version $Id$
 * @since 4.0M2
 */
public abstract class AbstractAdditionalRightsTestCase extends AbstractSecurityTestCase
{
    private static class TestRightDescription implements RightDescription
    {
        private String name;
        private RuleState defaultState;
        private RuleState tieResolution;
        private boolean override;
        private Set<Right> impliedRights;

        TestRightDescription(String name, RuleState defaultState, RuleState tieResolution, boolean override)
        {
            this.name = name;
            this.defaultState = defaultState;
            this.tieResolution = tieResolution;
            this.override = override;
        }

        TestRightDescription(String name, RuleState defaultState, RuleState tieResolution, boolean override,
            Set<Right> impliedRights)
        {
            this(name, defaultState, tieResolution, override);
            this.impliedRights = impliedRights;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public RuleState getDefaultState()
        {
            return defaultState;
        }

        @Override
        public RuleState getTieResolutionPolicy()
        {
            return tieResolution;
        }

        @Override
        public boolean getInheritanceOverridePolicy()
        {
            return override;
        }

        @Override
        public Set<Right> getImpliedRights()
        {
            return impliedRights;
        }

        @Override
        public Set<EntityType> getTargetedEntityType()
        {
            return Right.WIKI_SPACE_DOCUMENT;
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }
    }

    protected static List<Right> allTestRights = new ArrayList<Right>();

    protected static Right impliedTestRightsADT;
    protected static Right impliedTestRightsDAF;

    @BeforeAll
    public static void oneTimeSetUp()
    {
        allTestRights.add(getNewTestRight("AllowAllowTrue", ALLOW, ALLOW, true));
        allTestRights.add(getNewTestRight("AllowAllowFalse", ALLOW, ALLOW, false));
        allTestRights.add(getNewTestRight("AllowDenyTrue", ALLOW, DENY, true));
        allTestRights.add(getNewTestRight("AllowDenyFalse", ALLOW, DENY, false));
        allTestRights.add(getNewTestRight("DenyAllowTrue", DENY, ALLOW, true));
        allTestRights.add(getNewTestRight("DenyAllowFalse", DENY, ALLOW, false));
        allTestRights.add(getNewTestRight("DenyDenyTrue", DENY, DENY, true));
        allTestRights.add(getNewTestRight("DenyDenyFalse", DENY, DENY, false));

        Set<Right> allTestRightSet = new RightSet(allTestRights);

        impliedTestRightsADT = new Right(new TestRightDescription("impliedTestRightsADT", ALLOW, DENY, true, allTestRightSet));
        impliedTestRightsDAF = new Right(new TestRightDescription("impliedTestRightsDAF", DENY, ALLOW, false, allTestRightSet));
    }

    /**
     * Unregister test rights, so they don't interfere with other test classes.
     */
    @AfterAll
    public static void unregisterRights()
    {
        allTestRights.forEach(Right::unregister);
        allTestRights.clear();
        impliedTestRightsADT.unregister();
        impliedTestRightsDAF.unregister();
    }

    public static Right getNewTestRight(String name, RuleState defaultValue, RuleState tieResolution, boolean overridePolicy) {
        return new Right(new TestRightDescription(name, defaultValue, tieResolution, overridePolicy));
    }
}
