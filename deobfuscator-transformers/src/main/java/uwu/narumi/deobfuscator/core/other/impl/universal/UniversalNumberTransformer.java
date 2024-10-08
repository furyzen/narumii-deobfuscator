package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.UselessPopCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.MathBinaryOperationsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.MethodCallsOnLiteralsTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.MathUnaryOperationsTransformer;

/**
 * Simplifies number operations on constant values.
 */
public class UniversalNumberTransformer extends ComposedTransformer {
  public UniversalNumberTransformer() {
    super(
        () -> new ComposedTransformer(true,
            MethodCallsOnLiteralsTransformer::new,
            MathBinaryOperationsTransformer::new,
            MathUnaryOperationsTransformer::new
        ),

        UselessPopCleanTransformer::new
    );
  }
}
