package com.ericsson.gerrit.plugins.eventslog.sql;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;

/**
 * Annotation applied to the SQLClient connected to the main database
 */
@Retention(RUNTIME)
@BindingAnnotation
public @interface EventsDb {
}
