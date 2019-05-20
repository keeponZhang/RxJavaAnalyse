package rx.internal.tag;

import rx.functions.Action0;

/**
 * createBy	 keepon
 */
public class TagAction0 implements Action0 {
	private String name = "keepon";
	@Override
	public void call() {

	}

	@Override
	public String toString() {
		return "TagAction0{" +
				"name='" + name + '\'' +
				'}'+hashCode();
	}
}
