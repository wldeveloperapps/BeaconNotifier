package com.example.beaconnotifier;
/*
One-time device vibration java Rumble.once(500); // Vibrate for 500 milliseconds.

        Patterns

        Rumble.makePattern()
        .beat(300)
        .rest(250)
        .beat(720)
        .playPattern();

        Repeating patterns

        Rumble.makePattern()
        .rest(200)
        .beat(30)
        .beat(holdDuration) // Automatically adds to previous beat.
        .playPattern(4);      // Play 4 times in a row.

        Save patterns for later

        RumblePattern pattern = Rumble.makePattern()
        .beat(30).rest(150).beat(40).rest(40);

        pattern.rest(80).beat(700); // Add to a pattern later.
        pattern.playPattern();      // Play it whenever you like.
        Lock patterns to prevent mutation

        pattern.lock();
        pattern.playPattern(3);     // Works just fine.
        pattern.beat(500).rest(250) // Throws IllegalStateException.
*/

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

    public class VibradorHelper
    {
        private static Vibrator vibrator;
        private static boolean rumbleDisabled;

        public static void init(Context applicationContext)
        {
            vibrator = (Vibrator) applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
            rumbleDisabled = (vibrator == null || !vibrator.hasVibrator());
            if (rumbleDisabled && BuildConfig.DEBUG)
            {
                Log.w("Rumble", "System does not have a Vibrator, or the permission is disabled. " +
                        "Rumble has been turned rest. Subsequent calls to static methods will have no effect.");
            }
        }

        private static void apiIndependentVibrate(long milliseconds)
        {
            if (rumbleDisabled)
            {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            else
            {
                vibrator.vibrate(milliseconds);
            }
        }

        private static void apiIndependentVibrate(long[] pattern)
        {
            if (rumbleDisabled)
            {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
            else
            {
                vibrator.vibrate(pattern, -1);
            }
        }

        public static void stop()
        {
            if (rumbleDisabled)
            {
                return;
            }

            vibrator.cancel();
        }

        public static void once(long milliseconds)
        {
            apiIndependentVibrate(milliseconds);
        }

        public static RumblePattern makePattern()
        {
            return new RumblePattern();
        }

        public static class RumblePattern
        {
            private List<Long> internalPattern;
            private boolean locked;

            private RumblePattern()
            {
                locked = false;
                internalPattern = new ArrayList<>();
                internalPattern.add(0L);
            }

            public RumblePattern beat(long milliseconds)
            {
                if (locked)
                {
                    throw new IllegalStateException("RumblePattern is locked! Cannot modify its state.");
                }

                if (internalPattern.size() % 2 == 0)
                {
                    internalPattern.set(internalPattern.size() - 1, internalPattern.get(internalPattern.size() - 1) + milliseconds);
                }
                else
                {
                    internalPattern.add(milliseconds);
                }
                return this;
            }

            public RumblePattern rest(long milliseconds)
            {
                if (locked)
                {
                    throw new IllegalStateException("RumblePattern is locked! Cannot modify its state.");
                }

                if (internalPattern.size() % 2 == 0)
                {
                    internalPattern.add(milliseconds);
                }
                else
                {
                    internalPattern.set(internalPattern.size() - 1, internalPattern.get(internalPattern.size() - 1) + milliseconds);
                }
                return this;
            }

            public void lock()
            {
                if (locked)
                {
                    throw new IllegalStateException("RumblePattern is already locked! Use isLocked() to check.");
                }
                locked = true;
            }

            public boolean isLocked()
            {
                return locked;
            }

            public void playPattern()
            {
                playPattern(1);
            }

            public void playPattern(int numberOfTimes)
            {
                if (numberOfTimes < 0)
                {
                    throw new IllegalArgumentException("numberOfTimes must be >= 0");
                }

                boolean endsWithRest = internalPattern.size() % 2 == 0;

                // We have a List<Long> but we need a long[]. We can't simply use toArray because that yields a Long[].
                // Reserve enough space to hold the full pattern as many times as necessary to play the pattern the right number of times.
                long[] primitiveArray = new long[internalPattern.size() * numberOfTimes - (endsWithRest ? 0 : numberOfTimes - 1)];
                for (int i = 0; i < internalPattern.size(); i++)
                {
                    // Auto unboxing converts each Long to a long.
                    primitiveArray[i] = internalPattern.get(i);
                }

                // Copy the array into itself to duplicate the pattern enough times.
                // Not a simple copy - we must overlay the copies if the pattern ends in a rest.
                //   R    B    R
                // [100, 300, 500]
                //             +
                //           [100, 300, 500]
                for (int i = 1; i < numberOfTimes; i++)
                {
                    for (int j = 0; j < internalPattern.size(); j++)
                    {
                        int k = j + (internalPattern.size() * i) - (endsWithRest ? 0 : i);
                        primitiveArray[k] += primitiveArray[j];
                    }
                }

                apiIndependentVibrate(primitiveArray);
            }

            @Override
            public String toString()
            {
                return "RumblePattern{" +
                        "internalPattern=" + internalPattern +
                        '}';
            }
        }
    }

