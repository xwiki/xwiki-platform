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
package org.xwiki.store.hibernate.internal.patcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Build time tool producing the content of the patched Hibernate JAR.
 * <p>
 * It copies the content of a list of JARs in a classes directory, optionally relocating some packages, and merges the
 * mixins (interfaces annotated with {@link Mixin}) found in the classes directory into the interface they extend.
 * <p>
 * It can also make the classes of a package usable by code compiled against another package with the same API (see
 * {@link PackageBridge}).
 * <p>
 * Usage: {@code HibernatePatcher <classes directory> <mixins package path> [relocate:<from>=<to>]...
 * [bridge:<target prefix>,<source prefix>,<source jar>[,<bridged prefix>]...]... [jar:<path>]...}
 *
 * @version $Id$
 */
public final class HibernatePatcher
{
    private static final String MIXIN_DESCRIPTOR = Type.getDescriptor(Mixin.class);

    private static final String BINARY_ONLY_DESCRIPTOR = Type.getDescriptor(BinaryOnly.class);

    private static final String CLASS_EXTENSION = ".class";

    private static final String META_INF = "META-INF/";

    private static final String SERVICES = META_INF + "services/";

    private final Path classes;

    private final Map<String, String> relocations = new LinkedHashMap<>();

    private final Map<String, List<ClassNode>> mixins = new HashMap<>();

    private final Map<String, List<String>> interfaces = new HashMap<>();

    private final Set<String> patched = new HashSet<>();

    private final Map<String, StringBuilder> services = new LinkedHashMap<>();

    private final List<PackageBridge> bridges = new ArrayList<>();

    private final Remapper relocator = new Remapper(Opcodes.ASM9)
    {
        @Override
        public String map(String internalName)
        {
            return relocate(internalName);
        }

        @Override
        public Object mapValue(Object value)
        {
            // Also relocate the class and package names used in String constants (resource bundles, reflection, etc.)
            if (value instanceof String string) {
                // Path form (resources)
                String relocated = relocate(string);
                if (relocated != string) {
                    return relocated;
                }

                // Dotted form (class names, resource bundle names, etc.)
                String path = string.replace('.', '/');
                relocated = relocate(path);
                if (relocated != path) {
                    return relocated.replace('/', '.');
                }
            }

            return super.mapValue(value);
        }
    };

    private HibernatePatcher(Path classes)
    {
        this.classes = classes;
    }

    /**
     * @param args the command line arguments
     * @throws IOException when failing to read or write the classes
     */
    public static void main(String[] args) throws IOException
    {
        HibernatePatcher patcher = new HibernatePatcher(Paths.get(args[0]));

        patcher.loadMixins(args[1]);

        List<Path> jars = new ArrayList<>();
        for (int i = 2; i < args.length; ++i) {
            String argument = args[i];
            if (argument.startsWith("relocate:")) {
                String[] relocation = argument.substring("relocate:".length()).split("=", 2);
                patcher.relocations.put(relocation[0], relocation[1]);
            } else if (argument.startsWith("bridge:")) {
                String[] bridge = argument.substring("bridge:".length()).split(",");
                patcher.bridges.add(new PackageBridge(bridge[0], bridge[1], Paths.get(bridge[2]),
                    List.of(bridge).subList(3, bridge.length)));
            } else if (argument.startsWith("jar:")) {
                jars.add(Paths.get(argument.substring("jar:".length())));
            } else {
                throw new IllegalArgumentException("Unsupported argument [" + argument + "]");
            }
        }

        for (PackageBridge bridge : patcher.bridges) {
            bridge.plan(jars);
        }

        for (Path jar : jars) {
            patcher.copy(jar);
        }

        patcher.finish();
    }

    private String relocate(String internalName)
    {
        for (Map.Entry<String, String> entry : this.relocations.entrySet()) {
            if (internalName.startsWith(entry.getKey())) {
                return entry.getValue() + internalName.substring(entry.getKey().length());
            }
        }

        return internalName;
    }

    private void loadMixins(String mixinPackage) throws IOException
    {
        Path mixinDirectory = this.classes.resolve(mixinPackage);
        if (!Files.isDirectory(mixinDirectory)) {
            return;
        }

        try (Stream<Path> stream = Files.list(mixinDirectory)) {
            for (Iterator<Path> it = stream.iterator(); it.hasNext();) {
                Path file = it.next();
                if (file.toString().endsWith(CLASS_EXTENSION)) {
                    loadMixin(Files.readAllBytes(file));
                }
            }
        }
    }

