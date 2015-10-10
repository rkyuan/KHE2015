import java.awt.image.BufferedImage;
import java.util.Calendar;

public class Post {

    BufferedImage img;
    double latitude;
    double longitude;
    Calendar postTime;
    int upVotes;

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
    public Post(BufferedImage postImage, double lat, double longi) {
        this.latitude = lat;
        this.longitude = longi;
        this.upVotes = 1;
        this.img = postImage;
        this.postTime = Calendar.getInstance();

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

    public BufferedImage getImage() {
        return this.img;
    }

    public Calendar getPostTime() {
        return this.postTime;
    }

}
