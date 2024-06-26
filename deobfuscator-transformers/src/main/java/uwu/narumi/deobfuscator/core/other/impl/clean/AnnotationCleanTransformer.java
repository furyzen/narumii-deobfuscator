package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class AnnotationCleanTransformer extends Transformer {

  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              classWrapper.getClassNode().invisibleAnnotations = null;
              classWrapper.getClassNode().invisibleAnnotations = null;

              classWrapper
                  .methods()
                  .forEach(
                      methodNode -> {
                        methodNode.invisibleAnnotations = null;
                        methodNode.visibleAnnotations = null;
                        methodNode.invisibleParameterAnnotations = null;
                        methodNode.visibleParameterAnnotations = null;
                      });

              classWrapper
                  .fields()
                  .forEach(
                      fieldNode -> {
                        fieldNode.invisibleAnnotations = null;
                        fieldNode.visibleAnnotations = null;
                      });
            });
  }
}