    private void loadMixin(byte[] bytes)
    {
        ClassNode mixin = new ClassNode();
        new ClassReader(bytes).accept(mixin, 0);

        AnnotationNode annotation = getAnnotation(mixin.invisibleAnnotations, MIXIN_DESCRIPTOR);
        if (annotation == null) {
            throw new IllegalStateException(
                "Class [" + mixin.name + "] is located in the mixins package but is not annotated with @Mixin");
        }
        if (!mixin.fields.isEmpty()) {
            throw new IllegalStateException("Mixin [" + mixin.name + "] must not declare fields");
        }

        List<Type> staticTarget = getAnnotationValues(annotation, "target");
        String target;
        if (!staticTarget.isEmpty()) {
            target = staticTarget.get(0).getInternalName();
        } else if ((mixin.access & Opcodes.ACC_INTERFACE) != 0 && mixin.interfaces.size() == 1) {
            target = mixin.interfaces.get(0);
        } else {
            throw new IllegalStateException("Mixin [" + mixin.name + "] must either indicate a target or be an"
                + " interface extending exactly one interface");
        }

        this.mixins.computeIfAbsent(target, k -> new ArrayList<>()).add(mixin);
        List<String> mixinInterfaces = getAnnotationValues(annotation, "interfaces");
        this.interfaces.computeIfAbsent(target, k -> new ArrayList<>()).addAll(mixinInterfaces);
    }

    private static AnnotationNode getAnnotation(List<AnnotationNode> annotations, String descriptor)
    {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(descriptor)) {
                    return annotation;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getAnnotationValues(AnnotationNode annotation, String name)
    {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (annotation.values.get(i).equals(name)) {
                    Object value = annotation.values.get(i + 1);

                    return value instanceof List ? (List<T>) value : List.of((T) value);
                }
            }
        }

