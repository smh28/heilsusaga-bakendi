package controllers;

import com.google.inject.Inject;
import io.ebean.Ebean;
import models.Person;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.util.Date;

public class Application extends Controller{

    @Inject
    FormFactory formFactory;

    private final MessagesApi messagesApi;

    @Inject
    public Application(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public static Boolean isLoggedIn() {
        String strID = session("user_id");
        return (strID != null && strID.length() > 0);
    }

    public static Boolean isAdmin() {
        String strAdmin = session("role_admin");
        return strAdmin != null && strAdmin.equals("1");
    }


    public static Boolean hasAgreed() {
        String strAgreed = session("has_agreed");
        return strAgreed != null && strAgreed.equals("1");
    }

    public static Boolean hasRejected() {
        String strRejected = session("has_rejected");
        return strRejected != null && strRejected.equals("1");
    }

    public static Long getCurrentPersonID() {
        String strID = session("user_id");
        if (strID != null && strID.length() > 0) {
            return Long.valueOf(strID);
        }
        return null;
    }

    public static String getCurrentPersonEmail() {
        return session("user_email");
    }

    public static Person getCurrentPerson() {
        if(isLoggedIn()) {
            return Person.find.byId(getCurrentPersonID());
        }
        return null;
    }

    public Result english() {
        Controller.changeLang("en");
        return redirect(routes.Application.index());
    }

    public Result icelandic() {
        Controller.changeLang("is");
        return redirect(routes.Application.index());
    }

    public Result index() {

        // assign icelandic if no language is selected
        if (request().cookie("PLAY_LANG") == null) {
            Controller.changeLang("is");
        }

        if(!isLoggedIn()) {
            return redirect(routes.LoginCtrl.loginPage());
        }

        return redirect(routes.Application.consent());
    }


    @Security.Authenticated(Secured.class)
    public Result consent() {
        return ok(views.html.consent.render(getCurrentPerson()));
    }

    @Security.Authenticated(Secured.class)
    public Result consentPost() {

        play.i18n.Lang lang = Http.Context.current().lang();
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String chbox = requestData.get("agree");
        String email = requestData.get("email");
        if(chbox == null){
            chbox = "";
        }
        if(email == null){
            email = "";
        }
        email = email.trim();

        if(chbox.length() == 0) {
            flash("warning", messagesApi.get(lang, "general.warning_must_select_checkbox"));
        }
        else {
            Person user = Application.getCurrentPerson();
            if (user != null) {
                user.setAgreed(true);
                user.setRejected(false);
                user.setEmail(email);
                user.setAgreedLoginType( session("login_type"));
                user.setAgreedDate(new Date());
                Ebean.save(user);
                LogCtrl.log("agree", "");
                session("has_agreed", "1");
                session("has_rejected", "0");
                session("user_email", email);
                if (email.length() > 5) {
                  // TODO: send e-mail to confirm particapation
                    //  Boolean sentEmail = EmailCtrl.sendThankYou(user);
                    // check out mailgun.....
                }
            } else {
                flash("warning", messagesApi.get(lang, "general.warning_unable_to_update"));
            }
        }

        return ok(views.html.consent.render(Application.getCurrentPerson()));
    }

    @Security.Authenticated(Secured.class)
    public Result wouldNotLikeToParticipate() {
        play.i18n.Lang lang = Http.Context.current().lang();
        Person user = Application.getCurrentPerson();
        if(user != null) {
            user.setAgreed(false);
            user.setRejected(true);
            Ebean.save(user);
            LogCtrl.log("reject","");
            session("has_agreed", "0");
            session("has_rejected", "1");

        } else {
            flash("warning",messagesApi.get(lang,"general.warning_unable_to_update"));
        }
        return ok(views.html.consent.render(user));
    }


}
