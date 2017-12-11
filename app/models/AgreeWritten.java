package models;

import io.ebean.*;
import play.data.format.Formats;
import play.data.validation.Constraints;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class AgreeWritten extends Model {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private long id;

    @Constraints.Required
    @Formats.DateTime(pattern = "dd.MM.yyyy")
    private Date created;

    @Formats.DateTime(pattern = "dd.MM.yyyy")
    private Date dateAgreed;

    @Constraints.Required
    @ManyToOne
    @JoinColumn(name="person_id")
    private Person person;

    @Constraints.Required
    @ManyToOne
    @JoinColumn(name="staff_id")
    private Person staff;

    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getDateAgreed() {
        return dateAgreed;
    }

    public void setDateAgreed(Date dateAgreed) {
        this.dateAgreed = dateAgreed;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Person getStaff() {
        return staff;
    }

    public void setStaff(Person staff) {
        this.staff = staff;
    }

    public static Finder<Long, AgreeWritten> find = new Finder<>(AgreeWritten.class);

    public static List<AgreeWritten> findAll(){
        return  AgreeWritten.find.all();
    }

}
