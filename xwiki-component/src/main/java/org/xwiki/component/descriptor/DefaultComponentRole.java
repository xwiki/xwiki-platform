package org.xwiki.component.descriptor;

public class DefaultComponentRole implements ComponentRole
{
    private String role;

    private String roleHint = "default";

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getRole()
    {
        return role;
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
