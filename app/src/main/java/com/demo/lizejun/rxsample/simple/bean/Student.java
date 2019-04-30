package com.demo.lizejun.rxsample.simple.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @创建者 keepon
 * @创建时间 2019/4/30 0030 下午 11:33
 * @描述 ${TODO}
 * @版本 $$Rev$$
 * @更新者 $$Author$$
 * @更新时间 $$Date$$
 */
public class Student {
	private String studentId;
	private String studentName;

	public Student(String studentId, String studentName) {
		this.studentId = studentId;
		this.studentName = studentName;
	}

	public List<Course> getCourses() {
		return mCourses;
	}

	List<Course> mCourses =new ArrayList<>();
	public Student  addCourse(Course course){
		mCourses.add(course);
		return this;
	}
}
