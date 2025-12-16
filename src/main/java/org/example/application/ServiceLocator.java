package org.example.application;

import org.example.domain.repository.*;
import org.example.application.service.*;

public class ServiceLocator {
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final StudentService studentService;
    private final GradeService gradeService;
    private final ValidationService validationService;
    private final ExportService exportService;
    private final StatisticsService statisticsService;

    public ServiceLocator() {
        this.studentRepository = new InMemoryStudentRepository();
        this.gradeRepository = new InMemoryGradeRepository();
        this.validationService = new ValidationService();
        this.exportService = new ExportService();
        this.statisticsService = new StatisticsService();

        this.studentService = new StudentService(
                studentRepository,
                gradeRepository,
                validationService,
                exportService
        );

        this.gradeService = new GradeService(
                gradeRepository,
                studentRepository,
                validationService,
                exportService,
                statisticsService
        );
    }

    public StudentService getStudentService() { return studentService; }
    public GradeService getGradeService() { return gradeService; }
    public ValidationService getValidationService() { return validationService; }
    public ExportService getExportService() { return exportService; }
    public StatisticsService getStatisticsService() { return statisticsService; }
}