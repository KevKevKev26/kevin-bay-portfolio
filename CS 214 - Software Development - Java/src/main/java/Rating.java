/**
 * Represents a rating given by a user to a song.
 */
public class Rating {
    private final String song;
    private final String user;
    private final int rating;

    /**
     * Constructs a new Rating.
     *
     * @param song   the name of the song
     * @param user   the name of the user
     * @param rating the rating value
     */
    public Rating(String song, String user, int rating) {
        this.song = song;
        this.user = user;
        this.rating = rating;
    }

    /**
     * Gets the name of the song.
     *
     * @return the song name
     */
    public String getSong() {
        return song;
    }

    /**
     * Gets the name of the user.
     *
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the rating value.
     *
     * @return the rating
     */
    public int getRating() {
        return rating;
    }
}