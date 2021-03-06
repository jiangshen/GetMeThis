package com.example.jiangshen.getmethis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.CheckBox;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static android.provider.MediaStore.Images.Media;

public class GetMeThisMain extends AppCompatActivity {

    //clarifai vars
    private static final String TAG = GetMeThisMain.class.getSimpleName();
    private static final ArrayList<String> exclude = new ArrayList<>(Arrays.asList(new String[]{
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "isolated",
            "horizontal", "vertical",
            "simple",
            "nobody",
            "people",
            "adult",
            "empty",
            "blank",
            "background",
            "no person"})
    );

    //dropdown animation duration
    private static final int DURATION = 300;

    // IMPORTANT NOTE: you should replace these keys with your own App ID and secret.
    // These can be obtained at https://developer.clarifai.com/applications
    private static final String APP_ID = "q4rC90O0Ylhffbq84AJh4xpVHkRAVIxLPXQpSLQ3";
    private static final String APP_SECRET = "qbSCDZ0dnenRQDBAzzhl9fJ4NK7edV3LAtOyhiKb";
    private final ClarifaiClient client = new ClarifaiClient(APP_ID, APP_SECRET);

    public final static String MAP_FOOD = "com.example.jiangshen.feedmethis.MESSAGE";

    private static final int CODE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    ArrayList<String> masterTags = new ArrayList<>();
    ArrayList<Double> masterProb = new ArrayList<>();
    String displayedTag;

    ScrollView scrollView;
    CardView cardView;
    ImageView imageView;
    TextView tagText;

    ImageView expandImg;

    ViewGroup tagDetails;
    LinearLayout checkboxContainer;
    CheckBox checkBox;
    Button buttonMap;

    FloatingActionButton fabImage;
    FloatingActionButton fabCamera;
    TextView confidenceText;

    //backgound text
    TextView instrText;

    //keeps track of scrollView status
    boolean hasScrolledDown;

