package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import java.util.Arrays;
import java.util.function.BiFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class MethodMatch implements Match {

  private final int opcode;
  private String owner;
  private String[] name;
  private String desc;

  private BiFunction<MethodNode, AbstractInsnNode, Boolean> action;

  private MethodMatch(int opcode) {
    this.opcode = opcode;
  }

  public static MethodMatch of(int opcode) {
    return new MethodMatch(opcode);
  }

  public static MethodMatch create() {
    return new MethodMatch(-1);
  }

  public static MethodMatch invokeStatic() {
    return new MethodMatch(Opcodes.INVOKESTATIC);
  }

  public static MethodMatch invokeVirtual() {
    return new MethodMatch(Opcodes.INVOKEVIRTUAL);
  }

  public static MethodMatch invokeSpecial() {
    return new MethodMatch(Opcodes.INVOKESPECIAL);
  }

  public static MethodMatch invokeInterface() {
    return new MethodMatch(Opcodes.INVOKEINTERFACE);
  }

  public Match invokeAction(BiFunction<MethodNode, AbstractInsnNode, Boolean> function) {
    this.action = function;
    return this;
  }

  @Override
  public boolean invoke(MethodNode methodNode, AbstractInsnNode node) {
    return action.apply(methodNode, node);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node instanceof MethodInsnNode
        && (opcode == -1 || node.getOpcode() == opcode)
        && (owner == null || ((MethodInsnNode) node).owner.equals(owner))
        && (name == null || Arrays.binarySearch(name, ((MethodInsnNode) node).name) != -1)
        && (desc == null || ((MethodInsnNode) node).desc.equals(desc));
  }

  public MethodMatch owner(String owner) {
    this.owner = owner;
    return this;
  }

  public MethodMatch name(String... name) {
    this.name = name;
    return this;
  }

  public MethodMatch desc(String desc) {
    this.desc = desc;
    return this;
  }
}
