package models;

import io.ebean.*;
//import controllers.UtilsCtrl;
import play.data.validation.Constraints;

import javax.persistence.*;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Entity
public class Person  extends Model{

    @OneToMany
    @JoinTable(name = "log")
    @JoinColumn(name = "person_id")
    private List<Log> logs;

    @OneToMany(mappedBy = "person", cascade=CascadeType.ALL)
    @JoinTable(name = "agree_written")
    @JoinColumn(name = "person_id")
    private List<AgreeWritten> writtenAgreements;

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private long id;

    @Constraints.Required
    private String ssn;
    private String name;

    @Constraints.Required
    private String passwordHash;

    private String email;
    private Boolean agreed;
    private Boolean rejected;
    private Boolean has_logged_in;
    private Boolean admin;
    private Boolean alive;
    private Integer wrong_password;
    private String sex;
    private Date dob;
    private long agreedOnline;
    @Column(name = "agreed_logintype")
    private String agreedLoginType;

    private Date agreedDate;

    public static Finder<Long, Person> find = new Finder<>(Person.class);

    public static List<Person> findAllParticipants() {
        return Person.find.query().where().eq("agreed", "1").orderBy("ssn").findList();
    }

    public static List<Person> findAllRejects() {
        return Person.find.query().where().eq("rejected", "1").orderBy("ssn").findList();
    }

    public static List<Person> findAllAgreedAndRejected() {
        return Person.find.query().where().or(Expr.eq("agreed", "1"),Expr.eq("rejected", "1")).orderBy("ssn").findList();
    }

    public static Person findBySsn(String ssn) {
        List<Person> persons = Person.find.query().where().ieq("ssn", ssn).setMaxRows(1).findList();
        if (persons != null && persons.size() > 0) {
            return persons.get(0);
        }
        return null;
    }

    public static Person findByEmail(String email) {
        List<Person> persons = find.query().where().ieq("email", email).setMaxRows(1).findList();
        if (persons != null && persons.size() > 0) {
            return persons.get(0);
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean hasAgreed() {
        if (agreed == null) {
            return false;
        }
        return agreed;
    }

    public void setAgreed(Boolean agreed) {
        this.agreed = agreed;
    }

    public String getEmail() {
        if(email == null) {
            return "";
        }
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean hasRejected() {
        if(rejected == null) {
            return false;
        }
        return rejected;
    }

    public void setRejected(Boolean rejected) {
        this.rejected = rejected;
    }

    public Boolean hasLoggedIn() {
        if(has_logged_in == null) {
            return false;
        }
        return has_logged_in;
    }

    public long getAgreedOnline() {
        return agreedOnline;
    }

    public void setAgreedOnline(long agreedOnline) {
        this.agreedOnline = agreedOnline;
    }


    public void hasLoggedIn(Boolean has_logged_in) {
        this.has_logged_in = has_logged_in;
    }

    public Boolean isAlive() {
        if (alive == null) {
            return false;
        }
        return alive;
    }

    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean isAdmin() {
        if (admin == null) {
            return false;
        }
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getAgreedLoginType() {
        if(agreedLoginType == null) {
            return "";
        }
        return agreedLoginType;
    }

    public void setAgreedLoginType(String agreedLoginType) {
        this.agreedLoginType = agreedLoginType;
    }

    public Integer getWrongPasswordCount() {
        if(wrong_password == null) {
            return 0;
        }
        return wrong_password;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public void setWrongPasswordCount(Integer wrong_password) {
        this.wrong_password = wrong_password;
    }
    public void wrongPasswordEntered() {
        if(this.wrong_password == null) {
            this.wrong_password = 1;
        } else {
            this.wrong_password ++;
        }
    }

    public Boolean isCorrectPassword(String passwordEntered) {
        if(passwordEntered == null) {
            return false;
        }
        passwordEntered = utils.General.cleanPassword(passwordEntered);
        passwordEntered = passwordEntered.trim();
        if(passwordEntered.length() < 1) {
            return false;
        }
        if(this.passwordHash == null || this.passwordHash.length() == 0) {
            return false;
        }
        return utils.PasswordGenerator.checkPasswordHashed(passwordEntered,this.passwordHash);
    }

    public Date temp_findAggreedDate() {
        Date aggreedDate = null;
        if(this.agreed) {
            if(this.agreedOnline == 0) {
                if(this.writtenAgreements != null && this.writtenAgreements.size() > 0 ) {
                    AgreeWritten aw = this.writtenAgreements.get(0);
                    if(aw != null) {
                        return aw.getDateAgreed();
                    }
                }
            } else {
                aggreedDate = getLastDateOfLogType("agree");
            }
        }
        return aggreedDate;
    }

    public Date getAgreedDate() {
        return agreedDate;
    }

    public void setAgreedDate(Date agreedDate) {
        this.agreedDate = agreedDate;
    }

    public Date getRejectedDate() {
        Date rejectedDate = null;
        if(this.rejected) {
            rejectedDate = getLastDateOfLogType("reject");
        }
        return rejectedDate;
    }

    public Date getLastLoginDate() {
        return getLastDateOfLogType("login");
    }

    private Date getLastDateOfLogType(String logTypeCode) {
        Date lastDate = null;
        Iterator itLogs = this.logs.iterator();
        LogType logType = LogType.findByCode(logTypeCode);
        if (logType != null) {
            while (itLogs.hasNext()) {
                Log log = (Log) itLogs.next();
                if (log.getLogType().equals(logType)) {
                    if(lastDate == null || lastDate.before(log.getCreated()) ) {
                        lastDate = log.getCreated();
                    }
                }
            }
        }
        return lastDate;
    }


}
