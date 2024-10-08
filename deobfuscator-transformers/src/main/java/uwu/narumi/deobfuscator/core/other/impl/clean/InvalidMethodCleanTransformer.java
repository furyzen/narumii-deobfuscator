package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Remove invalid methods. WARNING: If some transformer will produce invalid bytecode in methods, this transformer will remove them.
 */
public class InvalidMethodCleanTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).parallelStream().forEach(classWrapper -> {
      var iterator = classWrapper.methods().iterator();
      while (iterator.hasNext()) {
        MethodNode methodNode = iterator.next();

        Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
        try {
          analyzer.analyze(classWrapper.name(), methodNode);
        } catch (AnalyzerException e) {
          // Remove invalid method
          LOGGER.warn("Found invalid method: {}#{}{}. Removing...", classWrapper.name(), methodNode.name, methodNode.desc);
          iterator.remove();
          markChange();
        }
      }
    });
  }
}
