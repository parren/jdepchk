package ch.parren.jdepchk.classes;

import java.io.IOException;

public final class SingleClassSet implements ClassSets {

	private final ClassSet set;

	public SingleClassSet(ClassSet set) {
		this.set = set;
	}
	
	public void accept(Visitor visitor) throws IOException {
		visitor.visitClassSet(set);
	}

}
