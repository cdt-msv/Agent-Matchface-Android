package com.example.facematcher;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import at.markushi.ui.CircleButton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static android.view.Gravity.CENTER;

public class MainActivity extends AppCompatActivity {


    private static String TAG = "MainActivity";
    private OkHttpClient okHttpClient = null;
    // Process child thread sent command to show server response text in activity main thread.
    private Handler displayRespTextHandler = null;
    private static final int COMMAND_DISPLAY_SERVER_RESPONSE = 1;
    private static final String KEY_SERVER_RESPONSE_OBJECT = "KEY_SERVER_RESPONSE_OBJECT";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8; cache-control=no-cache");


    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private String currentPhotoPath;
    private Uri currentPhotoURI;

    private ConstraintLayout constraintLayout;
    private TextView titleTextView;
    private TextView matchingScoreTitle;
    private ImageView topImageView;
    private ImageView bottomImageView;
    private CircleButton photoPickerBtn1;
    private CircleButton photoPickerBtn2;
    private CircleButton imageTakenBtn1;
    private CircleButton imageTakenBtn2;
    private CircleButton faceMatchBtn;
    private TextView scoreTextView;
    private AVLoadingIndicatorView loadingIndicator;


    private double CardEyeDistanceMin = 0.015;
    private double CardEyeDistanceMax = 0.2;
    private double EyeDistanceMin = 0.1;
    private double EyeDistanceMax = 1.0;


    private String base64StringTop;
    private String base64StringBottom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        constraintLayout = findViewById(R.id.constraint_layout);
        titleTextView = findViewById(R.id.title_txtview);
        topImageView = findViewById(R.id.top_imageView);
        bottomImageView = findViewById(R.id.bottom_imageView);
        photoPickerBtn1 = findViewById(R.id.photoBrowserBtn1);
        photoPickerBtn2 = findViewById(R.id.photoBrowserBtn2);
        imageTakenBtn1 = findViewById(R.id.imageCaptureBtn1);
        imageTakenBtn2 = findViewById(R.id.imageCaptureBtn2);
        faceMatchBtn = findViewById(R.id.facematchBtn);
        loadingIndicator = findViewById(R.id.loadingIndicatorView);
        matchingScoreTitle = findViewById(R.id.matching_scoretitle_txtview);
        scoreTextView = findViewById(R.id.score_result);

