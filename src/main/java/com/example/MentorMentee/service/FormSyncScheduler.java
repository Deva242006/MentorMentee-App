package com.example.MentorMentee.service;

import com.example.MentorMentee.config.GoogleProperties;
import com.example.MentorMentee.model.MentorForm;
import com.example.MentorMentee.repository.MentorFormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Optional background poller. When {@code google.forms.sync-interval-minutes > 0} (and Google is
 * configured), it periodically pulls responses for every stored form so mentors see submissions
 * without pressing Sync. When the interval is 0 or Google isn't configured, no task is registered at
 * all — the app runs perfectly well with on-demand syncing only.
 */
@Component
public class FormSyncScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(FormSyncScheduler.class);

    private final GoogleProperties props;
    private final MentorFormRepository forms;
    private final FormService formService;

    public FormSyncScheduler(GoogleProperties props, MentorFormRepository forms, FormService formService) {
        this.props = props;
        this.forms = forms;
        this.formService = formService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        int minutes = props.getSyncIntervalMinutes();
        if (!props.isConfigured() || minutes <= 0) {
            log.info("Form auto-sync disabled (sync-interval-minutes={}, google configured={}).",
                    minutes, props.isConfigured());
            return;
        }
        log.info("Form auto-sync enabled: every {} minute(s).", minutes);
        registrar.addFixedDelayTask(this::syncAll, Duration.ofMinutes(minutes));
    }

    /** Best-effort sync of every form; a mentor with an expired/disconnected token is simply skipped. */
    private void syncAll() {
        for (MentorForm form : forms.findAll()) {
            try {
                formService.sync(form.getMentorId(), form.getId());
            } catch (Exception e) {
                log.debug("Skipping auto-sync for form {} (mentor {}): {}",
                        form.getId(), form.getMentorId(), e.getMessage());
            }
        }
    }
}
