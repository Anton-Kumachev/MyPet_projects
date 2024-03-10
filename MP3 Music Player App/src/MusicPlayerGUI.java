import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

public class MusicPlayerGUI extends JFrame {
    //Color configuration
    public static final Color FRAME_COLOR = Color.RED;
    public static final Color TEXT_COLOR = Color.WHITE;
    private MusicPlayer musicPlayer;

    //Allow us to use file explorer in our app
    private JFileChooser jFileChooser;
    private JLabel songTitle, songArtist;
    private JPanel playbackBtns;
    private JSlider playbackSlider;

    public MusicPlayerGUI() throws IOException {
        //Calls JFrame constructor to configure out gui and set the title header to "Music Player"
        super("Music Player");

        //Set the width and height
        setSize(400,600);

        //End process when app is closed
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //Luanch the app at the center of screen
        setLocationRelativeTo(null);

        //Prevent the app from behind resized
        setResizable(false);

        //Set layout to null which allows us to control the (x, y) coordinates of our components
        //and also set the height and width
        setLayout(null);

        //Change the frame color
        getContentPane().setBackground(FRAME_COLOR);

        musicPlayer = new MusicPlayer(this);
        jFileChooser = new JFileChooser();

        //Set a default path for file explorer
        jFileChooser.setCurrentDirectory(new File("src/resources"));


        //Filter file chooser to only see .mp3 files
        jFileChooser.setFileFilter(new FileNameExtensionFilter("MP3","mp3"));

        addGuiComponents();

    }

    private void addGuiComponents() throws IOException {
        //Add toolbar
        addToolbar();

       //Load record image

       JLabel songImage = new JLabel(loadImage("src/resources/record.png"));
       songImage.setBounds(0, 50, getWidth() - 20, 225);
       add(songImage);


        //Song title
        songTitle = new JLabel("Song Title");
        songTitle.setBounds(0,285, getWidth() - 10, 30);
        songTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        songTitle.setForeground(TEXT_COLOR);
        songTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitle);

        //Song artist
        songArtist = new JLabel("Artist");
        songArtist.setBounds(0,315, getWidth() - 10, 30);
        songArtist.setFont(new Font("Dialog", Font.PLAIN, 24));
        songArtist.setForeground(TEXT_COLOR);
        songArtist.setHorizontalAlignment(SwingConstants.CENTER);
        add(songArtist);

        //Playbackslider
        playbackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        playbackSlider.setBounds(getWidth() / 2 - 300 / 2, 365, 300, 40);
        playbackSlider.setBackground(null);
        playbackSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                //When the user is holding the tick we want to the pause the song
                musicPlayer.pauseSong();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //When the user drops the tick
                JSlider source = (JSlider) e.getSource();

                //Get the frame value from where the user wants to playback to
                int frame = source.getValue();

                //Update the current frame in the music player to this frame
                musicPlayer.setCurrentFrame(frame);

                //Update current time in milli as well
                musicPlayer.setCurrentTimeInMilli((int) (frame / (2.08 * musicPlayer.getCurrentSong().getFrameRatePerMilliseconds())));

                //Resume the song
                musicPlayer.playCurrentSong();