    @Override
    //init all methods here
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_me_this_main);

        //create menu
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get the screen elements when loaded
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        cardView = (CardView) findViewById(R.id.card_view);     //cardView set invisible from XML
        imageView = (ImageView) findViewById(R.id.image_view);
        expandImg = (ImageView) findViewById(R.id.expand_img);
        instrText = (TextView) findViewById(R.id.instr_text);
        tagText = (TextView) findViewById(R.id.tag_text);
        tagDetails = (ViewGroup) findViewById(R.id.tag_details);
        checkboxContainer = (LinearLayout) findViewById(R.id.checkbox_container);
        confidenceText = (TextView) findViewById(R.id.confidence_text);
        buttonMap = (Button) findViewById(R.id.button_map);     //button set invisible from XML

        //init to be false
        hasScrolledDown = false;

        //Set scrollView to listen to layout changes
        ViewTreeObserver vto = scrollView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                //scroll only if still on top (haven't scroll down yet or went back to top)
                if(!hasScrolledDown) {
                    scrollView.smoothScrollTo(0, scrollView.getBottom());
                }
            }
        });

        //setter and parser of callback functions
        fabImage = (FloatingActionButton) findViewById(R.id.fab_img);
        fabImage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF5722")));
        fabImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get a picture from album
                final Intent intent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CODE_PICK);
            }
        });

        fabCamera = (FloatingActionButton) findViewById(R.id.fab_camera);
        fabCamera.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#70c209")));
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        //button actions
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
        selectedItemsIndexList = new ArrayList<>();

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
            .setPositiveButton("Go to map", new DialogInterface.OnClickListener() {
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
                        //instrText.setText(selectedTokens.substring(0, selectedTokens.length() - 2));
                    }
                }
            })
            .setNegativeButton("Back", new DialogInterface.OnClickListener() {
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
                bitmap = loadBitmapFromUri(intent.getData());
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                //if source from camera
                Bundle extras = intent.getExtras();
                bitmap = (Bitmap) extras.get("data");
            }
            //after all the checks
            //hides card from view before doing processing
            confidenceText.setVisibility(View.INVISIBLE);
            cardView.setVisibility(View.INVISIBLE);
            clarifaiAnalyze(bitmap);
        }
    }

    //Main request to clarifai to analyze image
    private void clarifaiAnalyze(final Bitmap bitmap) {
        final ProgressDialog progress = ProgressDialog.show(GetMeThisMain.this, "", "Analyzing image");
        new Thread()
        {
            public void run()
            {
                try{
                    GetMeThisMain.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.show();
                            //Clarifai send
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                                // Run recognition on a background thread since it makes a network call.
                                new AsyncTask<Bitmap, Void, RecognitionResult>() {
                                    @Override
                                    protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                                        return recognizeBitmap(bitmaps[0]);
                                    }
                                    @Override
                                    protected void onPostExecute(RecognitionResult result) {
                                        updateUIForResult(result);
                                        //method done display Card now
                                        imageView.setImageBitmap(analyzeForDisplay(bitmap));
                                        instrText.setVisibility(View.INVISIBLE);
                                        confidenceText.setVisibility(View.VISIBLE);
                                        cardView.setVisibility(View.VISIBLE);
                                        populateCheckBoxContainer();
                                        progress.dismiss();
                                    }
                                }.execute(bitmap);
                            }
                        }
                    });
                }
                catch(Exception e)
                {
                    Log.e(TAG, "Clarifai error", e);
                }
            }
        }.start();
    }

    private void populateCheckBoxContainer() {
        checkboxContainer.removeAllViews();
        //redraw the parent view
        tagDetails.invalidate();
        int maxList = masterTags.size() < 11 ? masterTags.size() : 10;
        for (int i = 0; i < maxList; i++) {
            checkBox = new CheckBox(this);
            checkBox.setTextSize(18);
            checkBox.setId(i);
            checkBox.setPadding(0,0,0,8);
            checkBox.setText(masterTags.get(i));
            checkboxContainer.addView(checkBox);
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
            Log.e("GetMeThisMain", "Error loading image: " + uri, e);
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

    private static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void sendToMap(View view, String str) {
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
                masterProb.clear();
                // Display the list of tags in the UI.
                for (Tag tag : result.getTags()) {
                    if (tag.getProbability() >= 0.85 && !exclude.contains(tag.getName())) {
                        //add all entries into masterTags & masterProb
                        masterTags.add(tag.getName());
                        masterProb.add(tag.getProbability());
                        //b.append(b.length() > 0 ? ", " : "").append(tag.getName());
                    }
                }
                //add the first tag into displayedTag
                displayedTag = masterTags.get(0);
                confidenceText.setText("Confidence: " + String.format("%2.0f%s", masterProb.get(0) * 100, "%"));
                tagText.setText(firstLetterCap(displayedTag));
            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_me_this_main, menu);
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
            alertDialog.setTitle("About");
            alertDialog.setMessage("Created at UGAHacks 2015\n\nWe hope our app will help you discover a new way of searching. Unleash your creativity, go out and take pictures! And see what surprises you can get :)");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                   dialog.cancel();
                }
            });
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    public String firstLetterCap(String str) {
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    public void toggleDetails(View view) {
        if (tagDetails.getVisibility() == View.GONE) {
            ExpandAndCollapseViewUtil.expand(tagDetails, DURATION);
            expandImg.setImageResource(R.drawable.ic_expand_more_black_48dp);
            rotate(-180.0f);
            hasScrolledDown = false;
        } else {
            ExpandAndCollapseViewUtil.collapse(tagDetails, DURATION);
            expandImg.setImageResource(R.drawable.ic_expand_less_black_48dp);
            rotate(180.0f);
            hasScrolledDown = true;
        }
    }

    private void rotate(float angle) {
        Animation animation = new RotateAnimation(0.0f, angle, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setFillAfter(true);
        animation.setDuration(DURATION);
        expandImg.startAnimation(animation);
    }
}