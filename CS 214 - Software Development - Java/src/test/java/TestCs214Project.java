import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestCs214Project {

    @TempDir
    Path tempDir;

    private void createInputFile(String fileName, String... lines) throws IOException {
        Files.write(tempDir.resolve(fileName), Arrays.asList(lines));
    }

    private List<String> readOutputFile(String fileName) throws IOException {
        return Files.readAllLines(tempDir.resolve(fileName));
    }

    private void createRockPopFile(Path path) throws IOException {
        Files.write(path, Arrays.asList(
            "Bohemian Rhapsody,rock_fan1,5",
            "Bohemian Rhapsody,rock_fan2,4",
            "Bohemian Rhapsody,rock_fan3,5",
            "Bohemian Rhapsody,pop_fan1,2",
            "Bohemian Rhapsody,pop_fan2,1",
            "Stairway to Heaven,rock_fan1,4",
            "Stairway to Heaven,rock_fan2,5",
            "Stairway to Heaven,rock_fan3,4",
            "Stairway to Heaven,pop_fan1,1",
            "Stairway to Heaven,pop_fan2,2",
            "Sweet Child O' Mine,rock_fan1,5",
            "Sweet Child O' Mine,rock_fan2,4",
            "Sweet Child O' Mine,rock_fan3,3",
            "Sweet Child O' Mine,pop_fan1,1",
            "Sweet Child O' Mine,pop_fan2,1",
            "November Rain,rock_fan1,4",
            "November Rain,rock_fan2,5",
            "November Rain,rock_fan3,5",
            "November Rain,pop_fan1,2",
            "November Rain,pop_fan2,3",
            "Hotel California,rock_fan1,3",
            "Hotel California,rock_fan2,4",
            "Hotel California,rock_fan3,2",
            "Hotel California,pop_fan1,1",
            "Hotel California,pop_fan2,1",
            "Bad Romance,pop_fan1,5",
            "Bad Romance,pop_fan2,4",
            "Bad Romance,rock_fan1,1",
            "Bad Romance,rock_fan2,2",
            "Poker Face,pop_fan1,4",
            "Poker Face,pop_fan2,5",
            "Poker Face,rock_fan1,2",
            "Poker Face,rock_fan2,1",
            "Shallow,pop_fan1,3",
            "Shallow,pop_fan2,4",
            "Shallow,rock_fan1,1",
            "Shallow,rock_fan2,1"
        ));
    }

    @Test
    public void testPlaylist_fewerThan5Args() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        createInputFile("in.csv", "songA,user1,5");
        String[] args = {inputFile.toString(), "out.csv", "-s", "1"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("Fewer than 5 arguments present for playlist generation", e.getMessage());
    }

    @Test
    public void testPlaylist_kLessThan1() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        createInputFile("in.csv", "songA,user1,5");
        String[] args = {inputFile.toString(), "out.csv", "-s", "0", "songA"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("K must be greater than or equal to 1", e.getMessage());
    }

    @Test
    public void testPlaylist_fewerSongsThanK() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        createInputFile("in.csv", "songA,user1,5");
        String[] args = {inputFile.toString(), "out.csv", "-s", "2", "songA"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("Fewer user-provided songs than K", e.getMessage());
    }

    @Test
    public void testPlaylist_songNotFound() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        Path outputFile = tempDir.resolve("out.csv");
        createInputFile("in.csv", "songA,user1,5", "songB,user1,4");

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "1", "songC"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("User-provided song not found", e.getMessage());
    }

    @Test
    public void testPlaylist_duplicateSongs() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        createInputFile("in.csv", "songA,user1,5");
        String[] args = {inputFile.toString(), "out.csv", "-s", "1", "songA", "songA"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("Duplicate song titles in input", e.getMessage());
    }

    @Test
    public void testPlaylist_preProcessingRemovesTooMany() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        Path outputFile = tempDir.resolve("out.csv");
        createInputFile("in.csv",
                "songA,user1,5", "songA,user2,5",
                "songB,user1,4", "songB,user2,4",
                "songC,user1,3", "songC,user2,2"
        );

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "1", "songC"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("Pre-processing leaves fewer than K+1 songs", e.getMessage());
    }

    @Test
    public void testPlaylist_preProcessingRemovesSelection() throws IOException {
        Path inputFile = tempDir.resolve("in.csv");
        Path outputFile = tempDir.resolve("out.csv");
        createInputFile("in.csv",
                "songA,user1,5", "songA,user2,5",
                "songB,user1,3", "songB,user2,2",
                "songC,user1,1", "songC,user2,4"
        );

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "1", "songA"};
        Exception e = assertThrows(IllegalArgumentException.class, () -> Cs214Project.run(args));
        assertEquals("User-provided song removed during pre-processing", e.getMessage());
    }

    @Test
    public void testPlaylist_rockExample() throws IOException {
        Path inputFile = tempDir.resolve("rock.csv");
        Path outputFile = tempDir.resolve("playlist.csv");
        createInputFile("rock.csv",
                "Bohemian Rhapsody,user1,5", "Bohemian Rhapsody,user2,4", "Bohemian Rhapsody,user3,5",
                "Stairway to Heaven,user1,5", "Stairway to Heaven,user2,4", "Stairway to Heaven,user3,5",
                "Hotel California,user1,5", "Hotel California,user2,4", "Hotel California,user3,3",
                "November Rain,user1,1", "November Rain,user2,5", "November Rain,user3,1"
        );

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "1", "Bohemian Rhapsody"};
        Cs214Project.run(args);

        List<String> expected = Arrays.asList("Stairway to Heaven", "Hotel California", "November Rain");
        List<String> actual = readOutputFile("playlist.csv");
        assertEquals(expected, actual);
    }

    @Test
    public void testPlaylist_rockPopExample() throws IOException {
        Path inputFile = tempDir.resolve("rockpop.csv");
        Path outputFile = tempDir.resolve("rock_playlist.csv");
        createRockPopFile(inputFile);

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "2", "Bohemian Rhapsody", "Stairway to Heaven"};
        Cs214Project.run(args);

        List<String> expected = Arrays.asList("November Rain", "Hotel California", "Sweet Child O' Mine");
        List<String> actual = readOutputFile("rock_playlist.csv");
        assertEquals(expected, actual);
    }

    @Test
    public void testPlaylist_mixedExample() throws IOException {
        Path inputFile = tempDir.resolve("rockpop.csv");
        Path outputFile = tempDir.resolve("mixed_playlist.csv");
        createRockPopFile(inputFile);

        String[] args = {inputFile.toString(), outputFile.toString(), "-s", "2", "Bohemian Rhapsody", "Bad Romance"};
        Cs214Project.run(args);

        List<String> expected = Arrays.asList("Shallow", "Sweet Child O' Mine", "Poker Face", "Stairway to Heaven", "November Rain", "Hotel California");
        List<String> actual = readOutputFile("mixed_playlist.csv");
        assertEquals(expected, actual);
    }

}