        setTypeface();
        checkRuntimePermission();
        ShowSampleImageGIF();
        setAction();
        initOkHttp3();


    }


    private void checkRuntimePermission() {
        //check realtime permission if run higher API 23
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
            return;
        }
    }

    private void setTypeface() {
        Typeface regularFace = Typeface.createFromAsset(getAssets(),
                "PrintAble4U_Regular.ttf");
        Typeface boldFace = Typeface.createFromAsset(getAssets(),
                "PrintAble4U_Bold.ttf");
        Typeface droidSanBold = Typeface.createFromAsset(getAssets(),
                "DroidSans-Bold.ttf");
        Typeface latoFace = Typeface.createFromAsset(getAssets(),
                "Lato-Bold.ttf");
        titleTextView.setTypeface(latoFace);
        titleTextView.setText(getResources().getString(R.string.titleText));
        titleTextView.setTextColor(Color.parseColor("#ffffff"));
        titleTextView.setGravity(CENTER);

        matchingScoreTitle.setTypeface(boldFace);
        matchingScoreTitle.setText(getResources().getString(R.string.matchingTitle));
        matchingScoreTitle.setTextColor(Color.parseColor("#ffffff"));
        matchingScoreTitle.setGravity(CENTER);

        scoreTextView.setTypeface(boldFace);
        scoreTextView.setText("0");
        scoreTextView.setTextColor(Color.parseColor("#ffffff"));
        scoreTextView.setGravity(CENTER);
    }

    private void setAction() {
        stopAnim();

        photoPickerBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentToGallery(1);
            }
        });

        photoPickerBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentToGallery(2);
            }
        });

        imageTakenBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent(3);
            }
        });

        imageTakenBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent(4);
            }
        });

        faceMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(base64StringTop != null && base64StringBottom != null ){
                    new AsyncTaskForBase64JSONBuilder().execute();
                    startAnim();
                    scoreTextView.setVisibility(View.INVISIBLE);
                }

            }
        });


        constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // custom dialog
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.parameters);

                Window window = dialog.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();

                wlp.gravity = Gravity.RIGHT;
                wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                window.setAttributes(wlp);


                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                final EditText CardEyeMinEditText = dialog.findViewById(R.id.cardeyemin_editText);
                final EditText CardEyeMaxEditText = dialog.findViewById(R.id.cardeyemax_editText);
                final EditText EyeMinEditText = dialog.findViewById(R.id.eyemin_editText);
                final EditText EyeMaxEditText = dialog.findViewById(R.id.eyemax_editText);

                CardEyeMaxEditText.setText(String.valueOf(CardEyeDistanceMax));
                CardEyeMinEditText.setText(String.valueOf(CardEyeDistanceMin));

                EyeMaxEditText.setText(String.valueOf(EyeDistanceMax));
                EyeMinEditText.setText(String.valueOf(EyeDistanceMin));
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        CardEyeDistanceMax = Double.parseDouble(CardEyeMaxEditText.getText().toString());
                        CardEyeDistanceMin = Double.parseDouble(CardEyeMinEditText.getText().toString());
                        EyeDistanceMax = Double.parseDouble(EyeMaxEditText.getText().toString());
                        EyeDistanceMin = Double.parseDouble(EyeMinEditText.getText().toString());


                        dialog.dismiss();
                        Snackbar snackbar = Snackbar
                                .make(constraintLayout, "บันทึกค่าพารามิเตอร์เรียบร้อย", Snackbar.LENGTH_SHORT);

                        snackbar.show();
                        //Toast.makeText(getApplicationContext(),"Dismissed..!!",Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
                return true;
            }
        });
    }


    private void ShowSampleImageGIF() {
        //Sample GIF image
        Glide
                .with(this)
                //.load("https://i.pinimg.com/originals/41/bd/26/41bd261336dbfd7d8f1936fcef0a40e3.gif")
                //.load("https://media2.giphy.com/media/HWe1Ug6iNc7Je/giphy.gif")
                .load("https://cdn.dribbble.com/users/1338391/screenshots/5676875/dribbble.gif")
                .centerCrop()
                .placeholder(R.drawable.progress_animation)
                .into(topImageView);

        //Sample GIF image
        Glide
                .with(this)
                //.load("https://cdn.dribbble.com/users/55063/screenshots/1491712/running-cycle.gif")
                .load("https://i.pinimg.com/originals/75/05/b5/7505b59ea4bad35a93b2f57c935969e8.gif")
                .centerCrop()
                .placeholder(R.drawable.progress_animation)
                .into(bottomImageView);

    }


    void startAnim() {
        loadingIndicator.show();
        // or avi.smoothToShow();
    }

    void stopAnim() {
        loadingIndicator.hide();
        // or avi.smoothToHide();
    }


    private void IntentToGallery(int RequestID) {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RequestID);
    }

    private void dispatchTakePictureIntent(int RequestID) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //startActivityForResult(takePictureIntent, RequestID);
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                currentPhotoURI = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, RequestID);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }


    /***
     *
     * Decode a scaled image
     *
     * Managing multiple full-sized images can be tricky with limited memory.
     * If you find your application running out of memory after displaying just a few images,
     * you can dramatically reduce the amount of dynamic heap used by expanding the JPEG into
     * a memory array that's already scaled to match the size of the destination view.
     * The following example method demonstrates this technique.
     *
     */
    /*
    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, boptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, boptions);
        imageView.setImageBitmap(bitmap);
    }*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));

                Glide
                        .with(this)
                        .load(bitmap)
                        .centerCrop()
                        .placeholder(R.drawable.progress_animation)
                        .into(topImageView);

                String path = getPathFromUri(this, uri);
                Log.d(TAG, path);
                new AsyncBase64WorkerTask(path, "face1").execute();


            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));

                Glide
                        .with(this)
                        .load(bitmap)
                        .centerCrop()
                        .placeholder(R.drawable.progress_animation)
                        .into(bottomImageView);

                String path = getPathFromUri(this, uri);
                Log.d(TAG, path);
                new AsyncBase64WorkerTask(path, "face2").execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 3 && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            Glide
                    .with(this)
                    .load(currentPhotoURI)
                    .centerCrop()
                    .placeholder(R.drawable.progress_animation)
                    .into(topImageView);
            galleryAddPic();
            new AsyncBase64WorkerTask(currentPhotoPath, "face1").execute();
        } else if (requestCode == 4 && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            Glide
                    .with(this)
                    .load(currentPhotoURI)
                    .centerCrop()
                    .placeholder(R.drawable.progress_animation)
                    .into(bottomImageView);
            galleryAddPic();
            new AsyncBase64WorkerTask(currentPhotoPath, "face2").execute();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "You can't use camera without permission",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    /*class AsyncTaskForImageFilePath extends AsyncTask<Integer, Void, String> {

        private final File file;

        // Constructor
        public AsyncTaskForImageFilePath(File file) {
            this.file = file;
        }

        // Compress and Decode image in background.
        @Override
        protected String doInBackground(Integer... params) {

            filePath = file.getPath();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            //After image is converted to base64
            //Log.d(TAG, "onPost execute: " + string);
            //if smart card capture mode is on, save base64 string to 'cardPicture_base64' variable


            new AsyncBase64WorkerTask(fivePicture_filePath[0], "face0").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[1], "face1").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[2], "face2").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[3], "face3").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[4], "face4").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[5], "face5").execute();
            new AsyncBase64WorkerTask(fivePicture_filePath[6], "face6").execute();
        }

    }*/





    public static String getPathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }




























    //Asynctask for base64 converter
    class AsyncBase64WorkerTask extends AsyncTask<Integer, Void, String> {
        private final String filePath;
        private String imageTag;

        // Constructor
        public AsyncBase64WorkerTask(String filePath, String imageTag) {
            this.filePath = filePath;
            this.imageTag = imageTag;
        }

        // Compress and Decode image in background.
        @Override
        protected String doInBackground(Integer... params) {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            //bitmap = toGrayscale(bitmap);
            //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            bitmap = toGrayscale(bitmap);
            //saveImageJpeg(bitmap);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] imageBytes = stream.toByteArray();
            String image_str = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            //.String image_str = "";
            return image_str;
        }

        // This method is run on the UI thread
        @Override
        protected void onPostExecute(String string) {
            //After image is converted to base64
            //Log.d(TAG, "onPost execute: " + string);

            // 4 pics + 1 straight
            if (imageTag.equals("face1")) {
                base64StringTop = string;
            } else if (imageTag.equals("face2")) {
                base64StringBottom = string;
            }

        }
    }


    class AsyncTaskForBase64JSONBuilder extends AsyncTask<Integer, Void, String> {

        private final String url = "http://171.100.69.126:8800/matchonetonoe"; //Liveness API path
        private String responseMsg = null;
        private Call call;

        // Constructor
        public AsyncTaskForBase64JSONBuilder() {

        }

        @Override
        protected String doInBackground(Integer... integers) {

            try {
                // Create okhttp3.Call object with post http request method.
                call = createHttpPostMethodCall(url);
                // Execute the request and get the response asynchronously.
                //call.execute();
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        sendChildThreadMessageToMainThread("Asynchronous http post request failed.");
                        Log.d(TAG, "Response error: " + e);
                        if (e.toString().contains("java.net.SocketTimeoutException")) {
                            //Toast.makeText(getApplicationContext(), "ส่งคำร้องขอไม่สำเร็จ", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, Response response) throws IOException {

                        if (response.isSuccessful()) {
                            Log.d(TAG, "Response success: " + String.valueOf(response));
                            responseMsg = String.valueOf(response);
                            // Parse and get server response text data.
                            String respData = parseResponseText(response);

                            // Notify activity main thread to update UI display text with Handler.
                            sendChildThreadMessageToMainThread(respData);
                        } else {
                            Log.d(TAG, "Response not success: " + String.valueOf(response));
                        }
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                sendChildThreadMessageToMainThread(ex.getMessage());
                stopAnim();
                scoreTextView.setVisibility(View.VISIBLE);
            }
            return responseMsg;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Log.d(TAG, s);
            }

        }
    }

    private void initOkHttp3() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient();

            //set timeout for 30 second for server response
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(60, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            builder.writeTimeout(60, TimeUnit.SECONDS);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            okHttpClient = builder.addNetworkInterceptor(logging).build();


        }
        if (displayRespTextHandler == null) {
            displayRespTextHandler = new Handler() {
                // When this handler receive message from child thread.
                @Override
                public void handleMessage(Message msg) {

                    // Check what this message want to do.
                    if (msg.what == COMMAND_DISPLAY_SERVER_RESPONSE) {
                        // Get server response text.
                        Bundle bundle = msg.getData();
                        String respText = bundle.getString(KEY_SERVER_RESPONSE_OBJECT);

                        Log.d(TAG, "Response handleMessage: " + respText);
                        stopAnim();
                        scoreTextView.setVisibility(View.VISIBLE);
                        try {
                            JSONObject json = new JSONObject(respText);
                            int errorcode = json.getInt("ErrorCode");
                            //errorCodeTextView.setText("ErrorCode: " + String.valueOf(errorcode));
                            if (errorcode == 0) {
                                //realResult.setVisibility(View.VISIBLE);
                                //realResultTitle.setVisibility(View.VISIBLE);
                                //statusBar.setVisibility(View.INVISIBLE);
                                //statusBar.setText("ผลการตรวจสอบ:");
                                //realResult.setText("ผ่าน");
                                //realResult.setTextColor(getResources().getColor(R.color.greenGoogle));
                            } else if (errorcode == 1) {
                                //realResult.setVisibility(View.VISIBLE);
                                //realResultTitle.setVisibility(View.VISIBLE);
                                //statusBar.setVisibility(View.INVISIBLE);
                                //statusBar.setText("ผลการตรวจสอบ:");
                                //realResult.setText("ไม่ผ่าน");
                                //realResult.setTextColor(getResources().getColor(R.color.redGoogle));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        try {
                            JSONObject json = new JSONObject(respText);
                            //Passed
                            String message = json.getString("Msg");
                            if (message.equals("liveness")) {
                                //MsgTextView.setText("");
                            } else {
                                //MsgTextView.setText(message);
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject json = new JSONObject(respText);
                            String liveness = json.getString("face_score1");
                            //ScoreTextView.setText(
                            //        String.format("%.4f", Double.parseDouble(liveness)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject json = new JSONObject(respText);
                            String liveness = json.getString("face_score2");
                            //ScoreTextView.setText(
                            //        String.format("%.4f", Double.parseDouble(liveness)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            JSONObject json = new JSONObject(respText);
                            double liveness = json.getDouble("score");
                            NumberFormat formatter = new DecimalFormat("##.####");
                            formatter.setRoundingMode(RoundingMode.FLOOR);
                            String resultNumber = formatter.format(liveness);
                            scoreTextView.setVisibility(View.VISIBLE);
                            scoreTextView.setText(resultNumber);
                            //ScoreTextView.setText(resultNumber);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
    }

    /* Create OkHttp3 Call object use post method with url. */
    private okhttp3.Call createHttpPostMethodCall(String url) {
        // Create okhttp3 form body builder.

        JSONObject jsonObject = new JSONObject();
        JSONObject jsonEyeDistance = new JSONObject();
        //JSONObject jsonCardEyeDistance = new JSONObject();

        // create POST request in JSON format
        //1. Eyedistance 'max','min'
        try {
            jsonEyeDistance.put("max", EyeDistanceMax);
            jsonEyeDistance.put("min", EyeDistanceMin);
            jsonObject.put("eyeDistance", jsonEyeDistance);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //2. CardEyeDistance 'max', 'min'
        /*try {
            jsonCardEyeDistance.put("max", CardEyeDistanceMax);
            jsonCardEyeDistance.put("min", CardEyeDistanceMin);
            jsonObject.put("CardeyeDistance", jsonCardEyeDistance);
        }catch (JSONException e) {
            e.printStackTrace();
        }*/


        try {
            jsonObject.put("image1", base64StringTop);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            jsonObject.put("image2", base64StringBottom);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        Log.d(TAG, "JSON body: " + jsonObject.toString());
        // Create a http request object.

        Request.Builder builder = new Request.Builder();
        builder = builder.url(url);
        builder = builder.post(body);
        Request request = builder.build();

        Log.d(TAG, "Post request: " + request.toString());

        // Create a new Call object with post method.
        okhttp3.Call call = okHttpClient.newCall(request);

        return call;
    }

    /* Parse response code, message, headers and body string from server response object. */
    private String parseResponseText(Response response) {
        // Get response code.
        int respCode = response.code();

        // Get message
        String respMsg = response.message();

        // Get headers.
        List<String> headerStringList = new ArrayList<String>();

        Headers headers = response.headers();
        Map<String, List<String>> headerMap = headers.toMultimap();
        Set<String> keySet = headerMap.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String headerKey = it.next();
            List<String> headerValueList = headerMap.get(headerKey);

            StringBuffer headerBuf = new StringBuffer();
            headerBuf.append(headerKey);
            headerBuf.append(" = ");

            for (String headerValue : headerValueList) {
                headerBuf.append(headerValue);
                headerBuf.append(" , ");
            }

            headerStringList.add(headerBuf.toString());
        }

        // Get body text.
        String respBody = "";
        try {
            respBody = response.body().string();
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        String returner = respBody;
        return returner;
    }

    // Send message from child thread to activity main thread.
    // Because can not modify UI controls in child thread directly.
    private void sendChildThreadMessageToMainThread(String respData) {
        // Create a Message object.
        Message message = new Message();

        // Set message type.
        message.what = COMMAND_DISPLAY_SERVER_RESPONSE;

        // Set server response text data.
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SERVER_RESPONSE_OBJECT, respData);
        message.setData(bundle);

        // Send message to activity Handler.
        displayRespTextHandler.sendMessage(message);
    }
}
