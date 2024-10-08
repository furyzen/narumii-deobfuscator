package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class ClassDebugInfoCleanTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              classWrapper.classNode().sourceDebug = null;
              classWrapper.classNode().sourceFile = null;
            });

    // There is always a change
    markChange();
  }
}
