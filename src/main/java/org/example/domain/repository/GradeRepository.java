package org.example.domain.repository;

import org.example.domain.model.Grade;
import java.util.List;

public interface GradeRepository {
    void save(Grade grade);
    Grade findById(String gradeId);
    List<Grade> findByStudentId(String studentId);
    Grade findGradeByStudentAndSubject(String studentId, String subjectCode);
    double calculateStudentAverage(String studentId);
    int getStudentSubjectCount(String studentId);
    int count();
    void update(Grade grade);
}