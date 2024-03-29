package rick.khe2015;

import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.http.HttpClient;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class MapsDisplay extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Post[] posts;
    private final int POSTSIZE = 25;
    private Handler h;
    String newPostPath;
    private boolean updateloop = false;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private String imageFileName;
    private File photoFile;
    private java.net.URL postUrl;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }


        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_display);
        setUpMapIfNeeded();
        posts = new Post[POSTSIZE];
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:73f3c20b-bd6b-4e56-ac8b-f315813a39cd", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        h = new Handler();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                //find marker in posts
                Post match = null;
                for (Post p : posts){
                    if (marker.getPosition().equals(new LatLng(p.latitude,p.longitude))){
                        match = p;
                    }
                }
                if (match!=null){
                    View v = getLayoutInflater().inflate(R.layout.windowlayout, null);
                    //add image to view
                    ImageView iv = (ImageView) v.findViewById(R.id.coolImage);
                    //add comment to view
                    TextView tv = (TextView) v.findViewById(R.id.funnyCaption);
                    tv.setText(match.comment);

                }
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        updateloop=true;
        //h.postDelayed(update,1000);

    }

    @Override
    protected void onPause(){
        super.onPause();
        updateloop = false;
    }



    private Runnable update = new Runnable(){
        @Override
        public void run() {
            if(updateloop) {
                CameraPosition pos = mMap.getCameraPosition();
                mMap.addMarker(new MarkerOptions().position(pos.target));
                
                h.postDelayed(update, 1000);
            }else{
                h.removeCallbacks(update);
            }
        }
    };

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * inserts the given element at the given index and pushes everything past it down
     */
    private static void insertElementAt(Post[] array, Post value, int index) {
        for (int i = array.length - 1; i > index; i--) {
            array[i] = array[i - 1];
        }
        array[index] = value;
    }
    
    
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //I am going to comment out this first line and start messing with shit
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

//        Post[] top25 = new Post[Math.min(25, posts.length)];
//        for (int i = 0; i < posts.length; i++) {
//            double ithScore = posts[i].calculateScore();
//            for (int j = 0; j < top25.length; j++) {
//                if (top25[j] == null) {
//                    top25[j] = posts[i];
//                    break;
//                } else {
//                    if (ithScore > top25[j].calculateScore()) {
//                        swap(top25, posts[i], j);
//                        break;
//
//                    }
//                }
//            }
//        }
//        for(int i = 0; i < top25.length; i++){
//
//            mMap.addMarker({
//                lat: top25[i].getLatitude(),
//                lng: top25[i].getLongitude(),
//                infoWindow: {
//                    content: "<p>" + top25[i].getComment + "</p><img src='myimage.jpg' alt='image in infowindow'>"
//                }
//            });
      //  }
    
    }

    private void updateLocation(){
        LatLngBounds limits = mMap.getProjection().getVisibleRegion().latLngBounds;
        double nb, sb,eb,wb;
        nb = limits.northeast.latitude;
        sb = limits.southwest.latitude;
        eb = limits.northeast.longitude;
        wb = limits.southwest.longitude;
        //pass bounds to server
        mMap.clear();

        //update posts from server
        for (int i = 0; i <POSTSIZE; i++ ){
            if(posts[i]==null){
                break;
            }
            mMap.addMarker(new MarkerOptions());
        }
    }

    private void createPost(){
        Post newPost = new Post();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                //...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 1);
                newPost.img=postUrl;
                //now start activity for commenting picture
//                Intent commentPic = new Intent(this,commentImage.class);
//                commentPic.putExtra("path",newPostPath);
//                startActivity(commentPic);


                //Log.v("photopath", newPostPath);
            }
        }


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == 1){//return of the image
            //upload file to server
            //File photoFile = new File(newPostPath);

            //now start activity for commenting picture
                Intent commentPic = new Intent(this,commentImage.class);
                commentPic.putExtra("path",newPostPath);
                startActivityForResult(commentPic,2);
        }
        if(requestCode == 2){//return of the comment

            //Log.v("comment:",data.getStringExtra("comment"));
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if(mLastLocation!=null){
                Log.v("lat",String.valueOf(mLastLocation.getLatitude()));
                Log.v("long",String.valueOf(mLastLocation.getLongitude()));

            }
            else{
                Log.v("loc","error");
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Log.v("time",timeStamp);

            AmazonS3Client s3 = new AmazonS3Client(credentialsProvider);

            TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
            TransferObserver observer = transferUtility.upload(
                    "khe2015",     /* The bucket to upload to */
                    imageFileName,    /* The key for the uploaded object */
                    photoFile        /* The file where the data to upload exists */
            );
            postUrl=  s3.getUrl("khe2015",imageFileName);
            submitPost(data.getStringExtra("comment"),mLastLocation.getLatitude(),mLastLocation.getLongitude(),timeStamp,postUrl);

        try {
            InputStream response = new URL("http://khe2015-env.elasticbeanstalk.com/testRow.php?").openStream();
        }
        catch (MalformedURLException e) {
            // new URL() failed
            // ...
        }
        catch (IOException e) {
            // openConnection() failed
            // ...
        }

        }
    }
    private void submitPost(String comment, double lat, double lon, String time, java.net.URL img){
        //pass all these these things to the server
    }



    private File createImageFile() throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        newPostPath = "file:" + image.getAbsolutePath();
        String temp = image.getAbsolutePath().substring(storageDir.toString().length()+1,image.getAbsolutePath().length());
        imageFileName=temp;
        return image;
    }

    /*
        Create camera button
     */

   /* final Button cameraButton = (Button) findViewById(R.id.button_id);
    cameraButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            // Perform action on click
        }
    });*/

    /*
        Action bar created
     */
    @Override
    public void onConnectionSuspended(int arg0) {

        // idk it says i have to have this

    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

       // ...
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
        Method for when something on the action bar is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_camera:
                createPost();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
