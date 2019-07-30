package no.sforman.myevents;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String id;
    private String firstname;
    private String surname;
    private String email;
    private String imgUrl;

    public User(String id, String firstname, String surname, String email, String imgUrl){
        this.id = id;
        this.firstname = firstname;
        this.surname = surname;
        this.email = email;
        this.imgUrl = imgUrl;
    }

    public String getId() {
        return id;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getFullname(){
        return getFirstname() + " " + getSurname();
    }



    protected User(Parcel in) {
        id = in.readString();
        firstname = in.readString();
        surname = in.readString();
        email = in.readString();
        imgUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(firstname);
        dest.writeString(surname);
        dest.writeString(email);
        dest.writeString(imgUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}