import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import support.PlayerWindow;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CurrentSong extends Thread{
    private AudioDevice device;
    private Bitstream bitstream;
    private Decoder decoder;
    private boolean exit, isPaused;
    private Lock lock;
    private Lock lockPlayPause = new ReentrantLock();
    private final Condition playndpause = lockPlayPause.newCondition();


    private int currentFrame;
    private PlayerWindow window;
    private String[] currentSong;

    public CurrentSong(AudioDevice device, Bitstream bitstream, Decoder decoder, PlayerWindow window, String[] currentSong) {
        this.device =  device;
        this.bitstream = bitstream;
        this.decoder = decoder;
        this.lock = new ReentrantLock();
        this.window = window;
        this.currentSong = currentSong;
        this.currentFrame = 0;
        this.exit = false; this.isPaused = false;
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
    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
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
        currentFrame++;
        return true;
    }

    public void setExit(boolean exit) { this.exit = exit; }

    public void setIsPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public Condition getPlayndpause() {
        return playndpause;
    }

    public Lock getLockPlayPause() {
        return lockPlayPause;
    }

    public void run() {
        while (!exit) {
            lock.lock();
            try {
                if (!(playNextFrame() == true)) break;
                currentFrame++;

                window.setTime(currentFrame * Integer.parseInt(currentSong[8]), Integer.parseInt(currentSong[6]));
                lockPlayPause.lock();
                try {
                    while (isPaused) {
                        playndpause.await();
                    }
                } finally {
                    lockPlayPause.unlock();
                }

            } catch (JavaLayerException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

        }
    }
}
