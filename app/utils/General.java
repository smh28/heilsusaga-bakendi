package utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class General {

    public static String formatSSN(String ssn) {
        if(ssn == null) {
            return "";
        }
        ssn = ssn.trim();
        ssn = ssn.replace("-","");
        if(ssn.length() == 10) {
            return ssn.substring(0,6) + "-" + ssn.substring(6);
        }
        return ssn;
    }

    public static String formatDateTime(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        if (date != null) {
            return df.format(date);
        }
        return "";
    }

    public static String formatDate(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        if (date != null) {
            return df.format(date);
        }
        return "";
    }

    public static long daysFromDate(Date fromDate) {
        if(fromDate == null) {
            return 0L;
        }
        Date today = new Date();
        long diff = today.getTime() - fromDate.getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    public static String formatInteger(Integer number) {
        if(number == null){
            return "";
        }
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.GERMAN);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(number);
    }

    public static Long stringToLong(String value, Long _default) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return _default;
        }
    }

    public static Integer getYearsOfAgeFromBirthday(Date birthday) {
        if(birthday == null) {
            return 0;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        return Integer.parseInt(df.format(new Date())) - Integer.parseInt(df.format(birthday));
    }

    public static boolean isValidEmailAddress(String email) {
        if(email == null) {
            return false;
        }
        if(!email.contains("@")) {
            return false;
        }
        return true;
    }

    public static String cleanPassword(String password) {
        if(password == null) {
            return "";
        }
        password = password.trim();
        password = password.replace(".","");
        password = password.replace(",","");
        password = password.toLowerCase();
        return password;
    }

    public static String parseLoginType(String logText) {
        if(logText == null) {
            return "";
        }
        if(logText.toLowerCase().contains("íslykill")) {
            if(logText.toLowerCase().contains("rafræn")) {
                return "rafræn skilríki";
            }
            return "íslykill";
        }
        if(logText.contains("manual")) {
            return "handvirkt";
        }
        return "lykilorð";
    }

}
