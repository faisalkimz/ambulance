package com.example.medicult;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider; // Import for Option 2
// import androidx.test.platform.app.InstrumentationRegistry; // Import for Option 1 (commented out)
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.

        // Option 1: Using getArguments (Recommended)
        // Context appContext = InstrumentationRegistry.getArguments().getApplicationContext();

        // Option 2: Using ApplicationProvider.getApplicationContext
        Context appContext = ApplicationProvider.getApplicationContext();

        assertEquals("sachdeva.saksham.medrescue", appContext.getPackageName());
    }
}
