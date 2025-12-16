package org.example.domain.repository;

import org.example.domain.model.Student;
import java.util.*;

public class InMemoryStudentRepository implements StudentRepository {
    private final Map<String, Student> studentMap = new HashMap<>();

    @Override
    public void save(Student student) {
        studentMap.put(student.getStudentId(), student);
    }

    @Override
    public Student findById(String studentId) {
        return studentMap.get(studentId);
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(studentMap.values());
    }

    @Override
    public void update(Student student) {
        studentMap.put(student.getStudentId(), student);
    }

    @Override
    public void delete(String studentId) {
        studentMap.remove(studentId);
    }
}