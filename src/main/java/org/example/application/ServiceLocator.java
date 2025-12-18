package org.example.application;

import org.example.application.service.*;
import org.example.domain.repository.*;
import org.example.service.BulkImportService;

public class ServiceLocator {
    private final StudentService studentService;
    private final GradeService gradeService;
    private final ValidationService validationService;
    private final ExportService exportService;
    private final StatisticsService statisticsService;
    private final AdvancedStatisticsService advancedStatisticsService;
    private final SearchService searchService;
    private final CacheService cacheService;
    private final AuditService auditService;
    private final TaskSchedulerService taskSchedulerService;
    private final BulkImportService bulkImportService;

    public ServiceLocator() {
        // Initialize repositories
        StudentRepository studentRepo = new InMemoryStudentRepository();
        GradeRepository gradeRepo = new InMemoryGradeRepository();

        // Initialize services
        validationService = new ValidationService();
        exportService = new ExportService();
        statisticsService = new StatisticsService();
        advancedStatisticsService = new AdvancedStatisticsService(studentRepo, gradeRepo);
        searchService = new SearchService(studentRepo, gradeRepo);
        cacheService = new CacheService(studentRepo, gradeRepo);
        auditService = new AuditService();
        taskSchedulerService = new TaskSchedulerService();
        bulkImportService = new BulkImportService(studentRepo, gradeRepo, validationService);
        studentService = new StudentService(
                studentRepo, gradeRepo, validationService, exportService
        );

        gradeService = new GradeService(
                gradeRepo, studentRepo, validationService,
                exportService, statisticsService
        );
    }

    // Getters for all services
    public BulkImportService getBulkImportService() { return bulkImportService; }
    public StudentService getStudentService() { return studentService; }
    public GradeService getGradeService() { return gradeService; }
    public ValidationService getValidationService() { return validationService; }
    public ExportService getExportService() { return exportService; }
    public StatisticsService getStatisticsService() { return statisticsService; }
    public AdvancedStatisticsService getAdvancedStatisticsService() { return advancedStatisticsService; }
    public SearchService getSearchService() { return searchService; }
    public CacheService getCacheService() { return cacheService; }
    public AuditService getAuditService() { return auditService; }
    public TaskSchedulerService getTaskSchedulerService() { return taskSchedulerService; }

}