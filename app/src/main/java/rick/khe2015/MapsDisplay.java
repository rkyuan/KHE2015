package rick.khe2015;

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

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MapsDisplay extends AppCompatActivity {


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private int numDisplay;
    private Post[] posts;
    private final int POSTSIZE = 25;
    private Handler h;
    String newPostPath;
    private boolean updateloop = false;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private String imageFileName;
    private File photoFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_display);
        setUpMapIfNeeded();
        posts = new Post[POSTSIZE];
        numDisplay = 5;
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:73f3c20b-bd6b-4e56-ac8b-f315813a39cd", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        h = new Handler();
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
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void updateLocation(){
        CameraPosition pos = mMap.getCameraPosition();
        mMap.clear();
        //pass pos to server
        //update posts from server
        for (int i = 0; i <numDisplay; i++ ){
            if(posts[i]==null){
                break;
            }
            mMap.addMarker(new MarkerOptions());
        }
    }

    private void createPost(){
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
            AmazonS3Client s3 = new AmazonS3Client(credentialsProvider);

            TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
            TransferObserver observer = transferUtility.upload(
                    "khe2015",     /* The bucket to upload to */
                    imageFileName,    /* The key for the uploaded object */
                    photoFile        /* The file where the data to upload exists */
            );
            java.net.URL postUrl =  s3.getUrl("khe2015",imageFileName);

        }
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
