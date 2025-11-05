import { ModelReferenceSerializer } from './modelReferenceSerializer';
/**
 * @since 0.12
 * @beta
 */
interface ModelReferenceSerializerProvider {
    get(type?: string): ModelReferenceSerializer | undefined;
}
export { type ModelReferenceSerializerProvider };
