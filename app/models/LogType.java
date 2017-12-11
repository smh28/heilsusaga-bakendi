package models;



import javax.persistence.*;

import io.ebean.*;
import play.data.format.*;
import play.data.validation.*;
import java.util.List;

@Entity
public class LogType extends Model {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private long id;

    @Constraints.Required
    private String code;

    private String name;

    public LogType() {}

    public static Finder<Long, LogType> find = new Finder<>(LogType.class);

    public static LogType findByCode(String searchCode) {
        List<LogType> logTypes = LogType.find.query().where().ieq("code",searchCode).setMaxRows(1).findList();
        if(logTypes != null && logTypes.size() > 0) {
            return logTypes.get(0);
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
