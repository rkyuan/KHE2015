import java.lang.Math;
import java.awt.image.BufferedImage;
import java.util.Calendar;

public class Post {

    BufferedImage img;
    double latitude;
    double longitude;
    Calendar postTime;
    int upVotes;
    int downVotes;

    public Post(BufferedImage postImage, double lat, double longi) {
        this.latitude = lat;
        this.longitude = longi;
        this.upVotes = 1;
        this.downVotes = 0;
        this.img = postImage;
        this.postTime = Calendar.getInstance();

    }

    /**
     * Put a short phrase describing the static method myMethod here.
     */
    public double calculateScore() {
        int voteCount = this.upVotes - this.downVotes;
        double sum = Math.log(voteCount);
        sum =

    }

}
