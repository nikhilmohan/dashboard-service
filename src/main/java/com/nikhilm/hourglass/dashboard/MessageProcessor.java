package com.nikhilm.hourglass.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.Event;
import com.nikhilm.hourglass.dashboard.models.Goal;
import com.nikhilm.hourglass.dashboard.models.Task;
import com.nikhilm.hourglass.dashboard.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;


@EnableBinding(MessageProcessor.MessageSink.class)
@Slf4j
public class MessageProcessor {

    private final DashboardService dashboardService;

    @Autowired
    public MessageProcessor(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @StreamListener(target = MessageSink.INPUT_TASKS)
    public void processTaskEvents(Event<String, Task> event) {

        log.info("Process message created at {}...", event.getEventCreatedAt());
        Task task = null;

        if (event.getData() != null)    {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                task = objectMapper.convertValue(event.getData(), Task.class);

            } catch(Exception e)   {
                log.error("Exception " + e.getMessage());
                throw new DashboardException(500, "Data type error!");
            }
        } else  {
            throw new DashboardException(500, "User record parse failed!");
        }

        switch (event.getEventType()) {

        case TASK_ADDED:

            log.info("Added Task with ID: {}", task.getId());
            dashboardService.computeScoreOnTaskAdd(task).subscribe();
            break;

        case TASK_COMPLETED:
            log.info("Completed Task with Id: {}", event.getKey());
            dashboardService.computeScoreOnTaskComplete(event.getData()).subscribe();
            break;

        default:
            String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a relevant Task event";
           log.warn(errorMessage);
           throw new RuntimeException(errorMessage);
        }

        log.info("Message processing done!");
    }
    @StreamListener(target = MessageSink.INPUT_GOALS)
    public void processGoalEvents(Event<String, Goal> event) {

        log.info("Process message created at {}...", event.getEventCreatedAt());

        Goal goal = null;

        if (event.getData() != null)    {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                goal = objectMapper.convertValue(event.getData(), Goal.class);

            } catch(Exception e)   {
                log.error("Exception " + e.getMessage());
                throw new DashboardException(500, "Data type error!");
            }
        } else  {
            throw new DashboardException(500, "User record parse failed!");
        }


        switch (event.getEventType()) {

            case GOAL_ADDED:
                Goal addedGoal = event.getData();
                log.info("Added goal with ID: {}", addedGoal.getId());
                dashboardService.computeScoreOnGoalAdd(addedGoal).block();
                break;

            case GOAL_DEFERRED:
                Goal deferredGoal = event.getData();
                log.info("Deferred goal with Id: {}", event.getKey());
                dashboardService.computeScoreOnGoalDeferred(deferredGoal).block();
                break;

            case GOAL_RESUMED:
                Goal resumedGoal = event.getData();
                log.info("Resumed goal with Id: {}", event.getKey());
                dashboardService.computeScoreOnGoalResumed(resumedGoal).block();
                break;
            case GOAL_COMPLETED:
                log.info("Completed goal with Id: {}", event.getKey());
                dashboardService.computeScoreOnGoalComplete(event.getData()).block();
                break;

            default:
                String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a relevant goal event";
                log.warn(errorMessage);
                throw new RuntimeException(errorMessage);
        }

        log.info("Message processing done!");
    }
    @StreamListener(target = MessageSink.INPUT_DASHBOARD)
    public void processUserEvents(Event<String, Object> event) {

        log.info("Process message created at {}...", event.getEventCreatedAt());

        switch (event.getEventType()) {

            case USER_ADDED:
                log.info("Added user with ID: {}", event.getKey());
                dashboardService.initializeUser(event);
                break;


            default:
                String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a relevant goal event";
                log.warn(errorMessage);
                throw new RuntimeException(errorMessage);
        }


        log.info("Message processing done!");
    }

    public interface MessageSink {

        String INPUT_TASKS = "input-tasks";
        String INPUT_GOALS = "input-goals";
        String INPUT_DASHBOARD = "input-dashboard";

        @Input(INPUT_TASKS)
        MessageChannel inputTasks();

        @Input(INPUT_GOALS)
        MessageChannel inputGoals();

        @Input(INPUT_DASHBOARD)
        MessageChannel inputDashboard();


    }
}
