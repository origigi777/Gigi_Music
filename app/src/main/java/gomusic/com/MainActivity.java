package gomusic.com;
/**
 * @author Ori Gigi
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private boolean checkPermission = false;
    Uri uri;
    String songName, songUrl;
    ListView listView;
    String[] items;
    ArrayList<String> songNameList = new ArrayList<>();
    ArrayList<String> songUrlList = new ArrayList<>();
    ArrayAdapter<String> Aadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.NoSongs);

        SongDisplay();


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.c_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_upload) {

            if (validatePermission()) {
                pickSong();

            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void pickSong() {
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload, 1);

    }
    //checks for no uploading issues and display the mp3 uploaded
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                uri = data.getData();
                Cursor mcursor = getApplicationContext().getContentResolver()
                        .query(uri, null, null, null, null);

                int indexName = mcursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                mcursor.moveToFirst();
                songName = mcursor.getString(indexName);
                mcursor.close();
                uploadSongToDataBase();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //while user select a song the function uploads the song into the DataBase
    private void uploadSongToDataBase() {
        StorageReference StorageReference = FirebaseStorage.getInstance().getReference()
                .child("Songs").child(uri.getLastPathSegment());

        final ProgressDialog ProDialog = new ProgressDialog(this);
        ProDialog.show();


        StorageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isComplete()) ;
            Uri urlSong = uriTask.getResult();
            songUrl = urlSong.toString();
            uploadDetailsToDataBase();
            ProDialog.dismiss();
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                ProDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progres = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                int currentProgress = (int) progres;
                ProDialog.setMessage("Uploading: " + currentProgress + "%");



            }
        });
    }

    //saves the url and name for organize inside the FireBase
    private void uploadDetailsToDataBase() {
        Song songObj = new Song(songName, songUrl);
        FirebaseDatabase.getInstance().getReference("Songs")
                .push().setValue(songObj).addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Song Uploaded", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    //ask for user permission before access to the device storage
    private boolean validatePermission() {

        Dexter.withContext(MainActivity.this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        checkPermission = true;

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        checkPermission = false;
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
        return checkPermission;
    }


    //find song from external storage function
    public ArrayList<File> SongFinder(@NonNull File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        //for "is mp3 file?"
        //assert files != null;
        for (File singleFile : files) {
            //if file have directory?
            if (singleFile.isDirectory() && !singleFile.isHidden()) {
                //error file list
                arrayList.addAll(SongFinder(singleFile));
            } else {
                if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav")) {
                    arrayList.add(singleFile);
                }
            }
        }
        return arrayList;
    }
    //finds the music file in the device and organize them in list of songs
    void SongDisplay() {

        final ArrayList<File> mySongs = SongFinder(Environment.getExternalStorageDirectory());
        items = new String[mySongs.size()];
        for (int i = 0; i < mySongs.size(); i++) {
            items[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");

        }

        customAdapter customAdapter = new customAdapter();
        listView.setAdapter(customAdapter);
        //the activity while the user tap on a song from the list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String songName = (String) listView.getItemAtPosition(i);
                startActivity(new Intent(getApplicationContext(),PlayerActivity.class)
                .putExtra("songs",mySongs).putExtra("songname",songName)
                .putExtra("pos",i));
            }
        });
    }
    //display the song list on the application's board uses in song display function
    class customAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View myView = getLayoutInflater().inflate(R.layout.list_item,null);
            TextView textSong =myView.findViewById(R.id.txtsongname);
            textSong.setSelected(true);
            textSong.setText(items[i]);
            return myView;
        }
    }

}