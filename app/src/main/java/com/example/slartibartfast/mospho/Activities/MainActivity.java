package com.example.slartibartfast.mospho.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.slartibartfast.mospho.Adapters.ImageAdapter;
import com.example.slartibartfast.mospho.ApplicationConstants;
import com.example.slartibartfast.mospho.R;
import com.example.slartibartfast.mospho.Utilities.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/*
MosPho: Display a simple interface at launch.  One button that the user uses to select an image from the
Android Gallery.  Create a mosaic based on the chosen image and save it.
Logic is simple overall
1.) FETCH IMAGE FROM WHEREVER THE USER WANTS TO
2.) SPLIT IMAGE INTO SMALLER PIECES
3.) RUN EACH ROW THROUGH THE NETWORK TO FIND THE EQUIVALENT IMAGE
4.) STITCH THE IMAGE BACK ROW BY ROW
5.) DISPLAY MOSAIC AND VOILA, WE'RE DONE.

Not using Palette class, since that would be cheating :)
*/

public class MainActivity extends AppCompatActivity {
    public final String TAG = this.getLocalClassName();
    private static int RESULT_LOAD_IMAGE = 1;
    Context mContext = this;
    ArrayList<Bitmap> smallImages;
    ArrayList<Bitmap> newSmallImagesList;
    //Global static for number of chunks to break the images.
    //Calculated as width * height
    // For 32X32 = 1024.
    int numberOfBlocks = 1024;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Good ol' OnCreate
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        //Simple button to select an image from the device
        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
    }

    /**
     * First Operation:
     * Open the Android gallery and let the user choose an image. A mosaic will be
     * created based on the chosen image, saved and inserted to the gallery.
     * The mosaic will also displayed on the screen.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            // Create a mutable Bitmap
            Bitmap originalImage = BitmapFactory.decodeFile(picturePath);
            //Keep a copy
            Bitmap originalMutable = originalImage.copy(originalImage.getConfig(), true);

            //Split the image into chunks
            new splitImageIntoChunksAsync(originalImage, numberOfBlocks).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            new fetchAverageColourPerChunkAsync().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            new stitchMosaicImageTogetherAsync().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        }
    }

    /**
     * Single Responsibility: Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    /**
     * Single Responsibility: Print to the grid.
     *
     * @param imgList
     */
    public void displayImageOnTheGrid(ArrayList<Bitmap> imgList) {
        //Getting the grid view and setting an adapter to it
        GridView grid = (GridView) findViewById(R.id.gridview);
        grid.setAdapter(new ImageAdapter(this, imgList));
        grid.setNumColumns((int) Math.sqrt(imgList.size()));
        smallImages = imgList;
    }


    /**
     * Single Responsibility: Print to the imageView.
     */
    public void displayImageOnTheImageView(Bitmap bmp, int resourceId) {
        ImageView imgView = (ImageView) findViewById(resourceId);
        imgView.setImageBitmap(bmp);
    }

    class fetchAverageColourPerChunkAsync extends AsyncTask<Void, Void, Void> {
        boolean isThereNetworkMate = true;
        Bitmap image;

        protected Void doInBackground(Void... arg0) {


            newSmallImagesList = new ArrayList<>();
            //If there's no internet, just default to the mosaic tile from the source image for good UX.
            if (!Utils.isConnected(mContext)) {
                isThereNetworkMate = false;
                for (Bitmap chunk : smallImages) {
                    String chunkAverage = Utils.getAverageDominantColourFromBitmap(chunk);
                    image = Bitmap.createBitmap(chunk.getWidth(), chunk.getHeight(), Bitmap.Config.ARGB_8888);
                    image.eraseColor(Integer.decode(chunkAverage));
                    newSmallImagesList.add(image);
                }
            }
            //Internet -- YESS!
            // Can use Picasso/Fresco, but again, that would be cheating. :)
            // Cheating a little with Volley, but Volley is almost a part of Android now, not like I am using Retrofit :P
            else {
                for (Bitmap chunk : smallImages) {
                    String chunkAverage = Utils.getAverageDominantColourFromBitmap(chunk);
                    // Instantiate the RequestQueue.
                    String buildUglyURL = ApplicationConstants.URL_OF_MOSAIC_SERVER + chunk.getWidth() + "/" + chunk.getHeight() + "/" + chunkAverage;
                    RequestQueue queue = Volley.newRequestQueue(mContext);
                    ImageRequest imageRequest = new ImageRequest(buildUglyURL, new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            // Assign the response to an ImageView
                            image = response;
                        }
                    }, chunk.getWidth(), chunk.getHeight(), ImageView.ScaleType.FIT_CENTER, Bitmap.Config.ARGB_8888, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, error.toString());
                        }
                    });
                    //add request to queue
                    queue.add(imageRequest);
                    newSmallImagesList.add(image);
                }

            }
            return null;
        }

        protected void onPostExecute(Void bitmap) {
            if (!isThereNetworkMate)
                Toast.makeText(mContext, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
        }
    }


    class stitchMosaicImageTogetherAsync extends AsyncTask<Bitmap, Bitmap, Bitmap> {
        Bitmap bitmap;

        protected Bitmap doInBackground(Bitmap... arg0) {

            try {
                //Get the width and height of the smaller chunks
                int chunkWidth = smallImages.get(0).getWidth();
                int chunkHeight = smallImages.get(0).getHeight();
                int widthBlock = (int) Math.sqrt(numberOfBlocks);
                int heightBlock = (int) Math.sqrt(numberOfBlocks);
                //create a bitmap of a size which can hold the complete image after merging
                bitmap = Bitmap.createBitmap(chunkWidth * widthBlock, chunkHeight * heightBlock, Bitmap.Config.ARGB_4444);

                //create a canvas for drawing all those small images
                Canvas canva = new Canvas(bitmap); //pun intended
                int count = 0;
                for (int rows = 0; rows < widthBlock; rows++) {
                    for (int cols = 0; cols < heightBlock; cols++) {
                        canva.drawBitmap(smallImages.get(count), chunkWidth * cols, chunkHeight * rows, null);
                        count++;
                    }
                }

            } catch (Exception ex) {
                //It will probably throw OOM for large chunks. Do nothing for now, ideally run it through crashlytics in production.
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            displayImageOnTheImageView(bitmap, R.id.imageView);
        }
    }

    class splitImageIntoChunksAsync extends AsyncTask<Bitmap, ArrayList<Bitmap>, ArrayList<Bitmap>> {
        Bitmap bitmap;
        int chunkNumbers;

        public splitImageIntoChunksAsync(Bitmap bmp, int chunksAsync) {
            super();
            bitmap = bmp;
            chunkNumbers = chunksAsync;
        }

        protected ArrayList<Bitmap> doInBackground(Bitmap... arg0) {
            //For the number of rows and columns of the grid to be displayed
            int rows, cols;

            //For height and width of the small image chunks
            int chunkHeight, chunkWidth;

            //To store all the small image chunks in bitmap format in this list
            ArrayList<Bitmap> chunkedImages = new ArrayList<Bitmap>(chunkNumbers);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

            rows = cols = (int) Math.sqrt(chunkNumbers);
            chunkHeight = bitmap.getHeight() / rows;
            chunkWidth = bitmap.getWidth() / cols;

            //x and y are the pixel positions of the image chunks
            int yCoord = 0;
            for (int x = 0; x < rows; x++) {
                int xCoord = 0;
                for (int y = 0; y < cols; y++) {
                    chunkedImages.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight));
                    xCoord += chunkWidth;
                }
                yCoord += chunkHeight;
            }
            smallImages = chunkedImages;
            return chunkedImages;

        }


        protected void onPostExecute(ArrayList<Bitmap> chunkedImages) {
            displayImageOnTheGrid(chunkedImages);
        }
    }


}
