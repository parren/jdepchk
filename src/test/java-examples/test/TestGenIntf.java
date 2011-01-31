package test;

import java.util.Set;

/** Tests occurrence of 'L' in generic param names. */
public interface TestGenIntf<L, LL, AL extends A, LA extends B, ALA extends C, LAL extends D, LALLA extends E> {
	L l();
	AL al();
	LA la();
	ALA ala();
	LAL lal();
	LALLA lalla();
	void ll(Set<? super LL> bla);
}
