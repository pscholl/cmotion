package de.uni_freiburg.es.sensorrecordingtool;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/**
 * Creates valid SubRip files.
 */

public class SubRip {

    static class Builder {
        private ArrayList<Tuple<Long, Long, String>> mList = new ArrayList<>();

        /**
         * Adds a subtitle at the given time, please note that subtitles are sorted by their start time.
         * @param start absolute starttime in milliseconds
         * @param end absolute endtime in milliseconds
         * @param text styled or unstyled subtitle text
         * @return
         */
        public SubRip.Builder addLine(long start, long end, String text) {
            mList.add(new Tuple<Long, Long, String>(start, end, text));
            return this;
        }

        /**
         * Formats an absolute timestamp to the cirrect subrip format, timezone independant.
         * @param time
         * @return
         */
        public String formatTime(long time) {
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss,SSS");
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = new Date(time);
            return fmt.format(date);
        }

        public String create() {
            StringBuilder stringBuilder = new StringBuilder();

            Collections.sort(mList, new Comparator<Tuple<Long, Long, String>>() {
                @Override
                public int compare(Tuple<Long, Long, String> o1, Tuple<Long, Long, String> o2) {
                    return Long.compare(o1.s, o2.s);
                }
            });

            for (int i = 0; i < mList.size(); i++) {
                long start = mList.get(i).s;
                long end = mList.get(i).t;
                String text = mList.get(i).u;

                stringBuilder.append((i + 1) + "\n");
                stringBuilder.append(String.format("%s --> %s", formatTime(start), formatTime(end)) + "\n");
                stringBuilder.append(text + "\n");
                stringBuilder.append("\n");
            }

            return stringBuilder.toString();
        }
    }

    private static class Tuple<S, T, U> {
        S s;
        T t;
        U u;

        public Tuple(S s, T t, U u) {
            this.s = s;
            this.t = t;
            this.u = u;
        }
    }
}
