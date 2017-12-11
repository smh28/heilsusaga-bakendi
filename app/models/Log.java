package models;

import io.ebean.*;
import play.data.format.Formats;
import play.data.validation.Constraints;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class Log extends io.ebean.Model {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private long id;
    @Constraints.Required
    @Formats.DateTime(pattern = "dd.MM.yyyy")
    private Date created;
    @ManyToOne
    @JoinColumn(name="person_id")
    private Person person;
    private String text;
    @ManyToOne
    @JoinColumn(name="log_type_id")
    private LogType logType;

    public Log(String code) {
        created = new Date();
        logType = LogType.findByCode(code);
        if(logType != null) {
            text = logType.getName();
        }
    }


    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(LogType logType) {
        this.logType = logType;
    }
}
