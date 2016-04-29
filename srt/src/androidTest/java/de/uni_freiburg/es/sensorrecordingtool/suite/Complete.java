package de.uni_freiburg.es.sensorrecordingtool.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.uni_freiburg.es.sensorrecordingtool.RecordingTest;
import de.uni_freiburg.es.sensorrecordingtool.VideoTest;

/**
 * Created by phil on 2/23/16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        RecordingTest.class,
        VideoTest.class
})
public class Complete {
}
