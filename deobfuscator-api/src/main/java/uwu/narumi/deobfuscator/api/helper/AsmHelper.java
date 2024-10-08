package uwu.narumi.deobfuscator.api.helper;

import java.util.*;
import java.util.function.Predicate;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.context.Context;

public class AsmHelper implements Opcodes {

  /**
   * Very useful utility that converts number to corresponding ASM instruction.
   *
   * @param number The number
   * @return An ASM instruction that represents this number
   */
  public static AbstractInsnNode numberInsn(int number) {
    if (number >= -1 && number <= 5) {
      return new InsnNode(number + 3);
    } else if (number >= -128 && number <= 127) {
      return new IntInsnNode(BIPUSH, number);
    } else if (number >= -32768 && number <= 32767) {
      return new IntInsnNode(SIPUSH, number);
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(long number) {
    if (number >= 0 && number <= 1) {
      return new InsnNode((int) (number + 9));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(float number) {
    if (number == 0 || number == 1 || number == 2) {
      return new InsnNode((int) (number + 11));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(double number) {
    if (number == 0 || number == 1) {
      return new InsnNode((int) (number + 14));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(Number number) {
    if (number instanceof Integer || number instanceof Byte || number instanceof Short) {
      return numberInsn(number.intValue());
    } else if (number instanceof Long) {
      return numberInsn(number.longValue());
    } else if (number instanceof Float) {
      return numberInsn(number.floatValue());
    } else if (number instanceof Double) {
      return numberInsn(number.doubleValue());
    }

    throw new IllegalArgumentException();
  }

  public static void visitNumber(MethodVisitor methodVisitor, int number) {
    if (number >= -1 && number <= 5) {
      methodVisitor.visitInsn(number + 3);
    } else if (number >= -128 && number <= 127) {
      methodVisitor.visitIntInsn(BIPUSH, number);
    } else if (number >= -32768 && number <= 32767) {
      methodVisitor.visitIntInsn(SIPUSH, number);
    } else {
      methodVisitor.visitLdcInsn(number);
    }
  }

  public static void visitNumber(MethodVisitor methodVisitor, long number) {
    if (number >= 0 && number <= 1) {
      methodVisitor.visitInsn((int) (number + 9));
    } else {
      methodVisitor.visitLdcInsn(number);
    }
  }

  public static boolean isAccess(int access, int opcode) {
    return (access & opcode) != 0;
  }

  public static Optional<MethodNode> findMethod(
      ClassNode classNode, MethodInsnNode methodInsnNode) {
    return classNode == null || classNode.methods == null
        ? Optional.empty()
        : classNode.methods.stream()
            .filter(methodNode -> methodNode.name.equals(methodInsnNode.name))
            .filter(methodNode -> methodNode.desc.equals(methodInsnNode.desc))
            .findFirst();
  }

  public static Optional<MethodNode> findMethod(
      ClassNode classNode, Predicate<MethodNode> predicate) {
    return classNode.methods == null
        ? Optional.empty()
        : classNode.methods.stream().filter(predicate).findFirst();
  }

  public static Optional<FieldNode> findField(ClassNode classNode, Predicate<FieldNode> predicate) {
    return classNode.methods == null
        ? Optional.empty()
        : classNode.fields.stream().filter(predicate).findFirst();
  }

  public static Optional<MethodNode> findClInit(ClassNode classNode) {
    return findMethod(classNode, methodNode -> methodNode.name.equals("<clinit>"));
  }

  public static List<AbstractInsnNode> getInstructionsBetween(
      AbstractInsnNode start, AbstractInsnNode end) {
    return getInstructionsBetween(start, end, true, true);
  }

  public static List<AbstractInsnNode> getInstructionsBetween(
      AbstractInsnNode start, AbstractInsnNode end, boolean includeStart, boolean includeEnd) {
    List<AbstractInsnNode> instructions = new ArrayList<>();

    if (includeStart) instructions.add(start);

    while ((start = start.getNext()) != null && start != end) {
      instructions.add(start);
    }

    if (includeEnd) instructions.add(end);

    return instructions;
  }

  /**
   * Convert constant value to instruction that represents this constant
   *
   * @param value A constant value
   * @return An instruction that represents this constant
   */
  public static AbstractInsnNode toConstantInsn(Object value) {
    if (value == null)
      return new InsnNode(ACONST_NULL);
    if (value instanceof String || value instanceof Type)
      return new LdcInsnNode(value);
    if (value instanceof Number number)
      return numberInsn(number);
    if (value instanceof Boolean bool)
      return numberInsn(bool ? 1 : 0);
    if (value instanceof Character character)
      return numberInsn(character);

    throw new IllegalArgumentException("Not a constant");
  }

  public static InsnList from(AbstractInsnNode... nodes) {
    InsnList insnList = new InsnList();
    for (AbstractInsnNode node : nodes) {
      insnList.add(node);
    }
    return insnList;
  }

  public static InsnList copy(InsnList insnList) {
    InsnList copiedInsnList = new InsnList();
    for (AbstractInsnNode node : insnList.toArray()) {
      copiedInsnList.add(node.clone(Map.of()));
    }

    return copiedInsnList;
  }

  public static MethodNode copyMethod(MethodNode methodNode) {
    MethodNode copyMethod =
        new MethodNode(
            methodNode.access,
            methodNode.name,
            methodNode.desc,
            methodNode.signature,
            methodNode.exceptions.toArray(new String[0]));
    methodNode.accept(copyMethod);

    return copyMethod;
  }

  public void removeField(FieldInsnNode fieldInsnNode, Context context) {
    if (!context.getClasses().containsKey(fieldInsnNode.owner)) return;

    context
        .getClasses()
        .get(fieldInsnNode.owner)
        .fields()
        .removeIf(
            fieldNode ->
                fieldNode.name.equals(fieldInsnNode.name)
                    && fieldNode.desc.equals(fieldInsnNode.desc));
  }
}
