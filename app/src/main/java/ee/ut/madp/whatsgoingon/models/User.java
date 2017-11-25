package ee.ut.madp.whatsgoingon.models;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Model for user in system
 *
 * Created by admin on 07.10.2017.
 */

public class User {
    private String id;
    private String name;
    private String email;
    private String photo;
    private String nationality;
    private String phoneNumber;
    private String school;
    private String work;
    private String birthday;
    private String city;

    public User() {
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public User(String id, String photo, String email, String name) {
        this.id = id;
        this.photo = photo;
        this.email = email;
        this.name = name;
    }

    public User(String id, String name, String email, String photo,
                String nationality, String city, String phoneNumber, String school, String work, Date birthday) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.id = id;
        this.name = name;
        this.email = email;
        this.photo = photo;
        this.nationality = nationality;
        this.phoneNumber = phoneNumber;
        this.school = school;
        this.work = work;
        this.city = city;
        this.birthday = dateFormat.format(birthday);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", photo='" + photo + '\'' +
                ", nationality='" + nationality + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", school='" + school + '\'' +
                ", work='" + work + '\'' +
                ", birthday='" + birthday + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}

