# Student Grade Management System

## Overview
The Student Grade Management System is a comprehensive Java application designed for educational institutions to manage student records, grades, and generate detailed reports. The system features advanced data processing, multi-format file operations, real-time statistics, and concurrent processing capabilities.

## Features

### 🎯 Core Features
- **Student Management**: Add, view, and manage student records with validation
- **Grade Management**: Record and update grades with comprehensive validation
- **Multi-format Support**: Import/export data in CSV, JSON, and Binary formats
- **Advanced Search**: Pattern-based search using regular expressions
- **Real-time Dashboard**: Live statistics with auto-refresh capabilities

### 📊 Analytics & Reporting
- **Class Statistics**: Mean, median, mode, range, standard deviation
- **Grade Distribution**: Visual representation of performance levels
- **Batch Reporting**: Concurrent report generation with thread pooling
- **Performance Metrics**: System performance monitoring and optimization

### ⚡ Advanced Features
- **Concurrent Processing**: Multi-threaded batch operations
- **Caching System**: LRU cache with performance tracking
- **Audit Trail**: Comprehensive logging of all operations
- **Task Scheduling**: Automated background task execution
- **Real-time Monitoring**: Live system performance metrics

## System Architecture

### Directory Structure

project-root/
├── src/main/java/
│ ├── org/example/
│ │ ├── Main.java # Application entry point
│ │ ├── presentation/
│ │ │ └── MenuController.java # User interface controller
│ │ ├── application/
│ │ │ ├── ServiceLocator.java # Dependency injection container
│ │ │ └── service/ # Business logic services
│ │ ├── domain/
│ │ │ ├── model/ # Domain entities
│ │ │ └── repository/ # Data access interfaces
│ │ └── infrastructure/
│ │ └── util/ # Utility classes
├── reports/ # Generated reports
│ ├── csv/ # CSV format reports
│ ├── json/ # JSON format reports
│ └── binary/ # Binary format reports
├── imports/ # Data import directory
├── logs/ # Application logs
└── README.md # This file


### Key Components

1. **MenuController** - Handles user interface and navigation
2. **ServiceLocator** - Manages service dependencies and lifecycle
3. **StudentService** - Manages student-related operations
4. **GradeService** - Handles grade recording and calculations
5. **AdvancedStatisticsService** - Provides comprehensive analytics
6. **CacheService** - Implements caching with LRU eviction
7. **AuditService** - Tracks all system operations

## Installation & Setup

### Prerequisites
- Java JDK 11 or higher
- Maven 3.6+ (for dependency management)
- Git (for version control)

### Build & Run

# Clone the repository
git clone <repository-url>
cd student-grade-management

# Compile the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="org.example.Main"

# Or run directly
java -cp target/classes org.example.Main


Dependencies
Gson (2.10.1) - JSON processing
JUnit 5 (5.9.2) - Unit testing
mockito         - mocking dependencies
Jacoco (0.8.10) - Code coverage

Usage Guide
1. Adding Students
Select option 1 from main menu

Enter student details with validation

Choose student type (Regular/Honors)

System validates all inputs using regex patterns

2. Recording Grades
Select option 4 from main menu

Enter Student ID

Select subject type (Core/Elective)

Enter grade value (0-100)

Confirm grade recording

3. Generating Reports
Select option 5 for single student reports

Choose format (CSV/JSON/Binary/All)

Select report type

Reports are saved to respective directories

4. Batch Operations
Select option 11 for batch reports

Specify number of threads (1-8)

Monitor progress with visual progress bar

View performance summary upon completion

5. Real-time Dashboard
Select option 10

Use commands:

Q - Quit dashboard

R - Refresh immediately

P - Pause auto-refresh

H - Show help

Dashboard auto-refreshes every 5 seconds

File Operations
Import Format
Place import files in ./imports/ directory:

CSV Format: student_id,subject_code,grade

JSON Format: Structured student/grade data

Binary Format: Serialized Java objects

Export Locations
CSV Reports: ./reports/csv/

JSON Reports: ./reports/json/

Binary Reports: ./reports/binary/

Validation Rules
Student ID
Format: STUXXX where XXX is 3 digits

Example: STU001, STU123

Email Address
Standard email format

Must contain @ and domain

Example: student@university.edu

Phone Number
Multiple formats supported:

(123) 456-7890

123-456-7890

+1-123-456-7890

1234567890

Grade Values
Range: 0-100

Can be decimal values

Validated at entry

Statistical Calculations
Formulas Used
Mean (Average):
mean = Σ(x_i) / n


Median:

For odd n: middle value

For even n: average of two middle values

Mode:

Most frequently occurring value(s)

Returns "No mode" if all values unique

Standard Deviation: 

σ = √[Σ(x_i - μ)² / (n - 1)]

Range:
range = max(x) - min(x)


Performance Optimization
Caching Strategy
LRU (Least Recently Used) eviction

Maximum cache size: 150 entries

Automatic cache warming

Hit rate tracking

Thread Pool Configuration
Report Generation: Fixed thread pool (configurable 1-8 threads)

Statistics Updates: Cached thread pool

Scheduled Tasks: Scheduled thread pool (3 threads)

Log Writing: Single thread executor

Memory Management
Streaming for large file operations

Object pooling where appropriate

Proper resource cleanup

Garbage collection monitoring

Testing
Unit Tests
# Run all tests
mvn test

# Generate coverage report
mvn jacoco:report

Test Coverage Areas
Data validation (95%+)

Statistical calculations (90%+)

File operations (85%+)

Concurrent operations (85%+)

Error Handling
Validation Errors
Clear error messages with examples

Pattern display for invalid inputs

Graceful recovery options

File Operations
Directory creation on startup

File existence checks

Format validation

Fallback options

Concurrency
Thread pool management

Proper shutdown procedures

Resource cleanup

Exception propagation

Contributing
Development Workflow
Create feature branch

Implement changes with tests

Run all tests

Create pull request

Code review and merge

Code Standards
Follow Java naming conventions

Add Javadoc for public methods

Use meaningful variable names

Maintain 85%+ test coverage

Troubleshooting
Common Issues
File not found errors

Check file exists in correct directory

Verify file permissions

Ensure directory structure is created

Memory issues

Check available heap space

Monitor cache size

Adjust thread pool sizes

Performance issues

Monitor thread pool utilization

Check cache hit rate

Review file I/O operations

Log Files
Application logs: ./logs/app.log

Error logs: ./logs/errors.log

Audit trail: ./logs/audit.log

License
This project is licensed under the MIT License - see LICENSE file for details.

Support
For issues and questions:

Check the troubleshooting guide

Review the documentation

Submit an issue on GitHub

Contact the development team

Acknowledgments
Built with Java SE

Uses Gson for JSON processing

Inspired by educational management systems

Developed with best practices in mind