                //Toggle on pause button and toggle off play button
                enablePauseButtonDisablePlayButton();
            }
        });
        add(playbackSlider);

        //Playback buttons (i.e. previous, play, next)
        addPlaybackBtns();
    }

    private void addToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setBounds(0, 0, getWidth(), 20);
        //Prevent toolbar from being moved
        toolBar.setFloatable(false);

        //Add drop down menu
        JMenuBar menuBar = new JMenuBar();
        toolBar.add(menuBar);

        //Now we will add a song menu where we will place the loading song option
        JMenu songMenu = new JMenu("Song");
        menuBar.add(songMenu);

        //Add the "load song" itemin the songMenu
        JMenuItem loadSong = new JMenuItem("Load Song");
        loadSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //An integer is returned to us to let us know what the user did
                int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = jFileChooser.getSelectedFile();

                //This means that we are also checking to see if the user pressed the "open" button
                if (result == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                    //Create a song obj based on selected file
                    Song song = new Song(selectedFile.getPath());

                    //Load song in music player
                    musicPlayer.loadSong(song);

                    //Update song title and artist
                    updateSongTitleAndArtist(song);

                    //Update playback slider
                    updatePlaybackSlider(song);

                    //Toggle on pause button and toggle off play button
                    enablePauseButtonDisablePlayButton();
                }
            }
        });

        songMenu.add(loadSong);

        //Now we will add the playlist menu
        JMenu playlistMenu = new JMenu("Playlist");
        menuBar.add(playlistMenu);

        //Then add the items to the playlist menu
        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        createPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Load music playlist dialog
                new MusicPlaylistDialog(MusicPlayerGUI.this).setVisible(true);
            }
        });
        playlistMenu.add(createPlaylist);

        JMenuItem loadPlaylist = new JMenuItem("Load Playlist");
        loadPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileFilter(new FileNameExtensionFilter("Playlist", "txt"));
                jFileChooser.setCurrentDirectory(new File("src/resources"));

                int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = jFileChooser.getSelectedFile();

                if(result == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                    //Stop the music
                    musicPlayer.stopSong();

                    //Load playlist
                    musicPlayer.loadPlaylist(selectedFile);
                }

            }
        });
        playlistMenu.add(loadPlaylist);

        add(toolBar);
    }

    private void addPlaybackBtns() throws IOException {

        playbackBtns = new JPanel();
        playbackBtns.setBounds(0, 435, getWidth() - 10, 80);
        playbackBtns.setBackground(null);

        //Previous button
        JButton prevButton = new JButton(loadImage("src/resources/previous.png"));
        prevButton.setBorderPainted(false);
        prevButton.setBackground(null);
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Go to the previous song
                musicPlayer.prevSong();
            }
        });
        playbackBtns.add(prevButton);

        //Play button
        JButton playButton = new JButton(loadImage("src/resources/play.png"));
        playButton.setBorderPainted(false);
        playButton.setBackground(null);
        playbackBtns.add(playButton);

        //Pause button
        JButton pauseButton = new JButton(loadImage("src/resources/pause.png"));
        pauseButton.setBorderPainted(false);
        pauseButton.setBackground(null);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Toggle off play button and toggle on pause button
                enablePauseButtonDisablePlayButton();

                //Play or resume song
                musicPlayer.playCurrentSong();

            }
        });
        pauseButton.setVisible(false);
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Toggle off pause button and toggle on play button
                enablePlayButtonDisablePauseButton();

                //Pause the song
                musicPlayer.pauseSong();
            }
        });
        playbackBtns.add(pauseButton);

        //Next button
        JButton nextButton = new JButton(loadImage("src/resources/next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setBackground(null);
        nextButton.addActionListener((new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //go to the next song
                musicPlayer.nextSong();
            }
        }));
        playbackBtns.add(nextButton);

        add(playbackBtns);

    }
    //This will be used to update our slider from the music player class
    public void setPlaybackSliderValue(int frame) {
        playbackSlider.setValue(frame);
    }
    public void updateSongTitleAndArtist(Song song) {
        songTitle.setText((song.getSongTitle()));
        songArtist.setText((song.getSongArtist()));
    }

    public void updatePlaybackSlider(Song song) {
        //Update max count for slider
        playbackSlider.setMaximum(song.getMp3File().getFrameCount());

        //Create the song length label
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        //Beginning will be 00:00
        JLabel labelBeginning = new JLabel("00:00");
        labelBeginning.setFont(new Font("Dialog", Font.BOLD, 18));
        labelBeginning.setForeground(TEXT_COLOR);

        //End will vary depending on the song
        JLabel labelEnd = new JLabel(song.getSongLength());
        labelEnd.setFont(new Font("Dialog", Font.BOLD, 18));
        labelEnd.setForeground(TEXT_COLOR);

        labelTable.put(0, labelBeginning);
        labelTable.put(song.getMp3File().getFrameCount(), labelEnd);

        playbackSlider.setLabelTable(labelTable);
        playbackSlider.setPaintLabels(true);
    }
    public void enablePauseButtonDisablePlayButton() {
        //Retrieve reference to play button from playbackBtns panel
        JButton playButton = (JButton) playbackBtns.getComponent(1);
        JButton pauseButton = (JButton) playbackBtns.getComponent(2);

        //Turn off play button
        playButton.setVisible(false);
        playButton.setEnabled(false);

        //Turn on pause button
        pauseButton.setVisible(true);
        pauseButton.setEnabled(true);
    }

    public void enablePlayButtonDisablePauseButton() {
        //Retrieve reference to play button from playbackBtns panel
        JButton playButton = (JButton) playbackBtns.getComponent(1);
        JButton pauseButton = (JButton) playbackBtns.getComponent(2);

        //Turn on play button
        playButton.setVisible(true);
        playButton.setEnabled(true);

        //Turn off pause button
        pauseButton.setVisible(false);
        pauseButton.setEnabled(false);
    }
    private ImageIcon loadImage(String imagePath) {
        try{
            //Read thr image file from the given path
            BufferedImage image = ImageIO.read(new File(imagePath));

            //Returns an image icon so that our component can render the image
            return  new ImageIcon(image);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Could not find resourse
        return null;
    }
}
