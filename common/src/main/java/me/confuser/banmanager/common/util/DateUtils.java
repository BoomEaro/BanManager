package me.confuser.banmanager.common.util;

import lombok.Getter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    @Getter
    private static final long timeDiff = 0;
    private static final Pattern timePattern = Pattern

            .compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);

    // Copyright essentials, all credits to them for this.
    public static long parseDateDiff(String time, boolean future) throws Exception {
        // Support raw timestamps
        if (time.length() >= 10) {
            try {
                long timestamp = Long.parseLong(time);

                if (future && (timestamp - (System.currentTimeMillis() / 1000L)) < 0) {
                    throw new Exception("Timestamp must be in the future");
                }

                return timestamp;
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher m = timePattern.matcher(time);
        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        boolean found = false;
        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue;
            }
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) {
                    years = Integer.parseInt(m.group(1));
                }
                if (m.group(2) != null && !m.group(2).isEmpty()) {
                    months = Integer.parseInt(m.group(2));
                }
                if (m.group(3) != null && !m.group(3).isEmpty()) {
                    weeks = Integer.parseInt(m.group(3));
                }
                if (m.group(4) != null && !m.group(4).isEmpty()) {
                    days = Integer.parseInt(m.group(4));
                }
                if (m.group(5) != null && !m.group(5).isEmpty()) {
                    hours = Integer.parseInt(m.group(5));
                }
                if (m.group(6) != null && !m.group(6).isEmpty()) {
                    minutes = Integer.parseInt(m.group(6));
                }
                if (m.group(7) != null && !m.group(7).isEmpty()) {
                    seconds = Integer.parseInt(m.group(7));
                }
                break;
            }
        }

        if (!found) {
            throw new Exception("Illegal Date");
        }

        Calendar c = new GregorianCalendar();
        if (years > 0) {
            c.add(Calendar.YEAR, years * (future ? 1 : -1));
        }
        if (months > 0) {
            c.add(Calendar.MONTH, months * (future ? 1 : -1));
        }
        if (weeks > 0) {
            c.add(Calendar.WEEK_OF_YEAR, weeks * (future ? 1 : -1));
        }
        if (days > 0) {
            c.add(Calendar.DAY_OF_MONTH, days * (future ? 1 : -1));
        }
        if (hours > 0) {
            c.add(Calendar.HOUR_OF_DAY, hours * (future ? 1 : -1));
        }
        if (minutes > 0) {
            c.add(Calendar.MINUTE, minutes * (future ? 1 : -1));
        }
        if (seconds > 0) {
            c.add(Calendar.SECOND, seconds * (future ? 1 : -1));
        }
        return c.getTimeInMillis() / 1000L;
    }

    public static String format(String pattern, long timestamp) {
        return format(pattern, timestamp, ZoneId.systemDefault());
    }

    public static String format(String pattern, long timestamp, ZoneId zoneId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        return Instant.ofEpochSecond(timestamp).atZone(zoneId).format(formatter);
    }

    public static String getDifferenceFormat(long timestamp) {
        return formatDifference(timestamp - (System.currentTimeMillis() / 1000L));
    }

    public static String formatDifference(long time) {
        long timeSecond = time;
        if (timeSecond <= 0) {
            return "0 секунд";
        }

        int year;
        int month;
        int week;
        int day;
        int hour;
        int min;
        int sec;

        year = (int) (timeSecond / 31536000);
        timeSecond = timeSecond - year * 31536000L;
        month = (int) (timeSecond / 2678400);
        timeSecond = timeSecond - month * 2678400L;
        week = (int) (timeSecond / 604800);
        timeSecond = timeSecond - week * 604800L;
        day = (int) (timeSecond / 86400);
        timeSecond = timeSecond - day * 86400L;
        hour = (int) (timeSecond / 3600);
        timeSecond = timeSecond - hour * 3600L;
        min = (int) (timeSecond / 60);
        timeSecond = timeSecond - min * 60L;
        sec = (int) timeSecond;

        StringBuilder sb = new StringBuilder();
        if (year > 0) {
            sb.append(year).append(" ").append(convertSu(year, "год", "года", "лет")).append(month > 0 || week > 0 || day > 0 || hour > 0 || min > 0 || sec > 0 ? " " : "");
        }
        if (month > 0) {
            sb.append(month).append(" ").append(convertSu(month, "месяц", "месяца", "месяцев")).append(week > 0 || day > 0 || hour > 0 || min > 0 || sec > 0 ? " " : "");
        }
        if (week > 0) {
            sb.append(week).append(" ").append(convertSu(week, "неделю", "недели", "недель")).append(day > 0 || hour > 0 || min > 0 || sec > 0 ? " " : "");
        }
        if (day > 0) {
            sb.append(day).append(" ").append(convertSu(day, "день", "дня", "дней")).append(hour > 0 || min > 0 || sec > 0 ? " " : "");
        }
        if (hour > 0) {
            sb.append(hour).append(" ").append(convertSu(hour, "час", "часа", "часов")).append(min > 0 || sec > 0 ? " " : "");
        }
        if (min > 0) {
            sb.append(min).append(" ").append(convertSu(min, "минуту", "минуты", "минут")).append(sec > 0 ? " " : "");
        }
        if (sec > 0) {
            sb.append(sec).append(" ").append(convertSu(sec, "секунду", "секунды", "секунд"));
        }
        return sb.toString();

    }

    public static String convertSu(long n, String s1, String s2, String s3) {
        n = Math.abs(n) % 100;
        long n1 = n % 10;
        if (n > 10 && n < 20) return s3;
        if (n1 > 1 && n1 < 5) return s2;
        if (n1 == 1) return s1;
        return s3;
    }

}
