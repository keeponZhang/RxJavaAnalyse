package com.demo.lizejun;

/**
 * Created by keepon on
 */

public class DoubleClickCheckUtils {
	/**
	 * 上一次点击时间
	 */
	public static long lastClickTime;

	/**
	 * 验证两次点击时间差
	 *
	 * @return 是否验证通过
	 */
	public static  boolean vertifyDuration()
	{
		return vertifyDurationByTime(1000);
	}
	public static  boolean vertify500Duration()
	{
		return vertifyDurationByTime(500);
	}
	public static  boolean vertifyDurationByTime(int veritfyTime)
	{
		long tmpTime = System.currentTimeMillis();
		if (tmpTime - lastClickTime <= veritfyTime && tmpTime - lastClickTime > 0)
		{
			return false;
		}
		lastClickTime = System.currentTimeMillis();
		return true;
	}

}
