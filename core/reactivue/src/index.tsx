/* eslint-disable vue/multi-word-component-names */
/* eslint-disable vue/one-component-per-file */

/*
 * See the LICENSE file distributed with this work for additional
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

import { createRef, useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { createApp, defineComponent, h, toRaw } from "vue";
import type { ReactElement, ReactNode } from "react";
import type { App, SlotsType, VNode } from "vue";

// This import is required as the return type of `reactComponentAdapter` references some types from `@vue/shared`
// If we remove this import, we will get a fatal error from the TypeScript compiler:
//
// > The inferred type of 'reactComponentAdapter' cannot be named without a reference to '[...]/@vue/shared'.
// > This is likely not portable. A type annotation is necessary.
//
// eslint-disable-next-line vue/prefer-import-from-vue
import "@vue/shared";

/**
 * Options for {@link reactComponentAdapter}
 *
 * @since 0.16
 */
type ReactComponentAdapterOptions = {
  dontHyphenizeProps?: boolean;
  modifyVueApp?: (app: App) => void;
};

/**
 * Wrap a React component to render it as a Vue component
 *
 * @param Component - A React component
 * @param options - Options for the adapter
 * @returns A Vue component
 *
 * @since 0.16
 */
function reactComponentAdapter<Props extends Record<string, unknown>>(
  Component: React.FC<Props>,
  options?: ReactComponentAdapterOptions,
) {
  return defineComponent({
    // We need to use '__typeProps' and 'slots' for typing
    //
    // Unfortunately, the `DefineComponent` type from Vue takes a *LOT* of generics,
    // so we resort to using this to get correct typing
    __typeProps: {} as ReactNonSlotProps<Props>,
    slots: {} as SlotsType<ReactSlotsTypeAdapter<Props>>,

    // Initialize the wrapper component's state
    data(): ReactAdapterComponentState<Props> {
      return {
        // Provided options
        options: options ?? {},
        // Create an Observable object with the props so the indirection layer (see below)
        // can be notified when props or slots change
        observableProps: new Observable<Props>(
          createPropsAndSlotsMergerProxy(
            () => this.$attrs,
            () => this.$slots,
            options,
          ),
        ),
      };
    },

    mounted() {
      // Ensure the container HTML element has been rendered correctly
      if (!(this.$el instanceof HTMLElement)) {
        throw new Error(
          "Rendered element is not defined in React component adapter",
        );
      }

      // Render the element inside the container element
      createRoot(this.$el).render(
        <ReactIndirectionLayer
          Component={Component}
          componentProps={this.$data.observableProps}
        />,
      );

      this.$watch(
        () => [{ ...this.$attrs }, { ...this.$slots }],
        () => {
          this.$data.observableProps.trigger();
        },
      );
    },

    // The component will be rendered inside this element
    render: () => h("div"),
  });
}

/**
 * State of the wrapper component
 */
type ReactAdapterComponentState<Props extends Record<string, unknown>> = {
  observableProps: Observable<Props>;
  options: ReactComponentAdapterOptions;
};

/**
 * Type of a Vue component
 */
type VueComponent<Props extends Record<string, unknown>> = (
  props: Props,
) => VNode[];

/**
 * Type of a Reactivue-compatible React child element
 *
 * @since 0.16
 */
type ReactivueChild<Props extends Record<string, unknown>> = (
  props: Props,
) => ReactElement;

/**
 * Remove all keys whose value is `never` from a record
 */
type FilterOutNeverKeys<T extends Record<string, unknown>> = Pick<
  T,
  {
    [K in keyof T]-?: T[K] extends never ? never : K;
  }[keyof T]
>;

/**
 * Transform the slot types in a shape that can be used in Vue's `defineComponent`
 */
type ReactSlotsTypeAdapter<Props extends Record<string, unknown>> =
  FilterOutNeverKeys<{
    [Prop in keyof Required<Props>]: Props[Prop] extends ReactivueChild<
      infer SlotProps
    >
      ? VueComponent<SlotProps>
      : never;
  }>;

/**
 * Filter out slots from a component's properties
 *
 * @since 0.16
 */
type ReactNonSlotProps<Props extends Record<string, unknown>> =
  FilterOutNeverKeys<{
    [Prop in keyof Props]: Props[Prop] extends (
      props: infer _ extends Record<string, unknown>,
    ) => ReactElement
      ? never
      : Props[Prop];
  }>;

