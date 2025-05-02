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
import BaseDisplayer from "../../../displayers/BaseDisplayer";
import {initWrapper} from "./displayerTestsHelper";

describe('BaseDisplayer.vue', () => {
  it('Renders an entry in view mode', () => {
    const wrapper = initWrapper(BaseDisplayer, {});

    expect(wrapper.text()).toMatch('red')
  })

  it('Switch to edit mode', () => {
    const wrapper = initWrapper(BaseDisplayer, {});

    // Manuelly triggers setEdit until we find a way to simulate the hovering of the displayer and get access the 
    // popover content.
    wrapper.vm.setEdit();

    expect(wrapper.emitted()).toEqual({"update:isView": [[false]]})
  })

  it('Renders an entry in edit mode', () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        isView: false
      }
    });

    expect(wrapper.find('input').element.value).toMatch('red')
    expect(wrapper.find('input').element).toHaveFocus()
  })

  it('Send an event on save', async () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        isView: false
      }
    })

    wrapper.find('input').setValue('test-value');

    await wrapper.find('.edit > div').trigger('keypress.enter');

    let events = wrapper.emitted();
    // Checks that the value is sent on the save event.
    // Then checks that we switch back to the view mode.
    expect(events.saveEdit[0]).toEqual(['test-value']);
    expect(events['update:isView'][0]).toEqual([true]);
  })

  it('Renders an non viewable entry with an empty content', () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined
        }
      },
      logic: {
        isActionAllowed()
        {
          return false;
        }
      }
    });

    expect(wrapper.find('div.view > div').text()).toBe('livedata.displayer.emptyValue*');
  })

  it('Renders a viewable entry with an empty content', () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined
        }
      }
    });

    expect(wrapper.find('div .view > div').text()).toBe('');
  })

  it('Renders an entry when isEmpty is set to false', () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined
        },
        isEmpty: false
      }
    });

    // Even when the action is not allowed and the property value is undefined, 'N/A' is not displayed
    // if the props isEmpty is set to false.
    // This is useful when a displayed has his own way to present empty values, such as the link displayer.
    expect(wrapper.find('div.view > div').text()).toBe('');
  })

})