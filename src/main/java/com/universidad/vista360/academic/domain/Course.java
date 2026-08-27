package com.universidad.vista360.academic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class Course {

    @Id
    private String courseCode;

    private String courseName;

    private Integer credits;

    protected Course() {
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public Integer getCredits() {
        return credits;
    }
}