/**
 * Create proxy to merge properties and slots together in a trap object
 * @param props - A getter for Vue's properties object
 * @param slots - A getter for Vue's slots object
 * @returns
 */
function createPropsAndSlotsMergerProxy<Props extends Record<string, unknown>>(
  props: () => Record<string | symbol, unknown>,
  slots: () => Record<
    string | symbol,
    VueComponent<Record<string, unknown>> | undefined
  >,
  options: ReactComponentAdapterOptions | undefined,
) {
  // This is a cache that maps Vue slot names to React adapter functions
  // Here, caching doesn't improve performance ; its purpose is to allow React
  // to determine we are re-using the same component every time we access the
  // same property through the returned proxy, which avoids resetting the
  // underlying component's state.
  //
  // If we got rid of this cache, React would think our slot adapter changes
  // every time and would mount a completely reset component on every render,
  // which would be extremely bad for performance but also for coherency.
  const slotsAdapterCache = new Map<
    string | symbol,
    {
      slot: VueComponent<Record<string, unknown>>;
      reactFn: (props: Record<string, unknown>) => ReactNode;
    }
  >();

  return new Proxy(
    {},
    {
      // eslint-disable-next-line max-statements
      get: (_, propName) => {
        if (Object.hasOwn(props(), propName)) {
          return props()[propName];
        }

        if (Object.hasOwn(slots(), propName)) {
          const slot = slots()[propName];

          if (!slot) {
            throw new Error(`Undefined slot: ${propName.toString()}`);
          }

          const cached = slotsAdapterCache.get(propName);

          if (cached?.slot === slot) {
            // console.log("Cached!");
            return cached.reactFn;
          }

          console.debug("Rendering uncached slot: ", { name: propName });

          let observableProps: Observable<Record<string, unknown>> | null =
            null;

          const reactFn = (props: Record<string, unknown>) => {
            if (observableProps) {
              observableProps.set(props);
            } else {
              observableProps = new Observable(props);
            }

            return (
              <VueComponentWrapper
                vueComponent={slot}
                props={observableProps}
                modifyVueApp={options?.modifyVueApp}
              />
            );
          };

          slotsAdapterCache.set(propName, { slot, reactFn });

          return reactFn;
        }

        console.debug("Unknown property: ", { propName });
        return undefined;
      },

      has: (_, propName) =>
        Object.hasOwn(props(), propName) || Object.hasOwn(slots(), propName),

      set: () => {
        throw new Error("Tried to set property on read-only object");
      },

      isExtensible: () => true,

      preventExtensions: () => true,

      ownKeys: () => [
        ...new Set(Reflect.ownKeys(props()).concat(Reflect.ownKeys(slots()))),
      ],

      getOwnPropertyDescriptor: (_, p) => {
        if (Object.hasOwn(props(), p)) {
          return Object.getOwnPropertyDescriptor(props(), p);
        }

        if (Object.hasOwn(slots(), p)) {
          return Object.getOwnPropertyDescriptor(slots(), p);
        }

        return undefined;
      },

      getPrototypeOf(/*target*/) {
        throw new Error("TODO: get prototype of (proxy)");
      },

      apply(/*target, thisArg, argArray*/) {
        throw new Error("TODO: function call on target (proxy)");
      },

      construct() {
        throw new Error("Tried to construct from proxied object");
      },

      defineProperty() {
        throw new Error("Tried to define a property on read-only object");
      },

      deleteProperty() {
        throw new Error("Tried to delete a propery on read-only object");
      },

      setPrototypeOf() {
        throw new Error("Tried to set prototype on read-only subject");
      },
    },
  ) as Props;
}

/**
 * Indirection layer, used to wrap the component to render and update its properties dynamically
 *
 * This only renders the underlying component with the provided properties, nothing else
 *
 * @param props - the props of this component, a Component and the component's props
 * @returns the instantiated component
 */
function ReactIndirectionLayer<Props extends Record<string, unknown>>({
  Component,
  componentProps,
}: ReactIndirectionLayerProps<Props>) {
  componentProps = toRaw(componentProps);

  const [props, setProps] = useState(componentProps.get());

  // Update this component when the provided underlying component's properties change
  useEffect(() => {
    componentProps.watch((props) => {
      setProps({ ...props });
    });
  }, [componentProps]);

  return <Component {...props} />;
}

/**
 * Properties for the React indirection layer component
 */
