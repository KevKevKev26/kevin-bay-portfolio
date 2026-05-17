import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("PMD")
public class PlaylistGenerator {

    public static void generate(List<Rating> ratings, String outputPath, int k, List<String> userSelections) throws FileNotFoundException {
        Map<String, Map<String, Integer>> userRawRatings = new HashMap<>();
        Map<String, Map<String, Integer>> songRawRatings = new HashMap<>();
        Set<String> allSongs = new TreeSet<>();
        Set<String> allUsers = new TreeSet<>();

        for (Rating r : ratings) {
            userRawRatings.computeIfAbsent(r.getUser(), key -> new HashMap<>()).put(r.getSong(), r.getRating());
            songRawRatings.computeIfAbsent(r.getSong(), key -> new HashMap<>()).put(r.getUser(), r.getRating());
            allSongs.add(r.getSong());
            allUsers.add(r.getUser());
        }

        for (String selection : userSelections) {
            if (!allSongs.contains(selection)) {
                throw new IllegalArgumentException("User-provided song not found");
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
            throw new IllegalArgumentException("at least two cooperative users are required");
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

        if (allSongs.size() < k + 1) {
            throw new IllegalArgumentException("Pre-processing leaves fewer than K+1 songs");
        }
        for (String selection : userSelections) {
            if (songsToRemove.contains(selection)) {
                throw new IllegalArgumentException("User-provided song removed during pre-processing");
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
        for (int i = 0; i < k; i++) {
            String selection = userSelections.get(i);
            List<Double> centroid = new ArrayList<>();
            for (String user : allUsersSorted) {
                centroid.add(normalizedSongs.get(selection).get(user));
            }
            centroids.add(centroid);
        }

        List<List<String>> clusters = new ArrayList<>();
        for (int iter = 0; iter < 10; iter++) {
            clusters = new ArrayList<>();
            for (int c = 0; c < k; c++) {
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

        Map<String, Double> candidateDistances = new HashMap<>();

        for (String s : userSelections) {
            List<String> targetCluster = null;
            for (List<String> cluster : clusters) {
                if (cluster.contains(s)) {
                    targetCluster = cluster;
                    break;
                }
            }

            if (targetCluster != null) {
                List<Double> sVec = new ArrayList<>();
                for (String user : allUsersSorted) {
                    sVec.add(normalizedSongs.get(s).get(user));
                }

                for (String t : targetCluster) {
                    if (!t.equals(s) && !userSelections.contains(t)) {
                        List<Double> tVec = new ArrayList<>();
                        for (String user : allUsersSorted) {
                            tVec.add(normalizedSongs.get(t).get(user));
                        }

                        double distSq = 0.0;
                        for (int i = 0; i < sVec.size(); i++) {
                            double diff = sVec.get(i) - tVec.get(i);
                            distSq += diff * diff;
                        }
                        double dist = Math.sqrt(distSq);
                        double d = dist == 0.0 ? Double.MAX_VALUE : 1.0 / dist;

                        candidateDistances.put(t, Math.max(candidateDistances.getOrDefault(t, -1.0), d));
                    }
                }
            }
        }

        List<Map.Entry<String, Double>> candidateList = new ArrayList<>(candidateDistances.entrySet());
        candidateList.sort((e1, e2) -> {
            int cmp = Double.compare(e2.getValue(), e1.getValue());
            if (cmp != 0) return cmp;
            return e1.getKey().compareTo(e2.getKey());
        });

        List<String> playlist = new ArrayList<>();
        for (int i = 0; i < Math.min(20, candidateList.size()); i++) {
            playlist.add(candidateList.get(i).getKey());
        }

        try (PrintWriter writer = new PrintWriter(outputPath)) {
            for (String song : playlist) {
                writer.println(song);
            }
        }
    }
}