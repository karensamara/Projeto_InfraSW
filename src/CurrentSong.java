import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import support.PlayerWindow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CurrentSong extends Thread{
    AudioDevice device;
    Bitstream bitstream;
    Decoder decoder;
    boolean exit = true, isPaused;
    Lock lock;
    int position;
    PlayerWindow window;
    String[] currentSong;
    int frame;

    public CurrentSong(AudioDevice device, Bitstream bitstream, Decoder decoder, PlayerWindow window, String[] currentSong) {
        this.device =  device;
        this.bitstream = bitstream;
        this.decoder = decoder;
        this.lock = new ReentrantLock();
        this.window = window;
        this.currentSong = currentSong;
        this.position = 0; this.frame = 0;
        this.exit = true; this.isPaused = false;
    }

    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }
    public void test(boolean isPlaying){
        if (isPlaying){

        }
    }
    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > position) {
            int framesToSkip = newFrame - position;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }
    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        position++;
        return true;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public void setIsPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }


    public void run(){
        lock.lock();
        while (exit) {
            try {
                    if (!(playNextFrame() == true)) break;
                    frame++;
                    //System.out.println(frame);
                    position = device.getPosition();
                    window.setTime(frame*Integer.parseInt(currentSong[8]), Integer.parseInt(currentSong[6]));
                    while(isPaused() == true){
                        sleep(1);
                    }
            } catch (JavaLayerException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    lock.unlock();
    }
}
