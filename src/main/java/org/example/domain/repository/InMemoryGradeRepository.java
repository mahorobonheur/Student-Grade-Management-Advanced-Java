package org.example.domain.repository;

import org.example.domain.model.Grade;
import java.util.*;

public class InMemoryGradeRepository implements GradeRepository {
    private final Map<String, Grade> gradeMap = new HashMap<>();
    private final Map<String, List<Grade>> studentGradeMap = new HashMap<>();

    @Override
    public void save(Grade grade) {
        gradeMap.put(grade.getGradeId(), grade);
        studentGradeMap.computeIfAbsent(grade.getStudentId(), k -> new ArrayList<>())
                .add(grade);
    }

    @Override
    public Grade findById(String gradeId) {
        return gradeMap.get(gradeId);
    }

    @Override
    public List<Grade> findByStudentId(String studentId) {
        return studentGradeMap.getOrDefault(studentId, new ArrayList<>());
    }

    @Override
    public Grade findGradeByStudentAndSubject(String studentId, String subjectCode) {
        List<Grade> grades = studentGradeMap.get(studentId);
        if (grades != null) {
            for (Grade grade : grades) {
                if (grade.getSubject().getSubjectCode().equals(subjectCode)) {
                    return grade;
                }
            }
        }
        return null;
    }

    @Override
    public double calculateStudentAverage(String studentId) {
        List<Grade> grades = studentGradeMap.get(studentId);
        if (grades == null || grades.isEmpty()) return 0.0;

        double total = grades.stream().mapToDouble(Grade::getGrade).sum();
        return total / grades.size();
    }

    @Override
    public int getStudentSubjectCount(String studentId) {
        List<Grade> grades = studentGradeMap.get(studentId);
        return grades != null ? grades.size() : 0;
    }

    @Override
    public int count() {
        return gradeMap.size();
    }

    @Override
    public void update(Grade grade) {
        gradeMap.put(grade.getGradeId(), grade);
    }
}