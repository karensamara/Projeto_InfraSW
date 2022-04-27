import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player {

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;

    private PlayerWindow window;

    private boolean repeat = false;
    private boolean shuffle = false;
    private boolean playerEnabled = false;
    private boolean playerPaused = true;
    private int currentFrame = 0;
    private String[][] queueArray;
    private int qtde, currentTime;
    private CurrentSong songPlaying;
    private String[] currentPlayerSong;
    private boolean isPlaying;
    private MouseEvent mouseDrag;
    //Lock lock = new ReentrantLock();


    public Player() {
        this.currentTime = 0;
        this.qtde = 0;
        this.queueArray = new String[1][1];
       //Criando instancias do actionlistener e direcionando os actionperformed para os seus respectivos metodos
       ActionListener playNowEvent = e -> {
           try {
               playNow(window.getSelectedSong());
           } catch (JavaLayerException ex) {
               ex.printStackTrace();
           } catch (FileNotFoundException ex) {
               ex.printStackTrace();
           } catch (InterruptedException ex) {
               ex.printStackTrace();
           }
       };
       ActionListener removeEvent = e -> new Thread(() -> removeFromQueue(window.getSelectedSong())).start();
       ActionListener addEvent = e -> new Thread(() -> {
           try {
               addToQueue(window.getNewSong());
           } catch (InvalidDataException ex) {
               ex.printStackTrace();
           } catch (UnsupportedTagException ex) {
               ex.printStackTrace();
           } catch (IOException ex) {
               ex.printStackTrace();
           } catch (BitstreamException ex) {
               ex.printStackTrace();
           }
       }).start();
       ActionListener shuffleEvent = e -> shuffle();
       ActionListener previousEvent = e -> {
           try {
               previous();
           } catch (FileNotFoundException ex) {
               ex.printStackTrace();
           } catch (JavaLayerException ex) {
               ex.printStackTrace();
           } catch (InterruptedException ex) {
               ex.printStackTrace();
           }
       };
       ActionListener playPauseEvent = e -> {
           try {
               playPause();
           } catch (BitstreamException ex) {
               ex.printStackTrace();
           }
       };
       ActionListener stopEvent = e -> new Thread(() ->stop()).start();
       ActionListener nextEvent = e -> {
           try {
               next();
           } catch (FileNotFoundException ex) {
               ex.printStackTrace();
           } catch (JavaLayerException ex) {
               ex.printStackTrace();
           } catch (InterruptedException ex) {
               ex.printStackTrace();
           }
       };
       ActionListener repeatEvent = e -> repeat();
        MouseListener mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(mouseDrag != null && isPlaying == true){
                    currentTime = window.getScrubberValue();
                    //System.out.println(currentTime);
                    mouseDrag = null;
                    try {
                        playNow(currentPlayerSong[5]);
                    } catch (JavaLayerException ex) {
                        ex.printStackTrace();
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }else if(mouseDrag != null && isPlaying == false) {
                    currentTime = window.getScrubberValue();
                    //System.out.println(currentTime);
                    mouseDrag = null;
                    try {
                        playNow(currentPlayerSong[5]);
                        playerPaused = true;
                        playPause();

                    } catch (JavaLayerException ex) {
                        ex.printStackTrace();
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
        MouseMotionListener mouseMotionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if(mouseDrag == null){
                    mouseDrag = e;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        };

        this.window = new PlayerWindow("SongTerrace", queueArray, playNowEvent, removeEvent, addEvent,
                                        shuffleEvent, previousEvent, playPauseEvent, stopEvent, nextEvent,
                                            repeatEvent, mouseListener, mouseMotionListener);
    }

    //</editor-fold>

    //<editor-fold desc="Queue Utilities">
    public void addToQueue(Song song) {
        if(search(song.getFilePath()) != -1) {
            JOptionPane.showMessageDialog(null, "A música já foi adicionada à playlist");
        } else {
            if (qtde < queueArray.length) {
                queueArray[qtde] = song.getDisplayInfo();
                this.qtde++;
            } else {
                String[][] newQueueArray = new String[queueArray.length * 2][1];
                for (int i = 0; i < queueArray.length; i++) {
                    newQueueArray[i] = queueArray[i];
                }
                this.queueArray = newQueueArray;
                queueArray[qtde] = song.getDisplayInfo();
                this.qtde++;
            }
            window.updateQueueList(queueArray);
        }
    }

    public void removeFromQueue(String filePath) {
        if (filePath.equals("null")){
            JOptionPane.showMessageDialog(null, "Selecione uma opção válida");
        } else{
            int index = search(filePath);
            if(index != -1) {
                for (int i = index; i < qtde - 1; i++) {
                    queueArray[i] = queueArray[i + 1];
                }
                queueArray[qtde-1] = null;
                this.qtde--;
            }
            window.updateQueueList(queueArray);
        }
    }

    public int search(String filePath){
        int index = -1;
        for(int i = 0; i < this.qtde; i++ ){
            if (filePath.equals(queueArray[i][5])){
                index = i;
                break;
            }
        }
        return index;
    }

    //</editor-fold>

    //<editor-fold desc="Controls">
    private void playNow(String filePath) throws JavaLayerException, FileNotFoundException, InterruptedException {
        if (filePath.equals("null")){
            JOptionPane.showMessageDialog(null, "Selecione uma opção válida");
        }else {
            int index = search(filePath);
            isPlaying = true;
            window.updatePlayingSongInfo(queueArray[index][0], queueArray[index][1], queueArray[index][2]);
            window.setEnabledScrubberArea(isPlaying);
            window.updatePlayPauseButtonIcon(!isPlaying);
            window.setTime(currentTime, Integer.parseInt(queueArray[index][6]));

            File file = new File(filePath);
            bitstream = new Bitstream(new BufferedInputStream(new FileInputStream(file)));
            /**
             * The AudioDevice the audio samples are written to.
             */
            AudioDevice device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());

            if (songPlaying != null) { //evita que as musicas se sobreponham
                songPlaying.setExit(true);
                Thread.sleep(100);
            }
            currentPlayerSong = queueArray[index];

            songPlaying = new CurrentSong(device, bitstream, decoder, currentTime, this);
            songPlaying.start();
            currentTime = 0;
            //if(songPlaying.getExit())

        }
    }

    private void repeat() {
    }


    private void playPause() throws BitstreamException {
        Lock lockPlayPause = songPlaying.getLockPlayPause();
        if (isPlaying == true) {
            isPlaying = false;
            window.updatePlayPauseButtonIcon(!isPlaying);
            songPlaying.setIsPaused(true);
        } else {
            lockPlayPause.lock();
            try {
                isPlaying = true;
                window.updatePlayPauseButtonIcon(!isPlaying);
                songPlaying.setIsPaused(false);
                songPlaying.getPlayndpause().signalAll();
            }finally { lockPlayPause.unlock(); }
        }
    }

    private void shuffle() {
    }

    public void stop() {
        isPlaying = false;
        songPlaying.setExit(true);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        window.resetMiniPlayer();
    }

    public void next() throws FileNotFoundException, JavaLayerException, InterruptedException {
        int indexCurrent = search(currentPlayerSong[5]);
        System.out.println(currentPlayerSong[5]);
        playNow(queueArray[indexCurrent+1][5]);
    }

    public void previous() throws FileNotFoundException, JavaLayerException, InterruptedException {
        int indexCurrent = search(currentPlayerSong[5]);
        System.out.println(currentPlayerSong[5]);
        playNow(queueArray[indexCurrent-1][5]);
    }
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">

    public PlayerWindow getWindow() {
        return window;
    }

    public String[][] getQueueArray() {
        return queueArray;
    }

    public String[] getCurrentPlayerSong() {
        return currentPlayerSong;
    }

    //</editor-fold>
}
