package com.groupproject.music;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest.permission.*;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    //member variables
    Context context;
    MusicDatabaseHelper dbHelper;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;
    //constructor
    public SongAdapter(Context context, List<Song> songs, ExoPlayer player, ConstraintLayout playerView) {
        dbHelper = new MusicDatabaseHelper(context);
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
    }

    public SongAdapter(List<Song> songs) {
        this.songs = songs;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate song row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item,parent,false);

        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //current Song
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        //Set values to view
        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.durationHolder.setText(getDuration(song.getDuration()));
        viewHolder.sizeHolder.setText(getSize(song.getSize()));

        //artwork
        Uri artworkUri = song.getArtworkUri();

        if (artworkUri!=null){
            //set uri to image view
            viewHolder.artworkHolder.setImageURI(artworkUri);

            //kiem tra uri
            if(viewHolder.artworkHolder.getDrawable() == null){
                viewHolder.artworkHolder.setImageResource(R.drawable.artwork);
            }
        }
        View deleteBtn = viewHolder.itemView.findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Delete the song from the database
                dbHelper.deleteSong(song.getUri());

                // Remove the song from the list and notify the adapter
                songs.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, songs.size());

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete Song");
                builder.setMessage("The song has been deleted.");
                builder.setPositiveButton("OK", null);
                builder.show();
            }
        });


        //play song on click

        viewHolder.itemView.setOnClickListener(view -> {
            //context.startService(new Intent(context.getApplicationContext(), PlayerService.class));

            playerView.setVisibility(View.VISIBLE);
            if ( !player.isPlaying()){
                player.setMediaItems(getMediaItems(), position,0);
            }else {
                player.pause();
                player.seekTo(position,0);
            }
            //chuan bi phat nhac
            player.prepare();
            player.play();

            Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();

            if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){

                ((MainActivity)context).recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);

            }

        });

    }

    private List<MediaItem> getMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs){
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();

            //cho media vao list
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }

    //View Holder
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        //member variables
        ImageView artworkHolder;
        TextView titleHolder,durationHolder,sizeHolder;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            artworkHolder = itemView.findViewById(R.id.artworkView);
            titleHolder = itemView.findViewById(R.id.titleView);
            durationHolder = itemView.findViewById(R.id.durationView);
            sizeHolder = itemView.findViewById(R.id.sizeView);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    //filter Song/Search
    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song> filteredList){
        songs = filteredList;
        notifyDataSetChanged();
    }
    @SuppressLint("DefaultLocale")
    public String getDuration(int totalDuration)
    {
        String totalDurationText;
        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((totalDuration % (1000 * 60)) % (1000 * 60 * 60)) % (1000 * 600)) / 1000;

        if ( hrs < 1){
            totalDurationText = String.format("%02d:%02d", min, secs);
        } else {
            totalDurationText = String.format("%id:%02d:%02d", hrs, min, secs);
        }
        return totalDurationText;
    }

    //size
    private String getSize(long bytes){
        String hrsize;
        double k = bytes / 1024.0;
        double m = ((bytes/1024.0)/1024.0);
        double g = (((bytes/1024.0)/1024.0)/1024.0);
        double t = ((((bytes/1024.0)/1024.0)/1024.0)/1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");
        if(t>1) {
            hrsize = dec.format(t).concat(" TB");
        } else if (g>1) {
            hrsize = dec.format(g).concat(" GB");
        } else if (m>1) {
            hrsize = dec.format(m).concat(" MB");
        } else if (k>1) {
            hrsize = dec.format(k).concat(" KB");
        } else {
            hrsize = dec.format(bytes).concat(" Bytes");
        }
        return hrsize;
    }
}
