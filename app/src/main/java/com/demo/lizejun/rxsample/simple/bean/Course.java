package com.demo.lizejun.rxsample.simple.bean;

/**
 * @创建者 keepon
 * @创建时间 2019/4/30 0030 下午 11:34
 * @描述 ${TODO}
 * @版本 $$Rev$$
 * @更新者 $$Author$$
 * @更新时间 $$Date$$
 */
public class Course {
	private String  id;
	private String courseName;
	public Course(String id, String courseName) {
		this.id = id;
		this.courseName = courseName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	@Override
	public String toString() {
		return "Course{" +
				"id=" + id +
				", courseName='" + courseName + '\'' +
				'}';
	}
}
