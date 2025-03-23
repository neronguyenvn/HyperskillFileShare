import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FileData {
    private String originalName;
    private byte[] contents;
    private String mimeType;

    private FileData() { }

    public static FileData of(String pathString) throws IOException {
        Path path = Path.of(pathString);
        String mime = Files.probeContentType(path);
        byte[] bytes = Files.readAllBytes(path);
        return new FileData()
                .setOriginalName(path.getFileName().toString())
                .setContents(bytes)
                .setMimeType(mime == null ? "application/octet-stream" : mime);
    }

    public String getOriginalName() {
        return originalName;
    }

    public FileData setOriginalName(String originalName) {
        this.originalName = originalName;
        return this;
    }

    public byte[] getContents() {
        return contents;
    }

    private FileData setContents(byte[] contents) {
        this.contents = contents;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileData setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }
}
