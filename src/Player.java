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
    /**
     * The AudioDevice the audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;

    private boolean repeat = false;
    private boolean shuffle = false;
    private boolean playerEnabled = false;
    private boolean playerPaused = true;
    private int currentFrame = 0;
    String[][] queueArray;
    private int qtde;
    CurrentSong songPlaying;
    String[] currentPlayerSong;
    boolean isPlaying;
    //Lock lock = new ReentrantLock();


    public Player() {
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
           }
       };
       ActionListener removeEvent = e -> new Thread(() -> removeFromQueue(window.getSelectedSong())).start();
       ActionListener addEvent = e -> {
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
       };
       ActionListener shuffleEvent = e -> shuffle();
       ActionListener previousEvent = e -> previous();
       ActionListener playPauseEvent = e -> new Thread(() -> {
           try {
               playPause();
           } catch (BitstreamException ex) {
               ex.printStackTrace();
           }
       }).start();
       ActionListener stopEvent = e -> stop();
       ActionListener nextEvent = e -> next();
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

            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        };

        this.window = new PlayerWindow("SongTerrace", queueArray, playNowEvent, removeEvent, addEvent,
                                        shuffleEvent, previousEvent, playPauseEvent, stopEvent, nextEvent,
                                            repeatEvent, mouseListener, mouseMotionListener);
    }

    private void playNow(String filePath) throws JavaLayerException, FileNotFoundException {
        if (filePath.equals("null")){
            JOptionPane.showMessageDialog(null, "Selecione uma opção válida");
        }else {
            int index = search(filePath);
            isPlaying = true;
            window.updatePlayingSongInfo(queueArray[index][0], queueArray[index][1], queueArray[index][2]);
            window.setEnabledScrubber(isPlaying);
            window.updatePlayPauseButtonIcon(!isPlaying);
            window.setEnabledPlayPauseButton(isPlaying);
            //System.out.println(queueArray[index][6]);
            window.setTime(0, Integer.parseInt(queueArray[index][6]));

            File file = new File(filePath);
            bitstream = new Bitstream(new BufferedInputStream(new FileInputStream(file)));
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());

            if (songPlaying != null) { //evita que as musicas se sobreponham
                songPlaying.setExit(false);
            }
            currentPlayerSong = queueArray[index];

            songPlaying = new CurrentSong(device, bitstream, decoder, window, currentPlayerSong);
            songPlaying.start();
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

    public String[][] getQueueAsArray() {
        return queueArray;
    }

    //</editor-fold>

    //<editor-fold desc="Controls">

    public void stop() {
    }

    public void next() {
    }

    public void previous() {
    }
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">





    //</editor-fold>
}
