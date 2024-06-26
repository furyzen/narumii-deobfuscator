package uwu.narumi.deobfuscator.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;

public class Context {

  private final Map<String, ClassWrapper> classes = new ConcurrentHashMap<>();
  private final Map<String, ClassWrapper> originalClasses = new ConcurrentHashMap<>();
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();

  private LibraryClassLoader loader;
  private SandBox sandBox;

  public LibraryClassLoader getLoader() {
    return loader;
  }

  public void setLoader(LibraryClassLoader loader) {
    this.loader = loader;
  }

  public SandBox getSandBox() {
    return sandBox;
  }

  public void setSandBox(SandBox sandBox) {
    this.sandBox = sandBox;
  }

  public Collection<ClassWrapper> classes() {
    return classes.values();
  }

  public Stream<ClassWrapper> stream() {
    return classes.values().stream();
  }

  public Stream<ClassWrapper> stream(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()));
  }

  public List<ClassWrapper> classes(ClassWrapper scope) {
    return classes.values().stream()
        .filter(classWrapper -> scope == null || classWrapper.name().equals(scope.name()))
        .collect(Collectors.toList());
  }

  public Optional<ClassWrapper> get(String name) {
    return Optional.ofNullable(classes.get(name));
  }

  public Optional<ClassWrapper> remove(ClassWrapper classWrapper) {
    return remove(classWrapper.name());
  }

  public Optional<ClassWrapper> remove(String name) {
    return Optional.ofNullable(classes.remove(name));
  }

  public Map<String, ClassWrapper> getClasses() {
    return classes;
  }

  public Map<String, ClassWrapper> getOriginalClasses() {
    return originalClasses;
  }

  public Map<String, byte[]> getFiles() {
    return files;
  }
}
