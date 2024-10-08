package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.ObjectValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StackMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Decrypts {@code long} numbers that uses a technique called Method Parameter Changes. When an author specified
 * the {@code classInitializationOrderStatement} option then you also need to pass it to the constructor. But don't worry.
 * The deobfuscator will tell you if this will be needed.
 *
 * <p>ZKM version: 22.0.3
 *
 * <p>References:
 * <ul>
 * <li>https://www.zelix.com/klassmaster/featuresLongEncryption.html</li>
 * <li>https://www.zelix.com/klassmaster/featuresMethodParameterChanges.html</li>
 * <li>https://www.zelix.com/klassmaster/docs/classInitializationOrderStatement.html</li>
 * </ul>
 *
 * <p>Example long decrypter usage in zelix is here {@link reverseengineering.zelix.longdecrypter.Main} and:
 * <pre>
 * {@code
 *   ldc 5832394289974403481L
 *   ldc -8943439614781261032L
 *   invokestatic java/lang/invoke/MethodHandles.lookup ()Ljava/lang/invoke/MethodHandles$Lookup;
 *   invokevirtual java/lang/invoke/MethodHandles$Lookup.lookupClass ()Ljava/lang/Class;
 *   // Create decrypter
 *   invokestatic me/frep/vulcan/spigot/Vulcan_m.a (JJLjava/lang/Object;)Lme/frep/vulcan/spigot/Vulcan_a;
 *   ldc 19597665297729L
 *   // Decrypt method
 *   invokeinterface me/frep/vulcan/spigot/Vulcan_a.a (J)J
 *   putstatic io/github/repooper/packetevents/PacketEventsPlugin.a J
 * }
 * </pre>
 */
public class ZelixLongEncryptionMPCTransformer extends Transformer {

  private static final Match DECRYPT_LONG_MATCHER = FieldMatch.putStatic().desc("J")
      // Decrypt
      .and(StackMatch.of(0, MethodMatch.invokeInterface().desc("(J)J")
          .and(StackMatch.of(0, NumberMatch.numLong().save("decrypt-key"))) // Decrypt key
          // Create decrypter
          .and(StackMatch.of(1, MethodMatch.invokeStatic().and(Match.predicate(context ->
                  ((MethodInsnNode) context.insn()).desc.startsWith("(JJLjava/lang/Object;)"))).save("create-decrypter-method")

              .and(StackMatch.of(0, MethodMatch.invokeVirtual().and(StackMatch.of(0, MethodMatch.invokeStatic())))) // Class lookup
              .and(StackMatch.of(1, NumberMatch.numLong().save("key-2"))) // Key 2
              .and(StackMatch.of(2, NumberMatch.numLong().save("key-1"))) // Key 1
          ))
          .save("decrypt-method")
      ));

  // Config
  /**
   * Example: key="mypackage.Class0", value="mypackage.Class1" - You guarantee Class0 always initialized before Class1
   *
   * <ul>
   * <li>key - Class that will always be initialized BEFORE {@code value}. It is an internal name
   * <li>value - Class that will always be initialized AFTER {@code key}. It is an internal name
   * </ul>
   */
  private final Map<String, String> classInitOrder;

  private SandBox sandBox;
  private final List<String> processedClasses = new ArrayList<>(); // class internal names

  public ZelixLongEncryptionMPCTransformer() {
    this.classInitOrder = new HashMap<>();
  }

  public ZelixLongEncryptionMPCTransformer(Map<String, String> classInitOrder) {
    this.classInitOrder = classInitOrder.entrySet().stream()
        .collect(
            // Replace all '.' with '/'
            Collectors.toMap(
                entry -> entry.getKey().replace('.', '/'), // Key
                entry -> entry.getValue().replace('.', '/') // Value
            )
        );
  }

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    sandBox = new SandBox(context);

    // Firstly, process the manual list of class initialization order
    for (var entry : classInitOrder.entrySet()) {
      ClassWrapper first = context.getClasses().get(entry.getKey());
      ClassWrapper second = context.getClasses().get(entry.getValue());

      decryptEncryptedLongs(context, first);
      decryptEncryptedLongs(context, second);
    }

    // Decrypt longs
    context.classes(scope).forEach(classWrapper -> {
      decryptEncryptedLongs(context, classWrapper);
    });

