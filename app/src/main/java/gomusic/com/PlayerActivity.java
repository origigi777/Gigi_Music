package gomusic.com;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    Button btnplay,btnnext,btnprev,btnff,btnfr;
    TextView txtsname,txtsstart,txtsstop;
    SeekBar seekmusic;
    String sname;

    public static final String EXTRA_NAME = "song_name";
    static MediaPlayer mediaPlayer;
    int position;
    ArrayList<File> mySongs;
    Thread updateSeekbar;

    //all buttons setup from player activity xml
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        btnprev= findViewById(R.id.prevbutton);
        btnnext= findViewById(R.id.nextbutton);
        btnplay= findViewById(R.id.playbutton);
        btnff= findViewById(R.id.fastforwardbutton);
        btnfr= findViewById(R.id.fastrewindbutton);
        txtsname= findViewById(R.id.txtsn);
        txtsstart= findViewById(R.id.txtsstart);
        txtsstop = findViewById(R.id.txtsstop);
        seekmusic=findViewById(R.id.seekbar);
        //while user presses a button
        if(mediaPlayer !=null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        Intent i= getIntent();
        Bundle bundle = i.getExtras();
        mySongs=(ArrayList) bundle.getParcelableArrayList("songs");
        String songName= i.getStringExtra("songmame");
        position =bundle.getInt("pos",0);
        txtsname.setSelected(true);
        Uri uri = Uri.parse(mySongs.get(position).toString());
        sname = mySongs.get(position).getName();
        txtsname.setText(sname);
        mediaPlayer= MediaPlayer.create(getApplicationContext(),uri);
        mediaPlayer.start();

        updateSeekbar=new Thread(){

            @Override
            public void run() {
                int totalDuration = mediaPlayer.getDuration();
                int currentPosion=0;

                while (currentPosion<totalDuration){
                    try {
                        sleep(500);
                        currentPosion=mediaPlayer.getCurrentPosition();
                        seekmusic.setProgress(currentPosion);
                    }
                    catch (InterruptedException | IllegalStateException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        //seekbar setup
        seekmusic.setMax(mediaPlayer.getDuration());
        updateSeekbar.start();
        seekmusic.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple_500), PorterDuff.Mode.MULTIPLY);
        seekmusic.getThumb().setColorFilter(getResources().getColor(R.color.purple_500),PorterDuff.Mode.SRC_IN);

        seekmusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
            }
        });
        String  endTime = createTimeChanger(mediaPlayer.getDuration());
        txtsstop.setText(endTime);
        final Handler handler = new Handler();
        final int delay = 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime = createTimeChanger(mediaPlayer.getCurrentPosition());
                txtsstart.setText(currentTime);
                handler.postDelayed(this,delay);
            }
        },delay);

        //play button action and switch while music stops
        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaPlayer.isPlaying()){
                    btnplay.setBackgroundResource(R.drawable.ic_play);
                    mediaPlayer.pause();
                }else{
                    btnplay.setBackgroundResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                }
            }

        });
        //setup the skipping time forward and backward
        btnff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+10000);
                }
            }
        });
        btnfr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()-10000);
                }
            }
        });
        //while song ended the next song will play immediately
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                btnnext.performClick();
            }
        });
        //next Button setup
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position=((position+1))%mySongs.size();
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer=MediaPlayer.create(getApplicationContext(),u);
                sname=mySongs.get(position).getName();
                txtsname.setText(sname);
                mediaPlayer.start();
                btnplay.setBackgroundResource(R.drawable.ic_pause);

            }
        });
        //previews button setup
        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position=((position-1))<0?(mySongs.size()-1):(position-1);
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer=MediaPlayer.create(getApplicationContext(),u);
                sname=mySongs.get(position).getName();
                txtsname.setText(sname);
                mediaPlayer.start();
                btnplay.setBackgroundResource(R.drawable.ic_pause);

            }
        });

    }

    //turn milliseconds into ,min and sec units
    public String createTimeChanger(int duration){
        String  time ="";
        int min = duration/1000/60;
        int sec = duration/1000%60;
        time+=min+":";
        if(sec<10){
            time+=0;
        }
        time+=sec;
        return time;
    }
}