package com.kay.cyberterrarium

import db.MigrationRunner
import db.DatabaseFactory
import job.controller.JobController
import job.model.JobConfig
import job.model.script.ScriptLanguage
import job.repository.JobConfigRepository
import notification.service.SystemNotification
import notification.model.Notification
import java.time.Duration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationTest {

    @BeforeTest
    fun setUp() {
        MigrationRunner.migrate()
    }

    @Test
    fun testNotificationSingleton() {
        val instance1 = SystemNotification.getInstance()
        val instance2 = SystemNotification.getInstance()
        assertTrue(instance1 === instance2, "SystemNotification should be a singleton")
    }

    @Test
    fun testTriggerNotifications() {
        val notifier = SystemNotification.getInstance()
        notifier.notify(
            "測試通知 - 正常資訊",
            "這是一則測試通知，用於驗證正常資訊的顯示。",
            Notification.Status.INFO
        )
        Thread.sleep(1000)
    }

    @Test
    fun testRealJobFailuresAndTimeouts() {
        val notifier = SystemNotification.getInstance()
        val controller = JobController.createDefault()
        
        val failFlowId = controller.createFlow("Integration Fail Flow")
        val failStageId = controller.listFlowStages(failFlowId).first().id
        
        val failJavaScript = """
            public class Fail {
                public static void main(String[] args) {
                    System.exit(1);
                }
            }
        """.trimIndent()
        
        controller.createJobForFlow(
            failFlowId,
            failStageId,
            "Failing Java Job",
            "This job exits with code 1",
            ScriptLanguage.JAVA,
            failJavaScript,
            1
        )
        
        controller.runFlow(failFlowId, 1)
        Thread.sleep(2000)

        val timeoutFlowId = controller.createFlow("Integration Timeout Flow")
        val timeoutStageId = controller.listFlowStages(timeoutFlowId).first().id
        
        val timeoutJavaScript = """
            public class Timeout {
                public static void main(String[] args) throws Exception {
                    Thread.sleep(10000);
                }
            }
        """.trimIndent()
        
        val timeoutJobId = controller.createJobForFlow(
            timeoutFlowId,
            timeoutStageId,
            "Timeout Java Job",
            "This job sleeps for 10 seconds",
            ScriptLanguage.JAVA,
            timeoutJavaScript,
            1
        )
        
        val db = DatabaseFactory.getInstance()
        val configRepository = JobConfigRepository(db)
        configRepository.updateOneById(timeoutJobId, JobConfig(Duration.ofSeconds(2), 0, 0, emptyMap()))
        
        controller.runFlow(timeoutFlowId, 1)
        Thread.sleep(2000)

        val successFlowId = controller.createFlow("Integration Success Flow")
        val successStageId = controller.listFlowStages(successFlowId).first().id
        
        val successJavaScript = """
            public class Success {
                public static void main(String[] args) {
                    System.out.println("Job ran successfully!");
                }
            }
        """.trimIndent()
        
        controller.createJobForFlow(
            successFlowId,
            successStageId,
            "Success Java Job",
            "This job runs and exits with code 0",
            ScriptLanguage.JAVA,
            successJavaScript,
            1
        )
        
        controller.runFlow(successFlowId, 1)
        Thread.sleep(2000)
        
        controller.deleteFlow(failFlowId)
        controller.deleteFlow(timeoutFlowId)
        controller.deleteFlow(successFlowId)
    }

    @Test
    fun testSimulateAnomalyScenarios() {
        val notifier = SystemNotification.getInstance()
        
        notifier.notify(
            "Job Failed",
            "Workflow [test-fail-flow], Job [Failing Job] failed",
            Notification.Status.ERROR
        )
        Thread.sleep(1500)
        
        notifier.notify(
            "Job Timeout",
            "Workflow [test-timeout-flow], Job [Timeout Job] timed out",
            Notification.Status.WARNING
        )
        Thread.sleep(1500)

        notifier.notify(
            "Workflow Failed",
            "Workflow [test-fail-flow] failed",
            Notification.Status.ERROR
        )
        Thread.sleep(1500)
        
        notifier.notify(
            "系統 CPU 負載過高",
            "CPU 使用率已連續 5 秒超過 90% (當前: 94%)",
            Notification.Status.WARNING
        )
        Thread.sleep(1500)

        notifier.notify(
            "系統記憶體不足",
            "記憶體使用率已連續 5 秒超過 90% (當前: 92%)",
            Notification.Status.WARNING
        )
        Thread.sleep(1500)

        notifier.notify(
            "CPU Overheating",
            "CPU temperature reached 85°C! Please check cooling.",
            Notification.Status.ERROR
        )
        Thread.sleep(1500)
        
        notifier.notify(
            "Workflow Success",
            "Workflow [test-success-flow] completed successfully",
            Notification.Status.INFO
        )
        Thread.sleep(2000)
    }

    @Test
    fun testNotificationDatabasePersistenceAndRetrieval() {
        SystemNotification.clearNotificationLogs()
        val logsBefore = SystemNotification.getNotificationLogs()
        assertTrue(logsBefore.isEmpty())

        val notifier = SystemNotification.getInstance()
        notifier.notify("Test Persistent Title 1", "Test message info 1", Notification.Status.ERROR)
        notifier.notify("Test Persistent Title 2", "Test message info 2", Notification.Status.WARNING)

        val logsAfter = SystemNotification.getNotificationLogs()
        assertTrue(logsAfter.size >= 2)

        val firstEntry = logsAfter[0]
        val secondEntry = logsAfter[1]

        assertTrue(firstEntry.title == "Test Persistent Title 2")
        assertTrue(firstEntry.message == "Test message info 2")
        assertTrue(firstEntry.status == "WARNING")

        assertTrue(secondEntry.title == "Test Persistent Title 1")
        assertTrue(secondEntry.message == "Test message info 1")
        assertTrue(secondEntry.status == "ERROR")

        SystemNotification.clearNotificationLogs()
        val logsCleared = SystemNotification.getNotificationLogs()
        assertTrue(logsCleared.isEmpty())
    }
}
