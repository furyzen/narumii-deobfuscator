package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class MethodDebugInfoCleanTransformer extends Transformer {


  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode -> {
              methodNode.parameters = null;
              methodNode.localVariables = null;
              methodNode.exceptions = null;
              this.markChange();
            });
  }
}
