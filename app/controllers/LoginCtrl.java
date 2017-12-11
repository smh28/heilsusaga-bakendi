package controllers;

import io.ebean.*;
import com.google.inject.Inject;
import models.Person;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import utils.SGSIslandIsSaml20Authentication;


public class LoginCtrl extends Controller {

    @Inject
    FormFactory formFactory;

    private final MessagesApi messagesApi;

    @Inject
    public LoginCtrl(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result loginPage() {
        return ok(views.html.login.render(""));
    }

    private boolean login(String ssn, String password) {
        play.i18n.Lang lang = Http.Context.current().lang();
        Person user = Person.findBySsn(ssn);
        if(user != null) {
            if(user.getWrongPasswordCount() == null || user.getWrongPasswordCount() < 5) {
                if (user.isCorrectPassword(password)) {
                   // session().clear();
                    session("user_ssn", ssn);
                    session("user_name", user.getName());
                    session("user_id", String.valueOf(user.getId()));
                    session("user_email", String.valueOf(user.getEmail()));

                    if (user.isAdmin()) {
                        session("role_admin", "1");
                    } else {
                        session("role_admin", "");
                    }
                    if (user.hasAgreed()) {
                        session("has_agreed", "1");
                    } else {
                        session("has_agreed", "");
                    }
                    if (user.hasRejected()) {
                        session("has_rejected", "1");
                    } else {
                        session("has_rejected", "");
                    }

                    session("login_type","lykilorð");

                    user.hasLoggedIn(true);
                    user.setWrongPasswordCount(0);
                    Ebean.save(user);
                    LogCtrl.log("login", "Login with password");
                    return true;
                } else {
                    // password incorrect
                    user.wrongPasswordEntered();
                    Ebean.save(user);
                    String countText = "";
                    if(user.getWrongPasswordCount() == 5) {
                        flash("danger", messagesApi.get(lang,"general.warning_password_wrongly_entered") + "<br>" + messagesApi.get(lang,"general.warning_password_wrongly_entered_2") +  "<a href='mailto:hjalp@blodskimun.is'>hjalp@blodskimun.is</a> " + messagesApi.get(lang,"general.warning_password_wrongly_entered_3"));
                    }
                    else if(user.getWrongPasswordCount() == 4) {
                        countText = "<strong>Ein</strong> tilraun eftir.";
                    } else {
                        Integer left = 5 - user.getWrongPasswordCount();
                        countText = "<strong>" + left.toString() + "</strong> " + messagesApi.get(lang,"general.warning_password_tries_left");
                    }
                    flash("warning", messagesApi.get(lang,"general.warning_password_incorrect") + "<br>" + countText);
                    return false;
                }
            } else {
                flash("danger", messagesApi.get(lang,"general.warning_password_lockout_1") + "<br>" + messagesApi.get(lang,"general.warning_password_lockout_2") + " <a href='mailto:hjalp@blodskimun.is'>hjalp@blodskimun.is</a> " + messagesApi.get(lang,"general.warning_password_lockout_3"));
                return false;
            }
        } else {
            flash("warning", messagesApi.get(lang,"general.warning_user_not_found"));
        }
        return false;
    }

    public Result loginPost() {
        play.i18n.Lang lang = Http.Context.current().lang();
        DynamicForm requestData = formFactory.form().bindFromRequest();
        String ssn = requestData.get("inputSSN");
        String password = requestData.get("password");
        if(ssn == null){
            ssn = "";
        } else {
            ssn = ssn.trim();
            ssn = ssn.replace("-","");
        }

        if(password == null) {
            password = "";
        }


        if (ssn.length() == 10 && password.length() > 0) {
            // Do login
            if(login(ssn, password)) {
                // login successful
                return redirect(routes.Application.consent());
            }
        } else {
            if(password.length() == 0) {
                flash("warning", messagesApi.get(lang,"general.warning_password_required"));
            } else {
                flash("warning", messagesApi.get(lang,"general.warning_ssn_incorrect"));
            }
        }
        return ok(views.html.login.render(ssn));
    }

    public Result logout() {
        LogCtrl.log("logout","");
        session().clear();
        return redirect(routes.LoginCtrl.loginPage());
    }

    public Result loginIslykill() {
        // todo assign different url if on staging server
        if(true) {
            return redirect("https://innskraning.island.is/?id=heilsusaga.tf.loftfar.is");
        }
        return redirect("https://innskraning.island.is/?id=heilsusaga.is");
    }

    public Result islykill() {
        play.i18n.Lang lang = Http.Context.current().lang();
        try {
            Map<String, String[]> stringMap = request().body().asFormUrlEncoded();

            String samlTokenStringBase64 = stringMap.get("token")[0];

            SGSIslandIsSaml20Authentication saml20Authentication = new SGSIslandIsSaml20Authentication(keyStore());
            Map<String, String> validateSaml = saml20Authentication.validateSaml(samlTokenStringBase64, request().remoteAddress(), request().getHeader("user-agent"), null);
            String persidno = validateSaml.get(SGSIslandIsSaml20Authentication.SAML_ATTRIBUTE_USERSSN);
            String name = validateSaml.get(SGSIslandIsSaml20Authentication.SAML_ATTRIBUTE_NAME );
            String authType = validateSaml.get(SGSIslandIsSaml20Authentication.SAML_ATTRIBUTE_AUTHENTICATION);
            if(authType == null) {
                authType = "Unknown";
            }

            // try to find user
            Person user = Person.findBySsn(persidno);
            // if user not found then create new user
            if(user == null) {
                user = new Person();
                user.setSsn(persidno);
                user.setName(name);
                user.setAdmin(false);
                user.setAlive(true);
                Ebean.save(user);
                LogCtrl.log_for_person(user,"create_person","");
            }
            // now either we found existing person or created a new one
            if(user != null) {
                session().clear();
                session("user_ssn", persidno);
                session("user_name", user.getName());
                session("user_id", String.valueOf(user.getId()));
                session("user_email", String.valueOf(user.getEmail()));

                if (user.isAdmin()) {
                    session("role_admin", "1");
                } else {
                    session("role_admin", "");
                }
                if (user.hasAgreed()) {
                    session("has_agreed", "1");
                } else {
                    session("has_agreed", "");
                }
                if (user.hasRejected()) {
                    session("has_rejected", "1");
                } else {
                    session("has_rejected", "");
                }

                session("login_type",authType);
                user.hasLoggedIn(true);
                user.setWrongPasswordCount(0);
                Ebean.save(user);
                LogCtrl.log("login", "Person logged in with Íslykill (" + authType + ")");
                return redirect(routes.Application.consent());
            }
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
        flash("info", messagesApi.get(lang,"general.warning_user_not_found_detailed"));
        return ok(views.html.login.render(""));
    }

    private static KeyStore keyStore() {

        try {
            InputStream stream = LoginCtrl.class.getClassLoader().getResourceAsStream( "Traustur_bunadur.pem" );
            System.out.println( "stream: " + stream );
            CertificateFactory fact = CertificateFactory.getInstance( "X.509" );
            X509Certificate cert = (X509Certificate)fact.generateCertificate( stream );
            KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
            ks.load( null, null );
            ks.setCertificateEntry( "Traustur Bunadur", cert );
            return ks;
        }

        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
