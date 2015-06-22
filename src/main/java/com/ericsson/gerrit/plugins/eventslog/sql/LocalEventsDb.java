package com.ericsson.gerrit.plugins.eventslog.sql;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;

/**
 * Annotation applied to the SQLClient connected to the local database
 */
@Retention(RUNTIME)
@BindingAnnotation
@interface LocalEventsDb {
}
