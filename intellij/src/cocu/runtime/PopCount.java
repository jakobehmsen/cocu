package cocu.runtime;

import java.util.function.BiFunction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PopCount {
	public static class Source {
		public static class Default implements BiFunction<Integer, Instruction, Integer> {
			@Override
			public Integer apply(Integer value, Instruction instruction) {
				return value;
			}
		}
		
		public static class ReceiverPlusOperand implements BiFunction<Integer, Instruction, Integer> {
			@Override
			public Integer apply(Integer value, Instruction instruction) {
				return 1 /*receiver*/ + (int)instruction.getOperand(value);
			}
		}
		
		public static class Operand implements BiFunction<Integer, Instruction, Integer> {
			@Override
			public Integer apply(Integer value, Instruction instruction) {
				return (int)instruction.getOperand(value);
			}
		}
		
		public static class OperandArrayLength implements BiFunction<Integer, Instruction, Integer> {
			@Override
			public Integer apply(Integer value, Instruction instruction) {
				return ((Object[])instruction.getOperand(value)).length;
			}
		}
	}
	
//	public static class Source {
//		public static final int VALUE = 0;
//		public static final int OPERAND = 1;
//		public static final int OPERAND_ARRAY_LENGTH = 2;
//	}
	public static class Timing {
		public static final int BEFORE_ERROR = 0;
		public static final int AFTER_ERROR = 1;
	}
	int timing() default Timing.AFTER_ERROR;
//	int source() default Source.VALUE;
	int value();
	Class<? extends BiFunction<Integer, Instruction, Integer>> source() default Source.Default.class;
}
