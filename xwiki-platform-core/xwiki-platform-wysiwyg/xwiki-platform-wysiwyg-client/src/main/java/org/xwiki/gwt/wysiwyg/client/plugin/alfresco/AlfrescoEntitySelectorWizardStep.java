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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import java.util.List;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.ui.ListBox;
import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step that selects an Alfresco entity.
 * 
 * @version $Id$
 */
public class AlfrescoEntitySelectorWizardStep extends AbstractInteractiveWizardStep implements DoubleClickHandler,
    KeyUpHandler, SourcesNavigationEvents
{
    /**
     * The CSS class name used to put this step into loading state.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * The object processed by this wizard step.
     */
    private EntityLink<EntityConfig> entityLink;

    /**
     * The parent whose children are currently displayed.
     */
    private EntityReference currentParent;

    /**
     * The service used to access an Alfresco content management system.
     */
    private final AlfrescoServiceAsync alfrescoService;

    /**
     * The label used to display the current path.
     */
    private final Label pathLabel = new Label();

    /**
     * The list box displaying the children of the {@link #currentParent}.
     */
    private final ListBox<AlfrescoEntity> childrenListBox = new ListBox<AlfrescoEntity>();

    /**
     * Navigation listeners to be notified by navigation events from this step. It generates navigation to the next step
     * when an item is double clicked in the list, or enter key is pressed on a selected item.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * Creates a new instance.
     * 
     * @param alfrescoService the service used to access an Alfresco content management system
     */
    public AlfrescoEntitySelectorWizardStep(AlfrescoServiceAsync alfrescoService)
    {
        super(new VerticalResizePanel());

        this.alfrescoService = alfrescoService;

        FlowPanel pathContainer = new FlowPanel();
        pathContainer.setStyleName("xPath");
        pathContainer.add(new Label(AlfrescoConstants.INSTANCE.pathLabel()));
        pathContainer.getWidget(0).setStyleName("xPath-label");
        pathLabel.setStyleName("xPath-value");
        pathContainer.add(pathLabel);
        display().add(pathContainer);

        childrenListBox.addKeyUpHandler(this);
        childrenListBox.addDoubleClickHandler(this);
        display().add(childrenListBox);
        display().setExpandingWidget(childrenListBox, false);
        display().addStyleName("xAlfrescoSelector");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#init(Object, AsyncCallback)
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, final AsyncCallback< ? > callback)
    {
        entityLink = (EntityLink<EntityConfig>) data;
        currentParent = entityLink.getDestination().getEntityReference();
        up(new AsyncCallback<Object>()
        {
            public void onFailure(Throwable caught)
            {
                callback.onFailure(caught);
            }

            public void onSuccess(Object result)
            {
                callback.onSuccess(null);
                Scheduler.get().scheduleDeferred(new FocusCommand(childrenListBox));
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#getResult()
     */
    public Object getResult()
    {
        return entityLink;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(AsyncCallback<Boolean> callback)
    {
        if (validate()) {
            save();
            callback.onSuccess(true);
            return;
        }
        callback.onSuccess(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#onCancel()
     */
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInteractiveWizardStep#display()
     */
    @Override
    public VerticalResizePanel display()
    {
        return (VerticalResizePanel) super.display();
    }

    /**
     * @return {@code true} if the current selection is valid, {@code false} otherwise
     */
    private boolean validate()
    {
        return childrenListBox.getSelectedItem() != null;
    }

    /**
     * Saves the current selection.
     */
    private void save()
    {
        AlfrescoEntity selectedEntity = childrenListBox.getSelectedItem().getData();
        if (!entityLink.getDestination().getEntityReference().equals(selectedEntity.getReference())) {
            entityLink.getDestination().setEntityReference(selectedEntity.getReference().clone());
            // We have a new target entity reference so we reset the previous entity configuration.
            entityLink.getData().setReference(null);
            entityLink.getData().setUrl(null);
        }
    }

    /**
     * Go one level up.
     * 
     * @param callback the object to be notified when the {@link #childrenListBox} is updated
     */
    private void up(final AsyncCallback< ? > callback)
    {
        alfrescoService.getParent(currentParent, new AsyncCallback<AlfrescoEntity>()
        {
            public void onFailure(Throwable caught)
            {
                callback.onFailure(caught);
            }

            public void onSuccess(AlfrescoEntity parent)
            {
                down(parent, currentParent, callback);
            }
        });
    }

    /**
     * Go one level down, inside the specified parent.
     * 
     * @param parent the parent whose children will be displayed
     * @param selectedChild the child to be selected after the {@link #childrenListBox} is updated
     * @param callback the object to be notified when the {@link #childrenListBox} is updated
     */
    private void down(AlfrescoEntity parent, final EntityReference selectedChild, final AsyncCallback< ? > callback)
    {
        currentParent = parent.getReference();
        pathLabel.setText(parent.getPath());
        alfrescoService.getChildren(currentParent, new AsyncCallback<List<AlfrescoEntity>>()
        {
            public void onFailure(Throwable caught)
            {
                callback.onFailure(caught);
            }

            public void onSuccess(List<AlfrescoEntity> children)
            {
                ListItem<AlfrescoEntity> selectedItem = fill(children, selectedChild);
                callback.onSuccess(null);
                childrenListBox.setSelectedItem(selectedItem);
            }
        });
    }

    /**
     * Updates the displayed list box with the given entries.
     * 
     * @param children the child entities that should fill the {@link #childrenListBox}
     * @param selectedChildReference the child entity that should be selected after the {@link #childrenListBox} is
     *            updated
     * @return the list item that should be selected
     */
    protected ListItem<AlfrescoEntity> fill(List<AlfrescoEntity> children, EntityReference selectedChildReference)
    {
        childrenListBox.clear();
        childrenListBox.addItem(createParentListItem());

        ListItem<AlfrescoEntity> selectedItem = null;
        for (AlfrescoEntity child : children) {
            ListItem<AlfrescoEntity> item = createListItem(child);
            childrenListBox.addItem(item);
            if (child.getReference().equals(selectedChildReference)) {
                selectedItem = item;
            }
        }

        return selectedItem != null ? selectedItem : childrenListBox.getItem(0);
    }

    /**
     * @param entity an entity
     * @return a new list item that has the given entity as its data
     */
    protected ListItem<AlfrescoEntity> createListItem(AlfrescoEntity entity)
    {
        ListItem<AlfrescoEntity> item = new ListItem<AlfrescoEntity>();
        item.setData(entity);
        item.add(new Label(entity.getName()));
        if (entity.getMediaType() != null) {
            item.getWidget(0).addStyleName(getStyleNameForMediaType(entity.getMediaType()));
        }
        return item;
    }

    /**
     * @param mediaType a media type
     * @return the style name corresponding to the specified media type
     */
    private String getStyleNameForMediaType(String mediaType)
    {
        String prefix = "mediaType";
        StringBuilder styleName = new StringBuilder(prefix);
        int separatorPosition = mediaType.indexOf('/');
        if (separatorPosition > 0) {
            styleName.append(' ').append(prefix).append('-');
            styleName.append(cleanStyleName(mediaType.substring(0, separatorPosition)));
        }
        styleName.append(' ').append(prefix).append('-').append(cleanStyleName(mediaType));
        return styleName.toString();
    }

    /**
     * @param dirtyStyleName a string that needs to be converted to a style name
     * @return the corresponding stlye name
     */
    private String cleanStyleName(String dirtyStyleName)
    {
        return dirtyStyleName.replaceAll("[^\\w\\-]+", "-");
    }

    /**
     * @return a new list item that has the current parent as its data
     */
    private ListItem<AlfrescoEntity> createParentListItem()
    {
        AlfrescoEntity parentEntity = new AlfrescoEntity();
        parentEntity.setReference(currentParent);
        parentEntity.setName("..");
        return createListItem(parentEntity);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#addNavigationListener(NavigationListener)
     */
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesNavigationEvents#removeNavigationListener(NavigationListener)
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyUpHandler#onKeyUp(KeyUpEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getSource() == childrenListBox && event.getNativeKeyCode() == KeyCodes.KEY_ENTER
            && childrenListBox.getSelectedItem() != null) {
            action();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DoubleClickHandler#onDoubleClick(DoubleClickEvent)
     */
    public void onDoubleClick(DoubleClickEvent event)
    {
        if (event.getSource() == childrenListBox && childrenListBox.getSelectedItem() != null) {
            action();
        }
    }

    /**
     * Triggers the default action on the selected list item.
     */
    private void action()
    {
        AlfrescoEntity selectedEntity = childrenListBox.getSelectedItem().getData();
        if (selectedEntity.getMediaType() != null) {
            navigationListeners.fireNavigationEvent(NavigationDirection.NEXT);
            return;
        }

        display().addStyleName(STYLE_LOADING);
        AsyncCallback< ? > callback = new AsyncCallback<Object>()
        {
            public void onFailure(Throwable caught)
            {
                display().removeStyleName(STYLE_LOADING);
            }

            public void onSuccess(Object result)
            {
                display().removeStyleName(STYLE_LOADING);
            }
        };

        if (selectedEntity.getReference().equals(currentParent)) {
            up(callback);
        } else {
            down(selectedEntity, null, callback);
        }
    }
}
