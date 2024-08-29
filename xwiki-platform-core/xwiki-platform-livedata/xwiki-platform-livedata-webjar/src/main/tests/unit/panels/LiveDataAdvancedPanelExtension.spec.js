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
import LiveDataAdvancedPanelExtension from "../../../panels/LiveDataAdvancedPanelExtension";
import {mount} from '@vue/test-utils';
import Vue from "vue";

/**
 * Vue Component initializer for LiveDataAdvancedPanelExtension component.
 *
 * @param container the container to use for the extension
 * @returns {*} a wrapper for the LiveDataAdvancedPanelExtension component
 */
function initWrapper({container}) {
  return mount(LiveDataAdvancedPanelExtension, {
    provide: {
      logic: {
        openedPanels: ['extensionPanel'],
        uniqueArrayHas(uniqueArray, item) {
          return uniqueArray.includes(item);
        }
      }
    },
    propsData: {
      panel: {
        id: 'extensionPanel',
        title: 'Test Panel',
        container: container,
        component: 'LiveDataAdvancedPanelExtension',
        icon: 'camera'
      }
    },
    stubs: {XWikiIcon: true}
  });
}

describe('LiveDataAdvancedPanelExtension.vue', () => {
  const container = document.createElement("span");
  container.textContent = "Hello World!";

  it('Displays the given container', async () => {
    const wrapper = initWrapper({container});
    expect(wrapper.element.querySelector('.panel-heading')).toHaveTextContent('Test Panel');
    expect(wrapper.element.querySelector('.extension-body')).toContainElement(container);
  });

  it('Re-displays the given container after toggling the visibility', async () => {
    const wrapper = initWrapper({container});
    expect(wrapper.element).toContainElement(container);
    await wrapper.find('.collapse-button').trigger('click');
    await Vue.nextTick();
    expect(wrapper.element).not.toContainElement(container);
    await wrapper.find('.collapse-button').trigger('click');
    await Vue.nextTick();
    expect(wrapper.element).toContainElement(container);
  });

  it('Updates the container when it is exchanged', async () => {
    const wrapper = initWrapper({container});
    expect(wrapper.element).toContainElement(container);
    const newContainer = document.createElement('p');
    newContainer.textContent = 'Changed!';
    wrapper.vm.panel.container = newContainer;
    await Vue.nextTick();
    expect(wrapper.element).toContainElement(newContainer);
  });
});