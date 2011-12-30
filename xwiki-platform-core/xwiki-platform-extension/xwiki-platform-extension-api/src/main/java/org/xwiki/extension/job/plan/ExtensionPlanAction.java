package org.xwiki.extension.job.plan;

import org.xwiki.extension.Extension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.version.VersionConstraint;

public class ExtensionPlanAction
{
    /**
     * The action to execute.
     * 
     * @version $Id$
     */
    public enum Action
    {
        /**
         * Nothing to do. Just here for information as why nothing is done here.
         */
        NONE,

        /**
         * Install the extension.
         */
        INSTALL,

        /**
         * Upgrade the extension.
         */
        UPGRADE,

        /**
         * Uninstall the extension.
         */
        UNINSTALL
    }

    /**
     * @see #getExtension()
     */
    private Extension extension;

    /**
     * @see #getPreviousExtension()
     */
    private LocalExtension previousExtension;

    /**
     * @see Action
     */
    private Action action;

    /**
     * @see #getNamespace()
     */
    private String namespace;

    /**
     * @see #getVersionConstraint()
     */
    private VersionConstraint versionConstraint;

    /**
     * @param extension the extension on which to perform the action
     * @param previousExtension the currently installed extension. Used when upgrading
     * @param action the action to perform
     * @param namespace the namespace in which the action should be executed
     * @param versionConstraint the version constraint that has been used to resolve the extension
     */
    public ExtensionPlanAction(Extension extension, LocalExtension previousExtension, Action action, String namespace,
        VersionConstraint versionConstraint)
    {
        this.extension = extension;
        this.previousExtension = previousExtension;
        this.action = action;
        this.namespace = namespace;
        this.versionConstraint = versionConstraint;
    }

    /**
     * @return the extension on which to perform the action
     */
    public Extension getExtension()
    {
        return this.extension;
    }

    /**
     * @return the currently installed extension. Used when upgrading
     */
    public LocalExtension getPreviousExtension()
    {
        return this.previousExtension;
    }

    /**
     * @return the action to perform
     */
    public Action getAction()
    {
        return this.action;
    }

    /**
     * @return the namespace in which the action should be executed
     */
    public String getNamespace()
    {
        return this.namespace;
    }

    /**
     * @return the version constraint that has been used to resolve the extension
     */
    public VersionConstraint getVersionConstraint()
    {
        return versionConstraint;
    }
}
