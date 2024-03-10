import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class MusicPlayer extends PlaybackListener {
    //This will be used to update isPaused more synchronously
    private static final Object playSignal = new Object();

    //Need reference so that we can update the gui in this class
    private MusicPlayerGUI musicPlayerGUI;

    //We will need a way to store our song's details, so we will be creating a song class
    private Song currentSong;
    public Song getCurrentSong() {
        return currentSong;
    }

    private ArrayList<Song> playlist;

    //We will need to keep track the index we are in the playlist
    private int currentPlaylistIndex;

    //Use JLayer library to create an AdvancedPlayer obj will handle playing the music
    private AdvancedPlayer advancedPlayer;

    //Pause boolean flag used to indicate whether the player has been paused
    private boolean isPaused;

    //Boolean flag used to tell when the song has finished
    private boolean songFinished;

    //
    private boolean pressedNext, pressedPrev;

    //Stores in teh lost frame when the playback is finished (used for pausing and resuming)
    private int currentFrame;
    public void setCurrentFrame(int frame) {
        currentFrame = frame;
    }

    //Track how many milliseconds has passed since playing the song (used for updating the slider)
    private int currentTimeInMilli;
    public void setCurrentTimeInMilli(int timeInMilli) {
        currentTimeInMilli = timeInMilli;
    }

    //Constructor
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI) {
        this.musicPlayerGUI = musicPlayerGUI;
    }

    public void loadSong(Song song) {
        currentSong = song;
        playlist = null;

        //Stop the song if possible
        if (!songFinished)
            stopSong();

        //Play the current song if not null
        if (currentSong != null) {
            //Reset frame
            currentFrame = 0;

            //Reset current time in multi
            currentTimeInMilli = 0;

            //Update gui
            musicPlayerGUI.setPlaybackSliderValue(0);

            playCurrentSong();
        }
    }

    public void loadPlaylist(File playlistFile) {
        playlist = new ArrayList<>();

        //Store the paths from the text file into the playlist array list
        try {
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //Reach each line from the text file and store the text into the songPath variable
            String songPath;
            while ((songPath = bufferedReader.readLine()) != null) {
                //Create song object based on song path
                Song song = new Song(songPath);

                //Add to playlist array list
                playlist.add(song);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playlist.size() > 0) {
            //Reset playback slider
            musicPlayerGUI.setPlaybackSliderValue(0);

            //Update current song to the first song in the playlist
            currentSong = playlist.get(0);

            //Start from the beginning frame
            currentFrame = 0;

            //Update gui
            musicPlayerGUI.enablePauseButtonDisablePlayButton();
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);
            musicPlayerGUI.updatePlaybackSlider(currentSong);

            //Start song
            playCurrentSong();

        }
    }
    public void pauseSong() {
        if (advancedPlayer != null) {
            //Update isPaused flag
            isPaused = true;

            //Then we want to stop thr player
            stopSong();
        }
    }

    public void stopSong() {
        if (advancedPlayer != null) {
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }

    public void nextSong() {
        //No need to go to the next song if there is no playlist
        if (playlist == null) return;

        //Check to see if we have reached the end of the playlist, if so then don't do anything
        if(currentPlaylistIndex + 1 > playlist.size() - 1) return;

        pressedNext = true;

        //Stop the song if possible
        if (!songFinished)
            stopSong();

        //Increase current playlist index
        currentPlaylistIndex++;

        //Update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //Reset frame
        currentFrame = 0;

        //Reset current time in milli
        currentTimeInMilli = 0;

        //Update gui
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        //Play the song
        playCurrentSong();
    }

    public void prevSong() {
        //No need to go to the next song if there is no playlist
        if (playlist == null) return;

        //Check to see if we can go to the previous song
        if (currentPlaylistIndex - 1 < 0) return;

        pressedPrev = true;

        //Stop the song if possible
        if (!songFinished)
            stopSong();

        //Decrease current playlist index
        currentPlaylistIndex--;

        //Update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //Reset frame
        currentFrame = 0;

        //Reset current time in milli
        currentTimeInMilli = 0;

        //Update gui
        musicPlayerGUI.enablePauseButtonDisablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);

        //Play the song
        playCurrentSong();
    }

    public void playCurrentSong() {
        if(currentSong == null) return;

        try {
            //Read mp3 audio data
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilaPath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            //Create a new advanced player
            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            //Start music
            startMusicThread();

            //Start playback slider thread
            startPlaybackSliderThread();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //Create a thread that will handle playing the music
    private void startMusicThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                     if (isPaused) {
                        synchronized (playSignal) {
                            //Update flag
                            isPaused = false;

                            //Notify the other thread to continue (makes sure that isPaused is updated to false property
                            playSignal.notify();
                        }

                         //Resume misic from last frame
                         advancedPlayer.play(currentFrame, Integer.MAX_VALUE);

                     } else {
                         //Play music
                         advancedPlayer.play();
                     }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Create a thread that will handle updating the slider
    private void startPlaybackSliderThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPaused) {
                    try {
                        //Wait till it gets notified by other thread to continue
                        //Makes sure that isPaused boolean flag updates to false before continuing
                        synchronized (playSignal) {
                            playSignal.wait();
                        }
                    } catch (Exception e) {
                         e.printStackTrace();
                    }
                }

                while (!isPaused && !songFinished && !pressedNext && !pressedPrev) {
                   try {
                       //Increment current time milli
                       currentTimeInMilli++;

                       //Calculate into frame value
                       int calculatedFrame = (int) ((double) currentTimeInMilli * 2.08 * currentSong.getFrameRatePerMilliseconds());

                       //Update gui
                       musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

                       //Mimic 1 millisecond using thread.sleep
                       Thread.sleep(1);
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        //This method gets called in the beginning of the song
        System.out.println("Playback Started");
        songFinished = false;
        pressedNext = false;
        pressedPrev = false;
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        //This method gets called when the song finishes or if the player gets closed
        System.out.println("Playback Finished");

        if (isPaused) {
            currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMilliseconds());
        } else {
            //If the user pressed next or prev we don't need to execute the rest of the code
            if (pressedNext || pressedPrev) return;

            //When the song ends
            songFinished = true;

            if (playlist == null) {
                //Update gui
                musicPlayerGUI.enablePlayButtonDisablePauseButton();
            } else {
                //Last song in the playlist
                if (currentPlaylistIndex == playlist.size() - 1) {
                    //Update gui
                    musicPlayerGUI.enablePlayButtonDisablePauseButton();
                } else {
                    //Go to the next song in the playlist
                    nextSong();
                }
            }
        }
    }
}
