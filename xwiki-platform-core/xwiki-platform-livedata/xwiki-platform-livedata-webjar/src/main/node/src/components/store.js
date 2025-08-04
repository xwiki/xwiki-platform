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

/**
 * Store containing the resolved or pending components.
 * It's a two levels map, the first level with components kinds as the key, and components id for the second level.
 * The values are the components
 */
const store = {};
const awaiting = {};

const componentStore = {

    register(kind, key, componentProvider)
    {
        if (!store[kind]) {
            store[kind] = {};
        }
        store[kind][key] = componentProvider;
        if (awaiting?.[kind]?.[key]) {
            console.log(`STORE: [${kind}][${key}] registered and required`)
            componentProvider().then(resolved => {
                store[kind][key] = resolved;
                console.log(`STORE: [${kind}][${key}] resolved on request`)
                awaiting[kind][key].resolve(resolved);
                delete awaiting[kind][key];
            });
        } else {
            console.log(`STORE: [${kind}][${key}] registered and not required`)
        }
    },

    async load(kind, key)
    {
        if (!store[kind] || !store[kind][key]) {
            console.log(`STORE: [${kind}][${key}] is missing from the store`)
            if (awaiting?.[kind]?.[key]) {
                console.log(`STORE: [${kind}][${key}] was already requested while missing`)
                return awaiting[kind][key].promise;
            }
            console.log(`STORE: [${kind}][${key}] is requested while missing for the first time`)

            let _resolve;
            const promise = new Promise((resolve) => {
                _resolve = resolve
            });
            if (!awaiting[kind]) {
                awaiting[kind] = {}
            }
            awaiting[kind][key] = {promise: promise, resolve: _resolve};
            console.log(`STORE: [${kind}][${key}] return a pending promise`)
            return promise;
        } else {
            console.log(`STORE: [${kind}][${key}] is found in the store`)
            // On the first access, we resolve the component provider and save its result instead.
            // On the next access, the cached result is returned without a lookup.
            // This is fine since the result of the componentProvider function is expected to be stable.
            if (typeof store[kind][key] === "function") {
                console.log(`STORE: [${kind}][${key}] is resolved on request`)
                store[kind][key] = await store[kind][key]()
            }

            console.log(`STORE: [${kind}][${key}] return a resolved component`)
            return store[kind][key];
        }
    },
};

export {componentStore};
