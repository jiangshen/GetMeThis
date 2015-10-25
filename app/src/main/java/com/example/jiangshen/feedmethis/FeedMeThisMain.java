package com.example.jiangshen.feedmethis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static android.provider.MediaStore.Images.Media;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;

public class FeedMeThisMain extends AppCompatActivity {

    //clarifai vars
    private static final String TAG = FeedMeThisMain.class.getSimpleName();
    private static final ArrayList<String> exclude = new ArrayList<String>(Arrays.asList(new String[]{
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "nobody",
            "people",
            "adult",
            "empty",
            "blank",
            "background"})
    );

    private ArrayList<String> masterTags = new ArrayList<>();

    // IMPORTANT NOTE: you should replace these keys with your own App ID and secret.
    // These can be obtained at https://developer.clarifai.com/applications
    private static final String APP_ID = "vM05qo55uhZard2dL4BixmMm4WsHIl6CsGCTgS_7";
    private static final String APP_SECRET = "rx4oPPiXiCWNRVcoJ0huLz02cKiQUZtq5JPVrhjM";
    private final ClarifaiClient client = new ClarifaiClient(APP_ID, APP_SECRET);

    public final static String MAP_FOOD = "com.example.jiangshen.feedmethis.MESSAGE";

    private static final int CODE_PICK = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    ImageView imageView;
    TextView titleText;
    Button buttonMap;

    FloatingActionButton fabImage;
    FloatingActionButton fabCamera;

    @Override
    //init all methods here
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_me_this_main);

        //create menu
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get the screen elements when loaded
        imageView = (ImageView) findViewById(R.id.image_view);
        titleText = (TextView) findViewById(R.id.title_text);
        buttonMap = (Button) findViewById(R.id.button_map);     //button set invisible from XML

        //setter and parser of callback functions

        fabImage = (FloatingActionButton) findViewById(R.id.fab_img);
        fabImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get a picture from album
                final Intent intent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CODE_PICK);
            }
        });

        fabCamera = (FloatingActionButton) findViewById(R.id.fab_camera);
        fabCamera.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1DE9B6")));
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCheckBoxDialog(masterTags.toArray(new CharSequence[masterTags.size()]));
            }
        });
    }

    private void showCheckBoxDialog(final CharSequence[] tagData) {
        // where we will store or remove selected items
        final ArrayList<Integer> selectedItemsIndexList;
        selectedItemsIndexList = new ArrayList<Integer>();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you looking for?")

            .setMultiChoiceItems(tagData, null, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked) {
                        // if the user checked the item, add it to the selected items
                        selectedItemsIndexList.add(which);
                    } else if (selectedItemsIndexList.contains(which)) {
                        // else if the item is already in the array, remove it
                        selectedItemsIndexList.remove(Integer.valueOf(which));
                    }
                    // you can also add other codes here,
                    // for example a tool tip that gives user an idea of what he is selecting
                    // showToast("Just an example description.");
                }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // user clicked OK, so save the mSelectedItems results somewhere
                    // here we are trying to retrieve the selected items indices
                    String selectedTokens = "";
                    if (!selectedItemsIndexList.isEmpty()) {
                        for (Integer i : selectedItemsIndexList) {
                            selectedTokens += tagData[i] + ", ";
                        }
                        //send the final info to maps
                        sendToMap(((Dialog) dialog).getCurrentFocus(), selectedTokens.substring(0, selectedTokens.length() - 2));
                        //titleText.setText(selectedTokens.substring(0, selectedTokens.length() - 2));
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // removes the AlertDialog in the screen
                }
            })
            .show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Bitmap bitmap = null;

        if (resultCode == RESULT_OK) {
            //if source from media
            if (requestCode == CODE_PICK) {
                // The user picked an image.
                Log.d("FeedMeThisMain", "User picked image: " + intent.getData());
                bitmap = loadBitmapFromUri(intent.getData());
                if (bitmap != null) {
                    imageView.setImageBitmap(analyzeForDisplay(bitmap));
                    buttonMap.setVisibility(View.VISIBLE);
                } else {
                    titleText.setText("Unable to load, try again!");
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                //if source from camera
                Bundle extras = intent.getExtras();
                bitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(analyzeForDisplay(bitmap));

                buttonMap.setVisibility(View.VISIBLE);
            }
            clarifaiAnalyze(bitmap);
        }
    }

    private void clarifaiAnalyze(Bitmap bitmap) {
        //clarifai send
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            titleText.setText("Analyzing visuals...");
            buttonMap.setEnabled(false);

            // Run recognition on a background thread since it makes a network call.
            new AsyncTask<Bitmap, Void, RecognitionResult>() {
                @Override protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                    return recognizeBitmap(bitmaps[0]);
                }
                @Override protected void onPostExecute(RecognitionResult result) {
                    updateUIForResult(result);
                }
            }.execute(bitmap);
        } else {
            titleText.setText("Unable to load selected image.");
        }
    }

    /** Loads a Bitmap from a content URI returned by the media picker. */
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            // The image may be large. Load an image that is sized for display. This follows best
            // practices from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
            int sampleSize = 1;
            while (opts.outWidth / (2 * sampleSize) >= imageView.getWidth() &&
                    opts.outHeight / (2 * sampleSize) >= imageView.getHeight()) {
                sampleSize *= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
        } catch (IOException e) {
            Log.e("FeedMeThisMain", "Error loading image: " + uri, e);
        }
        return null;
    }

    //if image is too large rotate the image back
    private Bitmap analyzeForDisplay(Bitmap bmp) {
        double ratio = (double) bmp.getHeight() / bmp.getWidth();
        if (ratio < 0.6) {
            return RotateBitmap(bmp, 90);
        }
        return bmp;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public void sendToMap(View view, String str) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra(MAP_FOOD, str);
        startActivity(intent);
    }

    /** Sends the given bitmap to Clarifai for recognition and returns the result. */
    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            // Scale down the image. This step is optional. However, sending large images over the
            // network is slow and  does not significantly improve recognition performance.
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                    320 * bitmap.getHeight() / bitmap.getWidth(), true);

            // Compress the image as a JPEG.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            // Send the JPEG to Clarifai and return the result.
            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }

    /** Updates the UI by displaying tags for the given result. */
    private void updateUIForResult(RecognitionResult result) {
        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                masterTags.clear();
                // Display the list of tags in the UI.
                //StringBuilder b = new StringBuilder();
                for (Tag tag : result.getTags()) {
                    //b.append(b.length() > 0 ? ", " : "").append(tag.getName() + "." + String.format("%.2f", tag.getProbability()));
                    if (tag.getProbability() >= 0.85 && !exclude.contains(tag.getName())) {
                        //add all entries into masterTags
                        masterTags.add(tag.getName());
                        //b.append(b.length() > 0 ? ", " : "").append(tag.getName());
                    }
                }
                titleText.setText("Analysis Complete!");
            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
                titleText.setText("Sorry, there was an error recognizing your image.");
            }
        } else {
            titleText.setText("Sorry, there was an error recognizing your image.");
        }
        buttonMap.setEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feed_me_this_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("About Get Me This");
            alertDialog.setMessage(String.format("Created at UGAHacks 2015\n\nWe hope our app will help you discover a new way of searching. Unleash your creativity, go out and take pictures! And see what surprises you can get :)"));
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                   dialog.cancel();
                }
            });
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }
}