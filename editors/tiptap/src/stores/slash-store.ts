import { defineStore, Store, StoreDefinition } from "pinia";
import { ActionCategoryDescriptor } from "../components/extensions/slash";

export type Props = { items: ActionCategoryDescriptor[] };

type State = {
  props: Props;
};

type Getters = {
  items: (state: State) => ActionCategoryDescriptor[];
};

type Actions = {
  updateProps: (props: Props) => void;
};

export type SlashStore = Store<"slash-store", State, Getters, Actions>;
const store: StoreDefinition<"slash-store", State, Getters, Actions> =
  defineStore("slash-store", {
    getters: {
      items: (state) => state.props.items,
    },
    state: () => {
      return {
        props: {
          items: [],
        },
      };
    },
    actions: {
      updateProps(props): void {
        this.props = props;
      },
    },
  });
export default store;
