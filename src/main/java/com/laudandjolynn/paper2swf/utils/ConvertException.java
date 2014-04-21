package com.laudandjolynn.paper2swf.utils;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 下午1:18:34
 * @copyright: www.laudandjolynn.com
 */
public class ConvertException extends RuntimeException {
	private static final long serialVersionUID = -1307222439762982151L;

	public ConvertException() {
		super();
	}

	public ConvertException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public ConvertException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ConvertException(String arg0) {
		super(arg0);
	}

	public ConvertException(Throwable arg0) {
		super(arg0);
	}

}
