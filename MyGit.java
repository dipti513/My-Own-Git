import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class MyGit {

    private static final Path MYGIT_PATH = Paths.get(".mygit");
    private static final Path OBJECTS_PATH = MYGIT_PATH.resolve("objects");
    private static final Path REFS_HEADS_PATH = MYGIT_PATH.resolve("refs/heads");
    private static final Path HEAD_FILE = MYGIT_PATH.resolve("HEAD");
    private static final Path INDEX_FILE = MYGIT_PATH.resolve("index");

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        try {
            switch (command) {
                case "init":
                    init();
                    break;
                case "add":
                    if (args.length < 2) {
                        System.out.println("Usage: mygit add <file>");
                        return;
                    }
                    add(args[1]);
                    break;
                case "commit":
                    if (args.length < 3 || !args[1].equals("-m")) {
                        System.out.println("Usage: mygit commit -m \"<message>\"");
                        return;
                    }
                    commit(args[2]);
                    break;
                case "log":
                    log();
                    break;
                default:
                    printUsage();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("MyGit - A simple Git-like VCS");
        System.out.println("Usage:");
        System.out.println("  mygit init - Initialize a new repository");
        System.out.println("  mygit add <file> - Stage a file for commit");
        System.out.println("  mygit commit -m \"<message>\" - Commit staged changes");
        System.out.println("  mygit log - Display commit history");
    }

    // =======================================================================
    // 1. INIT COMMAND
    // =======================================================================
    private static void init() throws IOException {
        if (Files.exists(MYGIT_PATH)) {
            System.out.println("Reinitializing existing MyGit repository in " + MYGIT_PATH.toAbsolutePath());
        } else {
            System.out.println("Initializing empty MyGit repository in " + MYGIT_PATH.toAbsolutePath());
        }

        Files.createDirectories(OBJECTS_PATH);
        Files.createDirectories(REFS_HEADS_PATH);
        Files.write(HEAD_FILE, "ref: refs/heads/master".getBytes());
        if (!Files.exists(INDEX_FILE)) {
            Files.createFile(INDEX_FILE);
        }
        System.out.println("Initialized empty MyGit repository.");
    }

    // =======================================================================
    // 2. ADD COMMAND
    // =======================================================================
    private static void add(String filePath) throws IOException, NoSuchAlgorithmException {
        Path file = Paths.get(filePath);
        if (!Files.exists(file)) {
            System.err.println("File not found: " + filePath);
            return;
        }

        byte[] content = Files.readAllBytes(file);
        String sha1 = hashObject("blob", content);

        // Update the index (staging area)
        Map<String, String> index = readIndex();
        index.put(filePath, sha1);
        writeIndex(index);

        System.out.println("Staged " + filePath);
    }


    // =======================================================================
    // 3. COMMIT COMMAND
    // =======================================================================
    private static void commit(String message) throws IOException, NoSuchAlgorithmException {
        Map<String, String> index = readIndex();
        if (index.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean.");
            return;
        }

        // 1. Create a tree object from the index
        String treeSha1 = writeTreeObject(index);

        // 2. Get the parent commit
        String parentCommitSha1 = getHeadCommitSha1();

        // 3. Create the commit object content
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("tree ").append(treeSha1).append("\n");
        if (parentCommitSha1 != null) {
            commitContent.append("parent ").append(parentCommitSha1).append("\n");
        }
        String author = System.getProperty("user.name") + " <" + System.getProperty("user.name") + "@example.com>";
        commitContent.append("author ").append(author).append(" ").append(System.currentTimeMillis() / 1000).append("\n");
        commitContent.append("committer ").append(author).append(" ").append(System.currentTimeMillis() / 1000).append("\n");
        commitContent.append("\n");
        commitContent.append(message).append("\n");

        // 4. Hash and save the commit object
        String commitSha1 = hashObject("commit", commitContent.toString().getBytes());

        // 5. Update the HEAD reference to point to the new commit
        updateHead(commitSha1);

        // 6. Clear the index after commit
        writeIndex(new HashMap<>());

        System.out.println("[" + getActiveBranch() + " " + commitSha1.substring(0, 7) + "] " + message);
    }
    
    // =======================================================================
    // 4. LOG COMMAND
    // =======================================================================
    private static void log() throws IOException {
        String commitSha1 = getHeadCommitSha1();
        if (commitSha1 == null) {
            System.out.println("No commits yet.");
            return;
        }

        while (commitSha1 != null) {
            byte[] commitObject = readObject(commitSha1);
            String commitData = new String(commitObject);
            
            String[] lines = commitData.split("\n");
            String tree = "";
            String parent = null;
            String author = "";
            String date = "";
            String message = "";
            
            boolean messageStarted = false;
            for(String line : lines) {
                if (messageStarted) {
                    message += line + "\n";
                } else if (line.startsWith("tree ")) {
                    tree = line.substring(5);
                } else if (line.startsWith("parent ")) {
                    parent = line.substring(7);
                } else if (line.startsWith("author ")) {
                    author = line.substring(7);
                    // Extract timestamp and format it
                    String[] parts = author.split(" ");
                    long timestamp = Long.parseLong(parts[parts.length - 1]);
                    date = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z").format(new Date(timestamp * 1000));
                    author = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length-1));

                } else if (line.isEmpty()) {
                    messageStarted = true;
                }
            }
            
            System.out.println("commit " + commitSha1);
            System.out.println("Author: " + author);
            System.out.println("Date:   " + date);
            System.out.println("\n    " + message.trim() + "\n");

            commitSha1 = parent;
        }
    }


    // =======================================================================
    // HELPER METHODS
    // =======================================================================

    /** Hashes content, writes it to the object store, and returns the SHA-1 hash. */
    private static String hashObject(String type, byte[] content) throws IOException, NoSuchAlgorithmException {
        // Prepend header "blob <size>\0" before hashing
        byte[] header = (type + " " + content.length + "\0").getBytes();
        ByteArrayOutputStream objStream = new ByteArrayOutputStream();
        objStream.write(header);
        objStream.write(content);
        byte[] objectData = objStream.toByteArray();

        // Calculate SHA-1
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        String sha1 = bytesToHex(digest.digest(objectData));

        // Store the compressed object
        Path objDir = OBJECTS_PATH.resolve(sha1.substring(0, 2));
        Files.createDirectories(objDir);
        Path objFile = objDir.resolve(sha1.substring(2));

        if (!Files.exists(objFile)) {
             try (FileOutputStream fos = new FileOutputStream(objFile.toFile());
                 DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
                dos.write(objectData);
            }
        }
        return sha1;
    }
    
    /** Reads a compressed object from the object store. */
    private static byte[] readObject(String sha1) throws IOException {
        Path objFile = OBJECTS_PATH.resolve(sha1.substring(0, 2)).resolve(sha1.substring(2));
        try (FileInputStream fis = new FileInputStream(objFile.toFile());
             InflaterInputStream iis = new InflaterInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            // Skip the header (e.g., "blob 12\0") to get raw content
            byte[] objectData = baos.toByteArray();
            int nullByteIndex = -1;
            for(int i = 0; i < objectData.length; i++) {
                if (objectData[i] == 0) {
                    nullByteIndex = i;
                    break;
                }
            }
            return Arrays.copyOfRange(objectData, nullByteIndex + 1, objectData.length);
        }
    }

    /** Writes a tree object from the index. */
    private static String writeTreeObject(Map<String, String> index) throws IOException, NoSuchAlgorithmException {
        // For simplicity, we create a flat tree. A full implementation would handle directories.
        StringBuilder treeContent = new StringBuilder();
        // Sort entries for consistent hashing
        List<String> sortedFiles = index.keySet().stream().sorted().collect(Collectors.toList());
        for (String filePath : sortedFiles) {
            String sha1 = index.get(filePath);
            // Mode '100644' for a normal file
            treeContent.append("100644 blob ").append(sha1).append("\t").append(filePath).append("\n");
        }
        return hashObject("tree", treeContent.toString().getBytes());
    }

    /** Reads the index file into a map. */
    private static Map<String, String> readIndex() throws IOException {
        Map<String, String> index = new HashMap<>();
        if (Files.exists(INDEX_FILE) && Files.size(INDEX_FILE) > 0) {
            List<String> lines = Files.readAllLines(INDEX_FILE);
            for (String line : lines) {
                String[] parts = line.split(" ");
                index.put(parts[1], parts[0]);
            }
        }
        return index;
    }

    /** Writes a map to the index file. */
    private static void writeIndex(Map<String, String> index) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : index.entrySet()) {
            sb.append(entry.getValue()).append(" ").append(entry.getKey()).append("\n");
        }
        Files.write(INDEX_FILE, sb.toString().getBytes());
    }
    
    /** Gets the SHA-1 of the commit pointed to by HEAD. */
    private static String getHeadCommitSha1() throws IOException {
        if (!Files.exists(HEAD_FILE)) return null;
        String headContent = new String(Files.readAllBytes(HEAD_FILE)).trim();
        if (headContent.startsWith("ref: ")) {
            Path refPath = MYGIT_PATH.resolve(headContent.substring(5));
            if (Files.exists(refPath)) {
                return new String(Files.readAllBytes(refPath)).trim();
            }
        } else { // Detached HEAD
            return headContent;
        }
        return null;
    }

    /** Updates the current branch HEAD to point to the new commit. */
    private static void updateHead(String commitSha1) throws IOException {
        String headContent = new String(Files.readAllBytes(HEAD_FILE)).trim();
         if (headContent.startsWith("ref: ")) {
            Path refPath = MYGIT_PATH.resolve(headContent.substring(5));
            Files.write(refPath, (commitSha1 + "\n").getBytes());
        }
    }
    
     /** Gets the name of the currently active branch from HEAD. */
    private static String getActiveBranch() throws IOException {
        if (!Files.exists(HEAD_FILE)) return "master";
        String headContent = new String(Files.readAllBytes(HEAD_FILE)).trim();
        if (headContent.startsWith("ref: refs/heads/")) {
            return headContent.substring(16);
        }
        return "detached";
    }

    /** Converts a byte array to a hex string. */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}