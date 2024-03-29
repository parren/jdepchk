package test;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({ "unused", "null" })//
@ClassAttr public abstract class TestClass<E extends GenericLower, F extends GenericLower1 & GenericLower2, G extends GenericLower3> extends Base implements IntfA, IntfB {

	@ConstAttr public static final Const CONST = null;

	@FieldAttr private final Field field = null;

	@PrivMtdAttr private Returned foo(Param1 p1, Param2 p2) throws Exception {
		return null;
	}

	@PublMtdAttr public void throwing() throws IllegalArgumentException {}

	private void impl() {
		if (ConstRef.CONST == 0) {} // will not be flagged
		if (StaticRef.value == 0) {}
		if (FieldRef.value == 0) {}
		if (StaticMethodRef.foo()) {}
		@CodeAttr MethodRef ref = null; // @CodeAttr will not be flagged
		if (ref.bar()) {}
		System.out.println(); // refs java/io/PrintStream
		if (new ArrayList<GenericArg>() != null) {} // GenericArg will not be flagged
	}

	protected <K extends GenericLowerMtd> void gen() {}

	protected Set<? super GenericUpper1> set = null;
	private Map<Set<? super GenericUpper2>, Set<? extends GenericLower2>> map = null;

	public <M> M foo(M bar) {
		return null;
	}

	private class Lala<E> {}
	private Lala<Object> lala;
		
	private class Gen<Set> {}
	private Gen<Set<Integer>> gen;
	private Gen<Integer>[][] arr;
	
}
