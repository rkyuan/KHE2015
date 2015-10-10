package rick.khe2015;

import android.media.Image;


import java.util.Calendar;

public class Post {

    Image img;
    double latitude;
    double longitude;
    Calendar postTime;
    int upVotes;
    String comment;
    /**
     *
     * @param postImage
     *            the image of the post
     * @param lat
     *            the latitude of the post
     * @param longi
     *            the longitude of the post
     * 
     * 
     */
    public Post(String inputComment, Image postImage, double lat, double longi) {
        this.latitude = lat;
        this.longitude = longi;
        this.upVotes = 1;
        this.img = postImage;
        this.postTime = Calendar.getInstance();
        this.comment = inputComment;

    }

    /**
     * Calculates the time (in milliseconds) since the post was posted
     *
     */
    public double calculateTime() {
        double currentTime = Calendar.getInstance().getTimeInMillis();
        return currentTime - this.postTime.getTimeInMillis();
    }

    /**
     * calculates a score given to the post
     */
    public double calculateScore() {
        double sum = 4;
        if (this.upVotes > 0) {
            sum += 2 * Math.log(this.upVotes);
        }
        if (this.upVotes < 0) {
            sum -= 4 * Math.log(Math.abs(this.upVotes));
        }
        double milliToHours = 1.0 / 3600000;
        sum -= 2 * this.calculateTime() * milliToHours;
        return sum;
}

    public int getUpVotes() {
        return this.upVotes;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public Image getImage() {
        return this.img;
    }

    public Calendar getPostTime() {
        return this.postTime;
    }
    public void incrementUpVote(){
        this.upVotes += 1;
    }
    
    public void decrementUpVote(){
        this.upVotes -= 1;
    }
    
    public String getComment(){
        return this.comment;
    }

}
