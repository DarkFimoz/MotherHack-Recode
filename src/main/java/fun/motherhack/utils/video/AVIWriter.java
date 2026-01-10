package fun.motherhack.utils.video;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Simple AVI video writer without external dependencies.
 * Creates uncompressed AVI files.
 */
public class AVIWriter implements Closeable {
    
    private final RandomAccessFile file;
    private final int width;
    private final int height;
    private final int fps;
    private int frameCount = 0;
    private long moviStart;
    private final ByteArrayOutputStream indexBuffer = new ByteArrayOutputStream();
    
    public AVIWriter(File outputFile, int width, int height, int fps) throws IOException {
        // Ensure even dimensions
        this.width = width - (width % 2);
        this.height = height - (height % 2);
        this.fps = fps;
        this.file = new RandomAccessFile(outputFile, "rw");
        
        writeHeader();
    }
    
    private void writeHeader() throws IOException {
        // RIFF header (will be updated at close)
        writeString("RIFF");
        writeInt(0); // File size placeholder
        writeString("AVI ");
        
        // hdrl LIST
        writeString("LIST");
        long hdrlSizePos = file.getFilePointer();
        writeInt(0); // Size placeholder
        writeString("hdrl");
        
        // avih chunk (main AVI header)
        writeString("avih");
        writeInt(56); // Size of avih
        writeInt(1000000 / fps); // Microseconds per frame
        writeInt(width * height * 3 * fps); // Max bytes per second
        writeInt(0); // Padding
        writeInt(0x10); // Flags (AVIF_HASINDEX)
        writeInt(0); // Total frames placeholder (updated at close)
        writeInt(0); // Initial frames
        writeInt(1); // Number of streams
        writeInt(width * height * 3); // Suggested buffer size
        writeInt(width);
        writeInt(height);
        writeInt(0); // Reserved
        writeInt(0);
        writeInt(0);
        writeInt(0);
        
        // strl LIST (stream list)
        writeString("LIST");
        long strlSizePos = file.getFilePointer();
        writeInt(0); // Size placeholder
        writeString("strl");
        
        // strh chunk (stream header)
        writeString("strh");
        writeInt(56);
        writeString("vids"); // Stream type
        writeString("DIB "); // Handler (uncompressed)
        writeInt(0); // Flags
        writeShort(0); // Priority
        writeShort(0); // Language
        writeInt(0); // Initial frames
        writeInt(1); // Scale
        writeInt(fps); // Rate
        writeInt(0); // Start
        writeInt(0); // Length placeholder
        writeInt(width * height * 3); // Suggested buffer size
        writeInt(10000); // Quality
        writeInt(0); // Sample size
        writeShort(0); // Left
        writeShort(0); // Top
        writeShort((short) width);
        writeShort((short) height);
        
        // strf chunk (stream format - BITMAPINFOHEADER)
        writeString("strf");
        writeInt(40);
        writeInt(40); // Size of BITMAPINFOHEADER
        writeInt(width);
        writeInt(height);
        writeShort(1); // Planes
        writeShort(24); // Bit count (24-bit RGB)
        writeInt(0); // Compression (BI_RGB = uncompressed)
        writeInt(width * height * 3); // Image size
        writeInt(0); // X pixels per meter
        writeInt(0); // Y pixels per meter
        writeInt(0); // Colors used
        writeInt(0); // Important colors
        
        // Update strl size
        long strlEnd = file.getFilePointer();
        file.seek(strlSizePos);
        writeInt((int) (strlEnd - strlSizePos - 4));
        file.seek(strlEnd);
        
        // Update hdrl size
        long hdrlEnd = file.getFilePointer();
        file.seek(hdrlSizePos);
        writeInt((int) (hdrlEnd - hdrlSizePos - 4));
        file.seek(hdrlEnd);
        
        // movi LIST
        writeString("LIST");
        writeInt(0); // Size placeholder (updated at close)
        writeString("movi");
        moviStart = file.getFilePointer();
    }
    
    public synchronized void addFrame(BufferedImage image) throws IOException {
        if (image.getWidth() != width || image.getHeight() != height) {
            // Resize if needed
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            resized.getGraphics().drawImage(image, 0, 0, width, height, null);
            image = resized;
        }
        
        // Convert to BGR bottom-up (AVI format)
        byte[] pixels = new byte[width * height * 3];
        int idx = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                pixels[idx++] = (byte) (rgb & 0xFF);        // B
                pixels[idx++] = (byte) ((rgb >> 8) & 0xFF); // G
                pixels[idx++] = (byte) ((rgb >> 16) & 0xFF); // R
            }
        }
        
        // Pad to 4-byte boundary
        int padding = (4 - (pixels.length % 4)) % 4;
        
        // Write frame chunk
        long chunkStart = file.getFilePointer();
        writeString("00dc"); // Video chunk
        writeInt(pixels.length);
        file.write(pixels);
        for (int i = 0; i < padding; i++) {
            file.write(0);
        }
        
        // Add to index
        ByteBuffer indexEntry = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        indexEntry.put("00dc".getBytes());
        indexEntry.putInt(0x10); // AVIIF_KEYFRAME
        indexEntry.putInt((int) (chunkStart - moviStart));
        indexEntry.putInt(pixels.length);
        indexBuffer.write(indexEntry.array());
        
        frameCount++;
    }
    
    @Override
    public void close() throws IOException {
        // Update movi size
        long moviEnd = file.getFilePointer();
        file.seek(moviStart - 4);
        writeInt((int) (moviEnd - moviStart + 4));
        file.seek(moviEnd);
        
        // Write index
        byte[] indexData = indexBuffer.toByteArray();
        writeString("idx1");
        writeInt(indexData.length);
        file.write(indexData);
        
        // Update total file size
        long fileEnd = file.getFilePointer();
        file.seek(4);
        writeInt((int) (fileEnd - 8));
        
        // Update frame count in avih
        file.seek(48);
        writeInt(frameCount);
        
        // Update length in strh
        file.seek(140);
        writeInt(frameCount);
        
        file.close();
    }
    
    private void writeString(String s) throws IOException {
        file.write(s.getBytes("ASCII"));
    }
    
    private void writeInt(int value) throws IOException {
        file.write(value & 0xFF);
        file.write((value >> 8) & 0xFF);
        file.write((value >> 16) & 0xFF);
        file.write((value >> 24) & 0xFF);
    }
    
    private void writeShort(int value) throws IOException {
        file.write(value & 0xFF);
        file.write((value >> 8) & 0xFF);
    }
    
    public int getFrameCount() {
        return frameCount;
    }
}
