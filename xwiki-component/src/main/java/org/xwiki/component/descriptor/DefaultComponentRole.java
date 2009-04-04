package org.xwiki.component.descriptor;

public class DefaultComponentRole implements ComponentRole
{
    private Class< ? > role;

    private String roleHint = "default";

    public void setRole(Class< ? > role)
    {
        this.role = role;
    }

    public Class< ? > getRole()
    {
        return this.role;
    }

    public void setRoleHint(String roleHint)
    {
        this.roleHint = roleHint;
    }

    public String getRoleHint()
    {
        return roleHint;
    }
}