        return List.of();
    }

    private void copy(Path jarPath) throws IOException
    {
        String jarName = jarPath.getFileName().toString().replaceFirst("\\.jar$", "");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (Iterator<JarEntry> it = jar.entries().asIterator(); it.hasNext();) {
                JarEntry entry = it.next();
                if (!entry.isDirectory()) {
                    try (InputStream stream = jar.getInputStream(entry)) {
                        copy(jarName, entry.getName(), stream.readAllBytes());
                    }
                }
            }
        }
    }

    private void copy(String jarName, String name, byte[] content) throws IOException
    {
        if (name.startsWith(META_INF)) {
            copyMetaInf(jarName, name, content);
        } else if (name.endsWith(CLASS_EXTENSION)) {
            if (!name.equals("module-info.class")) {
                copyClass(content);
            }
        } else {
            write(relocate(name), content);
        }
    }

    private void copyMetaInf(String jarName, String name, byte[] content) throws IOException
    {
        if (name.startsWith(SERVICES)) {
            // Merge the services coming from the various JARs
            String service = relocate(name.substring(SERVICES.length()).replace('.', '/')).replace('/', '.');
            StringBuilder builder = this.services.computeIfAbsent(service, k -> new StringBuilder());
            for (String line : new String(content, StandardCharsets.UTF_8).split("\n")) {
                String implementation = line.trim();
                if (!implementation.isEmpty() && !implementation.startsWith("#")) {
                    builder.append(relocate(implementation.replace('.', '/')).replace('/', '.')).append('\n');
                }
            }
        } else if (!name.equals(META_INF + "MANIFEST.MF") && !name.startsWith(META_INF + "maven/")
            && !name.startsWith(META_INF + "versions/") && !name.endsWith(".SF") && !name.endsWith(".RSA")
            && !name.endsWith(".DSA")) {
            // Keep the other files (licenses, notices, etc.) but isolated to avoid collisions between JARs
            write(META_INF + jarName + '/' + name.substring(META_INF.length()), content);
        }
    }

    private void copyClass(byte[] content) throws IOException
    {
        byte[] bytes = content;
        ClassReader reader = new ClassReader(bytes);
        String name = reader.getClassName();

        if (!this.relocations.isEmpty()) {
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassRemapper(writer, this.relocator), 0);
            bytes = writer.toByteArray();
            name = relocate(name);
        }

        List<ClassNode> classMixins = this.mixins.get(name);
        if (classMixins != null) {
            bytes = merge(bytes, name, classMixins);
        }

        for (PackageBridge bridge : this.bridges) {
            if (bridge.accept(name)) {
                bytes = bridge.bridge(bytes);
            }
        }

        write(name + CLASS_EXTENSION, bytes);
    }

    private byte[] merge(byte[] bytes, String name, List<ClassNode> classMixins)
    {
        ClassNode target = new ClassNode();
        new ClassReader(bytes).accept(target, 0);

        for (ClassNode mixin : classMixins) {
            merge(target, mixin);
        }

        for (String signature : this.interfaces.get(name)) {
            addInterface(target, signature);
        }

        this.patched.add(name);

        ClassWriter writer = new ClassWriter(0);
        target.accept(writer);

        return writer.toByteArray();
    }

    private void merge(ClassNode target, ClassNode mixin)
    {
        boolean staticMixin = (mixin.access & Opcodes.ACC_INTERFACE) == 0;

        // Replace all the references to the mixin with references to the target
        ClassNode remapped = new ClassNode();
        mixin.accept(new ClassRemapper(remapped, new SimpleRemapper(Opcodes.ASM9, mixin.name, target.name)));

        for (MethodNode method : remapped.methods) {
            if (method.name.equals("<clinit>")) {
                throw new IllegalStateException("Mixin [" + mixin.name + "] must not have a static initializer");
            }
            if (staticMixin) {
                if (method.name.equals("<init>")) {
                    continue;
                }
                toInstanceMethod(target, mixin, method);
            }

            AnnotationNode binaryOnly = getAnnotation(method.invisibleAnnotations, BINARY_ONLY_DESCRIPTOR);
            if (binaryOnly != null) {
                method.invisibleAnnotations.remove(binaryOnly);
                method.name = (String) getAnnotationValues(binaryOnly, "value").get(0);
                // Make sure javac ignores the method. The bridge flag is needed for libraries generating
                // implementations (like Mockito) to understand that it's an alternative of the method with the same
                // name and parameters, and it's fine for javac to read it since it uses visible types.
                method.access |= Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE;
                method.signature = null;
            }

            for (MethodNode existing : target.methods) {
                if (existing.name.equals(method.name) && existing.desc.equals(method.desc)) {
                    throw new IllegalStateException("Method [" + method.name + method.desc + "] from mixin ["
                        + mixin.name + "] already exist in [" + target.name + "]");
                }
            }

            target.methods.add(method);
        }

        // Keep the inner class entries (like the one generated for lambdas)
        for (InnerClassNode innerClass : remapped.innerClasses) {
            if (target.innerClasses.stream().noneMatch(i -> i.name.equals(innerClass.name))) {
                target.innerClasses.add(innerClass);
            }
        }
    }

    private static void toInstanceMethod(ClassNode target, ClassNode mixin, MethodNode method)
    {
        Type[] arguments = Type.getArgumentTypes(method.desc);
        if ((method.access & Opcodes.ACC_STATIC) == 0 || arguments.length == 0
            || !arguments[0].getInternalName().equals(target.name)) {
            throw new IllegalStateException("Method [" + method.name + method.desc + "] of static mixin ["
                + mixin.name + "] must be static and take [" + target.name + "] as first parameter");
        }

        method.access &= ~Opcodes.ACC_STATIC;
        method.desc = Type.getMethodDescriptor(Type.getReturnType(method.desc),
            java.util.Arrays.copyOfRange(arguments, 1, arguments.length));

        if (method.signature != null) {
            // Remove the first parameter from the generic signature
            SignatureWriter writer = new SignatureWriter()
            {
                private boolean first = true;

                @Override
                public SignatureVisitor visitParameterType()
                {
                    if (this.first) {
                        this.first = false;

                        return new SignatureWriter();
                    }

                    return super.visitParameterType();
                }
            };
            new SignatureReader(method.signature).accept(writer);
            method.signature = writer.toString();
        }

        // The first parameter is now "this"
        if (method.localVariables != null) {
            for (LocalVariableNode variable : method.localVariables) {
                if (variable.index == 0) {
                    variable.name = "this";
                }
            }
        }
        method.parameters = method.parameters != null && !method.parameters.isEmpty()
            ? method.parameters.subList(1, method.parameters.size()) : method.parameters;
        // Shift the parameter annotations
        if (method.invisibleParameterAnnotations != null) {
            method.invisibleParameterAnnotations = java.util.Arrays.copyOfRange(method.invisibleParameterAnnotations,
                1, method.invisibleParameterAnnotations.length);
            method.invisibleAnnotableParameterCount = 0;
        }
        if (method.visibleParameterAnnotations != null) {
            method.visibleParameterAnnotations = java.util.Arrays.copyOfRange(method.visibleParameterAnnotations, 1,
                method.visibleParameterAnnotations.length);
            method.visibleAnnotableParameterCount = 0;
        }
    }

    private static void addInterface(ClassNode target, String signature)
    {
        int end = signature.indexOf('<');
        String internalName = signature.substring(1, end < 0 ? signature.length() - 1 : end);

        if (!target.interfaces.contains(internalName)) {
            target.interfaces.add(internalName);

            // The generic signature is what the Java compiler uses to resolve the super types so it must be updated
            if (target.signature != null) {
                target.signature += signature;
            } else if (end >= 0) {
                StringBuilder builder = new StringBuilder();
                builder.append('L').append(target.superName).append(';');
                for (String interfaceName : target.interfaces) {
                    if (!interfaceName.equals(internalName)) {
                        builder.append('L').append(interfaceName).append(';');
                    }
                }
                builder.append(signature);
                target.signature = builder.toString();
            }
        }
    }

    private void write(String name, byte[] content) throws IOException
    {
        Path file = this.classes.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }

    private void finish() throws IOException
    {
        for (Map.Entry<String, StringBuilder> entry : this.services.entrySet()) {
            write(SERVICES + entry.getKey(), entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
        }

        Set<String> missing = new HashSet<>(this.mixins.keySet());
        missing.removeAll(this.patched);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Could not find the targets " + missing + " of the mixins");
        }

        for (PackageBridge bridge : this.bridges) {
            if (!bridge.getErrors().isEmpty()) {
                throw new IllegalStateException(
                    "Failed to bridge some methods:\n" + String.join("\n", bridge.getErrors()));
            }
            if (bridge.getBridgeCount() == 0) {
                throw new IllegalStateException("No bridge method generated, is a JAR missing?");
            }
            // Build tool output
            System.out.println("Generated " + bridge.getBridgeCount() + " bridge methods");
        }
    }
}
