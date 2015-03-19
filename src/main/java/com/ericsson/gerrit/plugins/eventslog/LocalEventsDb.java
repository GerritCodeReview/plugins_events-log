package com.ericsson.gerrit.plugins.eventslog;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
@BindingAnnotation
public @interface LocalEventsDb {
}
