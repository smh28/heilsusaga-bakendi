package controllers;


import io.ebean.*;
import models.Log;
import models.Person;
import play.mvc.Controller;

public class LogCtrl extends Controller {

    public static void log(String code, String message) {
        Person person = Application.getCurrentPerson();
        log_for_person(person,code, message);
    }

    public static void log_for_person(Person person, String code, String message) {
        if(code != null && code.length() > 0) {
            Log log = new Log(code);
            if(message != null && message.length() > 0) {
                log.setText(message);
            }
            log.setPerson(person);
            Ebean.save(log);
        }
    }


    public static void log_webservice(String code, String message) {
        log_for_person(null,code, message);
    }


}
