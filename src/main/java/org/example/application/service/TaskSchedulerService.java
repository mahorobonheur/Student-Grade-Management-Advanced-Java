package org.example.application.service;

import java.util.*;
import java.util.concurrent.*;

public class TaskSchedulerService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleTask(Scanner scanner) {
        System.out.println("\nSCHEDULE AUTOMATED TASKS");
        System.out.println("=".repeat(50));

        System.out.println("1. Daily GPA Recalculation");
        System.out.println("2. Weekly Report Generation");
        System.out.println("3. Hourly Cache Refresh");
        System.out.println("4. Daily Data Backup");
        System.out.println("5. View Scheduled Tasks");
        System.out.println("6. Cancel Scheduled Task");
        System.out.println("7. Return to Main Menu");
        System.out.print("Select option: ");

        int choice = Integer.parseInt(scanner.nextLine().trim());

        switch (choice) {
            case 1:
                scheduleDailyGPARecalculation(scanner);
                break;
            case 2:
                scheduleWeeklyReports(scanner);
                break;
            case 3:
                scheduleHourlyCacheRefresh();
                break;
            case 4:
                scheduleDailyBackup(scanner);
                break;
            case 5:
                displayScheduledTasks();
                break;
            case 6:
                cancelScheduledTask(scanner);
                break;
        }
    }

    private void scheduleDailyGPARecalculation(Scanner scanner) {
        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter minute (0-59): ");
        int minute = Integer.parseInt(scanner.nextLine().trim());

        Runnable task = () -> {
            System.out.println("Running daily GPA recalculation...");
            // Implement GPA recalculation logic
            System.out.println("✅ GPA recalculation completed!");
        };

        scheduleDailyTask("Daily GPA Recalculation", task, hour, minute);
        System.out.println("✅ Task scheduled for daily at " + hour + ":" + minute);
    }

    private void scheduleWeeklyReports(Scanner scanner) {
        System.out.println("Select day of week:");
        System.out.println("1. Monday");
        System.out.println("2. Tuesday");
        System.out.println("3. Wednesday");
        System.out.println("4. Thursday");
        System.out.println("5. Friday");
        System.out.println("6. Saturday");
        System.out.println("7. Sunday");
        System.out.print("Select: ");
        int dayOfWeek = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine().trim());

        Runnable task = () -> {
            System.out.println("Generating weekly reports...");
            // Implement report generation logic
            System.out.println("✅ Weekly reports generated!");
        };

        scheduleWeeklyTask("Weekly Reports", task, dayOfWeek, hour, 0);
        System.out.println("✅ Task scheduled for weekly on day " + dayOfWeek + " at " + hour + ":00");
    }

    private void scheduleHourlyCacheRefresh() {
        Runnable task = () -> {
            System.out.println(" Refreshing cache...");
            // Implement cache refresh logic
            System.out.println("✅ Cache refreshed!");
        };

        scheduleHourlyTask("Hourly Cache Refresh", task, 0);
        System.out.println("✅ Task scheduled for hourly at :00");
    }

    private void scheduleDailyBackup(Scanner scanner) {
        System.out.print("Enter hour (0-23): ");
        int hour = Integer.parseInt(scanner.nextLine().trim());

        Runnable task = () -> {
            System.out.println("Running daily backup...");
            // Implement backup logic
            System.out.println("✅ Backup completed!");
        };

        scheduleDailyTask("Daily Backup", task, hour, 0);
        System.out.println("✅ Backup scheduled for daily at " + hour + ":00");
    }

    private void scheduleDailyTask(String name, Runnable task, int hour, int minute) {
        // Calculate initial delay
        long initialDelay = calculateInitialDelay(hour, minute);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task, initialDelay, 24, TimeUnit.HOURS);

        scheduledTasks.put(name, future);
    }

    private void scheduleWeeklyTask(String name, Runnable task, int dayOfWeek, int hour, int minute) {
        long initialDelay = calculateWeeklyDelay(dayOfWeek, hour, minute);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task, initialDelay, 7, TimeUnit.DAYS);

        scheduledTasks.put(name, future);
    }

    private void scheduleHourlyTask(String name, Runnable task, int minute) {
        long initialDelay = calculateHourlyDelay(minute);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task, initialDelay, 1, TimeUnit.HOURS);

        scheduledTasks.put(name, future);
    }

    private long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.HOUR_OF_DAY, hour);
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);

        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return scheduledTime.getTimeInMillis() - now.getTimeInMillis();
    }

    private long calculateWeeklyDelay(int dayOfWeek, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        scheduledTime.set(Calendar.HOUR_OF_DAY, hour);
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);

        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.WEEK_OF_YEAR, 1);
        }

        return scheduledTime.getTimeInMillis() - now.getTimeInMillis();
    }

    private long calculateHourlyDelay(int minute) {
        Calendar now = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);

        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.HOUR_OF_DAY, 1);
        }

        return scheduledTime.getTimeInMillis() - now.getTimeInMillis();
    }

    private void displayScheduledTasks() {
        System.out.println("\nSCHEDULED TASKS");
        System.out.println("=".repeat(60));

        if (scheduledTasks.isEmpty()) {
            System.out.println("No scheduled tasks.");
            return;
        }

        scheduledTasks.forEach((name, future) -> {
            long delay = future.getDelay(TimeUnit.MILLISECONDS);
            System.out.printf("%-25s: Next run in %d minutes %d seconds%n",
                    name, delay / 60000, (delay % 60000) / 1000);
        });
    }

    private void cancelScheduledTask(Scanner scanner) {
        displayScheduledTasks();
        System.out.print("\nEnter task name to cancel: ");
        String taskName = scanner.nextLine().trim();

        ScheduledFuture<?> future = scheduledTasks.get(taskName);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(taskName);
            System.out.println("✅ Task cancelled: " + taskName);
        } else {
            System.out.println("❌ Task not found: " + taskName);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}