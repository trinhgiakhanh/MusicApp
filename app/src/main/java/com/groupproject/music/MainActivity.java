package com.groupproject.music;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {

    //members
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

    ExoPlayer player;
    ActivityResultLauncher<String> recordAudioPermissionLauncher;
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    ConstraintLayout playerView;
    TextView playerCloseBtn, deleteBtn, updateBtn;
    TextView songNameView, skipPreviousBtn, skipNextBtn, playPauseBtn, repeatModeBtn, playlistBtn;
    TextView homeSongNameView, homeSkipPreviousBtn, homeSkipNextBtn, homePlayPauseBtn;
    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper, audioVisualizerWrapper;
    CircleImageView artworkView;
    SeekBar seekBar;
    TextView progressView, durationView;
    BarVisualizer audioVisualizer;
    BlurImageView blurImageView;
    int defaultStatusColor;
    int repeatMode = 1;
    SearchView searchView;
    boolean isBound = false;
    MusicDatabaseHelper dbHelper;
    private static final int REQUEST_CODE_PICK_SONG = 1;
    private static final int READ_REQUEST_CODE = 42;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button addSongButton = findViewById(R.id.addSongButton);
        addSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        //save the  status color
        defaultStatusColor = getWindow().getStatusBarColor();
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        //set tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        //recycler view
        recyclerView = findViewById(R.id.recyclerView);
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{

            if(granted){
                //fetch song
                fetchSongs();
            }else {
                userResponses();
            }
        });
        storagePermissionLauncher.launch(permission);
        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{
            if(granted && player.isPlaying()){
                activateAudioVisualizer();
            }else{
                userResponsesOnRecordAudioPerm();
            }
        });
        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playlistBtn = findViewById(R.id.playlistBtn);

        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseBtn);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        seekbarWrapper = findViewById(R.id.seekbarWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);

        artworkView = findViewById(R.id.artworkView);
        seekBar = findViewById(R.id.seekBar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        audioVisualizer = findViewById(R.id.audioVisualizer);
        blurImageView = findViewById(R.id.blurImageView);

        deleteBtn = findViewById(R.id.deleteBtn);
        updateBtn = findViewById(R.id.updateBtn);
        playControls();
        //doBindService();
    }
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_SONG);
    }

    /*private void doBindService() {
        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) iBinder;
            player = binder.getPlayerService().player;
            isBound = true;
            storagePermissionLauncher.launch(permission);
            playControls();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            player = null;
        }
    };*/

    private void playControls() {
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        playerCloseBtn.setOnClickListener(view -> exitPlayerView());
        playlistBtn.setOnClickListener(view -> exitPlayerView());

        homeControlWrapper.setOnClickListener(view -> showPlayerView());

        player.addListener(new Player.Listener(){
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime( (int) player.getCurrentPosition()));

                seekBar.setProgress((int) player.getCurrentPosition());
                seekBar.setMax((int) player.getDuration());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_cutline,0,0,0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);


                showCurrentArtwork();
                updatePlayerPositionProgress();
                artworkView.setAnimation(loadRotation());

                activateAudioVisualizer();

                updatePlayerColors();

                if(!player.isPlaying()){
                    player.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState){
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_ENDED){
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekBar.setMax((int) player.getDuration());
                    seekBar.setProgress((int) player.getCurrentPosition());

                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_cutline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);

                    showCurrentArtwork();
                    updatePlayerPositionProgress();
                    artworkView.setAnimation(loadRotation());

                    activateAudioVisualizer();

                    updatePlayerColors();
                }else {
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
                }
            }

        });
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());
        skipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        playPauseBtn.setOnClickListener(view -> playOrPauseSong());

        homePlayPauseBtn.setOnClickListener(view -> playOrPauseSong());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            int progressValue = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){
                player.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                if(player.getPlaybackState() == ExoPlayer.STATE_READY){
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }
            }
        });
        repeatModeBtn.setOnClickListener(view -> {
            if(repeatMode == 1){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);
            }else if(repeatMode == 2){
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);
            }else if(repeatMode == 3){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_all,0,0,0);
            }
            updatePlayerColors();
        });
    }

    private void playOrPauseSong() {
        if (player.isPlaying()){
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
            artworkView.clearAnimation();
        }else {
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_cutline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
            artworkView.startAnimation(loadRotation());
        }
        updatePlayerColors();
    }

    private void skipToPreviousSong() {
        if (player.hasPreviousMediaItem()){
            player.seekToPrevious();
        }
    }
    private void skipToNextSong() {
        if (player.hasNextMediaItem()){
            player.seekToNext();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0,360, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                if(player.isPlaying()){
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekBar.setProgress((int) player.getCurrentPosition());

                }
                updatePlayerPositionProgress();
            }
        }, 1000);
    }

    String getReadableTime(int duration) {
        String time;
        int hsr = duration / (1000 * 60 * 60);
        int min = (duration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((duration % (1000 * 60)) % (1000 * 60 * 60)) % (1000 * 600)) / 1000;

        if(hsr<1){
            time = min+ ":" + secs;
        }else{
            time = hsr + ":" + min + ":" + secs;
        }
        return time;
    }

    private void showCurrentArtwork() {
        artworkView.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);
        if (artworkView.getDrawable() == null){
            artworkView.setImageResource(R.drawable.artwork);
        }
    }


    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }

    private void updatePlayerColors() {

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();
        if (bitmapDrawable != null){
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this,R.drawable.artwork);

        }
        assert bitmapDrawable != null;
        Bitmap bmp = bitmapDrawable.getBitmap();

        blurImageView.setImageBitmap(bmp);
        blurImageView.setBlur(4);

        Palette.from(bmp).generate(palette -> {
            if (palette != null){
                Palette.Swatch swatch = palette.getDominantSwatch();
                if(swatch == null){
                    swatch = palette.getMutedSwatch();
                    if(swatch == null){
                        swatch = palette.getDominantSwatch();
                    }
                }
                assert swatch != null;
                int titleTextColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rgb = swatch.getRgb();

                songNameView.setTextColor(titleTextColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);

                repeatModeBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                playlistBtn.getCompoundDrawables()[0].setTint(titleTextColor);
            }
        });
    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

    }

    private void userResponsesOnRecordAudioPerm() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(recordAudioPermission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show Audio Visualizer")
                        .setMessage("Allow this app to display audio visualizer")
                        .setPositiveButton("Allow", (dialogInterface, i) -> recordAudioPermissionLauncher.launch(recordAudioPermission))
                        .setNegativeButton("Deny", (dialogInterface, i) -> {
                            Toast.makeText(MainActivity.this, "You denied to record audio", Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        })
                        .show();
            }
            else {
                Toast.makeText(this, "You denied to record audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void activateAudioVisualizer() {
        if(ContextCompat.checkSelfPermission(this, recordAudioPermission) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        audioVisualizer.setColor(ContextCompat.getColor(this,R.color.secondary_color));
        audioVisualizer.setDensity(10);
        audioVisualizer.setPlayer(player.getAudioSessionId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()){
            player.stop();
        }
        player.release();
        /*doUnBindService();*/
    }
    /*private void doUnBindService() {
        if (isBound){
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }*/

    private void userResponses() {
        if(ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED){
            fetchSongs();
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(permission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting Permission")
                        .setMessage("Allow us to fetch songs on your device")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                storagePermissionLauncher.launch(permission);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "You denied to show songs", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        }
        else {
            Toast.makeText(this, "You canceled to show songs", Toast.LENGTH_SHORT).show();
        }
    }

    /*private void fetchSongs() {
        //define list to show

        dbHelper = new MusicDatabaseHelper(this);
        Cursor cursor = dbHelper.getAllSongs();
        List<Song> songs = getSongsFromCursor(cursor);
        SongAdapter songAdapter = new SongAdapter(songs);
        recyclerView.setAdapter(songAdapter);
        List<Song> songs = new ArrayList<>();
        Uri mediaStoreUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }else {
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        //projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID
        };

        //order
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        //get the songs
        try(Cursor cursor = getContentResolver().query(mediaStoreUri,projection,null,null,sortOrder)) {
            //cache cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            //clear previous songs before load
            while (cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);

                Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                name = name.substring(0,name.lastIndexOf('.'));

                Song song = new Song(name,uri,albumArtworkUri,size,duration);

                songs.add(song);
            }
            showSongs(songs);
        }
    }*/
    private void fetchSongs() {
        dbHelper = new MusicDatabaseHelper(this);

        // Lấy dữ liệu từ cơ sở dữ liệu SQLite
        Cursor cursor = dbHelper.getAllSongs();
        List<Song> songs = new ArrayList<>();

        // Kiểm tra xem Cursor có dữ liệu hay không
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnId()));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnTitle()));
                String uri = cursor.getString(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnUri()));
                String artworkUri = cursor.getString(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnArtworkUri()));
                int size = cursor.getInt(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnSize()));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MusicDatabaseHelper.getColumnDuration()));

                // Tạo đối tượng Song từ dữ liệu trong Cursor
                Song song = new Song(title, Uri.parse(uri), Uri.parse(artworkUri), size, duration);
                songs.add(song);
            } while (cursor.moveToNext());
        }

        // Hiển thị danh sách bài hát
        showSongs(songs);
    }

    private void showSongs(List<Song> songs) {
        if (songs.size() == 0){
            Toast.makeText(this, "No songs found", Toast.LENGTH_SHORT).show();
            return;
        }
        allSongs.clear();
        allSongs.addAll(songs);

        String title = getResources().getString(R.string.app_name) + " - " + songs.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        songAdapter = new SongAdapter(this,songs,player,playerView);

        //recyclerView.setAdapter(songAdapter);

        //animator
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);

    }

    //setting the menu /searchbtn
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_btn, menu);
        MenuItem menuItem = menu.findItem(R.id.searchBtn);
        SearchView searchView = (SearchView) menuItem.getActionView();
        SearchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void SearchSong(SearchView searchView) {

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });

    }
    private void filterSongs(String query) {
        List<Song> filteredList = new ArrayList<>();
        if (allSongs.size() > 0){
            for (Song song : allSongs){
                if (song.getTitle().toLowerCase().contains(query)){
                    filteredList.add(song);
                }
            }
            if (songAdapter != null){
                songAdapter.filterSongs(filteredList);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_SONG && resultCode == Activity.RESULT_OK){
            ContentResolver contentResolver = getContentResolver();
            Uri uri = data.getData();
            String title = getTitleFromUri(uri,contentResolver);
            int size = getSizeFromUri(uri, contentResolver);
            int duration = getDurationFromUri(uri,contentResolver);
            int albumId = 1;

            MusicDatabaseHelper dbHelper = new MusicDatabaseHelper(this);
            if (dbHelper.isUriExist(uri)) {
                // Uri already exists in the database
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The song already exists in the database.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                // Uri does not exist in the database, add the song
                boolean isSuccess = dbHelper.addSong(title, uri.toString(), "", size, duration);
                if (isSuccess) {
                    // Thêm thành công
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(title + " - " + size + " - " + duration)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    // Thêm thất bại
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Thêm bài hát thất bại")
                            .setPositiveButton("OK", null)
                            .show();
                }
                fetchSongs();
            }
        }
    }
    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }
    private String getTitleFromUri(Uri uri, ContentResolver contentResolver) {
        String title = null;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int titleIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (titleIndex != -1) {
                title = cursor.getString(titleIndex);
            }
            cursor.close();
        }
        return title;
    }
    public int getSizeFromUri(Uri uri, ContentResolver contentResolver) {
        int size = 0;
        String[] projection = { MediaStore.Audio.Media.SIZE };
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                    size = cursor.getInt(sizeIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return size;
    }
    public int getDurationFromUri(Uri uri, ContentResolver contentResolver) {
        int duration = 0;
        String[] projection = { MediaStore.Audio.Media.DURATION };
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    duration = cursor.getInt(durationIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return duration;
    }

}
















