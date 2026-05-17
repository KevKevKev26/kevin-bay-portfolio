import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Main class. Handles argument parsing and dispatching
 * to the appropriate processing methods based on the selected mode.
 */
@SuppressWarnings("PMD")
public class Cs214Project {

    /**
     * Private constructor to prevent instantiation.
     */
    private Cs214Project() {
    }

    /**
     * Main method.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the application logic based on provided arguments.
     *
     * @param args the command-line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void run(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Invalid number of arguments.");
        }

        boolean analysisMode = args.length >= 3 && args[2].equals("-a");
        boolean similarityMode = args.length >= 3 && args[2].equals("-u");
        boolean predictionMode = args.length >= 3 && args[2].equals("-p");
        boolean recommendationMode = args.length >= 3 && args[2].equals("-r");
        boolean playlistMode = args.length >= 3 && args[2].equals("-s");
        
        if (args.length >= 3 && !analysisMode && !similarityMode && !predictionMode && !recommendationMode && !playlistMode) {
            throw new IllegalArgumentException("Unsupported argument '" + args[2] + "'.");
        }
        if (!recommendationMode && !playlistMode && args.length > 3) {
            throw new IllegalArgumentException("Invalid number of arguments.");
        }

        String inputPath = args[0];
        String outputPath = args[1];

        List<Rating> ratings = readFile(inputPath);

        if (analysisMode) {
            processUserAnalysis(ratings, outputPath);
        } else if (similarityMode) {
            processSongSimilarity(ratings, outputPath);
        } else if (predictionMode) {
            processUserPredictions(ratings, outputPath);
        } else if (recommendationMode) {
            List<String> userSelections = new ArrayList<>();
            for (int i = 3; i < args.length; i++) {
                userSelections.add(args[i]);
            }
            if (userSelections.isEmpty()) {
                throw new IllegalArgumentException("Must select at least one song for recommendations.");
            }
            Set<String> uniqueSelections = new HashSet<>(userSelections);
            if (uniqueSelections.size() < userSelections.size()) {
                throw new IllegalArgumentException("Duplicate user selections found.");
            }
            processRecommendations(ratings, outputPath, userSelections);
        } else if (playlistMode) {
            if (args.length < 5) {
                throw new IllegalArgumentException("Fewer than 5 arguments present for playlist generation");
            }
            int k;
            try {
                k = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("K must be an integer.");
            }
            if (k < 1) {
                throw new IllegalArgumentException("K must be greater than or equal to 1");
            }
            List<String> userSelections = new ArrayList<>();
            for (int i = 4; i < args.length; i++) {
                userSelections.add(args[i]);
            }
            if (userSelections.size() < k) {
                throw new IllegalArgumentException("Fewer user-provided songs than K");
            }
            Set<String> uniqueSelections = new HashSet<>(userSelections);
            if (uniqueSelections.size() < userSelections.size()) {
                throw new IllegalArgumentException("Duplicate song titles in input");
            }
            PlaylistGenerator.generate(ratings, outputPath, k, userSelections);
        } else {
            processSongStats(ratings, outputPath);
        }
    }

    private static List<Rating> readFile(String inputPath) throws FileNotFoundException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file does not exist: " + inputPath);
        }

        List<Rating> ratings = new ArrayList<>();
        try (Scanner scanner = new Scanner(inputFile)) {
            if (!scanner.hasNextLine()) {
                throw new IllegalArgumentException("Input file is empty.");
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid data format: Each line must have 3 fields (song, user, rating).");
                }
                String song = parts[0].trim();
                String user = parts[1].trim();
                int rating;
                try {
                    rating = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Rating must be an integer.");
                }
                if (rating < 1 || rating > 5) {
                    throw new IllegalArgumentException("Rating must be between 1 and 5.");
                }
                ratings.add(new Rating(song, user, rating));
            }
        }
        return ratings;
    }

    private static void processSongStats(List<Rating> ratings, String outputPath) throws FileNotFoundException {
        Map<String, List<Integer>> songRatings = new HashMap<>();
        for (Rating r : ratings) {
            songRatings.computeIfAbsent(r.getSong(), k -> new ArrayList<>()).add(r.getRating());
        }

        List<String> sortedSongs = new ArrayList<>(songRatings.keySet());
        Collections.sort(sortedSongs);

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("song,number of ratings,mean,standard deviation");
            for (String song : sortedSongs) {
                List<Integer> songScores = songRatings.get(song);
                int numRatings = songScores.size();
                double mean = songScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                double stdDev = 0.0;
                if (numRatings > 0) {
                    double sumSqDiff = songScores.stream().mapToDouble(score -> Math.pow(score - mean, 2)).sum();
                    stdDev = Math.sqrt(sumSqDiff / numRatings);
                }
                writer.println(song + "," + numRatings + "," + mean + "," + stdDev);
            }
        }
    }

    private static void processUserAnalysis(List<Rating> ratings, String outputPath) throws FileNotFoundException {
        Map<String, List<Rating>> userRatings = ratings.stream()
                .collect(Collectors.groupingBy(Rating::getUser));

        Set<String> uncooperativeUsers = userRatings.entrySet().stream()
                .filter(entry -> entry.getValue().stream().map(Rating::getRating).distinct().count() <= 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Rating> cooperativeRatings = ratings.stream()
                .filter(r -> !uncooperativeUsers.contains(r.getUser()))
                .collect(Collectors.toList());

        Set<String> allSongs = cooperativeRatings.stream()
                .map(Rating::getSong)
                .collect(Collectors.toSet());
        List<String> sortedSongs = new ArrayList<>(allSongs);
        Collections.sort(sortedSongs);

        List<String> allUsers = ratings.stream().map(Rating::getUser).distinct().sorted().collect(Collectors.toList());
        allUsers.removeAll(uncooperativeUsers);
        
        Map<String, Map<String, Double>> userProfiles = new HashMap<>();
        for(String user : allUsers) {
            Map<String, Double> songRatings = new HashMap<>();
            for(String song : sortedSongs) {
                songRatings.put(song, Double.NaN);
            }
            userProfiles.put(user, songRatings);
        }

        for (Rating r : cooperativeRatings) {
            userProfiles.get(r.getUser()).put(r.getSong(), (double) r.getRating());
        }

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("username,song,rating");
            for (String user : allUsers) {
                for (String song : sortedSongs) {
                    Double rating = userProfiles.get(user).get(song);
                    writer.printf("%s,%s,%s%n", user, song, rating.isNaN() ? "NaN" : String.format("%.0f", rating));
                }
            }
        }
    }

    private static void processSongSimilarity(List<Rating> ratings, String outputPath) throws FileNotFoundException {
        Map<String, List<Rating>> userRatingsMap = ratings.stream()
                .collect(Collectors.groupingBy(Rating::getUser));

        List<String> cooperativeUsers = new ArrayList<>();
        Map<String, Map<String, Double>> normalizedUserRatings = new HashMap<>();

        for (Map.Entry<String, List<Rating>> entry : userRatingsMap.entrySet()) {
            String user = entry.getKey();
            List<Rating> userRatings = entry.getValue();

            long distinctRatings = userRatings.stream().map(Rating::getRating).distinct().count();
            if (distinctRatings > 1) {
                cooperativeUsers.add(user);

                double mean = userRatings.stream().mapToInt(Rating::getRating).average().orElse(0.0);
                double sumSqDiff = userRatings.stream().mapToDouble(r -> Math.pow(r.getRating() - mean, 2)).sum();
                double stdDev = Math.sqrt(sumSqDiff / userRatings.size());

                Map<String, Double> normalized = new HashMap<>();
                for (Rating r : userRatings) {
                    normalized.put(r.getSong(), (r.getRating() - mean) / stdDev);
                }
                normalizedUserRatings.put(user, normalized);
            }
        }

        if (cooperativeUsers.size() < 2) {
            throw new IllegalArgumentException("At least two cooperative users are required for song similarity.");
        }

        Set<String> allSongs = ratings.stream().map(Rating::getSong).collect(Collectors.toSet());
        
        List<String> sortedSongs = new ArrayList<>(allSongs);
        Collections.sort(sortedSongs);

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("name1,name2,similarity");
            for (int i = 0; i < sortedSongs.size(); i++) {
                for (int j = i + 1; j < sortedSongs.size(); j++) {
                    String song1 = sortedSongs.get(i);
                    String song2 = sortedSongs.get(j);

                    double sumSqDiff = 0.0;
                    boolean hasCommonUser = false;

                    for (String user : cooperativeUsers) {
                        Map<String, Double> userNorms = normalizedUserRatings.get(user);
                        if (userNorms.containsKey(song1) && userNorms.containsKey(song2)) {
                            double diff = userNorms.get(song1) - userNorms.get(song2);
                            sumSqDiff += diff * diff;
                            hasCommonUser = true;
                        }
                    }

                    double similarity = hasCommonUser ? Math.sqrt(sumSqDiff) : Double.NaN;
                    writer.println(song1 + "," + song2 + "," + similarity);
                }
            }
        }
    }

    private static void processUserPredictions(List<Rating> ratings, String outputPath) throws FileNotFoundException {
        Map<String, Map<String, Integer>> userRawRatings = new HashMap<>();
        Map<String, Map<String, Integer>> songRawRatings = new HashMap<>();
        Set<String> allSongs = new TreeSet<>();
        Set<String> allUsers = new TreeSet<>();

        for (Rating r : ratings) {
            userRawRatings.computeIfAbsent(r.getUser(), k -> new HashMap<>()).put(r.getSong(), r.getRating());
            songRawRatings.computeIfAbsent(r.getSong(), k -> new HashMap<>()).put(r.getUser(), r.getRating());
            allSongs.add(r.getSong());
            allUsers.add(r.getUser());
        }

        Map<String, Double> userMeansForAll = new HashMap<>();
        Map<String, Integer> userNumRatings = new HashMap<>();
        for (String user : allUsers) {
            Map<String, Integer> uRatings = userRawRatings.getOrDefault(user, Collections.emptyMap());
            double mean = uRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            userMeansForAll.put(user, mean);
            userNumRatings.put(user, uRatings.size());
        }

        Map<String, Double> songMeans = new HashMap<>();
        Map<String, Integer> songNumRatings = new HashMap<>();
        for (String song : allSongs) {
            Map<String, Integer> sRatings = songRawRatings.getOrDefault(song, Collections.emptyMap());
            double mean = sRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            songMeans.put(song, mean);
            songNumRatings.put(song, sRatings.size());
        }

        List<String> cooperativeUsers = new ArrayList<>();
        Map<String, Map<String, Double>> normalizedUserRatings = new HashMap<>();
        Map<String, Double> userMeans = new HashMap<>();
        Map<String, Double> userStdDevs = new HashMap<>();

        for (String user : allUsers) {
            Map<String, Integer> uRatings = userRawRatings.getOrDefault(user, Collections.emptyMap());
            long distinctRatings = uRatings.values().stream().distinct().count();
            if (distinctRatings > 1) {
                cooperativeUsers.add(user);
                double mean = uRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
                double sumSqDiff = uRatings.values().stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum();
                double stdDev = Math.sqrt(sumSqDiff / uRatings.size());

                userMeans.put(user, mean);
                userStdDevs.put(user, stdDev);

                Map<String, Double> normalized = new HashMap<>();
                for (Map.Entry<String, Integer> entry : uRatings.entrySet()) {
                    normalized.put(entry.getKey(), (entry.getValue() - mean) / stdDev);
                }
                normalizedUserRatings.put(user, normalized);
            }
        }

        if (cooperativeUsers.size() < 2) {
            throw new IllegalArgumentException("At least two cooperative users are required.");
        }

        Map<String, Map<String, Double>> userSimilarities = new HashMap<>();
        for (String u1 : cooperativeUsers) {
            userSimilarities.put(u1, new HashMap<>());
        }

        for (int i = 0; i < cooperativeUsers.size(); i++) {
            for (int j = i + 1; j < cooperativeUsers.size(); j++) {
                String u1 = cooperativeUsers.get(i);
                String u2 = cooperativeUsers.get(j);

                double sumSqDiff = 0.0;
                boolean hasCommonSong = false;

                Map<String, Double> norms1 = normalizedUserRatings.get(u1);
                Map<String, Double> norms2 = normalizedUserRatings.get(u2);

                for (String song : norms1.keySet()) {
                    if (norms2.containsKey(song)) {
                        double diff = norms1.get(song) - norms2.get(song);
                        sumSqDiff += diff * diff;
                        hasCommonSong = true;
                    }
                }

                if (hasCommonSong) {
                    double sim = Math.sqrt(sumSqDiff);
                    userSimilarities.get(u1).put(u2, sim);
                    userSimilarities.get(u2).put(u1, sim);
                }
            }
        }

        List<String> outputLines = new ArrayList<>();
        for (String song : allSongs) {
            for (String user : allUsers) {
                if (!userRawRatings.getOrDefault(user, Collections.emptyMap()).containsKey(song)) {
                    Integer predictedRaw = null;

                    if (cooperativeUsers.contains(user)) {
                        List<String> similarUsers = new ArrayList<>();
                        for (Map.Entry<String, Double> entry : userSimilarities.get(user).entrySet()) {
                            String otherUser = entry.getKey();
                            if (userRawRatings.get(otherUser).containsKey(song)) {
                                similarUsers.add(otherUser);
                            }
                        }

                        if (!similarUsers.isEmpty()) {
                            similarUsers.sort((a, b) -> {
                                int cmp = Double.compare(userSimilarities.get(user).get(a), userSimilarities.get(user).get(b));
                                return cmp == 0 ? a.compareTo(b) : cmp;
                            });

                            String bestUser = similarUsers.get(0);
                            double otherNorm = normalizedUserRatings.get(bestUser).get(song);
                            double mean = userMeans.get(user);
                            double stdDev = userStdDevs.get(user);

                            long rounded = Math.round(otherNorm * stdDev + mean);
                            if (rounded < 1) rounded = 1;
                            if (rounded > 5) rounded = 5;
                            predictedRaw = (int) rounded;
                        }
                    }

                    if (predictedRaw == null) {
                        double sMean = songMeans.get(song);
                        int sNum = songNumRatings.get(song);
                        double uMean = userMeansForAll.get(user);
                        int uNum = userNumRatings.get(user);

                        long weighted = Math.round((sMean * sNum + uMean * uNum) / (double) (sNum + uNum));
                        predictedRaw = (int) weighted;
                    }

                    String ratingStr = String.valueOf(predictedRaw);
                    outputLines.add(song + "," + user + "," + ratingStr);
                }
            }
        }

        if (outputLines.isEmpty()) {
            throw new IllegalArgumentException("No predictions to be made.");
        }

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("song,user,predicted rating");
            for (String line : outputLines) {
                writer.println(line);
            }
        }
    }

    private static void processRecommendations(List<Rating> ratings, String outputPath, List<String> userSelections) throws FileNotFoundException {
        Map<String, Map<String, Integer>> userRawRatings = new HashMap<>();
        Map<String, Map<String, Integer>> songRawRatings = new HashMap<>();
        Set<String> allSongs = new TreeSet<>();
        Set<String> allUsers = new TreeSet<>();

        for (Rating r : ratings) {
            userRawRatings.computeIfAbsent(r.getUser(), k -> new HashMap<>()).put(r.getSong(), r.getRating());
            songRawRatings.computeIfAbsent(r.getSong(), k -> new HashMap<>()).put(r.getUser(), r.getRating());
            allSongs.add(r.getSong());
            allUsers.add(r.getUser());
        }

        for (String selection : userSelections) {
            if (!allSongs.contains(selection)) {
                throw new IllegalArgumentException("User selection not found in input data.");
            }
        }

        Map<String, Double> userMeans = new HashMap<>();
        Map<String, Integer> userNumRatings = new HashMap<>();
        for (String user : allUsers) {
            Map<String, Integer> uRatings = userRawRatings.getOrDefault(user, Collections.emptyMap());
            double mean = uRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            userMeans.put(user, mean);
            userNumRatings.put(user, uRatings.size());
        }

        Map<String, Double> songMeans = new HashMap<>();
        Map<String, Integer> songNumRatings = new HashMap<>();
        for (String song : allSongs) {
            Map<String, Integer> sRatings = songRawRatings.getOrDefault(song, Collections.emptyMap());
            double mean = sRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            songMeans.put(song, mean);
            songNumRatings.put(song, sRatings.size());
        }

        List<String> cooperativeUsers = new ArrayList<>();
        Map<String, Map<String, Double>> normalizedUserRatings = new HashMap<>();
        Map<String, Double> userStdDevs = new HashMap<>();

        for (String user : allUsers) {
            Map<String, Integer> uRatings = userRawRatings.getOrDefault(user, Collections.emptyMap());
            long distinctRatings = uRatings.values().stream().distinct().count();
            if (distinctRatings > 1) {
                cooperativeUsers.add(user);
                double mean = userMeans.get(user);
                double sumSqDiff = uRatings.values().stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum();
                double stdDev = Math.sqrt(sumSqDiff / uRatings.size());
                userStdDevs.put(user, stdDev);

                Map<String, Double> normalized = new HashMap<>();
                for (Map.Entry<String, Integer> entry : uRatings.entrySet()) {
                    normalized.put(entry.getKey(), (entry.getValue() - mean) / stdDev);
                }
                normalizedUserRatings.put(user, normalized);
            }
        }

        if (cooperativeUsers.size() < 2) {
            throw new IllegalArgumentException("At least two cooperative users are required.");
        }

        Map<String, Map<String, Double>> userSimilarities = new HashMap<>();
        for (String u1 : cooperativeUsers) {
            userSimilarities.put(u1, new HashMap<>());
        }

        for (int i = 0; i < cooperativeUsers.size(); i++) {
            for (int j = i + 1; j < cooperativeUsers.size(); j++) {
                String u1 = cooperativeUsers.get(i);
                String u2 = cooperativeUsers.get(j);

                double sumSqDiff = 0.0;
                boolean hasCommonSong = false;

                Map<String, Double> norms1 = normalizedUserRatings.get(u1);
                Map<String, Double> norms2 = normalizedUserRatings.get(u2);

                for (String song : norms1.keySet()) {
                    if (norms2.containsKey(song)) {
                        double diff = norms1.get(song) - norms2.get(song);
                        sumSqDiff += diff * diff;
                        hasCommonSong = true;
                    }
                }

                if (hasCommonSong) {
                    double sim = Math.sqrt(sumSqDiff);
                    userSimilarities.get(u1).put(u2, sim);
                    userSimilarities.get(u2).put(u1, sim);
                }
            }
        }

        Map<String, Map<String, Integer>> filledMatrix = new HashMap<>();
        for (String song : allSongs) {
            filledMatrix.put(song, new HashMap<>());
        }

        for (String song : allSongs) {
            for (String user : allUsers) {
                if (userRawRatings.getOrDefault(user, Collections.emptyMap()).containsKey(song)) {
                    filledMatrix.get(song).put(user, userRawRatings.get(user).get(song));
                } else {
                    Integer predictedRaw = null;
                    if (cooperativeUsers.contains(user)) {
                        List<String> similarUsers = new ArrayList<>();
                        for (Map.Entry<String, Double> entry : userSimilarities.get(user).entrySet()) {
                            String otherUser = entry.getKey();
                            if (userRawRatings.get(otherUser).containsKey(song)) {
                                similarUsers.add(otherUser);
                            }
                        }

                        if (!similarUsers.isEmpty()) {
                            similarUsers.sort((a, b) -> {
                                int cmp = Double.compare(userSimilarities.get(user).get(a), userSimilarities.get(user).get(b));
                                return cmp == 0 ? a.compareTo(b) : cmp;
                            });

                            String bestUser = similarUsers.get(0);
                            double otherNorm = normalizedUserRatings.get(bestUser).get(song);
                            double mean = userMeans.get(user);
                            double stdDev = userStdDevs.get(user);

                            long rounded = Math.round(otherNorm * stdDev + mean);
                            if (rounded < 1) rounded = 1;
                            if (rounded > 5) rounded = 5;
                            predictedRaw = (int) rounded;
                        }
                    }

                    if (predictedRaw == null) {
                        double sMean = songMeans.get(song);
                        int sNum = songNumRatings.get(song);
                        double uMean = userMeans.get(user);
                        int uNum = userNumRatings.get(user);

                        long weighted = Math.round((sMean * sNum + uMean * uNum) / (double) (sNum + uNum));
                        predictedRaw = (int) weighted;
                    }
                    
                    filledMatrix.get(song).put(user, predictedRaw);
                }
            }
        }

        Set<String> songsToRemove = new HashSet<>();
        for (String song : allSongs) {
            long distinct = filledMatrix.get(song).values().stream().distinct().count();
            if (distinct <= 1) {
                songsToRemove.add(song);
            }
        }
        allSongs.removeAll(songsToRemove);

        if (allSongs.size() < userSelections.size() + 1) {
            throw new IllegalArgumentException("No songs to recommend. Songs may have been removed. Try with a larger file or fewer selections.");
        }
        for (String selection : userSelections) {
            if (songsToRemove.contains(selection)) {
                throw new IllegalArgumentException("Selected songs must have more than one distinct rating.");
            }
        }

        Map<String, Map<String, Double>> normalizedSongs = new HashMap<>();
        for (String song : allSongs) {
            Map<String, Integer> fRatings = filledMatrix.get(song);
            double mean = fRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double sumSqDiff = fRatings.values().stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum();
            double stdDev = Math.sqrt(sumSqDiff / fRatings.size());
            
            Map<String, Double> norms = new HashMap<>();
            for (Map.Entry<String, Integer> entry : fRatings.entrySet()) {
                double norm = stdDev == 0 ? 0.0 : (entry.getValue() - mean) / stdDev;
                norms.put(entry.getKey(), norm);
            }
            normalizedSongs.put(song, norms);
        }

        List<String> allUsersSorted = new ArrayList<>(allUsers);
        Collections.sort(allUsersSorted);
        List<String> allSongsSorted = new ArrayList<>(allSongs);
        Collections.sort(allSongsSorted);

        List<List<Double>> centroids = new ArrayList<>();
        for (String selection : userSelections) {
            List<Double> centroid = new ArrayList<>();
            for (String user : allUsersSorted) {
                centroid.add(normalizedSongs.get(selection).get(user));
            }
            centroids.add(centroid);
        }

        List<List<String>> clusters = new ArrayList<>();
        for (int iter = 0; iter < 10; iter++) {
            clusters = new ArrayList<>();
            for (int c = 0; c < userSelections.size(); c++) {
                clusters.add(new ArrayList<>());
            }

            for (String song : allSongsSorted) {
                List<Double> songVec = new ArrayList<>();
                for (String user : allUsersSorted) {
                    songVec.add(normalizedSongs.get(song).get(user));
                }

                double minDist = Double.MAX_VALUE;
                int bestCluster = -1;
                for (int c = 0; c < centroids.size(); c++) {
                    double dist = 0.0;
                    List<Double> centroid = centroids.get(c);
                    for (int i = 0; i < songVec.size(); i++) {
                        double diff = songVec.get(i) - centroid.get(i);
                        dist += diff * diff;
                    }
                    dist = Math.sqrt(dist);

                    if (dist < minDist) {
                        minDist = dist;
                        bestCluster = c;
                    }
                }
                clusters.get(bestCluster).add(song);
            }

            for (int c = 0; c < centroids.size(); c++) {
                List<String> clusterSongs = clusters.get(c);
                if (clusterSongs.isEmpty()) continue;

                List<Double> newCentroid = new ArrayList<>(Collections.nCopies(allUsersSorted.size(), 0.0));
                for (String song : clusterSongs) {
                    for (int i = 0; i < allUsersSorted.size(); i++) {
                        String user = allUsersSorted.get(i);
                        newCentroid.set(i, newCentroid.get(i) + normalizedSongs.get(song).get(user));
                    }
                }
                for (int i = 0; i < newCentroid.size(); i++) {
                    newCentroid.set(i, newCentroid.get(i) / clusterSongs.size());
                }
                centroids.set(c, newCentroid);
            }
        }

        List<String> outputLines = new ArrayList<>();
        boolean hasAnyValidRecommendation = false;
        for (int c = 0; c < userSelections.size(); c++) {
            String userChoice = userSelections.get(c);
            List<String> clusterSongs = clusters.get(c);
            
            boolean clusterHasValid = false;
            for (String song : clusterSongs) {
                if (!userSelections.contains(song)) {
                    outputLines.add(userChoice + "," + song);
                    clusterHasValid = true;
                }
            }
            if (clusterHasValid) {
                hasAnyValidRecommendation = true;
            }
        }

        if (!hasAnyValidRecommendation) {
            throw new IllegalArgumentException("All resulting clusters are empty or only include user selections.");
        }

        outputLines.sort((a, b) -> {
            String[] partsA = a.split(",");
            String[] partsB = b.split(",");
            int cmp = partsA[0].compareTo(partsB[0]);
            if (cmp != 0) return cmp;
            return partsA[1].compareTo(partsB[1]);
        });

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("user choice,recommendation");
            for (String line : outputLines) {
                writer.println(line);
            }
        }
    }
}