    // Remove decrypter classes
    sandBox.getUsedCustomClasses().forEach(clazz -> context.getClasses().remove(clazz.getInternalName()));
  }

  /**
   * Decrypts encrypted longs
   */
  private void decryptEncryptedLongs(Context context, ClassWrapper classWrapper) {
    // Don't process already processed classes
    if (processedClasses.contains(classWrapper.name())) return;

    // Find clinit
    if (classWrapper.findClInit().isEmpty()) return;
    MethodNode clinit = classWrapper.findClInit().get();

    // Zelix came up with a great idea to infer class initialization order by the super classes.
    // So firstly, process encrypted longs in the super class
    if (classWrapper.classNode().superName != null && !classWrapper.classNode().superName.equals("java/lang/Object")) {
      ClassWrapper superClass = context.getClasses().get(classWrapper.classNode().superName);
      if (superClass != null) {
        decryptEncryptedLongs(context, superClass);
      }
    }

    MethodContext methodContext = MethodContext.framed(classWrapper, clinit);

    for (AbstractInsnNode insn : clinit.instructions) {
      InstructionContext insnContext = methodContext.newInsnContext(insn);
      if (insnContext.frame() == null) return;

      MatchContext result = DECRYPT_LONG_MATCHER.matchResult(insnContext);
      if (result != null) {
        // Get instructions from storage
        MethodInsnNode createDecrypterInsn = (MethodInsnNode) result.storage().get("create-decrypter-method").insn();
        MatchContext decryptContext = result.storage().get("decrypt-method");
        MethodInsnNode decryptInsn = (MethodInsnNode) decryptContext.insn();

        // Some keys
        long key1 = result.storage().get("key-1").insn().asLong();
        long key2 = result.storage().get("key-2").insn().asLong();
        long decryptKey = result.storage().get("decrypt-key").insn().asLong();

        ClassWrapper longDecrypterCreatorClass = context.getClasses().get(createDecrypterInsn.owner);

        try {
          // Create decrypter
          InstanceClass clazz = sandBox.getHelper().loadClass(longDecrypterCreatorClass.canonicalName());
          ObjectValue longDecrypterInstance = sandBox.getInvocationUtil().invokeReference(
              clazz.getMethod(createDecrypterInsn.name, createDecrypterInsn.desc),
              Argument.int64(key1), // Key 1
              Argument.int64(key2), // Key 2
              Argument.reference(sandBox.getMemoryManager().nullValue()) // Lookup class
          );
          InstanceClass longDecrypterClass = (InstanceClass) sandBox.getMemoryManager().readClass(longDecrypterInstance);

          //String instanceStringified = sandBox.vm().getOperations().toString(longDecrypterInstance);
          //System.out.println(classWrapper.name() + " -> " + instanceStringified);

          if (isFallbackDecrypter(longDecrypterClass)) {
            LOGGER.error("Detected that '{}' class is decrypted out of order. Decrypted number will have wrong value.", classWrapper.name());
            LOGGER.error("The author used 'classInitializationOrderStatement' (https://www.zelix.com/klassmaster/docs/classInitializationOrderStatement.html) " +
                "during jar obfuscation to specify class initialization order manually. " +
                "You need to pass to ZelixLongEncryptionMPCTransformer a class initialization order. The easiest way wille be doing a static analysis " +
                "and find where the mentioned class is used."
            );
          }

          // Decrypt long value
          long value = sandBox.getInvocationUtil().invokeLong(
              longDecrypterClass.getMethod(decryptInsn.name, decryptInsn.desc),
              Argument.reference(longDecrypterInstance),
              Argument.int64(decryptKey)
          );

          // Remove all instructions that creates decrypter
          decryptContext.removeAll();

          // Set field to decrypted long value!
          insnContext.methodNode().instructions.insertBefore(insnContext.insn(), AsmHelper.numberInsn(value));
          markChange();
        } catch (VMException ex) {
          sandBox.logVMException(ex);
          throw new RuntimeException(ex);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }

    processedClasses.add(classWrapper.name());
  }

  /**
   * Checks if the class is {@link reverseengineering.zelix.longdecrypter.FallbackLongDecrypter}
   */
  private boolean isFallbackDecrypter(InstanceClass clazz) {
    String selfDesc = "L" + clazz.getInternalName() + ";";
    return clazz.staticFieldArea().list().size() == 2
        && clazz.staticFieldArea().list().get(0).getDesc().equals("Z")
        && clazz.staticFieldArea().list().get(1).getDesc().equals(selfDesc)
        && clazz.virtualFieldArea().list().size() == 4
        && clazz.virtualFieldArea().list().get(0).getDesc().equals("Ljava/util/concurrent/ConcurrentHashMap;")
        //&& clazz.virtualFieldArea().list().get(1).getDesc().equals("too hard to infer XD")
        && clazz.virtualFieldArea().list().get(2).getDesc().equals("[I")
        && clazz.virtualFieldArea().list().get(3).getDesc().equals("J");
  }
}