type ReactIndirectionLayerProps<Props extends Record<string, unknown>> = {
  Component: React.FC<Props>;
  componentProps: Observable<Props>;
};

function VueComponentWrapper<Props extends Record<string, unknown>>({
  vueComponent,
  /**
   * Wrap a Vue component to render it inside React
   *
   * Note that this spawns an entire Vue application for every component
   *
   * @param param0 - The Vue component to render
   * @returns A wrapping React component
   *
   * @since 0.16
   */
  props,
  modifyVueApp,
}: {
  vueComponent: VueComponent<Props>;
  props: Observable<Props>;
  modifyVueApp: ReactComponentAdapterOptions["modifyVueApp"];
}) {
  // The element the Vue component is going to be rendered into
  const containerRef = createRef<HTMLDivElement>();

  // Instance of a Vue application
  const vueInstanceRef = useRef<App<Element> | null>(null);

  // NOTE: We deliberately don't list "props" as a dependency of this useEffect()
  // Indeed, this would cause the useEffect() to re-run on each properties change,
  // which would then cause a new Vue app to be created everytime - which would reset
  // the entire underlying components' state.
  // biome-ignore lint/correctness/useExhaustiveDependencies: <explanation>
  useEffect(() => {
    // Only import Vue when the component mounts
    if (containerRef.current) {
      // Clean up previous instance if it exists
      if (vueInstanceRef.current) {
        vueInstanceRef.current.unmount();
      }

      // Create a new Vue app with your component
      // TODO: if perf is bad, consider implementing https://github.com/gloriasoft/veaury?tab=readme-ov-file#context
      const app = createApp(VueIndirectionLayer, {
        vueComponent,
        props,
      } satisfies VueIndirectionLayerProps<Props>);

      // Apply Vue app modification function
      modifyVueApp?.(app);

      // Mount the app into the (HTML element) container
      app.mount(containerRef.current);

      vueInstanceRef.current = app;
    }

    // Clean up when component unmounts
    return () => vueInstanceRef.current?.unmount();
  }, [vueComponent, modifyVueApp, containerRef.current]);

  // Use a placeholder <div> to wrap the future mounted component
  return <div ref={containerRef} />;
}

/**
 * Indirection layer, used to wrap the component to render and update its properties dynamically
 *
 * This only renders the underlying component with the provided properties, nothing else
 */

const VueIndirectionLayer = defineComponent({
  __typeProps: {} as VueIndirectionLayerProps<Record<string, unknown>>,

  data(): VueIndirectionLayerState {
    const { props } = this.$attrs as VueIndirectionLayerProps<
      Record<string, unknown>
    >;

    return {
      props: props.get(),
    };
  },

  mounted() {
    const { props } = this.$attrs as VueIndirectionLayerProps<
      Record<string, unknown>
    >;

    props.watch((props) => {
      this.$data.props = props;
    });
  },

  render() {
    const { vueComponent } = this.$attrs as VueIndirectionLayerProps<
      Record<string, unknown>
    >;

    return vueComponent(
      // TODO: using 'this.$data' may create some problems as Vue put it behind a Proxy
      this.$data.props,
    );
  },
});

type VueIndirectionLayerProps<Props extends Record<string, unknown>> = {
  vueComponent: VueComponent<Props>;
  props: Observable<Props>;
};

type VueIndirectionLayerState = {
  props: Record<string, unknown>;
};

/**
 * Lightweight observable type
 * @since 0.16
 */
class Observable<T> {
  private readonly _listeners = new Array<(value: T) => void>();

  /**
   * Create a new observable value
   * @param _value - the value of the observable
   */
  constructor(private _value: T) {}

  /**
   * Get the observed value
   * @returns The observed value
   */
  get(): T {
    return this._value;
  }

  /**
   * Replaces the observed value with another value, and triggers all listeners.
   * @param value - the new value of the observable
   */
  set(value: T): void {
    this._value = value;
    this.trigger();
  }

  /**
   * Trigger all listeners (without changing the observable value)
   */
  trigger(): void {
    for (const listener of this._listeners) {
      listener(this._value);
    }
  }

  // TODO: return a cleanup ID
  // TODO: use that ID in useEffect()'s return callback
  watch(listener: (value: T) => void): void {
    this._listeners.push(listener);
  }
}

export { VueComponentWrapper, reactComponentAdapter };
export type { ReactComponentAdapterOptions, ReactNonSlotProps, ReactivueChild };
